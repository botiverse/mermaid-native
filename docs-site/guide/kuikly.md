# Mermaid Native 接入官方 Kuikly：从解析与布局到原生 Canvas 绘制

Mermaid Native 把 Mermaid 图表的解析、模型和布局放进 Kotlin Multiplatform，再由 Kuikly 负责页面承载和原生渲染。这样做的目标不是在业务页面里重新实现一套画图逻辑，而是让同一份 Mermaid Native 图形模型被 Android、iOS 和鸿蒙等宿主复用。

已发布的 `mermaid-kuikly` 集成已经有 Android、iOS 和鸿蒙三端验证记录。另一个需要单独说明的是新的 Compose Canvas adapter：它是下一条渲染路径，Android proof 已通过，iOS/OHOS 页面级运行证据仍在补齐。下面把这两层证据分开，避免把 adapter 的状态误写成整个 Mermaid 集成的状态。

## 这套集成解决什么问题

业务层只需要提供 Mermaid 文本和页面容器。Mermaid Native 负责把文本解析成图模型，布局模块计算节点、文本和连线的位置，Kuikly 页面负责生命周期和宿主接入，渲染器再把矩形、椭圆、路径、文本和样式绘制到 Canvas。

这种分层有两个好处：图表逻辑可以在 KMP 代码中复用，宿主页面也不需要依赖 WebView 或浏览器 DOM。Android 示例没有自定义图形绘制代码，页面调用 Mermaid Native 的解析、布局和 `MermaidView`，原生宿主只负责创建 Kuikly 页面。

## 可复现的官方 Kuikly 示例

示例目录：<https://github.com/botiverse/mermaid-native/tree/main/kuikly-examples>

示例使用以下版本：

- Kotlin 2.1.21
- 官方 Kuikly 2.28.0-2.1.21
- Mermaid Native 0.1.11
- Gradle 8.13，Android Gradle Plugin 8.13.2

共享模块的依赖形态如下：

```kotlin
dependencies {
    implementation("build.raft.mermaid:mermaid-layout-simple:$mermaidVersion")
    implementation("build.raft.mermaid:mermaid-kuikly:$mermaidVersion") {
        // The published Mermaid artifact was built with another Kuikly line.
        // The example supplies the official Kuikly version explicitly.
        exclude(group = "com.tencent.kuikly-open")
    }
}
```

官方 Kuikly 依赖通过 Tencent Maven 获取，Mermaid Native 依赖通过公开 Mermaid Maven 获取。这个排除和显式供应 Kuikly 的写法是示例中的消费侧兼容边界，不代表任意 Kuikly 版本都可以互换。

构建 Android 示例：

```sh
./gradlew :androidApp:assembleDebug
```

编译 iOS 共享代码：

```sh
./gradlew :shared:compileKotlinIosSimulatorArm64 :shared:compileKotlinIosArm64
```

Android 示例支持通过启动参数选择 `flow`、`treemap`、`shapes` 和 `sequence` 场景。已核验的 Android AVD 结果包含流程图、扩展图形、带标题的 Treemap 和时序图。已发布的 Kuikly 集成另有 iOS 与 OHOS 的三端验证记录；这些记录不等同于本独立示例的 Android 截图。H5 和小程序也不能从这组结果推导出运行时结论。

## Compose Canvas adapter 的位置

为了接入 Kuikly Compose Canvas，Mermaid Native 单独维护了 `mermaid-kuikly-compose` adapter。它把 Mermaid 的基础图元映射到 Compose `DrawScope`，包括：

- 矩形、圆角矩形和椭圆
- 线段、折线、二次曲线和圆弧
- 多边形、渐变和文本
- 缩放变换
- Mermaid 固定的 `[6,4]` 虚线

这条 adapter 依赖 Kuikly Canvas 的两项能力补丁：fork PR192 已合入 staging4；对应 Tencent-TDS/KuiklyUI 官方 PR #1783（dash/path effect）和 #1784（drawText）截至 2026-10-13 仍是 open，尚未合入官方 main。raft.11 是我们 fork/发布线上的可用坐标，不等于官方 main 已接受。

adapter 已在 Kuikly raft.11 上完成 Android assemble、iOS arm64/simArm64/x64 编译以及 OHOS KLIB 编译。Android proof 已实际安装启动并截图验证这些图元；OHOS 专属 HAP 已完成安装、启动和身份回读，但页面停在 Terms/sign-in，Mermaid adapter 的页面级断言尚未运行；iOS 页面级运行证据也尚未完成。

因此，adapter 当前适合作为独立预览和评审对象，不能把它写成已经替换生产渲染路径。生产移动端继续使用 raft.9、现有 Canvas 和 fork 的 `drawCallback` 补丁，直到独立 review、iOS/OHOS 页面运行和视觉回归全部完成。

## 现阶段的 Native 引擎进展

泳道避障路由已经合入 Mermaid Native。它让跨泳道连线根据最终节点和标题位置选择折线路径，并保留四向布局、自环和跳线行为。该批次完成了三端编译、实际 SVG/Canvas 检查和公开 Wasm 页面核验。

原版回放结果仍按事实记录为 25 条通过、3 条差异、2 条跳过。复杂连线中的已知差异继续单列，不能把它们折算成“全部兼容”。完整报告见：<https://raft-artifacts.com/p/botiverse/nathan-mermaid-native-swimlane-routing/v/1>。

## 结论

现在接入官方 Kuikly 的推荐路径是使用 `kuikly-examples` 验证 Mermaid Native 的解析、布局和 Android 原生绘制；已发布的 Mermaid Kuikly 集成已有三端验证记录，Compose Canvas adapter 则作为下一阶段的渲染路径继续补独立 review 和页面级证据。

这条路径保留了 Mermaid Native 的核心能力，也把宿主生命周期、页面状态和设备差异留在 Kuikly/Mobile 层，避免把 fork 依赖和未验证的运行时假设带入生产。

## 相关链接

- Mermaid Native：<https://github.com/botiverse/mermaid-native>
- 官方 Kuikly：<https://github.com/Tencent-TDS/KuiklyUI>
- 官方 Kuikly 示例：<https://github.com/botiverse/mermaid-native/tree/main/kuikly-examples>
- Mermaid Native 文档与示例：<https://github.com/botiverse/mermaid-native/tree/main/kuikly-examples/README.md>
