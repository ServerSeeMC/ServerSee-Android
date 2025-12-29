# ServerSee-Android

ServerSee 客户端是一个用于监控和管理 Minecraft 服务器的 Android 应用。它配合 [ServerSee-Plugin](https://github.com/ServerSeeMC/ServerSee-Plugin) 使用，提供极速的实时监控和远程管理体验。

## 主要功能

- **极速同步 (WebSocket)**: 全量接入 WebSocket 通信协议，实现毫秒级的性能数据刷新和日志推送。
- **现代化控制台终端**:
    - **实时日志流**: 基于 Log4j2 捕获，实时同步服务器控制台全量输出。
    - **自动滚动控制**: 支持开启/关闭自动滚动，方便回溯历史日志。
    - **全彩解析**: 支持 Minecraft 颜色代码 (`§` 和 `&`) 解析。
    - **指令执行**: 快速发送控制台命令并即时查看结果。
- **多模式监控**:
    - **API 模式**: 配合插件使用，支持 CPU、内存、TPS 历史趋势、白名单和终端管理。
    - **地址模式 (JAVA/BE)**: 支持普通服务器，查看在线人数、版本及 MOTD。
- **精美 UI 设计**:
    - **仪表盘**: 首页展示服务器头像、离线统计和性能卡片。
    - **详情页**: 渐变背景、状态色彩指示器、硬件占用进度条。
    - **历史趋势**: 基于 Canvas 绘制的 TPS 波动曲线图。
- **Material 3 & 动态色彩**: 适配 Android 12+ 系统的动态取色功能。

## 技术栈

- **Jetpack Compose**: 声明式 UI 架构。
- **WebSocket**: 实现高性能长连接通信。
- **Room**: 配置与数据的本地持久化。
- **Coil**: 支持 Base64 及第三方服务 (mcsrvstat.us) 的图标加载。
- **MinecraftTextParser**: 自研文本解析器，支持富文本日志展示。

## 开发者信息

- **版本**: 1.0.0-beta.2
- **组织**: [ServerSeeMC](https://github.com/ServerSeeMC)
- **仓库**: [ServerSee-Android](https://github.com/ServerSeeMC/ServerSee-Android)
