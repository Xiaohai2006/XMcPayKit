package cn.xiaohai.xmcpaykit.data;

import cn.xiaohai.xmcpay.api.config.ConfigAPI;
import cn.xiaohai.xmcpay.api.config.PluginConfigManager;
import cn.xiaohai.xmcpaykit.config.KitConfig;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 礼包数据管理器（数据层）。
 *
 * <p>只负责礼包配置的读写，不包含任何业务规则；业务规则见
 * {@link cn.xiaohai.xmcpaykit.service.KitPurchaseService} 与
 * {@link cn.xiaohai.xmcpaykit.service.KitRewardService}。
 *
 * <p>数据文件：{@code plugins/XMcPay/plugins/XMcPayKit/kits.yml}
 */
public class KitDataManager {

    private final PluginConfigManager configManager;

    public KitDataManager() {
        this.configManager = ConfigAPI.getPluginConfigManager(KitConfig.PLUGIN_NAME);
        if (configManager != null) {
            // 确保 kits 配置已注册（首次运行会自动创建 kits.yml）
            configManager.registerConfig(KitConfig.KIT_FILE);
        }
    }

    // ============================================
    // 创建 / 删除 / 更新
    // ============================================

    /**
     * 创建新礼包
     *
     * @return false 表示配置不可用或礼包已存在
     */
    public boolean createKit(String kitId, String name, int price, int money) {
        reload();

        FileConfiguration config = getConfig();
        if (config == null || kitId == null || kitId.isEmpty()) {
            return false;
        }

        String path = KitConfig.kit(kitId);
        if (config.contains(path)) {
            return false; // 礼包已存在
        }

        config.set(KitConfig.kit(kitId, KitConfig.FIELD_NAME), name);
        config.set(KitConfig.kit(kitId, KitConfig.FIELD_PRICE), price);
        config.set(KitConfig.kit(kitId, KitConfig.FIELD_MONEY), money);
        config.set(KitConfig.kit(kitId, KitConfig.FIELD_DESCRIPTION), defaultDescription());
        config.set(KitConfig.kit(kitId, KitConfig.FIELD_PLAYER_INV), 1);
        config.set(KitConfig.kit(kitId, KitConfig.FIELD_CASH_ONLY), false);
        config.set(KitConfig.kit(kitId, KitConfig.FIELD_PERMISSION), "");
        config.set(KitConfig.kit(kitId, KitConfig.FIELD_PURCHASE_LIMIT), -1);
        config.set(KitConfig.kit(kitId, KitConfig.FIELD_COMMAND), new ArrayList<String>());
        config.set(KitConfig.kit(kitId, KitConfig.FIELD_ITEMS), new ArrayList<Map<String, Object>>());

        save();
        return true;
    }

    /**
     * 删除礼包
     */
    public boolean deleteKit(String kitId) {
        FileConfiguration config = getConfig();
        if (config == null || !config.contains(KitConfig.kit(kitId))) {
            return false;
        }

        config.set(KitConfig.kit(kitId), null);
        save();
        return true;
    }

    /**
     * 更新礼包名称
     */
    public boolean updateKitName(String kitId, String name) {
        return set(kitId, KitConfig.FIELD_NAME, name);
    }

    /**
     * 更新礼包点券价格
     */
    public boolean updateKitPrice(String kitId, int price) {
        return set(kitId, KitConfig.FIELD_PRICE, price);
    }

    /**
     * 更新礼包现金价格（元）
     */
    public boolean updateKitMoney(String kitId, int money) {
        return set(kitId, KitConfig.FIELD_MONEY, money);
    }

    /**
     * 更新礼包描述
     */
    public boolean updateKitDescription(String kitId, List<String> description) {
        return set(kitId, KitConfig.FIELD_DESCRIPTION, description);
    }

    /**
     * 更新礼包奖励命令列表（支付成功后由本插件执行）
     */
    public boolean updateKitCommands(String kitId, List<String> commands) {
        return set(kitId, KitConfig.FIELD_COMMAND,
                commands != null ? commands : new ArrayList<String>());
    }

    /**
     * 设置是否仅允许现金购买
     */
    public boolean updateCashOnly(String kitId, boolean cashOnly) {
        return set(kitId, KitConfig.FIELD_CASH_ONLY, cashOnly);
    }

    // ============================================
    // 礼包物品
    // ============================================

