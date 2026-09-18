package cn.xiaohai.xmcpaykit.command;

import cn.xiaohai.xmcpay.api.command.SubCommand;
import cn.xiaohai.xmcpay.utility.TextUtil;
import cn.xiaohai.xmcpaykit.data.KitDataManager;
import cn.xiaohai.xmcpaykit.data.KitEntry;
import cn.xiaohai.xmcpaykit.data.LanguageEntry;
import cn.xiaohai.xmcpaykit.menu.KitMenu;
import cn.xiaohai.xmcpaykit.service.KitPurchaseService;
import cn.xiaohai.xmcpaykit.util.MessageUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 玩家命令：{@code /xpay kit} 打开礼包菜单，{@code /xpay kit <礼包名或ID>} 直接发起现金支付。
 */
public class KitCommand implements SubCommand {

    private final KitDataManager dataManager = new KitDataManager();
    private final KitPurchaseService purchaseService = new KitPurchaseService(dataManager);

    @Override
    public String getName() {
        return "kit";
    }

    @Override
    public String getDescription() {
        return LanguageEntry.getDescription();
    }

    @Override
    public String getUsage() {
        return "/xpay kit | /xpay kit [礼包名称]";
    }

    @Override
    public boolean execute(CommandSender commandSender, Command command, String s, String[] args) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage("§c请在游戏内执行此命令！");
            return true;
        }

        Player player = (Player) commandSender;

        if (args.length == 0) {
            new KitMenu(player).open();
            return true;
        }

        String query = String.join(" ", args).trim();
        String kitId = findKitId(query);
        if (kitId == null) {
            MessageUtil.send(player, LanguageEntry.getNotFound() + query);
            MessageUtil.send(player, LanguageEntry.getViewAvailable());
            return true;
        }

        // 直接按名称购买时走现金支付
        purchaseService.notifyIfFailed(player, purchaseService.openCashPayment(player, kitId));
        return true;
    }

    @Override
    public List<String> getTabCompletions(CommandSender commandSender, String[] args) {
        if (!(commandSender instanceof Player)) {
            return Collections.emptyList();
        }

        String prefix = String.join(" ", args).trim().toLowerCase(Locale.ROOT);
        Set<String> suggestions = new LinkedHashSet<>();

        for (KitEntry entry : dataManager.getKitEntries()) {
            String kitName = stripColors(entry.getName());
            if (kitName.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                suggestions.add(kitName);
            }
            if (entry.getId().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                suggestions.add(entry.getId());
            }
        }
        return new ArrayList<>(suggestions);
    }

    /**
     * 按礼包ID或礼包名称查找（ID 优先，避免名称与其他礼包ID重名时选错）
     */
    private String findKitId(String query) {
        Set<String> kitIds = dataManager.getAllKitIds();

        for (String kitId : kitIds) {
            if (kitId.equalsIgnoreCase(query)) {
                return kitId;
            }
        }

        String normalizedQuery = stripColors(query);
        for (String kitId : kitIds) {
            if (stripColors(dataManager.getKitName(kitId)).equalsIgnoreCase(normalizedQuery)) {
                return kitId;
            }
        }
        return null;
    }

    private String stripColors(String value) {
        if (value == null) {
            return "";
        }

        String colored = TextUtil.colorize(value);
        String stripped = ChatColor.stripColor(colored != null ? colored : value);
        return stripped == null ? "" : stripped.trim();
    }
}
