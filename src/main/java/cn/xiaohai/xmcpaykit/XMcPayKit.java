package cn.xiaohai.xmcpaykit;

import cn.xiaohai.xmcpay.api.XMcPayAPI;
import cn.xiaohai.xmcpaykit.command.KitAdminCommand;
import cn.xiaohai.xmcpaykit.command.KitCommand;
import cn.xiaohai.xmcpaykit.command.LanguageAdminCommand;
import cn.xiaohai.xmcpaykit.listener.ChatInputListener;
import cn.xiaohai.xmcpaykit.listener.PaymentSuccessListener;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * XMcPayKit 插件主类。
 *
 * <p>只负责生命周期与组件注册，具体实现分布在：
 * <ul>
 *     <li>{@code command} 子命令（{@code /xpay kit}、{@code /xpay kitadmin}）</li>
 *     <li>{@code listener} 事件监听（支付成功发放奖励、聊天栏输入）</li>
 *     <li>{@code menu} / {@code menu.admin} 玩家菜单与管理菜单</li>
 *     <li>{@code service} 业务逻辑（购买校验、奖励发放）</li>
 *     <li>{@code data} 礼包配置读写</li>
 *     <li>{@code config} / {@code util} 常量与工具</li>
 * </ul>
 */
public final class XMcPayKit extends JavaPlugin {

    private static XMcPayKit instance;
    private static ChatInputListener chatInputListener;

    /** 支付成功监听器是否注册成功（决定奖励命令由谁来执行） */
    private static boolean paymentListenerReady = false;

    private XMcPayAPI api;

    public static XMcPayKit getInstance() {
        return instance;
    }

    public static ChatInputListener getChatInputListener() {
        return chatInputListener;
    }

    public static XMcPayAPI getApi() {
        return instance != null ? instance.api : null;
    }

    /**
     * 支付成功监听器是否可用。
     *
     * <p>可用时奖励命令由本插件在支付成功后执行，传给 XMcPay 的命令列表必须为空；
     * 不可用时（XMcPay 版本过旧）回退为把命令列表交给 XMcPay 执行。
     */
    public static boolean isPaymentListenerReady() {
        return paymentListenerReady;
    }

    @Override
    public void onEnable() {
        instance = this;

        // 依赖检查：XMcPay 是硬依赖，未安装时礼包功能整体不可用
        if (!XMcPayAPI.isAvailable()) {
            getLogger().warning("未检测到 XMcPay 插件，礼包功能不可用！");
            return;
        }

        api = new XMcPayAPI(this);

        registerListeners();
        registerSubCommands();

        getLogger().info("XMcPayKit 已启用（依赖 XMcPay 正常）。");
    }

    @Override
    public void onDisable() {
        chatInputListener = null;
        paymentListenerReady = false;
        instance = null;
    }

    // ============================================
    // 组件注册
    // ============================================

    private void registerListeners() {
        // 支付成功监听器：发放礼包奖励（物品 + 奖励命令 + 购买次数）
        try {
            getServer().getPluginManager().registerEvents(new PaymentSuccessListener(this), this);
            paymentListenerReady = true;
            getLogger().info("支付成功监听器已注册（支付成功后由本插件发放奖励）");
        } catch (Throwable t) {
            paymentListenerReady = false;
            getLogger().warning("支付成功监听器注册失败，奖励命令将回退由 XMcPay 执行: " + t);
        }

        // 聊天输入监听器：管理 GUI 的文本输入（名称/价格/描述/奖励命令）
        chatInputListener = new ChatInputListener(this);
        getServer().getPluginManager().registerEvents(chatInputListener, this);
    }

    private void registerSubCommands() {
        boolean kitRegistered = api.registerSubCommand(new KitCommand());
        boolean adminRegistered = api.registerSubCommand(new KitAdminCommand());
        boolean langRegistered= api.registerSubCommand(new LanguageAdminCommand());
        if (kitRegistered && adminRegistered && langRegistered) {
            getLogger().info("子命令注册成功: /xpay kit, /xpay kitadmin,/xpay langadmin");
        } else {
            getLogger().warning("部分子命令注册失败（kit=" + kitRegistered
                    + ", kitadmin=" + adminRegistered + ",langadmin"+langRegistered+"）");
        }
    }
}
