package cn.xiaohai.xmcpaykit.command;

import cn.xiaohai.xmcpay.api.command.SubCommand;
import cn.xiaohai.xmcpaykit.data.KitDataManager;
import cn.xiaohai.xmcpaykit.data.KitEntry;
import cn.xiaohai.xmcpaykit.menu.admin.KitAdminMenu;
import cn.xiaohai.xmcpaykit.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理员命令：{@code /xpay kitadmin} 打开管理界面，或使用 create / delete / list / reload / clear 子命令。
 *
 * <p>需要权限 {@code xmcpaykit.admin}。
 */
public class KitAdminCommand implements SubCommand {

    private static final String PERMISSION = "xmcpaykit.admin";
    private static final List<String> SUB_COMMANDS =
            Arrays.asList("create", "delete", "reload", "list", "clear");

    private final KitDataManager dataManager = new KitDataManager();

    @Override
    public String getName() {
        return "kitadmin";
    }

    @Override
    public String getDescription() {
        return "打开礼包管理界面（需要管理员权限）";
    }

    @Override
    public String getUsage() {
        return "/xpay kitadmin";
    }

    @Override
    public boolean execute(CommandSender commandSender, Command command, String s, String[] args) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage("§c请在游戏内执行此命令！");
            return true;
        }

        Player player = (Player) commandSender;
        if (!player.hasPermission(PERMISSION)) {
            MessageUtil.send(player, "&c你没有权限执行此命令！");
            return true;
        }

        if (args.length > 0) {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "create":
                    return handleCreate(player, args);
                case "delete":
                    return handleDelete(player, args);
                case "reload":
                    return handleReload(player);
                case "list":
                    return handleList(player);
                case "clear":
                    return handleClear(player, args);
                default:
                    MessageUtil.send(player, "&c未知子命令！可用命令: create, delete, reload, list, clear");
                    return true;
            }
        }

        new KitAdminMenu(player, dataManager).open();
        return true;
    }

    @Override
    public List<String> getTabCompletions(CommandSender commandSender, String[] args) {
        if (args.length <= 1) {
            String prefix = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
            return SUB_COMMANDS.stream()
                    .filter(sub -> sub.startsWith(prefix))
                    .collect(Collectors.toList());
        }
        return Arrays.asList();
    }

    // ============================================
    // 子命令
    // ============================================

    /**
     * {@code /xpay kitadmin create <ID> <名称> <点券价格> <现金价格>}
     */
    private boolean handleCreate(Player player, String[] args) {
        if (args.length < 5) {
            MessageUtil.send(player, "&c用法: /xpay kitadmin create <ID> <名称> <点券价格> <现金价格>");
            return true;
        }

        String kitId = args[1];
        String name = args[2];
        int price;
        int money;

        try {
            price = Integer.parseInt(args[3]);
            money = Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            MessageUtil.send(player, "&c价格必须是数字！");
            return true;
        }

        if (dataManager.createKit(kitId, name, price, money)) {
            MessageUtil.send(player, "&a礼包创建成功！ID: " + kitId);
            MessageUtil.send(player, "&7名称: " + name + " | 点券价格: " + price + " | 现金价格: " + money);
            MessageUtil.send(player, "&7使用 /xpay kitadmin 打开管理界面编辑礼包物品与奖励命令");
            return true;
        }

        if (dataManager.hasKit(kitId)) {
            MessageUtil.send(player, "&c礼包ID '" + kitId + "' 已存在！");
            MessageUtil.send(player, "&7当前所有礼包ID: " + dataManager.getAllKitIds());
        } else {
            MessageUtil.send(player, "&c礼包创建失败，请检查控制台日志。");
        }
        return true;
    }

    /**
     * {@code /xpay kitadmin delete <ID>}
     */
    private boolean handleDelete(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.send(player, "&c用法: /xpay kitadmin delete <ID>");
            return true;
        }

        String kitId = args[1];
        if (dataManager.deleteKit(kitId)) {
            MessageUtil.send(player, "&a礼包删除成功！ID: " + kitId);
        } else {
            MessageUtil.send(player, "&c礼包删除失败！ID不存在。");
        }
        return true;
    }

    private boolean handleReload(Player player) {
        dataManager.reload();
        MessageUtil.send(player, "&a礼包配置已重载！");
        return true;
    }

    private boolean handleList(Player player) {
        List<KitEntry> entries = dataManager.getKitEntries();
        if (entries.isEmpty()) {
            MessageUtil.send(player, "&a当前没有任何礼包");
            return true;
        }

        MessageUtil.send(player, "&6========== 礼包列表 ==========");
        for (KitEntry entry : entries) {
            MessageUtil.send(player, String.format(
                    "&7ID: &f%s &7| 名称: &f%s &7| 点券: &f%d &7| 现金: &f%d &7| 物品: &f%d &7| 命令: &f%d",
                    entry.getId(), entry.getName(), entry.getPrice(), entry.getMoney(),
                    entry.getItemCount(), entry.getCommandCount()));
        }
        MessageUtil.send(player, "&6================================");
        return true;
    }

    /**
     * {@code /xpay kitadmin clear confirm}
     */
    private boolean handleClear(Player player, String[] args) {
        if (args.length < 2 || !args[1].equalsIgnoreCase("confirm")) {
            MessageUtil.send(player, "&c&l警告：此操作将删除所有礼包！");
            MessageUtil.send(player, "&c如果确定要清空，请使用: /xpay kitadmin clear confirm");
            return true;
        }

        Set<String> kitIds = dataManager.getAllKitIds();
        int count = 0;
        for (String kitId : kitIds) {
            if (dataManager.deleteKit(kitId)) {
                count++;
            }
        }

        MessageUtil.send(player, "&a已清空所有礼包！共删除 " + count + " 个礼包");
        return true;
    }
}
