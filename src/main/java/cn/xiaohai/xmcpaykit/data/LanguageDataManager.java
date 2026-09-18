package cn.xiaohai.xmcpaykit.data;

import cn.xiaohai.xmcpay.api.config.ConfigAPI;
import cn.xiaohai.xmcpay.api.config.PluginConfigManager;
import cn.xiaohai.xmcpaykit.config.KitConfig;
import org.bukkit.configuration.file.FileConfiguration;

public class LanguageDataManager {
    private final PluginConfigManager configManager;

    public LanguageDataManager() {
        this.configManager = ConfigAPI.getPluginConfigManager(KitConfig.PLUGIN_NAME);
        if (configManager != null) {
            // 确保 kits 配置已注册（首次运行会自动创建 lang.yml）
            configManager.registerConfig(KitConfig.LANG_FILE);
        }
    }
    public boolean createKit(String kitId) {
        reload();

        FileConfiguration config = getConfig();
        if (config == null || kitId == null || kitId.isEmpty()) {
            return false;
        }

        String path = KitConfig.kit(kitId);
        if (config.contains(path)) {
            return false; // 語言已存在
        }

        config.set(KitConfig.LANG_PAY_SUCCEED, "&a恭喜！支付成功！您购买了: %kit_name%");
        config.set(KitConfig.LANG_PAY_BALANCE, "&c你的点券余额不足！");
        config.set(KitConfig.LANG_PAY_PLAYER_INV, "&c你的背包空间不足！");
        config.set(KitConfig.LANG_PAY_UNAVAILABLE, "&c支付系统不可用！");
        config.set(KitConfig.LANG_KIT_NO_PERMISSION, "&c你没有权限购买这个礼包");
        config.set(KitConfig.LANG_KIT_PURCHASE_LIMIT, "&c你已达到该礼包的购买次数上限");
        config.set(KitConfig.LANG_KIT_NOT_FOUND, "&c礼包配置未加载！");
        config.set(KitConfig.LANG_KIT_INVALID_PRICE, "&c该礼包未配置有效的现金价格！");
        config.set(KitConfig.LANG_KIT_MANAGEMENT_INTERFACE,"打开礼包管理界面（需要管理员权限");
        config.set(KitConfig.LANG_KIT_EXECUTE_SUCCEED,"§c请在游戏内执行此命令！");
        config.set(KitConfig.LANG_KIT_EXECUTE_ERROR,"&c你没有权限执行此命令！");
        config.set(KitConfig.LANG_KIT_EXECUTE_UNKNOWN,"&c未知子命令！可用命令: create, delete, reload, list, clear");
        config.set(KitConfig.LANG_KIT_USAGE,"&c用法: /xpay kitadmin create <ID> <名称> <点券价格> <现金价格>");
        config.set(KitConfig.LANG_KIT_DESCRIPTION,"打开礼包菜单，或按礼包名称直接购买");
        config.set(KitConfig.LANG_KIT_NOTFOUND,"&c未找到礼包: &f");
        config.set(KitConfig.LANG_KIT_VIEW_AVAILABLE,"&7请使用 &f/xpay kit &7查看可用礼包。");
        config.set(KitConfig.LANG_KIT_UPDATE_PACKAGENAME,"&a礼包名称已更新为:");
        config.set(KitConfig.LANG_KIT_UPDATE_PACKAGE_NAME_ERROR,"&c更新礼包名称失败！");
        config.set(KitConfig.LANG_KIT_NEGATIVE_PRICE,"&c价格不能为负数！");
        save();
        return true;
    }
    /**
     * 重载礼包配置
     */
    public void reload() {
        if (configManager != null) {
            configManager.reloadConfig(KitConfig.LANG_FILE);
        }
    }
// ============================================
    // 内部辅助
    // ============================================

    private FileConfiguration getConfig() {
        return configManager != null ? configManager.getConfig(KitConfig.LANG_FILE) : null;
    }

    private void save() {
        if (configManager != null) {
            configManager.saveConfig(KitConfig.LANG_FILE);
        }
    }

}