    /**
     * 添加物品到礼包
     */
    @SuppressWarnings("unchecked")
    public boolean addItemToKit(String kitId, ItemStack item) {
        FileConfiguration config = getConfig();
        if (config == null || item == null || !config.contains(KitConfig.kit(kitId))) {
            return false;
        }

        String path = KitConfig.kit(kitId, KitConfig.FIELD_ITEMS);
        List<Map<String, Object>> items = new ArrayList<>();
        if (config.isList(path)) {
            Object raw = config.getList(path);
            if (raw instanceof List) {
                items = (List<Map<String, Object>>) raw;
            }
        }

        items.add(item.serialize());
        config.set(path, items);
        save();
        return true;
    }

    /**
     * 从礼包移除物品（根据索引）
     */
    @SuppressWarnings("unchecked")
    public boolean removeItemFromKit(String kitId, int index) {
        FileConfiguration config = getConfig();
        if (config == null || !config.contains(KitConfig.kit(kitId))) {
            return false;
        }

        String path = KitConfig.kit(kitId, KitConfig.FIELD_ITEMS);
        if (!config.isList(path)) {
            return false;
        }

        List<Map<String, Object>> items = (List<Map<String, Object>>) config.getList(path);
        if (items == null || index < 0 || index >= items.size()) {
            return false;
        }

        items.remove(index);
        config.set(path, items);
        save();
        return true;
    }

    /**
     * 清空礼包内的所有物品
     */
    public boolean clearKitItems(String kitId) {
        return set(kitId, KitConfig.FIELD_ITEMS, new ArrayList<Map<String, Object>>());
    }

    /**
     * 获取礼包的所有物品（无法解析的物品会被忽略）
     */
    @SuppressWarnings("unchecked")
    public List<ItemStack> getKitItems(String kitId) {
        FileConfiguration config = getConfig();
        if (config == null || !config.contains(KitConfig.kit(kitId))) {
            return new ArrayList<>();
        }

        String path = KitConfig.kit(kitId, KitConfig.FIELD_ITEMS);
        if (!config.isList(path)) {
            return new ArrayList<>();
        }

        Object raw = config.getList(path);
        if (!(raw instanceof List)) {
            return new ArrayList<>();
        }

        List<ItemStack> items = new ArrayList<>();
        for (Object itemData : (List<Object>) raw) {
            if (!(itemData instanceof Map)) {
                continue;
            }
            try {
                items.add(ItemStack.deserialize((Map<String, Object>) itemData));
            } catch (Exception e) {
                // 忽略无法解析的物品
            }
        }
        return items;
    }

    // ============================================
    // 查询
    // ============================================

    /**
     * 获取所有礼包ID
     */
    public Set<String> getAllKitIds() {
        FileConfiguration config = getConfig();
        if (config == null) {
            return new HashSet<>();
        }

        ConfigurationSection section = config.getConfigurationSection(KitConfig.KITS_ROOT);
        return section != null ? section.getKeys(false) : new HashSet<String>();
    }

    /**
     * 获取礼包配置段
     */
    public ConfigurationSection getKitSection(String kitId) {
        FileConfiguration config = getConfig();
        if (config == null || kitId == null) {
            return null;
        }
        return config.getConfigurationSection(KitConfig.kit(kitId));
    }

    /**
     * 礼包是否存在
     */
    public boolean hasKit(String kitId) {
        return getKitSection(kitId) != null;
    }

    /**
     * 获取礼包名称
     */
    public String getKitName(String kitId) {
        ConfigurationSection section = getKitSection(kitId);
        return section != null ? section.getString(KitConfig.FIELD_NAME, kitId) : kitId;
    }

    /**
     * 获取礼包点券价格
     */
    public int getKitPrice(String kitId) {
        ConfigurationSection section = getKitSection(kitId);
        return section != null ? section.getInt(KitConfig.FIELD_PRICE, 0) : 0;
    }

    /**
     * 获取礼包现金价格（元）
     */
    public int getKitMoney(String kitId) {
        ConfigurationSection section = getKitSection(kitId);
        return section != null ? section.getInt(KitConfig.FIELD_MONEY, 0) : 0;
    }

    /**
     * 获取礼包描述
     */
    public List<String> getKitDescription(String kitId) {
        ConfigurationSection section = getKitSection(kitId);
        if (section == null) {
            return new ArrayList<>();
        }
        List<String> description = section.getStringList(KitConfig.FIELD_DESCRIPTION);
        return description != null ? description : new ArrayList<String>();
    }

    /**
     * 获取礼包所需背包空位
     */
    public int getRequiredSlots(String kitId) {
        ConfigurationSection section = getKitSection(kitId);
        return section != null ? section.getInt(KitConfig.FIELD_PLAYER_INV, 0) : 0;
    }

    /**
     * 获取礼包购买权限节点（未配置返回空字符串）
     */
    public String getKitPermission(String kitId) {
        ConfigurationSection section = getKitSection(kitId);
        if (section == null) {
            return "";
        }
        String permission = section.getString(KitConfig.FIELD_PERMISSION, "");
        return permission != null ? permission : "";
    }

