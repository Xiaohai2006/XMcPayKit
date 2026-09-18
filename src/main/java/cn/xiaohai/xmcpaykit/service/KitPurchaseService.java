package cn.xiaohai.xmcpaykit.service;

import cn.xiaohai.xmcpay.api.XMcPayAPI;
import cn.xiaohai.xmcpaykit.XMcPayKit;
import cn.xiaohai.xmcpaykit.config.KitConfig;
import cn.xiaohai.xmcpaykit.data.KitDataManager;
import cn.xiaohai.xmcpaykit.util.InventoryUtil;
import cn.xiaohai.xmcpaykit.util.MessageUtil;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * 礼包购买服务（业务层）。
 *
 * <p>把购买流程中重复的校验与支付逻辑收口到一处，菜单和命令都调用这里，
 * 避免两边规则不一致（例如只在一侧校验权限、次数或价格）。
 *
 * <p>典型用法：
 * <pre>
 * KitPurchaseService.Result result = purchaseService.validate(player, kitId);
 * if (purchaseService.notifyIfFailed(player, result)) {
 *     return;
 * }
 * purchaseService.openCashPayment(player, kitId);
 * </pre>
 */
public class KitPurchaseService {

    /** 现金支付 customData 前缀，XMcPay 支付成功事件靠它识别是哪个礼包 */
    public static final String CUSTOM_DATA_PREFIX = "kit:";

    /**
     * 购买校验/执行结果
     */
    public enum Result {
        /** 校验通过 */
        OK,
        /** 礼包不存在或配置未加载 */
        KIT_NOT_FOUND,
        /** 背包空间不足 */
        NO_INVENTORY_SPACE,
        /** 没有购买权限 */
        NO_PERMISSION,
        /** 已达购买次数上限 */
        LIMIT_REACHED,
        /** 未配置有效的现金价格 */
        INVALID_PRICE,
        /** 点券余额不足 */
        INSUFFICIENT_BALANCE,
        /** XMcPay 不可用 */
        PAYMENT_UNAVAILABLE
    }

    private final KitDataManager dataManager;
    private final KitRewardService rewardService;

    public KitPurchaseService() {
        this(new KitDataManager());
    }

    public KitPurchaseService(KitDataManager dataManager) {
        this.dataManager = dataManager;
        this.rewardService = new KitRewardService(dataManager);
    }

    public KitDataManager getDataManager() {
        return dataManager;
    }

    public KitRewardService getRewardService() {
        return rewardService;
    }

    // ============================================
    // 校验
    // ============================================

    /**
     * 购买前校验：礼包存在、背包空间、购买权限、购买次数上限。
     */
    public Result validate(Player player, String kitId) {
        if (player == null || kitId == null || !dataManager.hasKit(kitId)) {
            return Result.KIT_NOT_FOUND;
        }

        if (!InventoryUtil.hasEmptySlots(player, dataManager.getRequiredSlots(kitId))) {
            return Result.NO_INVENTORY_SPACE;
        }

        String permission = dataManager.getKitPermission(kitId);
        if (!permission.isEmpty() && !player.hasPermission(permission)) {
            return Result.NO_PERMISSION;
        }

        int limit = dataManager.getPurchaseLimit(kitId);
        if (limit > -1 && dataManager.getPurchaseCount(player.getUniqueId().toString(), kitId) >= limit) {
            return Result.LIMIT_REACHED;
        }

        return Result.OK;
    }

    // ============================================
    // 现金购买（微信 / 支付宝）
    // ============================================

    /**
     * 打开现金支付界面（内部会先校验）。
     *
     * @return 校验结果，{@link Result#OK} 表示已成功打开支付界面
     */
    public Result openCashPayment(Player player, String kitId) {
        Result result = validate(player, kitId);
        if (result != Result.OK) {
            return result;
        }

        if (dataManager.getKitMoney(kitId) <= 0) {
            return Result.INVALID_PRICE;
        }

        XMcPayAPI api = XMcPayKit.getApi();
        if (api == null) {
            return Result.PAYMENT_UNAVAILABLE;
        }

        api.openPayGui(player, dataManager.getKitName(kitId), dataManager.getKitMoney(kitId),
                customDataFor(kitId), commandsForApi(kitId));
        return Result.OK;
    }

