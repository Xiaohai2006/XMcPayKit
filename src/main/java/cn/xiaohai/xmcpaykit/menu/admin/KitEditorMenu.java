package cn.xiaohai.xmcpaykit.menu.admin;

import cn.xiaohai.xmcpay.gui.Menu;
import cn.xiaohai.xmcpaykit.XMcPayKit;
import cn.xiaohai.xmcpaykit.data.KitDataManager;
import cn.xiaohai.xmcpaykit.listener.ChatInputListener;
import cn.xiaohai.xmcpaykit.util.MessageUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 礼包编辑菜单：编辑名称 / 点券价格 / 现金价格 / 描述 / 物品 / 奖励命令 / 购买方式。
 *
 * <p>点击的按钮使用“槽位 → 动作”映射识别，不再依赖物品名称字符串匹配。
 */
public class KitEditorMenu extends Menu {

    /**
     * 编辑菜单里的动作
     */
    private enum Action {
        EDIT_NAME,
        EDIT_PRICE,
        EDIT_MONEY,
        EDIT_DESCRIPTION,
        EDIT_ITEMS,
        EDIT_COMMANDS,
        TOGGLE_CASH_ONLY,
        BACK,
        DELETE
    }

    private final KitDataManager dataManager;
    private final String kitId;

    /** 槽位 → 动作 */
    private final Map<Integer, Action> actions = new HashMap<>();

    public KitEditorMenu(Player player, KitDataManager dataManager, String kitId) {
        super(player);
        this.dataManager = dataManager;
        this.kitId = kitId;
    }

