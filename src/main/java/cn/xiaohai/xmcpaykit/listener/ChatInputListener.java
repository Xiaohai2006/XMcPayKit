package cn.xiaohai.xmcpaykit.listener;

import cn.xiaohai.xmcpaykit.XMcPayKit;
import cn.xiaohai.xmcpaykit.data.KitDataManager;
import cn.xiaohai.xmcpaykit.data.LanguageEntry;
import cn.xiaohai.xmcpaykit.menu.admin.KitEditorMenu;
import cn.xiaohai.xmcpaykit.util.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 聊天输入监听器。
 *
 * <p>管理 GUI 里的“编辑名称 / 价格 / 描述 / 奖励命令”需要玩家在聊天栏输入内容，
 * 这里负责收集输入并写回礼包配置。
 *
 * <p>聊天事件是异步线程触发的，因此所有配置读写与 GUI 操作都会调度回主线程执行。
 */
public class ChatInputListener implements Listener {

    /** 等待输入的玩家及其状态 */
    private final Map<UUID, InputState> pendingInputs = new HashMap<>();

    private final XMcPayKit plugin;
    private final KitDataManager dataManager;

    public ChatInputListener(XMcPayKit plugin) {
        this.plugin = plugin;
        this.dataManager = new KitDataManager();
    }

    /**
     * 添加等待输入的玩家
     */
    public void addPendingInput(Player player, InputType type, String kitId) {
        pendingInputs.put(player.getUniqueId(), new InputState(type, kitId));
    }

    /**
     * 移除等待输入的玩家
     */
    public void removePendingInput(Player player) {
        pendingInputs.remove(player.getUniqueId());
    }

    /**
     * 检查玩家是否正在等待输入
     */
    public boolean isPendingInput(Player player) {
        return pendingInputs.containsKey(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        final Player player = event.getPlayer();

        if (!isPendingInput(player)) {
            return;
        }

        final InputState state = pendingInputs.get(player.getUniqueId());
        final String message = event.getMessage();

        // 取消事件，防止输入内容显示在聊天栏
        event.setCancelled(true);

        plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                removePendingInput(player);
                handleInput(player, state, message);
            }
        });
    }

    /**
     * 分发输入内容（主线程）
     */
    private void handleInput(Player player, InputState state, String message) {
        switch (state.getType()) {
            case KIT_NAME:
                handleKitNameInput(player, state.getKitId(), message);
                break;
            case KIT_PRICE:
                handleKitPriceInput(player, state.getKitId(), message);
                break;
            case KIT_MONEY:
                handleKitMoneyInput(player, state.getKitId(), message);
                break;
            case KIT_DESCRIPTION:
                handleKitDescriptionInput(player, state.getKitId(), message);
                break;
            case KIT_COMMAND:
                handleKitCommandInput(player, state.getKitId(), message);
                break;
            default:
                break;
        }
    }

    /**
     * 处理礼包名称输入
     */
    private void handleKitNameInput(Player player, String kitId, String message) {
        if (dataManager.updateKitName(kitId, message)) {
            MessageUtil.send(player, LanguageEntry.getUpdatePackageName() + message);
        } else {
            MessageUtil.send(player, LanguageEntry.getUpdatePackageNameError());
        }
        reopenEditor(player, kitId);
    }

    /**
     * 处理礼包点券价格输入
     */
    private void handleKitPriceInput(Player player, String kitId, String message) {
        try {
            int price = Integer.parseInt(message.trim());
            if (price < 0) {
                MessageUtil.send(player, LanguageEntry.getNegativePrice());
                reopenEditor(player, kitId);
                return;
            }

            if (dataManager.updateKitPrice(kitId, price)) {
                MessageUtil.send(player, "&a礼包点券价格已更新为: " + price);
            } else {
                MessageUtil.send(player, "&c更新点券价格失败！");
            }
        } catch (NumberFormatException e) {
            MessageUtil.send(player, "&c请输入有效的数字！");
        }
        reopenEditor(player, kitId);
    }

    /**
     * 处理礼包现金价格输入
     */
    private void handleKitMoneyInput(Player player, String kitId, String message) {
        try {
            int money = Integer.parseInt(message.trim());
            if (money < 0) {
                MessageUtil.send(player, LanguageEntry.getNegativePrice());
                reopenEditor(player, kitId);
                return;
            }

            if (dataManager.updateKitMoney(kitId, money)) {
                MessageUtil.send(player, "&a礼包现金价格已更新为: " + money);
            } else {
                MessageUtil.send(player, "&c更新现金价格失败！");
            }
        } catch (NumberFormatException e) {
            MessageUtil.send(player, "&c请输入有效的数字！");
        }
        reopenEditor(player, kitId);
    }

    /**
     * 处理礼包描述输入（多条用 | 分隔）
     */
    private void handleKitDescriptionInput(Player player, String kitId, String message) {
        List<String> description = new ArrayList<>();
        for (String line : message.split("\\|")) {
            description.add(line.trim());
        }

        if (dataManager.updateKitDescription(kitId, description)) {
            MessageUtil.send(player, "&a礼包描述已更新！");
        } else {
            MessageUtil.send(player, "&c更新描述失败！");
        }
        reopenEditor(player, kitId);
    }

    /**
     * 处理礼包奖励命令输入（多条用 | 分隔，clear 清空）
     */
    private void handleKitCommandInput(Player player, String kitId, String message) {
        List<String> commands = new ArrayList<>();
        String input = message == null ? "" : message.trim();

        if (!input.isEmpty() && !input.equalsIgnoreCase("clear") && !input.equalsIgnoreCase("none")) {
            for (String part : input.split("\\|")) {
                String command = part.trim();
                if (command.startsWith("/")) {
                    command = command.substring(1);
                }
                if (!command.isEmpty()) {
                    commands.add(command);
                }
            }
        }

        if (dataManager.updateKitCommands(kitId, commands)) {
            MessageUtil.send(player, "&a奖励命令已更新，共 &f" + commands.size() + " &a条：");
            for (String command : commands) {
                MessageUtil.send(player, "&7- &f" + command);
            }
        } else {
            MessageUtil.send(player, "&c更新奖励命令失败！");
        }
        reopenEditor(player, kitId);
    }

    private void reopenEditor(Player player, String kitId) {
        new KitEditorMenu(player, dataManager, kitId).open();
    }

    /**
     * 输入状态
     */
    public static class InputState {
        private final InputType type;
        private final String kitId;

        public InputState(InputType type, String kitId) {
            this.type = type;
            this.kitId = kitId;
        }

        public InputType getType() {
            return type;
        }

        public String getKitId() {
            return kitId;
        }
    }

    /**
     * 输入类型
     */
    public enum InputType {
        KIT_NAME,
        KIT_PRICE,
        KIT_MONEY,
        KIT_DESCRIPTION,
        KIT_COMMAND
    }
}
