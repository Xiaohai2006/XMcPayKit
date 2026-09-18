package cn.xiaohai.xmcpaykit.menu.admin;

import cn.xiaohai.xmcpay.gui.Menu;
import cn.xiaohai.xmcpaykit.data.KitDataManager;
import cn.xiaohai.xmcpaykit.data.KitEntry;
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
 * 礼包管理菜单（{@code /xpay kitadmin}）：列出所有礼包，点击进入编辑界面。
 */
public class KitAdminMenu extends Menu {

    private static final int FIRST_SLOT = 10;
    private static final int COLUMNS = 7;
    private static final int MAX_SLOT = 44;
    private static final int SLOT_CREATE = 49;

    private final KitDataManager dataManager;

    /** 槽位 → 礼包ID */
    private final Map<Integer, String> slotKits = new HashMap<>();

    public KitAdminMenu(Player player, KitDataManager dataManager) {
        super(player);
        this.dataManager = dataManager;
    }

    @Override
    public String getMenuName() {
        return "§6礼包管理";
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

        String kitId = slotKits.get(slot);
        if (kitId != null) {
            new KitEditorMenu(player, dataManager, kitId).open();
            return;
        }

        if (slot == SLOT_CREATE) {
            player.closeInventory();
            MessageUtil.send(player, "&a请使用命令创建礼包:");
            MessageUtil.send(player, "&7/xpay kitadmin create <ID> <名称> <点券价格> <现金价格>");
            MessageUtil.send(player, "&7例如: /xpay kitadmin create vip1 VIP礼包 100 50");
        }
    }

    @Override
    public void setMenuItems() {
        slotKits.clear();
        setBorder();
        addKitItems();
        addCreateButton();
    }

    private void setBorder() {
        ItemStack borderItem = new ItemStack(Material.PAPER);
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

    private void addKitItems() {
        List<KitEntry> entries = dataManager.getKitEntries();

        int slot = FIRST_SLOT;
        int placed = 0;
        for (KitEntry entry : entries) {
            if (slot >= MAX_SLOT) {
                break;
            }

            this.inventory.setItem(slot, createKitItem(entry));
            slotKits.put(slot, entry.getId());

            slot++;
            placed++;

            // 每行 7 个，行末空出边框
            if ((slot - FIRST_SLOT) % COLUMNS == 0 && slot > FIRST_SLOT) {
                slot += 2;
            }
        }

        if (placed == 0) {
            ItemStack item = new ItemStack(Material.BARRIER);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§c暂无礼包");
            meta.setLore(java.util.Collections.singletonList("§7请使用命令创建礼包"));
            item.setItemMeta(meta);
            this.inventory.setItem(22, item);
        }
    }

    private ItemStack createKitItem(KitEntry entry) {
        ItemStack item = new ItemStack(Material.CHEST);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§b礼包: " + entry.getName());

        List<String> lore = new ArrayList<>();
        lore.add("§7ID: §f" + entry.getId());
        lore.add("§7点券价格: §a" + entry.getPrice());
        lore.add("§7现金价格: §e" + entry.getMoney() + " 元");
        lore.add("§7物品数量: §f" + entry.getItemCount());
        lore.add("§7奖励命令: §f" + entry.getCommandCount() + " 条");
        lore.add("§7购买方式: " + (entry.isCashOnly() ? "§c仅现金购买" : "§a点券优先"));
        lore.add("");
        lore.add("§e§n点击编辑此礼包");
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }

    private void addCreateButton() {
        ItemStack item = new ItemStack(Material.EMERALD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§a创建新礼包");

        List<String> lore = new ArrayList<>();
        lore.add("§7点击获取创建命令");
        lore.add("");
        lore.add("§7用法:");
        lore.add("§7/xpay kitadmin create");
        lore.add("§7  <ID> <名称>");
        lore.add("§7  <点券价格> <现金价格>");
        meta.setLore(lore);

        item.setItemMeta(meta);
        this.inventory.setItem(SLOT_CREATE, item);
    }
}
