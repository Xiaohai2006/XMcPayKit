# XMcPayKit

XMcPay 的礼包插件：可以用 GUI 可视化配置礼包（物品、价格、描述、奖励命令），
玩家通过 `/xpay kit` 用**点券**或**微信/支付宝**购买礼包，支付成功后由本插件发放奖励。

## 环境要求

| 项目 | 版本 |
|------|------|
| 服务端 | Spigot / Paper 1.12.2（`pom.xml` 使用 spigot-api 1.12.2） |
| Java | 8（`pom.xml` 中 `java.version=1.8`） |
| 依赖插件 | **XMcPay 1.11.2+**（`plugin.yml` 中为硬依赖 `depend`） |

## 目录结构

```
XMcPayKit/
├── pom.xml                       Maven 构建配置
├── lib/
│   └── XmcPay-Plugin-1.11.2.jar  编译期依赖（XMcPay API，system scope）
├── docs/                         文档
│   ├── QUICK_START.md            快速开始（管理员上手）
│   ├── KIT_MANAGEMENT_GUIDE.md   礼包管理完整说明
│   ├── TROUBLESHOOTING.md        问题排查
│   ├── API_USAGE.md              XMcPay API 使用文档
│   ├── PLUGIN_CONFIG_API.md      XMcPay 配置 API
│   └── SUBCOMMAND_AND_CONFIG_API.md  XMcPay 子命令/配置 API
└── src/main/
    ├── java/cn/xiaohai/xmcpaykit/
    │   ├── XMcPayKit.java              插件主类：生命周期 + 组件注册（不写业务）
    │   ├── command/                    /xpay 子命令
    │   │   ├── KitCommand.java         玩家命令
    │   │   └── KitAdminCommand.java    管理员命令
    │   ├── listener/                   事件监听
    │   │   ├── PaymentSuccessListener.java  支付成功 → 发放奖励
    │   │   └── ChatInputListener.java       聊天栏文本输入（管理GUI用）
    │   ├── menu/                       界面
    │   │   ├── KitMenu.java            玩家礼包菜单
    │   │   └── admin/                  管理界面
    │   │       ├── KitAdminMenu.java       礼包列表
    │   │       ├── KitEditorMenu.java      礼包编辑
    │   │       └── KitItemsEditorMenu.java 物品编辑
    │   ├── service/                    业务逻辑
    │   │   ├── KitPurchaseService.java 购买校验/点券购买/现金支付
    │   │   └── KitRewardService.java   奖励发放（物品 + 命令 + 次数）
    │   ├── data/                       数据层
    │   │   ├── KitDataManager.java     礼包配置读写
    │   │   └── KitEntry.java           礼包数据快照（菜单渲染用）
    │   ├── config/
    │   │   └── KitConfig.java          配置键/路径常量
    │   └── util/
    │       ├── MessageUtil.java        玩家文案/颜色
    │       └── InventoryUtil.java      背包空位与物品发放
    └── resources/
        └── plugin.yml
```

## 构建

```bash
mvn clean package
```

产物：`target/XMcPayKit-1.0-SNAPSHOT.jar`，放入服务端 `plugins/` 目录即可。

> `lib/XmcPay-Plugin-1.11.2.jar` 是编译期依赖（`system` scope），已随仓库提供，
> 因此不需要额外配置私服；打包产物**不会**包含 XMcPay 的类，运行时由服务端的 XMcPay 插件提供。

## 命令与权限

| 命令 | 说明 | 权限 |
|------|------|------|
| `/xpay kit` | 打开礼包购买菜单 | 无 |
| `/xpay kit <礼包名或ID>` | 直接对该礼包发起现金支付 | 无 |
| `/xpay kitadmin` | 打开礼包管理界面 | `xmcpaykit.admin` |
| `/xpay kitadmin create <ID> <名称> <点券价格> <现金价格>` | 创建礼包 | `xmcpaykit.admin` |
| `/xpay kitadmin delete <ID>` | 删除礼包 | `xmcpaykit.admin` |
| `/xpay kitadmin list` | 列出所有礼包 | `xmcpaykit.admin` |
| `/xpay kitadmin reload` | 重载礼包配置 | `xmcpaykit.admin` |
| `/xpay kitadmin clear confirm` | 清空所有礼包（危险） | `xmcpaykit.admin` |

## 配置文件

礼包数据由 XMcPay 的 `ConfigAPI` 托管，实际路径：

```
plugins/XMcPay/plugins/XMcPayKit/kits.yml   礼包数据
plugins/XMcPay/plugins/XMcPayKit/lang.yml   提示文案（可选，缺失时使用内置默认文案）
```

`kits.yml` 结构：

