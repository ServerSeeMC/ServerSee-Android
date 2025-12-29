# ServerSee-Android

ServerSee 客户端是一个用于监控和管理 Minecraft 服务器的 Android 应用。它配合 [ServerSee-Plugin](https://github.com/ServerSeeMC/ServerSee-Plugin) 使用，提供直观的 UI 界面来查看服务器性能指标并执行管理操作。

## 主要功能

- **多服务器管理**: 支持添加多个服务器端点。
- **实时监控卡片**: 首页展示服务器头像、在线人数、TPS、CPU 和内存使用率。
- **现代化详情页**: 
    - 渐变背景与卡片式布局。
    - TPS/MSPT 状态色彩指示。
    - 内存与磁盘占用进度条。
- **管理功能 (需 Token)**:
    - **白名单管理**: 查看、添加和移除白名单玩家。
    - **终端控制**: 直接向服务器发送控制台指令。
- **自动获取头像**: 支持通过服务器地址自动获取图标 (由 api.mcsrvstat.us 提供支持)。
- **动态色彩**: 支持 Android 12+ 的 Material You 动态色彩。

## 屏幕截图

*(此处可添加应用截图)*

## 技术栈

- **Jetpack Compose**: 声明式 UI 构建。
- **Material 3**: 现代化的设计语言。
- **Room**: 本地数据库持久化。
- **Retrofit & OkHttp**: 网络请求。
- **Coil**: 图片异步加载。
- **Navigation Compose**: 应用内导航。

## 开发者信息

- **组织**: [ServerSeeMC](https://github.com/ServerSeeMC)
- **仓库**: [ServerSee-Android](https://github.com/ServerSeeMC/ServerSee-Android)
