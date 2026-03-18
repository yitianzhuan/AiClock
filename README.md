# AiClock - 状态感知闹钟

一个 Android 闹钟 Demo：仅当用户正在使用手机（亮屏且已解锁）时提醒；否则静默忽略并记录。

## 已实现能力
- 闹钟 CRUD（新增、编辑、启停、删除）
- 重复周期设置（周一到周日多选，不选则单次）
- `AlarmManager` 精准调度（`setExactAndAllowWhileIdle`）
- 到点状态判断：
  - 亮屏 + 未锁屏 -> 高优先级提醒通知 + 响铃震动
  - 其他状态 -> 静默记录通知
- 通知交互：`我知道了` / `推迟10分钟`
- 历史记录页（查看“已提醒”与“静默忽略”）
- 开机重启后自动恢复启用中的闹钟
- Android 13+ 通知权限申请
- Android 12+ 精准闹钟权限引导
- 修复单次闹钟的 Snooze 触发边界问题

## 技术结构
- `app/src/main/java/com/aiclock/smartalarm/model` 数据模型
- `app/src/main/java/com/aiclock/smartalarm/data` 本地存储（SharedPreferences + JSON）
- `app/src/main/java/com/aiclock/smartalarm/alarm` 调度、触发、通知、开机恢复
- `app/src/main/java/com/aiclock/smartalarm/ui` 列表页与交互

## 后续建议
- 加入 Room + WorkManager 以提升数据和任务管理能力
- 对不同厂商 ROM 增加后台保活指引

## 本地构建
1. 安装 Android SDK，并确保 `sdk.dir` 在项目根目录 `local.properties` 中可用。
2. 运行 `./gradlew assembleDebug`。
