package cn.xiaohai.xmcpaykit.listener;

import cn.xiaohai.xmcpay.Event.PaymentSuccessEvent;
import cn.xiaohai.xmcpaykit.XMcPayKit;
import cn.xiaohai.xmcpaykit.data.KitDataManager;
import cn.xiaohai.xmcpaykit.service.KitPurchaseService;
import cn.xiaohai.xmcpaykit.service.KitRewardService;
import cn.xiaohai.xmcpaykit.util.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 支付成功监听器。
 *
 * <p>现金支付（微信/支付宝）成功后由 XMcPay 触发 {@link PaymentSuccessEvent}，
 * 本插件在这里统一发放礼包奖励（物品 + 奖励命令 + 购买次数记录）。
 *
 * <p>注意：该监听器必须在 {@link XMcPayKit#onEnable()} 中注册，
 * 否则支付成功后不会有任何奖励发放。
 */
public class PaymentSuccessListener implements Listener {

    /** 已处理过的订单号，避免 XMcPay 重复派发事件导致奖励重复发放 */
    private static final Set<String> HANDLED_ORDERS =
            Collections.synchronizedSet(new LinkedHashSet<String>());

    private static final int MAX_HANDLED_ORDERS = 1000;

    private final XMcPayKit plugin;
    private final KitDataManager dataManager = new KitDataManager();
    private final KitRewardService rewardService = new KitRewardService(dataManager);

    public PaymentSuccessListener(XMcPayKit plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPaymentSuccess(PaymentSuccessEvent event) {
        // 只处理来自本插件的支付事件
        if (plugin == null || !plugin.getName().equals(event.getSourcePlugin())) {
            return;
        }

        Player player = event.getPlayer();
        String orderNo = event.getOrderNo();
        String displayName = event.getDisplayName();
        String paymentMethod = event.getPaymentMethod();
        int money = event.getMoney();
        String customData = event.getCustomData();

        if (player == null) {
            plugin.getLogger().warning("支付成功事件中没有玩家信息，无法发放奖励！订单号: " + orderNo);
            return;
        }

        plugin.getLogger().info(String.format(
                "玩家 %s 支付成功! 订单号: %s, 商品: %s, 金额: %d元, 支付方式: %s, 自定义数据: %s",
                player.getName(), orderNo, displayName, money, paymentMethod, customData
        ));

        // 订单去重，防止重复发放
        if (orderNo != null && !orderNo.isEmpty() && !markHandled(orderNo)) {
            plugin.getLogger().warning("订单 " + orderNo + " 已经发放过奖励，跳过重复发放。");
            return;
        }

        String kitId = KitPurchaseService.kitIdFromCustomData(customData);

        // 礼包购买
        if (kitId != null) {
            if (!dataManager.hasKit(kitId)) {
                plugin.getLogger().warning("礼包 " + kitId + " 不存在，无法发放奖励！订单号: " + orderNo);
                MessageUtil.send(player, "&c礼包配置异常，请联系管理员！订单号: " + orderNo);
                return;
            }

            // 发放物品 + 执行奖励命令 + 记录购买次数
            rewardService.grantKit(player, kitId, String.valueOf(money), "cash");
            return;
        }

        // 其他来源的支付：只提示玩家
        MessageUtil.send(player, "&a恭喜！支付成功！您购买了: " + displayName);
    }

    /**
     * 标记订单已处理，返回 false 表示该订单已经处理过。
     */
    private static synchronized boolean markHandled(String orderNo) {
        if (HANDLED_ORDERS.contains(orderNo)) {
            return false;
        }

        if (HANDLED_ORDERS.size() >= MAX_HANDLED_ORDERS) {
            Iterator<String> iterator = HANDLED_ORDERS.iterator();
            if (iterator.hasNext()) {
                iterator.next();
                iterator.remove();
            }
        }

        HANDLED_ORDERS.add(orderNo);
        return true;
    }
}
