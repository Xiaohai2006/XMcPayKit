package cn.xiaohai.xmcpaykit.data;

import java.util.ArrayList;
import java.util.List;

/**
 * 礼包数据快照（不可变）。
 *
 * <p>菜单渲染时一次性从 {@link KitDataManager#getKitEntries()} 取出，
 * 避免在循环里反复读配置，也避免界面代码散落配置键。
 */
public class KitEntry {

    private final String id;
    private final String name;
    private final int price;
    private final int money;
    private final List<String> description;
    private final int requiredSlots;
    private final boolean cashOnly;
    private final String permission;
    private final int purchaseLimit;
    private final int commandCount;
    private final int itemCount;

    public KitEntry(String id, String name, int price, int money, List<String> description,
                    int requiredSlots, boolean cashOnly, String permission, int purchaseLimit,
                    int commandCount, int itemCount) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.money = money;
        this.description = description != null ? description : new ArrayList<String>();
        this.requiredSlots = requiredSlots;
        this.cashOnly = cashOnly;
        this.permission = permission;
        this.purchaseLimit = purchaseLimit;
        this.commandCount = commandCount;
        this.itemCount = itemCount;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    /**
     * 点券价格
     */
    public int getPrice() {
        return price;
    }

    /**
     * 现金价格（元）
     */
    public int getMoney() {
        return money;
    }

    public List<String> getDescription() {
        return description;
    }

    /**
     * 购买所需背包空位
     */
    public int getRequiredSlots() {
        return requiredSlots;
    }

    /**
     * 是否仅允许现金购买
     */
    public boolean isCashOnly() {
        return cashOnly;
    }

    public String getPermission() {
        return permission;
    }

    /**
     * 购买次数上限，-1 表示不限
     */
    public int getPurchaseLimit() {
        return purchaseLimit;
    }

    public int getCommandCount() {
        return commandCount;
    }

    public int getItemCount() {
        return itemCount;
    }
}
