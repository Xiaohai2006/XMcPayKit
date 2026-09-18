package cn.xiaohai.xmcpaykit.util;

import cn.xiaohai.xmcpay.utility.TextUtil;
import org.bukkit.command.CommandSender;

/**
 * 玩家可见文案工具。
 *
 * <p>所有发往玩家的文本都通过这里输出，便于以后统一加前缀、支持 hex 颜色或接入多语言。
 */
public final class MessageUtil {

    private MessageUtil() {
    }

    /**
     * 转换颜色代码（&amp;a、&amp;c 等）
     */
    public static String color(String text) {
        if (text == null) {
            return "";
        }
        String colored = TextUtil.colorize(text);
        return colored != null ? colored : text;
    }

    /**
     * 给命令发送者发送一条带颜色的消息
     */
    public static void send(CommandSender sender, String text) {
        if (sender == null || text == null) {
            return;
        }
        sender.sendMessage(color(text));
    }
}
