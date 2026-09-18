package cn.xiaohai.xmcpaykit.menu;

import cn.xiaohai.xmcpay.gui.Menu;
import cn.xiaohai.xmcpaykit.data.KitDataManager;
import cn.xiaohai.xmcpaykit.data.KitEntry;
import cn.xiaohai.xmcpaykit.service.KitPurchaseService;
import cn.xiaohai.xmcpaykit.util.MessageUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 玩家礼包购买菜单（{@code /xpay kit}）。
 *
 * <p>点击礼包物品后由 {@link KitPurchaseService} 完成全部校验与支付流程：
 * <ul>
 *     <li>“点券优先”礼包：点券足够直接扣点券并发放奖励，不足则弹出微信/支付宝支付界面</li>
 *     <li>“仅现金购买”礼包：直接弹出微信/支付宝支付界面</li>
 * </ul>
 */
public class KitMenu extends Menu {

    /** 礼包物品所在槽位（每页 28 个） */
    private static final int[] ITEM_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private static final int SLOT_PREVIOUS = 45;
    private static final int SLOT_NEXT = 53;
    private static final int SLOT_EMPTY = 22;

    private final KitDataManager dataManager = new KitDataManager();
    private final KitPurchaseService purchaseService = new KitPurchaseService(dataManager);

    /** 当前页面“槽位 → 礼包ID”映射，避免再用物品名称/lore 反查礼包 */
    private final Map<Integer, String> slotKits = new HashMap<>();

    private int page = 0;
    private int maxPages = 1;

    public KitMenu(Player player) {
        super(player);
    }

    @Override
    public String getMenuName() {
        return "礼包 - 第 " + (page + 1) + " 页";
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        int slot = e.getSlot();
        Player player = (Player) e.getWhoClicked();

        // 翻页
        if (slot == SLOT_NEXT) {
            if (page + 1 < maxPages) {
                page++;
                super.open();
            }
            return;
        }
        if (slot == SLOT_PREVIOUS) {
            if (page > 0) {
                page--;
                super.open();
            }
            return;
        }

        String kitId = slotKits.get(slot);
        if (kitId == null) {
            return;
        }
        handleKitClick(player, kitId);
    }

    @Override
    public void setMenuItems() {
        slotKits.clear();

        List<KitEntry> entries = dataManager.getKitEntries();
        maxPages = Math.max(1, (int) Math.ceil(entries.size() / (double) ITEM_SLOTS.length));
        if (page >= maxPages) {
            page = maxPages - 1;
        }
        if (page < 0) {
            page = 0;
        }

        addControls(entries.isEmpty());
        addKitItems(entries);
    }

    // ============================================
    // 点击处理
    // ============================================

    private void handleKitClick(Player player, String kitId) {
        // 仅现金购买：直接走现金支付
        if (dataManager.isCashOnly(kitId)) {
            purchaseService.notifyIfFailed(player, purchaseService.openCashPayment(player, kitId));
            return;
        }

        // 点券优先：先尝试点券购买
        KitPurchaseService.Result result = purchaseService.purchaseWithPoints(player, kitId);
        if (result == KitPurchaseService.Result.OK) {
            return;
        }

        // 点券不足时提示并引导玩家使用现金支付
        if (result == KitPurchaseService.Result.INSUFFICIENT_BALANCE) {
            purchaseService.notifyIfFailed(player, result);
            purchaseService.notifyIfFailed(player, purchaseService.openCashPayment(player, kitId));
            return;
        }

        purchaseService.notifyIfFailed(player, result);
    }

    // ============================================
    // 界面渲染
    // ============================================

    private void addControls(boolean empty) {
        if (empty) {
            ItemStack item = new ItemStack(Material.BARRIER);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(MessageUtil.color("&c还没有礼包呢"));
            meta.setLore(Collections.singletonList(MessageUtil.color("&7快联系管理添加吧！")));
            item.setItemMeta(meta);
            this.inventory.setItem(SLOT_EMPTY, item);
        }

        if (page + 1 < maxPages) {
            this.inventory.setItem(SLOT_NEXT, createArrow("&a下一页", "&7前往 第 &a" + (page + 2) + "&7 页"));
        }
        if (page > 0) {
            this.inventory.setItem(SLOT_PREVIOUS, createArrow("&a上一页", "&7前往 第 &a" + page + "&7 页"));
        }
    }

    private void addKitItems(List<KitEntry> entries) {
        int start = page * ITEM_SLOTS.length;
        for (int i = 0; i < ITEM_SLOTS.length; i++) {
            int index = start + i;
            if (index >= entries.size()) {
                break;
            }

            KitEntry entry = entries.get(index);
            int slot = ITEM_SLOTS[i];
            this.inventory.setItem(slot, createKitItem(entry));
            slotKits.put(slot, entry.getId());
        }
    }

    private ItemStack createKitItem(KitEntry entry) {
        ItemStack item = new ItemStack(Material.CHEST);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MessageUtil.color("&b礼包: &f" + entry.getName()));

        List<String> lore = new ArrayList<>();
        if (entry.isCashOnly()) {
            lore.add(MessageUtil.color("&7价格: &c" + entry.getMoney() + " 元 &7(仅现金购买)"));
            lore.add(MessageUtil.color("&c此礼包仅支持现金购买"));
        } else {
            lore.add(MessageUtil.color("&7点券价格: &a" + entry.getPrice()));
            lore.add(MessageUtil.color("&7现金价格: &e" + entry.getMoney() + " 元"));
        }
        lore.add(MessageUtil.color("&7所需背包空间: &a" + entry.getRequiredSlots()));
        lore.add(MessageUtil.color("&7奖励命令: &a" + entry.getCommandCount() + " 条"));

        if (!entry.getDescription().isEmpty()) {
            lore.add("");
            for (String line : entry.getDescription()) {
                lore.add(MessageUtil.color("&f" + line));
            }
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createArrow(String name, String lore) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MessageUtil.color(name));
        meta.setLore(Collections.singletonList(MessageUtil.color(lore)));
        item.setItemMeta(meta);
        return item;
    }
}
