# 状态感知闹钟 PRD v1.0

## 核心定义
- 正在使用：屏幕亮起且处于解锁状态
- 未使用：屏幕熄灭，或亮屏但在锁屏界面

## 功能需求
1. 闹钟管理
- 新建/编辑时间、重复周期、标签、开关
- 列表展示并支持删除

2. 触发分支
- 命中“正在使用”：响铃 + 震动 + 高优先级提醒
- 命中“未使用”：静默忽略 + 留存静默记录

3. 提醒交互
- 我知道了（关闭）
- 推迟 10 分钟（Snooze）

## 非功能要求
- 触发误差 <= 1 分钟
- 低功耗后台待机
- 兼容 Android 10-14+
- 重启后恢复闹钟

## 当前实现映射
- 状态检测：`PowerManager.isInteractive` + `KeyguardManager.isKeyguardLocked`
- 调度：`AlarmManager.setExactAndAllowWhileIdle`
- 触发入口：`AlarmReceiver`
- 重启恢复：`BootReceiver`
- 交互提醒：`NotificationCompat` 高优先级通知 + 操作按钮
