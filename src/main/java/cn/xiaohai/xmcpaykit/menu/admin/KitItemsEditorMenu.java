package cn.xiaohai.xmcpaykit.menu.admin;

import cn.xiaohai.xmcpay.gui.Menu;
import cn.xiaohai.xmcpaykit.data.KitDataManager;
import cn.xiaohai.xmcpaykit.util.MessageUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 礼包物品编辑菜单：添加 / 删除 / 替换 / 清空礼包内的物品。
 *
 * <p>操作方式：
 * <ul>
 *     <li>手持物品点击“添加物品”：加入礼包</li>
 *     <li>左键点击礼包内物品：删除</li>
 *     <li>右键点击礼包内物品：用手持物品替换</li>
 * </ul>
 */
public class KitItemsEditorMenu extends Menu {

    private static final int ITEMS_PER_PAGE = 45;
    private static final int SLOT_ADD = 45;
    private static final int SLOT_CLEAR = 46;
    private static final int SLOT_BACK = 49;
    private static final int SLOT_PREVIOUS = 50;
    private static final int SLOT_NEXT = 52;

    private final KitDataManager dataManager;
    private final String kitId;
    private List<ItemStack> kitItems;

    private int currentPage = 0;

    public KitItemsEditorMenu(Player player, KitDataManager dataManager, String kitId) {
        super(player);
        this.dataManager = dataManager;
        this.kitId = kitId;
        this.kitItems = dataManager.getKitItems(kitId);
    }

    @Override
    public String getMenuName() {
        return "§6物品管理: " + dataManager.getKitName(kitId);
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        e.setCancelled(true);

        Player player = (Player) e.getWhoClicked();
        int slot = e.getSlot();

        // 礼包物品槽位（0-44）
        if (slot < ITEMS_PER_PAGE) {
            handleKitItemClick(player, e, slot);
            return;
        }

        switch (slot) {
            case SLOT_ADD:
                addHeldItem(player);
                break;
            case SLOT_CLEAR:
                clearItems(player);
                break;
            case SLOT_BACK:
                new KitEditorMenu(player, dataManager, kitId).open();
                break;
            case SLOT_PREVIOUS:
                if (currentPage > 0) {
                    currentPage--;
                    refreshMenu();
                }
                break;
            case SLOT_NEXT:
                if ((currentPage + 1) * ITEMS_PER_PAGE < kitItems.size()) {
                    currentPage++;
                    refreshMenu();
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void setMenuItems() {
        addKitItems();
        addFunctionButtons();
    }

    // ============================================
    // 物品操作
    // ============================================

    private void handleKitItemClick(Player player, InventoryClickEvent e, int slot) {
        int index = currentPage * ITEMS_PER_PAGE + slot;
        if (index < 0 || index >= kitItems.size()) {
            return;
        }

        if (e.isLeftClick()) {
            if (dataManager.removeItemFromKit(kitId, index)) {
                MessageUtil.send(player, "&a已删除物品！");
                reloadItems();
            }
            return;
        }

        if (e.isRightClick()) {
            ItemStack handItem = player.getItemInHand();
            if (handItem == null || handItem.getType() == Material.AIR) {
                MessageUtil.send(player, "&c请手持要替换的物品！");
                return;
            }

            // 先删除旧物品，再添加手持物品
            dataManager.removeItemFromKit(kitId, index);
            dataManager.addItemToKit(kitId, handItem);
            MessageUtil.send(player, "&a已替换物品！");
            reloadItems();
        }
    }

    private void addHeldItem(Player player) {
        ItemStack handItem = player.getItemInHand();
        if (handItem == null || handItem.getType() == Material.AIR) {
            MessageUtil.send(player, "&c请手持要添加的物品！");
            return;
        }

        if (dataManager.addItemToKit(kitId, handItem)) {
            MessageUtil.send(player, "&a已添加物品到礼包！");
            reloadItems();
        }
    }

    private void clearItems(Player player) {
        if (dataManager.clearKitItems(kitId)) {
            MessageUtil.send(player, "&a已清空所有物品！");
            reloadItems();
            return;
        }
        MessageUtil.send(player, "&c清空失败！");
    }

    private void reloadItems() {
        this.kitItems = dataManager.getKitItems(kitId);
        int maxPage = getMaxPage();
        if (currentPage > maxPage) {
            currentPage = maxPage;
        }
        refreshMenu();
    }

    private int getMaxPage() {
        return Math.max(0, (kitItems.size() - 1) / ITEMS_PER_PAGE);
    }

    // ============================================
    // 界面渲染
    // ============================================

    private void addKitItems() {
        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, kitItems.size());

        for (int i = startIndex; i < endIndex; i++) {
            this.inventory.setItem(i - startIndex, kitItems.get(i));
        }
    }

    private void addFunctionButtons() {
        createButton(SLOT_ADD, Material.HOPPER, "§a添加物品",
                "§7手持物品后点击",
                "",
                "§e将手中物品添加到礼包");

        createButton(SLOT_CLEAR, Material.BARRIER, "§c清空所有物品",
                "§c点击清空礼包内所有物品",
                "",
                "§c§l此操作不可撤销！");

        createButton(SLOT_BACK, Material.ARROW, "§e返回上一级",
                "§7返回礼包编辑界面");

        if (currentPage > 0) {
            createButton(SLOT_PREVIOUS, Material.ARROW, "§a上一页",
                    "§7查看上一页物品");
        }

        if ((currentPage + 1) * ITEMS_PER_PAGE < kitItems.size()) {
            createButton(SLOT_NEXT, Material.ARROW, "§a下一页",
                    "§7查看下一页物品");
        }
    }

    private void createButton(int slot, Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(new ArrayList<>(Arrays.asList(lore)));
        item.setItemMeta(meta);

        this.inventory.setItem(slot, item);
    }

    private void refreshMenu() {
        this.inventory.clear();
        setMenuItems();
    }
}
