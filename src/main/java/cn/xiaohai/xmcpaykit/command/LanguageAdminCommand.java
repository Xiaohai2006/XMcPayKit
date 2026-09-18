package cn.xiaohai.xmcpaykit.command;

import cn.xiaohai.xmcpay.api.command.SubCommand;
import cn.xiaohai.xmcpaykit.data.KitDataManager;
import cn.xiaohai.xmcpaykit.data.LanguageDataManager;
import cn.xiaohai.xmcpaykit.data.LanguageEntry;
import cn.xiaohai.xmcpaykit.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class LanguageAdminCommand implements SubCommand {
    private static final String PERMISSION = "xmcpaykit.admin";
    private static final List<String> SUB_COMMANDS =
            Arrays.asList("build");
    private final LanguageDataManager dataManager = new LanguageDataManager();
    @Override
    public String getName() {
        return "langadmin";
    }

    @Override
    public String getDescription() {
        return "加载语言配置文件(需要管理员权限)";
    }

    @Override
    public String getUsage() {
        return "/xpay langadmin";
    }

    @Override
    public boolean execute(CommandSender commandSender, Command command, String s, String[] args) {
        if(!(commandSender instanceof Player)){
            commandSender.sendMessage(LanguageEntry.getExecuteSucceed());
            return true;
        }
        Player player= (Player) commandSender;
        if (!player.hasPermission(PERMISSION)){
            MessageUtil.send(player, LanguageEntry.getExecuteError());
            return true;
        }
        if (args.length > 0) {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "build":
                    return handleCreate(player, args);
                default:
                    MessageUtil.send(player, LanguageEntry.getExecuteUnknown());
                    return true;
            }
        }

        return false;
    }

    @Override
    public List<String> getTabCompletions(CommandSender commandSender, String[] args) {
        if (args.length<=1){
            String prefix=args.length==0?"":args[0].toLowerCase(Locale.ROOT);
            return SUB_COMMANDS.stream()
                    .filter(sub -> sub.startsWith(prefix))
                    .collect(Collectors.toList());
        }
        return Arrays.asList();
    }
    /**
     * {@code /xpay langadmin build}
     */
    private boolean handleCreate(Player player, String[] args) {
        if (args.length < 1) {
            MessageUtil.send(player, "&c用法:/xpay langadmin build");
            return true;
        }

        String kitId = args[0];
        if (dataManager.createKit(kitId)){
            MessageUtil.send(player, "&a语言配置构建成功");
            return true;
        }
        return true;
    }
}
