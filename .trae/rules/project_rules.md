# 项目规则 - ServerSee APK

## 基础要求
- 操作系统: Windows Server 2022
- 终端: PowerShell
- 语言: 简体中文 (无 emoji)
- Maven 路径: `E:\apache-maven-3.9.12-bin\apache-maven-3.9.12\bin` (需设置环境变量)
- JAVA 路径: `E:\jdk21` (JDK 21) 或 `E:\jdk` (JDK 25)
- 插件端目录: `E:\project\serversee`

## 功能设计要求
- **首页**: 标题 "ServerSee 客户端"
- **添加服务器**: 右下角 FAB 按钮，弹窗包含：
    - API 端点 (必填)
    - Token (选填)
- **展示方式**: 首页横向显示卡片，包含：
    - 服务器头像 (Avatar)
    - 内存 (Memory)
    - CPU 使用率
    - 实时 TPS
    - 玩家数量
- **详情页**: 点击卡片进入
    - 底部栏：信息查看、白名单管理、指令执行
    - **UI 风格**: 现代化卡片式设计，包含渐变背景、圆角卡片、图标辅助展示，以及性能指标的色彩状态提示（如 TPS 正常显示绿色，过低显示红色）。
    - **权限控制**: 若无 Token，不显示白名单管理和指令执行，也不显示底部栏。

## 知识点记录
- **API 鉴权**: 管理员接口需使用 `Authorization: Bearer <Token>` 请求头。
- **数据刷新**: 首页采用 5 秒轮询机制更新各服务器的 TPS、CPU 和内存状态。
- **权限隔离**: 详情页根据 `token` 是否为空动态切换 UI 布局，隐藏敏感操作入口。
- **主题管理**: 支持系统自适应主题（深色/浅色模式），并集成了 Android 12+ 的动态色彩 (Dynamic Color) 功能。
- **持久化**: 使用 Room 存储服务器配置，确保应用重启后数据不丢失。
- **Gradle 配置**: 必须启用 `android.useAndroidX` 和 `android.enableJetifier` 以支持现代库。
- **Compose 实验性 API**: 使用 `basicMarquee` 或 `CenterAlignedTopAppBar` 等实验性组件时，需添加 `@OptIn` 注解及对应库引用。
- **图标资源**: 已根据 `ai_studio_code.txt` 中的 SVG 设计实现了 Material 3 风格的自适应图标 (Adaptive Icon)。包含背景层 (`#E0F2F1`) 和前景层 (深青色条纹与高亮装饰点)，并适配了 API 26+ 的自适应特性及旧版本的兼容显示。
- **资源引用**: `AndroidManifest.xml` 中引用的图标或主题必须在 `res` 目录中真实存在，否则会导致 AAPT 编译错误。目前已配置 `@mipmap/ic_launcher`。
- **明文传输**: Android 9+ 默认禁止 HTTP 明文传输。需在 `res/xml` 创建 `network_security_config.xml` 允许明文传输，并在 `AndroidManifest.xml` 的 `application` 标签中通过 `android:networkSecurityConfig` 引用。
- **403 错误处理**: 若 API 返回 403 Forbidden，通常是由于缺少 `User-Agent` 或服务器配置了强制鉴权。已在 `ServerRepository` 中通过 `OkHttpClient` 拦截器统一添加 `User-Agent`，并确保所有请求（包括状态查询）在有 Token 时都会携带 `Authorization` 头。
- **添加服务器优化**: 在添加服务器弹窗中集成了“获取服务器信息并测试”功能。支持实时预览服务器图标 (Avatar) 和 MOTD（支持颜色解析）。增加“自动使用 MOTD 作为名称”选项，并支持可选的“服务器连接地址”存储，简化添加流程。
- **UI 适配**: 首页和卡片组件已全面适配 Material 3 动态颜色和系统主题（深色/浅色模式），确保在不同主题下均有良好的视觉表现。
- **数据库升级**: 引入了 Room 数据库版本升级机制 (v1 -> v2 -> v3)，并配置了破坏性迁移以支持新字段的添加。
- **系统指标采集**: 后端插件引入了 `oshi-core` 库以采集物理主机的内存和磁盘使用情况。由于 Minecraft 插件环境的特殊性，通过 `maven-shade-plugin` 对 `oshi` 进行重定向 (Relocation) 以避免类加载冲突。注意：**不可对 JNA (com.sun.jna) 进行重定向**，否则会导致 Native 库加载失败。
- **UI 增强**: 详情页引入了 `LinearProgressIndicator` 展示 CPU、内存和磁盘的百分比进度，并支持垂直滚动以适配更多指标。
- **数据精度**: 统一了后端指标的单位（内存 MB，磁盘 GB，CPU 0-100 百分比），并在 Android 端实现了自动单位转换逻辑。注意：Spark API 返回的 CPU 原始值为 0-1 的小数，后端需乘以 100 转换为百分比，以便客户端使用 `%.0f%%` 格式化。
- **服务器图标**: 后端支持读取 `server-icon.png` 并以 Base64 编码形式通过 API 返回，客户端可直接解析并展示服务器图标。同时支持通过“根据服务器地址获取头像”选项，利用第三方服务 (api.mcsrvstat.us) 获取服务器图标。
- **详情页现代化**: 详情页采用 Material 3 规范进行了现代化重构，增加了渐变背景、统计卡片、色彩状态指示器（TPS/MSPT）以及更直观的白名单和终端管理界面。