    /**
     * 获取礼包购买次数上限（-1 表示不限）
     */
    public int getPurchaseLimit(String kitId) {
        ConfigurationSection section = getKitSection(kitId);
        return section != null ? section.getInt(KitConfig.FIELD_PURCHASE_LIMIT, -1) : -1;
    }

    /**
     * 获取礼包奖励命令列表
     */
    public List<String> getKitCommands(String kitId) {
        ConfigurationSection section = getKitSection(kitId);
        if (section == null) {
            return new ArrayList<>();
        }
        List<String> commands = section.getStringList(KitConfig.FIELD_COMMAND);
        return commands != null ? commands : new ArrayList<String>();
    }

    /**
     * 是否仅允许现金购买
     */
    public boolean isCashOnly(String kitId) {
        ConfigurationSection section = getKitSection(kitId);
        return section != null && section.getBoolean(KitConfig.FIELD_CASH_ONLY, false);
    }

    /**
     * 获取全部礼包的数据快照（菜单展示用）
     */
    public List<KitEntry> getKitEntries() {
        List<KitEntry> entries = new ArrayList<>();
        for (String kitId : getAllKitIds()) {
            entries.add(new KitEntry(
                    kitId,
                    getKitName(kitId),
                    getKitPrice(kitId),
                    getKitMoney(kitId),
                    getKitDescription(kitId),
                    getRequiredSlots(kitId),
                    isCashOnly(kitId),
                    getKitPermission(kitId),
                    getPurchaseLimit(kitId),
                    getKitCommands(kitId).size(),
                    getKitItems(kitId).size()
            ));
        }
        return entries;
    }

    /**
     * 获取玩家对某个礼包的购买次数。
     *
     * <p>兼容两种记录方式：XMcPay 内部用 customData（{@code kit:礼包ID}）记录一次，
     * 本插件用纯礼包ID记录一次，取两者最大值作为真实购买次数。
     */
    public int getPurchaseCount(String playerUuid, String kitId) {
        if (playerUuid == null || kitId == null) {
            return 0;
        }

        try {
            int plain = cn.xiaohai.xmcpay.XMcPay.database.getKitPurchaseCount(playerUuid, kitId);
            int prefixed = cn.xiaohai.xmcpay.XMcPay.database.getKitPurchaseCount(playerUuid, "kit:" + kitId);
            return Math.max(plain, prefixed);
        } catch (Throwable t) {
            return 0;
        }
    }

    /**
     * 获取玩家点券余额（占位实现：数据存在 kits.yml 的 players 节点下）。
     *
     * <p>接入真实点券/经济插件时，只需要替换这里和 {@link #setBalance(String, double)}。
     */
    public double getBalance(String playerUuid) {
        FileConfiguration config = getConfig();
        if (config == null || playerUuid == null) {
            return 0;
        }
        return config.getDouble(KitConfig.balance(playerUuid), 0);
    }

    /**
     * 设置玩家点券余额（占位实现）
     */
    public void setBalance(String playerUuid, double balance) {
        FileConfiguration config = getConfig();
        if (config == null || playerUuid == null) {
            return;
        }
        config.set(KitConfig.balance(playerUuid), balance);
        save();
    }

    /**
     * 获取语言配置字符串（lang.yml），不存在时返回默认值
     */
    public String getLangString(String path, String defaultValue) {
        if (configManager == null || path == null) {
            return defaultValue;
        }
        String value = configManager.getString(KitConfig.LANG_FILE, path);
        return value != null ? value : defaultValue;
    }

    /**
     * 重载礼包配置
     */
    public void reload() {
        if (configManager != null) {
            configManager.reloadConfig(KitConfig.KIT_FILE);
        }
    }

    // ============================================
    // 内部辅助
    // ============================================

    private FileConfiguration getConfig() {
        return configManager != null ? configManager.getConfig(KitConfig.KIT_FILE) : null;
    }

    private void save() {
        if (configManager != null) {
            configManager.saveConfig(KitConfig.KIT_FILE);
        }
    }

    /**
     * 写入礼包字段的通用实现
     */
    private boolean set(String kitId, String field, Object value) {
        FileConfiguration config = getConfig();
        if (config == null || !config.contains(KitConfig.kit(kitId))) {
            return false;
        }

        config.set(KitConfig.kit(kitId, field), value);
        save();
        return true;
    }

    /**
     * {@code kits.&lt;id&gt;.description} 默认值，供创建礼包时使用
     */
    public static List<String> defaultDescription() {
        return new ArrayList<>(Arrays.asList("这是一个礼包"));
    }
}
