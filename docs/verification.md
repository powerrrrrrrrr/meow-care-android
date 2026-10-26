# 验证记录

## 2026-10-26 自定义图标白边修复

- 问题：旧版以 `Icon.createWithBitmap` 发布相册图片，桌面将其当作普通位图，缩小后放进带白底的图标容器；右下角另有桌面绘制的应用来源角标。
- 修复：发布 `Icon.createWithAdaptiveBitmap`。原图保持铺满可见区域，按系统 `AdaptiveIconDrawable.getExtraInsetFraction()` 向外扩展边缘像素，提供遮罩和视差所需区域；不添加白色外框、不为圆形桌面预先把图片裁成圆形，也不通过放大丢弃图片四周内容。
- 设置预览使用同一自适应图标渲染方式。应用图标与自定义快捷方式分组明确标注，解释快捷方式角标由桌面控制。应用不承诺去除桌面角标。
- 升级后首次启动在后台使用原快捷方式 ID 更新已有图标，不调用重新固定的确认流程，也不添加重复快捷方式。成功后记录迁移标记；失败会记录日志，后续启动或图标设置操作可重试。
- 新增真实图形渲染回归测试，先在旧版得到 `Custom photos must use adaptive icons, not legacy white wrappers` 失败，再验证修复后圆形、方形、圆角方形三个遮罩边缘无白底、四周色条保持原图取景、遮罩外透明。
- 完整构建 `testDebugUnitTest,assembleDebug,assembleDebugAndroidTest,lintDebug`：`BUILD SUCCESSFUL in 36s`；23 项 JVM 测试通过；Lint：`No issues found.`。
- API 36 模拟器覆盖安装应用与测试包，均返回 `Success`。设备集成测试：`OK (11 tests)`，记录在 `captures/adaptive-icon-instrumentation.txt`。
- 覆盖安装后首次打开，私有迁移标记 `adaptive-shortcut=true`。已有桌面快捷方式自动更新，实拍显示图片填满圆形遮罩且原白色外框消失；右下角仍保留桌面添加的应用角标。没有重新添加图标，点击原快捷方式成功打开「今天」。截图：`captures/adaptive-icon-desktop.png`。
- 圆形效果在模拟器桌面实际验证；方形与圆角方形通过 Android 图形层的遮罩渲染测试验证，未声称已验证所有厂商桌面。
- APK：`app/build/outputs/apk/debug/app-debug.apk`，SHA-256：`DCF64FEAD4E1347A71A178C15740EA507BBA8BABEE0D65240A0DFFED69E31345`。
- 参考：[Android adaptive icons](https://developer.android.com/develop/ui/views/launch/icon_design_adaptive)、[Icon API](https://developer.android.com/reference/android/graphics/drawable/Icon)、[AOSP Launcher3 角标绘制](https://android.googlesource.com/platform/packages/apps/Launcher3/%2B/3f442007d0/src/com/android/launcher3/icons/IconCache.java)。

## 2026-10-26 图标与自定义外观

- 新默认图标由内置 image_gen 生成，用户已确认不要求指定生图模型版本。原图和完整提示词保存在 `assets/app-icon/`；构建资源在项目内，支持 adaptive icon 和单色主题图标。
- 新入口：设置 → 外观设置 → App 图标。可切换喵洁日历、经典猫咪，恢复默认，以及从系统选图器导入图片并创建自定义桌面快捷方式。
- 改动只涉及图标、启动别名及外观界面，没有修改清洁数据库结构、清洁日期规则或提醒协议。
- 完整执行 `scripts/build.ps1 -Tasks testDebugUnitTest,assembleDebug,assembleDebugAndroidTest,lintDebug`：`BUILD SUCCESSFUL in 37s`。文案收尾后再执行 `testDebugUnitTest,assembleDebug,lintDebug`：`BUILD SUCCESSFUL in 37s`。23 项 JVM 测试通过，0 failures / 0 errors；Lint：`No issues found.`。
- 在已连接的 Android 16 / API 36 模拟器 `emulator-5554` 覆盖安装最终 APK 和测试包，均返回 `Success`；测试返回 `OK (10 tests)`，详见 `captures/icon-instrumentation.txt`。
- 新增 5 项设备集成测试，覆盖切换/恢复后只有一个可用启动入口、已有自定义快捷方式迁移后保持启用、居中裁剪和保存后重载、损坏图片不覆盖原图、旧版解码分支的大图采样与 EXIF 旋转，以及快捷方式固定 ID 和直达 MainActivity 的启动目标。原有 5 项数据库集成测试通过。
- 实际操作验证：设置页入口、取消选图、从系统选图器导入生成图、预览、系统确认添加桌面、从桌面点击快捷方式打开「今天」；覆盖安装及进程重启后仍保留图片。创建的快捷方式在内置图标切换测试后仍启用。实际点击切换至经典猫咪时，模拟器返回桌面；启动 `.LauncherClassic` 成功打开「今天」，且包管理器只返回一个启动入口。页面已补充可能返回桌面的说明。
- 深色模式与 200% 字体下检查了图标选项、滚动、图片预览及选图/添加按钮，未发生内容重叠或按钮不可达。测试后恢复浅色模式、100% 字体和默认图标。
- 截图包括 `captures/icon-customization.png`、`captures/icon-photo-preview.png`、`captures/icon-dark-font200.png`、`captures/icon-dark-font200-photo.png`；最终交付截图为 `captures/icon-final-customization.png`。
- 验证范围为本机 API 36 模拟器；未验证其他厂商桌面的图标刷新时间、快捷方式角标和重复固定行为，也未在 Android 8/9 真机验证旧版组件切换分支。旧版图片解码分支已由集成测试直接调用。
- 最终 Debug APK：`app/build/outputs/apk/debug/app-debug.apk`，SHA-256：`AD4A1D2A6CDB69CAECDB018C807D934F59138C6B1C8E0A2DDA99536C714B8F9E`。

## 2026-09-06 已执行

- Android 16 / API 36 Pixel 5 模拟器：SQLite 集成测试 5 项通过，0 failures / 0 errors。报告：`app/build/outputs/androidTest-results/connected/debug/`。
- 覆盖空数据库仅有 5 个任务且无完成历史、乱序补记、删除最新和唯一记录、修改时间、关闭并重开数据库后的持久化与停用状态。
- vivo V2405A 真机：旧版 debug APK 安装、启动成功；完成清洁、日历查看记录、确认删除测试记录已操作并截图。测试记录随后已删除。

## 2026-09-10 收尾

- 手机未连接；用户启动的 Android 16 / API 36 模拟器 `emulator-5554` 已连接。没有下载设备镜像。
- 最终构建：`scripts/build.ps1 -Tasks testDebugUnitTest,assembleDebug,lintDebug`，`BUILD SUCCESSFUL in 26s`。
- 清除旧报告影响，执行 `scripts/build.ps1 -Tasks lintDebug,--rerun-tasks`：`BUILD SUCCESSFUL in 29s`；最终文本报告为 `No issues found.`。
- JVM 报告合计 23 项，0 failures / 0 errors：日期规则 8、记录时间 3、任务状态 3、提醒策略 9。
- 修正了打开 App 跳过延迟提醒、夏令时回拨产生过去闹钟、编辑记录丢失时区偏移、昨日延迟广播占用今日通知次数。回归测试实际经历失败后通过；独立审查复核通过。
- 最终 APK 覆盖安装返回 `Success`；模拟器数据库测试返回 `OK (5 tests)`；启动成功。
- 本轮界面操作验证：完成任务后待处理数 5 → 4；月历显示记录；原时间保存后显示不变；删除确认后显示空历史。测试记录已删除。
- 通知权限：拒绝后提醒开关为 false，允许后为 true；系统闹钟列表包含 `com.meowcare.app.DAILY_REMINDER`，时间为所选 20:00。测试后全局提醒已关闭。
- 最终启动后仅检查本应用进程的 AndroidRuntime / MeowCare 错误日志，没有输出。截图：`captures/final-today.png`。
- APK SHA-256：`8028646D6769DA8D0D81DA40B7280193DC22FCC56B0AC0B5F7EFB6F54996E599`。

## 2026-09-10 界面改版

- 按已确认的奶油白与鼠尾草绿方案完成今天、月历、设置三页改版；数据结构、记录规则和提醒协议未修改。
- 首页改为紧凑任务卡、独立到期/未记录摘要、分类图标、主次操作和猫咪线稿；默认字号下一屏可完整看到四项任务。
- 月历改为无阴影平面日期网格，选中日期使用圆形主色，未选中的有记录日期显示圆点；记录区采用时间线和图标编辑入口。
- 设置改为清洁任务、提醒设置、关于与数据三个分组，任务行与通知行保持至少 48dp 点击区域。
- 无缓存执行 `testDebugUnitTest, assembleDebug, assembleDebugAndroidTest, lintDebug, --rerun-tasks`：`BUILD SUCCESSFUL in 31s`，75/75 个任务实际执行；23 项 JVM 测试通过，Lint 为 `No issues found.`。
- 最终 APK 在 API 36 模拟器覆盖安装和冷启动成功（615ms）；独立测试包返回 `OK (5 tests)`。
- 手动验证三页导航、完成任务、月历时间线、编辑入口和删除确认；临时记录已删除。浅色与深色界面均截图检查，测试后恢复浅色模式。
- 截图：`captures/final-beautified-today.png`、`captures/final-beautified-calendar.png`、`captures/final-beautified-settings.png`、`captures/final-beautified-dark.png`、`captures/final-beautified-timeline.png`。

## 2026-09-10 界面改版最终复核

- 按独立界面审查意见修正：日历切月时同步选中日期并按目标月份天数截断；今天页完成/补记、日历日期和图标按钮均保持至少 48dp 点击区域；补记按钮和设置行补充任务、周期与状态语义；日期读屏说明改为中文日期。
- 修正日历网格在自适应高度下首周占满页面的问题，六周日期均可正常显示；有记录日期改为同一行圆点标记，避免大字号下两行裁切。
- 在模拟器将系统字号临时调至 200%，检查今天、月历和设置三页：内容可滚动，日期完整，底部导航可用，“＋ 补记”和“＋ 添加”文字完整；检查后已恢复 100%。
- 最终重新执行 `testDebugUnitTest, assembleDebug, assembleDebugAndroidTest, lintDebug, --rerun-tasks` 生成全部报告和 APK；随后执行相同目标的一致性复查，`BUILD SUCCESSFUL in 5s`。JVM 23 项通过，Lint 为 `No issues found.`。
- API 36 模拟器覆盖安装正式 APK 与测试 APK 均返回 `Success`，数据库测试返回 `OK (5 tests)`；最终冷启动 `Status: ok`，耗时 713ms，本应用 AndroidRuntime / MeowCare 错误日志为空。
- 独立界面复核确认此前 4 个 Important 问题均已解决，没有仍会阻止交付的问题。
- 最终浅色截图：`captures/release-today.png`、`captures/release-calendar.png`、`captures/release-settings.png`。APK SHA-256：`173F4E368C8C6E773D6262C40E648BBBFEAF79DCA066E32DB679CEE37A6D013E`。

## 尚需真机验证

- 当前 APK 在 vivo 手机上的安装启动和完整交互回归。
- 指定时间实际通知投递、锁屏与厂商省电限制、重启后的提醒恢复（本轮只验证权限和系统闹钟登记）。
- 屏幕旋转时的表单体验。

每日提醒为非精确闹钟，不能保证准点。应用强行停止后需要重新打开。数据库测试使用独立测试库，不清除用户数据库。
