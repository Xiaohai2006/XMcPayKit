package cn.xiaohai.xmcpaykit.config;

/**
 * 配置常量集中管理。
 *
 * <p>礼包数据与语言文件由 XMcPay 的 {@code ConfigAPI} 统一托管，
 * 实际文件位置：
 * <pre>
 * plugins/XMcPay/plugins/XMcPayKit/kits.yml
 * plugins/XMcPay/plugins/XMcPayKit/lang.yml
 * </pre>
 *
 * <p>请勿在业务代码里直接写配置路径字符串，统一使用本类的方法拼接，
 * 以后调整配置结构时只需要改这一个文件。
 */
public final class KitConfig {

    private KitConfig() {
    }

    /** 本插件在 XMcPay 配置系统中的插件名（同时是插件名） */
    public static final String PLUGIN_NAME = "XMcPayKit";

    /** 礼包数据配置文件名（kits.yml） */
    public static final String KIT_FILE = "kits";

    /** 语言配置文件名（lang.yml） */
    public static final String LANG_FILE = "lang";

    /** 礼包根节点 */
    public static final String KITS_ROOT = "kits";

    /**
     * 玩家数据根节点（当前用于存点券余额占位实现）
     */
    public static final String PLAYERS_ROOT = "players";

    /**
     * 玩家余额字段
     */
    public static final String FIELD_BALANCE = "balance";

    // ==================== 礼包字段名 ====================

    /** 显示名称 */
    public static final String FIELD_NAME = "name";
    /** 点券价格 */
    public static final String FIELD_PRICE = "price";
    /** 现金价格（元） */
    public static final String FIELD_MONEY = "money";
    /** 描述（多行） */
    public static final String FIELD_DESCRIPTION = "description";
    /** 购买所需背包空位 */
    public static final String FIELD_PLAYER_INV = "playerinv";
    /** 是否仅现金购买 */
    public static final String FIELD_CASH_ONLY = "UseMoney";
    /** 购买权限节点 */
    public static final String FIELD_PERMISSION = "permission";
    /** 购买次数上限（-1 表示不限） */
    public static final String FIELD_PURCHASE_LIMIT = "movice";
    /** 奖励命令列表 */
    public static final String FIELD_COMMAND = "command";
    /** 礼包物品列表 */
    public static final String FIELD_ITEMS = "items";

    // ==================== 语言配置键 ====================

    public static final String LANG_PAY_SUCCEED = "pay.succeed";
    public static final String LANG_PAY_BALANCE = "pay.balance";
    public static final String LANG_PAY_PLAYER_INV = "pay.player-inv";
    public static final String LANG_PAY_UNAVAILABLE = "pay.unavailable";
    public static final String LANG_KIT_NO_PERMISSION = "pay.no-permission";
    public static final String LANG_KIT_PURCHASE_LIMIT = "pay.purchase-limit";
    public static final String LANG_KIT_NOT_FOUND = "pay.not-found";
    public static final String LANG_KIT_INVALID_PRICE = "pay.invalid-price";
    public static final String LANG_KIT_MANAGEMENT_INTERFACE = "pay.management-interface";
    public static final String LANG_KIT_EXECUTE_SUCCEED = "pay.execute_succeed";
    public static final String LANG_KIT_EXECUTE_ERROR = "pay.execute_error";
    public static final String LANG_KIT_EXECUTE_UNKNOWN="pay.execute_unknown";
    public static final String LANG_KIT_USAGE="pay.execute_usage";
    public static final String LANG_KIT_DESCRIPTION="pay.execute_description";
    public static final String LANG_KIT_NOTFOUND="pay.execute_notfound";
    public static final String LANG_KIT_VIEW_AVAILABLE="pay.execute_view-available";
    public static final String LANG_KIT_UPDATE_PACKAGENAME="pay.execute_update-package-name";
    public static final String LANG_KIT_UPDATE_PACKAGE_NAME_ERROR="pay.execute_update-package-name-error";
    public static final String LANG_KIT_NEGATIVE_PRICE="pay.execute_negative-price";
    /**
     * 礼包根节点路径，例如 {@code kits.name}
     */
    public static String kits(String field) {
        return KITS_ROOT + "." + field;
    }

    /**
     * 某个礼包的配置路径，例如 {@code kits.vip1}
     */
    public static String kit(String kitId) {
        return KITS_ROOT + "." + kitId;
    }

    /**
     * 某个礼包字段的配置路径，例如 {@code kits.vip1.price}
     */
    public static String kit(String kitId, String field) {
        return kit(kitId) + "." + field;
    }

    /**
     * 玩家余额配置路径，例如 {@code players.&lt;uuid&gt;.balance}
     */
    public static String balance(String playerUuid) {
        return PLAYERS_ROOT + "." + playerUuid + "." + FIELD_BALANCE;
    }
}
