package cn.xiaohai.xmcpaykit.util;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

/**
 * 背包相关工具。
 *
 * <p>原先背包空位统计、物品发放逻辑在菜单和命令类里各写了一份，统一收口到这里。
 */
public final class InventoryUtil {

    private InventoryUtil() {
    }

    /**
     * 统计玩家背包空位数量
     */
    public static int countEmptySlots(Player player) {
        if (player == null) {
            return 0;
        }

        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() == Material.AIR) {
                count++;
            }
        }
        return count;
    }

    /**
     * 判断玩家背包空位是否足够
     */
    public static boolean hasEmptySlots(Player player, int requiredSlots) {
        return countEmptySlots(player) >= Math.max(0, requiredSlots);
    }

    /**
     * 发放物品给玩家，背包放不下的部分掉落在地上（避免奖励丢失）。
     *
     * @return 实际发放的物品件数
     */
    public static int giveItems(Player player, List<ItemStack> items) {
        if (player == null || items == null || items.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (ItemStack item : items) {
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }

            Map<Integer, ItemStack> leftover = player.getInventory().addItem(item.clone());
            count++;

            for (ItemStack rest : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), rest);
            }
        }
        return count;
    }
}
