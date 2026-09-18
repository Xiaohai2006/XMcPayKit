package cn.xiaohai.xmcpaykit.service;

import cn.xiaohai.xmcpaykit.XMcPayKit;
import cn.xiaohai.xmcpaykit.config.KitConfig;
import cn.xiaohai.xmcpaykit.data.KitDataManager;
import cn.xiaohai.xmcpaykit.util.InventoryUtil;
import cn.xiaohai.xmcpaykit.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * 礼包奖励发放服务（业务层）。
 *
 * <p>支付成功后（现金支付 / 点券支付）统一由这里发放奖励：
 * <ol>
 *     <li>发放礼包内的物品（背包放不下时掉落在地上，避免物品丢失）</li>
 *     <li>执行礼包配置的奖励命令（支持 %player% / %money% / %point% 等变量）</li>
 *     <li>记录购买次数</li>
 *     <li>给玩家发送购买成功提示</li>
 * </ol>
 *
 * <p><b>约定：</b>调用方传给 XMcPay 的命令列表必须为空，
 * 奖励命令只在这里执行一次，否则会和 XMcPay 内部的命令执行逻辑重复执行。
 */
public class KitRewardService {

    private final KitDataManager dataManager;

    public KitRewardService() {
        this(new KitDataManager());
    }

    public KitRewardService(KitDataManager dataManager) {
        this.dataManager = dataManager;
    }

    /**
     * 发放礼包奖励。
     *
     * <p>该方法必须在主线程执行；如果当前是异步线程会自动调度回主线程。
     *
     * @param player     购买礼包的玩家
     * @param kitId      礼包ID
     * @param paidAmount 实际支付金额（现金为元，点券为点券数），用于 %money% / %point% 变量
     * @param source     触发来源，仅用于日志（cash / point）
     * @return 是否成功发放
     */
    public boolean grantKit(final Player player, final String kitId, final String paidAmount, final String source) {
        if (player == null || kitId == null || kitId.isEmpty()) {
            return false;
        }

        if (!Bukkit.isPrimaryThread()) {
            final XMcPayKit plugin = XMcPayKit.getInstance();
            if (plugin == null) {
                return false;
            }
            Bukkit.getScheduler().runTask(plugin, new Runnable() {
                @Override
                public void run() {
                    grantKit(player, kitId, paidAmount, source);
                }
            });
            return true;
        }

        if (!dataManager.hasKit(kitId)) {
            log("礼包 " + kitId + " 不存在，无法发放奖励！");
            return false;
        }

        String kitName = dataManager.getKitName(kitId);

        // 1. 发放物品
        int givenItems = InventoryUtil.giveItems(player, dataManager.getKitItems(kitId));

        // 2. 执行奖励命令
        int executedCommands = executeCommands(player, dataManager.getKitCommands(kitId), paidAmount, kitId, kitName);

        // 3. 记录购买次数
        recordPurchase(player, kitId);

        // 4. 提示玩家
        String succeed = dataManager.getLangString(KitConfig.LANG_PAY_SUCCEED,
                "&a恭喜！支付成功！您购买了: %kit_name%");
        MessageUtil.send(player, succeed.replace("%kit_name%", kitName));

        log(String.format("已发放礼包奖励 [%s] 玩家: %s | 来源: %s | 支付: %s | 物品: %d 件 | 命令: %d 条",
                kitId, player.getName(), source, paidAmount, givenItems, executedCommands));
        return true;
    }

    /**
     * 执行奖励命令（主线程）。
     *
     * <p>支持变量：%player% 玩家名、%player_uuid% 玩家UUID、%money% 支付金额、
     * %point% 支付金额、%kit_id% 礼包ID、%kit_name% 礼包名称。
     *
     * @return 成功执行的命令条数
     */
    public static int executeCommands(Player player, List<String> commands, String paidAmount,
                                      String kitId, String kitName) {
        if (player == null || commands == null || commands.isEmpty()) {
            return 0;
        }

        String amount = paidAmount == null ? "0" : paidAmount;
        int executed = 0;

        for (String raw : commands) {
            if (raw == null) {
                continue;
            }

            String command = raw.trim();
            if (command.isEmpty() || command.startsWith("#")) {
                continue;
            }

            command = command
                    .replace("%player%", player.getName())
                    .replace("%player_uuid%", player.getUniqueId().toString())
                    .replace("%money%", amount)
                    .replace("%point%", amount)
                    .replace("%kit_id%", kitId == null ? "" : kitId)
                    .replace("%kit_name%", kitName == null ? "" : kitName);

            if (command.startsWith("/")) {
                command = command.substring(1);
            }

            try {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                executed++;
            } catch (Exception e) {
                log("执行奖励命令失败: " + command + " (" + e.getMessage() + ")");
            }
        }
        return executed;
    }

    /**
     * 记录一次购买（用于购买次数上限判断）
     */
    private void recordPurchase(Player player, String kitId) {
        try {
            cn.xiaohai.xmcpay.XMcPay.database.addKitPurchaseRecord(player.getUniqueId().toString(), kitId);
        } catch (Throwable t) {
            log("记录礼包购买次数失败: " + t.getMessage());
        }
    }

    private static void log(String message) {
        XMcPayKit plugin = XMcPayKit.getInstance();
        if (plugin != null) {
            plugin.getLogger().info(message);
        }
    }
}
