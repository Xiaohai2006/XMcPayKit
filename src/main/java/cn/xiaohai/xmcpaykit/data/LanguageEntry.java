package cn.xiaohai.xmcpaykit.data;

public class LanguageEntry {


    private static String succeed;
    private static String balance;
    private static String playerInv;
    private static String unavailable;
    private static String noPermission;
    private static String purchase_limit;
    private static String notFound;
    private static String invalidPrice;
    private static String managementInterface;
    private static String executeSucceed;
    private static String executeError;
    private static String executeUnknown;
    private static String usage;
    private static String description;
    private static String viewAvailable;
    private static String updatePackageName;
    private static String updatePackageNameError;
    private static String negativePrice;

    public static void setUpdatePackageName(String updatePackageName) {
        LanguageEntry.updatePackageName = updatePackageName;
    }

    public static void setUpdatePackageNameError(String updatePackageNameError) {
        LanguageEntry.updatePackageNameError = updatePackageNameError;
    }

    public static void setNegativePrice(String negativePrice) {
        LanguageEntry.negativePrice = negativePrice;
    }

    public static void setViewAvailable(String viewAvailable) {
        LanguageEntry.viewAvailable = viewAvailable;
    }

    public static String getNegativePrice() {
        return negativePrice;
    }

    public static String getUpdatePackageNameError() {
        return updatePackageNameError;
    }

    public static String getUpdatePackageName() {
        return updatePackageName;
    }

    public static String getViewAvailable() {

        return viewAvailable;
    }

    public static String getDescription() {
        return description;
    }

    public static void setDescription(String description) {
        LanguageEntry.description = description;
    }

    public static String getUsage() {
        return usage;
    }

    public static void setUsage(String usage) {
        LanguageEntry.usage = usage;
    }

    public static String getExecuteUnknown() {
        return executeUnknown;
    }

    public static void setExecuteUnknown(String executeUnknown) {
        LanguageEntry.executeUnknown = executeUnknown;
    }

    public static String getExecuteError() {
        return executeError;
    }

    public static void setExecuteError(String executeError) {
        LanguageEntry.executeError = executeError;
    }

    public static String getExecuteSucceed() {
        return executeSucceed;
    }

    public static void setExecuteSucceed(String execute) {
        LanguageEntry.executeSucceed = execute;
    }

    public static String getManagementInterface() {
        return managementInterface;
    }

    public static void setManagementInterface(String managementInterface) {
        LanguageEntry.managementInterface = managementInterface;
    }

    public static String getBalance() {
        return balance;
    }

    public static void setBalance(String balance) {
        LanguageEntry.balance = balance;
    }

    public static String getPlayerInv() {
        return playerInv;
    }

    public static void setPlayerInv(String playerInv) {
        LanguageEntry.playerInv = playerInv;
    }

    public static String getUnavailable() {
        return unavailable;
    }

    public static void setUnavailable(String unavailable) {
        LanguageEntry.unavailable = unavailable;
    }

    public static String getNoPermission() {
        return noPermission;
    }

    public static void setNoPermission(String noPermission) {
        LanguageEntry.noPermission = noPermission;
    }

    public static String getPurchase_limit() {
        return purchase_limit;
    }

    public static void setPurchase_limit(String purchase_limit) {
        LanguageEntry.purchase_limit = purchase_limit;
    }

    public static String getNotFound() {
        return notFound;
    }

    public static void setNotFound(String notFound) {
        LanguageEntry.notFound = notFound;
    }

    public static String getInvalidPrice() {
        return invalidPrice;
    }

    public static void setInvalidPrice(String invalidPrice) {
        LanguageEntry.invalidPrice = invalidPrice;
    }

    public static String getSucceed() {
        return succeed;
    }

    public static void setSucceed(String succeed) {
        LanguageEntry.succeed = succeed;
    }
}
