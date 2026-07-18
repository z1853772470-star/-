# 连点器

Android 自动连点器，支持多任务、多按钮、可拖动悬浮球、参数自定义。

## 功能特性

- 创建多个任务，每个任务可包含多个点击按钮
- 每个按钮可独立配置：点击次数、点击时长、点击间隔
- 橙色悬浮球显示在屏幕任意位置，可随意拖动
- 位置实时保存，再次打开自动恢复
- 一键启动/停止连点
- 所有数据本地持久化，重启不丢失

## 使用前准备

### 方法一：Android Studio 编译（推荐）

1. 安装 Android Studio (https://developer.android.com/studio)
2. 打开此项目文件夹 (auto-clicker)
3. Android Studio 会自动下载 Gradle 和依赖
4. 连接手机或启动模拟器
5. 点击 Run 按钮

### 方法二：GitHub Actions 自动构建

1. 将项目推送到 GitHub 仓库
2. 进入仓库 Actions 页面
3. 选择 Build Android APK 工作流
4. 点击 Run workflow，等待构建完成
5. 下载生成的 APK 文件

## 首次使用授权

应用需要两个权限：

1. 悬浮窗权限 - 点击顶部悬浮球图标，系统会引导开启
2. 无障碍服务权限 - 启动时自动跳转到无障碍设置页，找到连点器并开启

## 使用说明

1. 点击右下角 + 创建新任务
2. 选中任务后点击添加按钮创建点击点
3. 点击按钮卡片编辑参数（点击次数、时长、间隔）
4. 点击顶部悬浮球图标启动悬浮模式
5. 橙色圆形按钮会出现在屏幕上，可拖到任意位置
6. 点击播放按钮开始自动连点

## 技术架构

Kotlin + Jetpack Compose - 现代 Android UI
Room (SQLite) - 本地数据持久化
AccessibilityService - 模拟真实点击（无需Root）
System Overlay - 悬浮球绘制

## 最低要求

Android 8.0 (API 26) 及以上