    // ============================================
    // 点券购买
    // ============================================

    /**
     * 点券购买：余额足够时扣费并立即发放奖励。
     *
     * @return {@link Result#INSUFFICIENT_BALANCE} 表示余额不足，调用方可以引导玩家走现金支付
     */
    public Result purchaseWithPoints(Player player, String kitId) {
        Result result = validate(player, kitId);
        if (result != Result.OK) {
            return result;
        }

        double balance = dataManager.getBalance(player.getUniqueId().toString());
        double price = dataManager.getKitPrice(kitId);
        if (balance <= 0 || balance < price) {
            return Result.INSUFFICIENT_BALANCE;
        }

        dataManager.setBalance(player.getUniqueId().toString(), Math.max(0, balance - price));
        rewardService.grantKit(player, kitId, String.valueOf((int) price), "point");
        return Result.OK;
    }

    // ============================================
    // 提示
    // ============================================

    /**
     * 校验失败时给玩家发送提示。
     *
     * @return true 表示失败（调用方应中断流程）
     */
    public boolean notifyIfFailed(Player player, Result result) {
        if (result == Result.OK) {
            return false;
        }

        String message = messageFor(result);
        if (message != null) {
            MessageUtil.send(player, message);
        }
        return true;
    }

    /**
     * 结果对应的玩家提示（文案来自 lang.yml，可取默认值）
     */
    public String messageFor(Result result) {
        if (result == null) {
            return null;
        }

        switch (result) {
            case KIT_NOT_FOUND:
                return dataManager.getLangString(KitConfig.LANG_KIT_NOT_FOUND, "&c礼包配置未加载！");
            case NO_INVENTORY_SPACE:
                return dataManager.getLangString(KitConfig.LANG_PAY_PLAYER_INV, "&c你的背包空间不足！");
            case NO_PERMISSION:
                return dataManager.getLangString(KitConfig.LANG_KIT_NO_PERMISSION, "&c你没有权限购买这个礼包");
            case LIMIT_REACHED:
                return dataManager.getLangString(KitConfig.LANG_KIT_PURCHASE_LIMIT, "&c你已达到该礼包的购买次数上限");
            case INVALID_PRICE:
                return dataManager.getLangString(KitConfig.LANG_KIT_INVALID_PRICE, "&c该礼包未配置有效的现金价格！");
            case INSUFFICIENT_BALANCE:
                return dataManager.getLangString(KitConfig.LANG_PAY_BALANCE, "&c你的点券余额不足！");
            case PAYMENT_UNAVAILABLE:
                return dataManager.getLangString(KitConfig.LANG_PAY_UNAVAILABLE, "&c支付系统不可用！");
            default:
                return null;
        }
    }

    // ============================================
    // customData 工具
    // ============================================

    /**
     * 生成传给 XMcPay 的 customData
     */
    public static String customDataFor(String kitId) {
        return CUSTOM_DATA_PREFIX + kitId;
    }

    /**
     * 从 customData 解析礼包ID，不是礼包支付时返回 null
     */
    public static String kitIdFromCustomData(String customData) {
        if (customData == null || !customData.startsWith(CUSTOM_DATA_PREFIX)) {
            return null;
        }

        String kitId = customData.substring(CUSTOM_DATA_PREFIX.length()).trim();
        return kitId.isEmpty() ? null : kitId;
    }

    /**
     * 传给 XMcPay 的命令列表。
     *
     * <p>本插件的支付成功监听器注册成功时，命令由本插件执行，这里必须传空列表，
     * 否则命令会被执行两次；只有监听器不可用（XMcPay 版本过旧）时才回退给 XMcPay 执行。
     */
    private List<String> commandsForApi(String kitId) {
        return XMcPayKit.isPaymentListenerReady()
                ? Collections.<String>emptyList()
                : dataManager.getKitCommands(kitId);
    }
}