    @Override
    public String getMenuName() {
        return "§6编辑礼包: " + dataManager.getKitName(kitId);
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        e.setCancelled(true);

        Player player = (Player) e.getWhoClicked();
        Action action = actions.get(e.getSlot());
        if (action == null) {
            return;
        }

        switch (action) {
            case EDIT_NAME:
                requestChatInput(player, ChatInputListener.InputType.KIT_NAME,
                        "&a请在聊天栏输入新的礼包名称:");
                break;
            case EDIT_PRICE:
                requestChatInput(player, ChatInputListener.InputType.KIT_PRICE,
                        "&a请在聊天栏输入新的点券价格（数字）:");
                break;
            case EDIT_MONEY:
                requestChatInput(player, ChatInputListener.InputType.KIT_MONEY,
                        "&a请在聊天栏输入新的现金价格（数字）:");
                break;
            case EDIT_DESCRIPTION:
                requestChatInput(player, ChatInputListener.InputType.KIT_DESCRIPTION,
                        "&a请在聊天栏输入新的描述（用 | 分隔多行）:");
                break;
            case EDIT_ITEMS:
                new KitItemsEditorMenu(player, dataManager, kitId).open();
                break;
            case EDIT_COMMANDS:
                requestChatInput(player, ChatInputListener.InputType.KIT_COMMAND,
                        "&a请在聊天栏输入奖励命令（多条用 | 分隔，输入 clear 清空）:");
                MessageUtil.send(player, "&7支持变量: &f%player% &7玩家名, &f%money% &7支付金额, "
                        + "&f%point% &7支付金额, &f%kit_id% &7礼包ID");
                MessageUtil.send(player, "&7例如: &fgive %player% diamond 10|say %player% 购买了礼包");
                break;
            case TOGGLE_CASH_ONLY:
                toggleCashOnly(player);
                break;
            case BACK:
                new KitAdminMenu(player, dataManager).open();
                break;
            case DELETE:
                if (dataManager.deleteKit(kitId)) {
                    MessageUtil.send(player, "&a礼包已删除！");
                    new KitAdminMenu(player, dataManager).open();
                } else {
                    MessageUtil.send(player, "&c删除失败！");
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void setMenuItems() {
        actions.clear();
        setBorder();
        addEditOptions();
    }

    // ============================================
    // 交互
    // ============================================

    /**
     * 关闭菜单并请求玩家在聊天栏输入
     */
    private void requestChatInput(Player player, ChatInputListener.InputType type, String prompt) {
        player.closeInventory();
        MessageUtil.send(player, prompt);

        ChatInputListener chatListener = XMcPayKit.getChatInputListener();
        if (chatListener != null) {
            chatListener.addPendingInput(player, type, kitId);
        }
    }

    private void toggleCashOnly(Player player) {
        boolean cashOnly = !dataManager.isCashOnly(kitId);
        if (dataManager.updateCashOnly(kitId, cashOnly)) {
            MessageUtil.send(player, cashOnly
                    ? "&a已设置为 &e仅现金购买 &a（微信/支付宝）"
                    : "&a已设置为 &e点券优先 &a（点券不足时可用现金购买）");
        } else {
            MessageUtil.send(player, "&c切换购买方式失败！");
        }
        new KitEditorMenu(player, dataManager, kitId).open();
    }

    // ============================================
    // 界面渲染
    // ============================================

    private void setBorder() {
        ItemStack borderItem = new ItemStack(Material.STAINED_GLASS_PANE, 1, (byte) 7);
        ItemMeta meta = borderItem.getItemMeta();
        meta.setDisplayName(" ");
        borderItem.setItemMeta(meta);

        for (int i = 0; i < 9; i++) {
            this.inventory.setItem(i, borderItem);
            this.inventory.setItem(i + 45, borderItem);
        }
        for (int i = 0; i < 5; i++) {
            this.inventory.setItem(i * 9, borderItem);
            this.inventory.setItem(i * 9 + 8, borderItem);
        }
    }

    private void addEditOptions() {
        addOption(11, Action.EDIT_NAME, Material.NAME_TAG, "§b编辑名称",
                "§7当前: §f" + dataManager.getKitName(kitId),
                "",
                "§e点击修改");

        addOption(13, Action.EDIT_PRICE, Material.GOLD_INGOT, "§e编辑点券价格",
                "§7当前: §f" + dataManager.getKitPrice(kitId) + " 点券",
                "",
                "§e点击修改");

        addOption(15, Action.EDIT_MONEY, Material.DIAMOND, "§b编辑现金价格",
                "§7当前: §f" + dataManager.getKitMoney(kitId) + " 元",
                "",
                "§e点击修改");

        addOption(20, Action.EDIT_DESCRIPTION, Material.BOOK, "§a编辑描述",
                "§7当前描述行数: §f" + dataManager.getKitDescription(kitId).size(),
                "",
                "§e点击修改",
                "§7多行用 | 分隔");

        addOption(22, Action.EDIT_ITEMS, Material.CHEST, "§6编辑物品",
                "§7当前物品数: §f" + dataManager.getKitItems(kitId).size(),
                "",
                "§e点击管理礼包物品",
                "§7添加/删除物品");

        List<String> commands = dataManager.getKitCommands(kitId);
        List<String> commandLore = new ArrayList<>();
        commandLore.add("§7当前命令数: §f" + commands.size());
        int preview = 0;
        for (String command : commands) {
            if (preview >= 3) {
                commandLore.add("§7... 还有 §f" + (commands.size() - preview) + " §7条");
                break;
            }
            commandLore.add("§8- §f" + command);
            preview++;
        }
        commandLore.add("");
        commandLore.add("§e点击编辑奖励命令");
        commandLore.add("§7支付成功后由本插件执行");
        addOption(23, Action.EDIT_COMMANDS, Material.COMMAND, "§6编辑奖励命令",
                commandLore.toArray(new String[0]));

        boolean cashOnly = dataManager.isCashOnly(kitId);
        addOption(24, Action.TOGGLE_CASH_ONLY, Material.GOLD_NUGGET, "§d购买方式",
                "§7当前: " + (cashOnly ? "§c仅现金购买" : "§a点券优先（点券不足可现金）"),
                "",
                "§e点击切换");

        addOption(45, Action.BACK, Material.ARROW, "§c返回",
                "§7返回礼包管理列表");

        addOption(49, Action.DELETE, Material.BARRIER, "§4删除礼包",
                "§c点击删除此礼包",
                "",
                "§c§l此操作不可撤销！");
    }

    private void addOption(int slot, Action action, Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(new ArrayList<>(java.util.Arrays.asList(lore)));
        item.setItemMeta(meta);

        this.inventory.setItem(slot, item);
        actions.put(slot, action);
    }
}
