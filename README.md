# ActivityTest · 学习任务管理器

一款基于 Android 原生开发的学习任务管理 App，帮助学习者规划、跟踪和回顾学习任务，支持任务提醒、多主题、多语言、深色模式等特性。

## 项目信息

- 应用名称：ActivityTest（学习任务管理器）
- 包名：`com.example.activitytest`
- minSdk：34　targetSdk / compileSdk：36
- 开发语言：Kotlin
- 构建工具：Gradle 8.x + AGP 8.13.0

## 功能介绍

### 任务管理（任务 Tab）
- 新增 / 编辑 / 删除任务，支持标题、内容、截止时间（日期+时间选择器）、优先级（高/普通/低）
- 未完成任务列表按截止时间 / 创建时间 / 优先级排序，支持标题关键词搜索
- 左滑任务卡片快捷"编辑 / 删除"，删除前二次确认
- 点击任务进入详情页，可标记完成状态、编辑、删除，支持"测试提醒"一键触发通知

### 已完成 & 历史记录（已完成 Tab）
- 集中展示已完成任务
- 删除的任务不会直接清除，而是归档到"历史记录"（回收站），可查看详情
- 历史记录支持右滑多选，批量恢复为正常任务

### 个人中心（我的 Tab）
- 昵称编辑与保存（本地持久化）
- 一键"同步任务"（模拟后台 Service 异步同步，完成后通知提示）
- 多主题切换：海军蓝 / 森林绿 / 紫罗兰 / 樱花粉 / 天空蓝 / 小鸡黄
- 中英文语言切换（应用内实时生效）
- 清除全部数据、关于弹窗

### 提醒通知
- 任务详情页"测试提醒"按钮通过显式广播触发 `ReminderReceiver`，模拟任务截止提醒通知，点击通知可跳转任务详情
- 首次进入自动申请 `POST_NOTIFICATIONS` 通知权限（Android 13+）

### 界面适配
- 深色模式（`values-night`）
- 横屏（`values-land`）
- 大屏 / 平板适配（`values-w600dp`、`values-w1240dp`）
- 中英文双语（`values-en`）

## 技术栈

| 模块 | 技术 |
|---|---|
| UI | ViewBinding、Material Components、Fragment + BottomNavigationView |
| 列表 | RecyclerView + ListAdapter + DiffUtil，自定义左滑/右滑手势 View |
| 数据层 | Room（Entity/DAO/Database，含 4 次版本迁移 Migration） |
| 异步 | Kotlin Coroutines + Flow，`viewModelScope`、`repeatOnLifecycle` |
| 架构 | MVVM（`AndroidViewModel` + Repository 式 DAO 调用） |
| 后台任务 | Service（模拟同步）、BroadcastReceiver + NotificationCompat（模拟提醒通知） |
| 本地存储 | SharedPreferences（昵称、主题、语言） |

## 项目结构

```
app/src/main/java/com/example/activitytest/
├── data/            # Room：Task / TaskHistory Entity，TaskDao / TaskHistoryDao，AppDatabase
├── viewmodel/       # TaskViewModel
├── ui/
│   ├── main/        # MainActivity（底部导航容器）
│   ├── task/        # 任务列表/详情/编辑 Activity、Fragment、Adapter、自定义滑动 View
│   ├── profile/     # 个人中心 Fragment
│   └── widget/      # 键盘收起容器等通用自定义 View
├── notification/    # 通知渠道封装
├── receiver/        # 提醒广播接收器
├── service/         # 模拟同步 Service
└── util/            # 主题 / 语言 / 弹窗 / 菜单 / WindowInsets 工具类
```

## 构建与运行

```bash
# 编译 Debug APK
./gradlew assembleDebug

# 产物路径
app/build/outputs/apk/debug/app-debug.apk
```

也可直接使用 Android Studio 打开项目，选择设备后点击 Run。

## 安装说明

仓库根目录下的 `ActivityTest-debug.apk` 为已构建好的可安装 Debug 包（使用默认 debug 签名）。

1. 将 APK 传输到 Android 手机（USB / 网盘 / IM 均可）
2. 手机端允许"安装未知来源应用"后点击安装
3. 或使用 ADB 安装：
   ```bash
   adb install -r ActivityTest-debug.apk
   ```

首次启动会请求通知权限（用于任务提醒），建议允许以体验完整功能。


## 目录清单

```
ActivityTest/
├── app/                       # 应用模块源码
├── ActivityTest-debug.apk     # 可安装的 Debug APK
├── screenshots/               # 功能截图
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── README.md
```
