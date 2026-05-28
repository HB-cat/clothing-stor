# 服装店记账系统

> 毕业设计作品 · 面向小商户生态的进销存管理工具 · 绿色主题

## 📖 项目简介

一款面向小商户生态的安卓进销存管理应用。v1.4及之前版本为单用户离线版本，v1.5升级为多用户协同版本（局域网 + 公网），支持零售商、供应商、买家三种角色。

**真实用户驱动**：本项目以真实服装摊主为初始用户，从需求分析、交互设计到功能实现，全程基于实际经营场景进行定制开发。

### 🎯 用户画像

- **零售商**：个体服装摊主，非技术背景
- **供应商**：上游供货商
- **买家**：普通顾客

## ✨ 核心功能

| 模块 | 功能 |
| :--- | :--- |
| 🏠 **首页** | 店铺名称动态显示、快捷指令记账、分类列表、底部导航 |
| 📦 **商品管理** | 添加商品（拍照+动态属性标签）、商品列表、点击销售 |
| 🏷️ **属性管理** | 属性组增删、属性值批量添加/单独删除、二次确认 |
| 📂 **分类管理** | 动态增删分类、预置默认分类、二次确认 |
| 💰 **销售记账** | 点击记账（改价）、快捷指令（规则解析+确认框） |
| 📥 **进货入库** | 多属性组合、逐行填写、实时计算总价、二次确认 |
| 📊 **账单查询** | 日/月/年切换、日历+滚轮、默认隐藏、左滑退款 |
| 📈 **经营分析** | 今日快报四卡片、趋势折线图、热销柱状图、滞销/补货弹窗、时段柱状图 |
| 📦 **库存查看** | 实时计算、低库存预警（≤5件红色） |
| 👥 **多角色协同** | 零售商/供应商/买家三种角色，数据隔离，订单流转 |
| 💾 **数据管理** | 文件选择器导出/恢复、自动备份、一键初始化 |
| 🔊 **音效反馈** | 9种操作音效 + 全局点击音效 + 声音开关 |
| 🎨 **视觉风格** | 绿色渐变背景、28dp大圆角按钮、胶囊导航、沉浸式状态栏 |

## 🛠️ 技术栈

| 层级 | v1.4及之前 | v1.5.1 局域网 |
| :--- | :--- | :--- |
| **开发语言** | Java | Java |
| **UI框架** | Android XML + Drawable | 同左 |
| **数据库** | Room（SQLite） | MySQL 8.0 |
| **后端** | 无 | Spring Boot |
| **网络库** | 无 | Retrofit 2.9.0 + Gson |
| **图表库** | MPAndroidChart v3.1.0 | 同左 |
| **架构模式** | 多Activity + BaseActivity | 同左 + IDataProvider |
| **最低SDK** | Android 11（API 30） | 同左 |
| **网络依赖** | 零 | 局域网WiFi |

### 数据库设计

| 表名 | 说明 |
| :--- | :--- |
| `products` | 商品信息（图片路径、进价、售价） |
| `categories` | 商品分类 |
| `attribute_groups` | 属性组 |
| `attribute_values` | 属性值 |
| `product_attributes` | 商品-属性关联 |
| `sales` | 销售记录（含退款） |
| `purchases` | 进货记录 |
| `users` | 用户表（v1.5新增） |
| `orders` | 订单表（v1.5新增） |

## 📁 项目结构

clothing-store/ # 安卓端
├── app/src/main/java/com/heben/clothingstore/
│ ├── MainActivity.java # 首页
│ ├── AddProductActivity.java # 添加商品
│ ├── ViewProductsActivity.java # 商品列表/销售
│ ├── CategoriesActivity.java # 分类管理
│ ├── AttributeGroupsActivity.java # 属性组管理
│ ├── AttributeValuesActivity.java # 属性值管理
│ ├── PurchaseActivity.java # 进货入库
│ ├── BillActivity.java # 账单查询/退款
│ ├── InventoryActivity.java # 库存查看
│ ├── AnalysisActivity.java # 经营分析
│ ├── ChartLandscapeActivity.java # 横屏图表
│ ├── MyActivity.java # 我的/设置
│ ├── GuideActivity.java # 使用指南
│ ├── LoginActivity.java # 登录/角色选择
│ ├── SupplierActivity.java # 供应商主页
│ ├── BuyerActivity.java # 买家主页
│ ├── BaseActivity.java # 基类
│ ├── CommandParser.java # 指令解析
│ ├── BusinessAdvisor.java # 规则引擎
│ ├── DatabaseInitializer.java # 数据库初始化
│ ├── AutoBackupHelper.java # 自动备份
│ ├── MediaSoundHelper.java # 音效管理
│ ├── CrashHandler.java # 崩溃捕获
│ ├── TestRunner.java # 内置测试
│ ├── IDataProvider.java # 数据提供者接口
│ ├── LocalDataProvider.java # Room实现
│ ├── CloudDataProvider.java # Retrofit实现
│ ├── DataProviderFactory.java # 工厂类
│ ├── ApiService.java # Retrofit接口
│ ├── entity/ # 实体类
│ ├── dao/ # DAO接口
│ └── database/AppDatabase.java # 数据库管理器

clothing-server/ # 后端（v1.5）
├── src/main/java/com/heben/clothingserver/
│ ├── controller/
│ ├── service/
│ ├── mapper/
│ ├── entity/
│ └── config/
└── pom.xml

## 🚀 使用说明

### 安装方式
1. Android Studio 打开项目
2. Build → Build APK
3. 安装 `app-debug.apk` 到设备
4. 首次启动自动初始化预置数据

### 基本操作（零售商）
- **添加商品**：我的 → 添加商品
- **进货入库**：进货 → 选择商品 → 填写数量进价
- **日常销售**：首页点分类 → 点商品 → 确认卖出
- **看账**：账单 → 选择日期 → 查询
- **经营分析**：我的 → 经营分析

### 多角色协同（v1.5）
- **买家**：登录 → 浏览商品 → 下单
- **零售商**：登录 → 查看订单 → 确认发货 → 自动扣库存
- **供应商**：登录 → 查看库存 → 提交补货建议

### 快捷指令
- `卖了连衣裙一件90块` → 弹出确认框 → 记账
- `进了连衣裙5件60块` → 进货
- `退了连衣裙一件` → 退货

### 数据管理
- **导出**：我的 → 导出备份 → 文件选择器选择位置
- **恢复**：我的 → 恢复备份 → 授权码 `0831` → 选择文件
- **初始化**：我的 → 初始化系统 → 授权码 `0831`

## 🎨 视觉风格

| 元素 | 样式 |
| :--- | :--- |
| **主题色** | 稳重绿 #43A047 |
| **背景** | 绿色渐变 |
| **按钮** | 28dp大圆角，绿色实心白字 |
| **卡片** | 16dp圆角，半透明白色 |
| **导航** | 白色胶囊，选中绿底白字 |

## ⚠️ 注意事项

- v1.4及之前版本为纯离线，v1.5.1需要局域网WiFi
- 建议定期导出备份
- 删除操作均有二次确认
- 进价录入后不可修改
- 账单数据默认隐藏
- 恢复备份和初始化需授权码 `0831`

## 👤 作者

**HenBen_Cat**
计算机科学与技术专业 · 毕业设计作品

---

> 本项目遵循开源协议。仅供学习交流使用。