```yaml
kits:
  vip1:
    name: "VIP礼包"          # 显示名称
    price: 100               # 点券价格
    money: 50                # 现金价格（元）
    description:             # 描述（多行）
      - "这是一个VIP礼包"
    playerinv: 1             # 购买所需背包空位
    UseMoney: false          # true = 仅现金购买
    permission: ""           # 购买权限节点（空 = 不校验）
    movice: -1               # 购买次数上限，-1 = 不限
    command:                 # 支付成功后执行的命令
      - "give %player% diamond 10"
    items:                   # 礼包物品
      - ==: org.bukkit.inventory.ItemStack
        type: DIAMOND
        amount: 10
```

奖励命令支持变量：`%player%`、`%player_uuid%`、`%money%`、`%point%`、`%kit_id%`、`%kit_name%`。

## 关键流程

```
玩家点击礼包 / 输入 /xpay kit <名称>
        │
        ▼
KitPurchaseService.validate()         礼包存在？背包空位？权限？次数上限？
        │ OK
        ├── 仅现金购买(UseMoney) ──────────► openCashPayment() → XMcPay 支付界面
        └── 点券优先
              ├─ 点券足够 → purchaseWithPoints() → KitRewardService.grantKit() → 立即发奖
              └─ 点券不足 → 提示 + openCashPayment() → XMcPay 支付界面
                                                        │
                                     玩家扫码支付成功 ────┘
                                                        ▼
                        XMcPay 触发 PaymentSuccessEvent（sourcePlugin = XMcPayKit）
                                                        ▼
                        PaymentSuccessListener（订单去重 + 按 customData=kit:<ID> 识别礼包）
                                                        ▼
                        KitRewardService.grantKit()：发物品 → 执行奖励命令 → 记录次数 → 提示玩家
```

## 开发约定

1. **文案统一走 `MessageUtil`**，不要在业务代码里直接 `sendMessage("§a...")`，便于以后加前缀/多语言。
2. **配置键统一走 `KitConfig`**，不要在业务代码里写 `"kits.xxx.yyy"` 这类字符串。
3. **购买规则只写在 `KitPurchaseService`**，菜单和命令都调用它，避免两侧校验不一致。
4. **奖励发放只写在 `KitRewardService`**；由于奖励命令已由本插件执行，
   调用 `XMcPayAPI#openPayGui` 时命令列表必须传空（`KitPurchaseService` 已处理），否则命令会被执行两次。
5. **菜单点击使用「槽位 → 礼包ID/动作」映射**，不要再用物品名称或 lore 反查（名称可被玩家/管理员修改，容易失效）。
6. **主线程约束**：`AsyncPlayerChatEvent` 是异步的，涉及配置读写与 GUI 的操作必须回到主线程；
   奖励发放同理（`KitRewardService` 内部已自动调度）。
7. **物品发放使用 `InventoryUtil.giveItems`**：背包放不下时掉落在地上，避免奖励丢失。

## 文档索引

- [快速开始](docs/QUICK_START.md)
- [礼包管理说明](docs/KIT_MANAGEMENT_GUIDE.md)
- [问题排查](docs/TROUBLESHOOTING.md)
- [XMcPay API 使用](docs/API_USAGE.md)
- [XMcPay 配置 API](docs/PLUGIN_CONFIG_API.md)
- [XMcPay 子命令与配置 API](docs/SUBCOMMAND_AND_CONFIG_API.md)

## 更新日志

### v1.3
- ♻️ 项目结构整理：`event` → `listener`、`data` 拆出 `service`、管理菜单移入 `menu.admin`，
  新增 `config` / `util` 包
- ♻️ 新增 `KitPurchaseService`，统一购买校验（背包/权限/次数/点券余额）与现金支付入口
- ♻️ 新增 `KitEntry` 数据快照，菜单渲染不再反复读取配置
- ♻️ 菜单点击改为槽位映射，去掉基于物品名称/lore 的脆弱匹配
- ♻️ 主类只保留生命周期与组件注册，移除示例代码与示例命令 `/buypack`
- 🐛 修复“清空所有物品”按钮实际未清空配置的问题
- 📝 文档归入 `docs/`，新增本 README（结构说明 + 开发约定）

### v1.2
- 🐛 修复支付成功后不发放奖励/不执行命令的问题（支付成功监听器此前未注册）
- ✨ 管理 GUI 新增“编辑奖励命令”和“购买方式”
- 🐛 奖励命令改由本插件执行，并对订单去重，避免重复发奖
- 🐛 修复 `UseMoney` 读取路径错误、聊天输入异步写配置等问题

### v1.1
- ✨ 礼包 GUI 管理系统（创建、编辑、物品管理）
- ✨ 管理员命令与权限系统
