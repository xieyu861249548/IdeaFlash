# iOS 18 人机界面指南 — Android 开发实施规范

> 版本：1.0 | 日期：2026-04-05 | 适用项目：灵感胶囊（及所有 iOS 风格 Android 应用）
>
> 本文档将 Apple iOS 18 Human Interface Guidelines 的设计规范转译为 Android 开发可直接使用的具体数值（dp/sp），是开发与设计的唯一真实来源。

---

## 目录

1. [字体规范](#1-字体规范-typography)
2. [色彩规范](#2-色彩规范-colors)
3. [按钮规范](#3-按钮规范-buttons)
4. [卡片与容器规范](#4-卡片与容器规范-cards--containers)
5. [间距规范](#5-间距规范-spacing)
6. [导航规范](#6-导航规范-navigation)
7. [列表规范](#7-列表规范-lists)
8. [底部操作表规范](#8-底部操作表规范-bottom-sheet--action-sheet)
9. [Segmented Control 规范](#9-segmented-control-规范)
10. [动画规范](#10-动画规范-animation)
11. [磨砂玻璃效果规范](#11-磨砂玻璃效果规范-frosted-glass--blur)
12. [图标规范](#12-图标规范-icons)
13. [本项目（灵感胶囊）应用规范摘要](#13-本项目灵感胶囊应用规范摘要)

---

## 1. 字体规范 (Typography)

### 1.1 字体族映射

iOS 使用 **SF Pro**（西文）与 **PingFang SC**（中文）作为系统字体。Android 端对应方案如下：

| iOS 字体 | Android 替代方案 | 说明 |
|---|---|---|
| SF Pro Display | `fontFamily="sans-serif"` (Roboto) | 系统默认，大标题场景 |
| SF Pro Text | `fontFamily="sans-serif"` (Roboto) | 正文场景 |
| SF Pro Rounded | `fontFamily="sans-serif-rounded"` | 圆润风格，数字/标签用 |
| PingFang SC Regular | `fontFamily="sans-serif"` (Noto Sans SC) | 中文正文 |
| PingFang SC Medium | `fontFamily="sans-serif-medium"` | 中文强调 |
| PingFang SC Semibold | `fontFamily="sans-serif-medium"` + `textStyle="bold"` | 中文标题 |

**推荐实践：** 在 `res/font/` 目录引入 Noto Sans SC 字体族，通过 `FontFamily` XML 定义完整字重链。

```xml
<!-- res/font/noto_sans_sc.xml -->
<font-family xmlns:android="http://schemas.android.com/apk/res/android"
             xmlns:app="http://schemas.android.com/apk/res-auto">
    <font app:fontStyle="normal" app:fontWeight="300" app:font="@font/noto_sans_sc_light"/>
    <font app:fontStyle="normal" app:fontWeight="400" app:font="@font/noto_sans_sc_regular"/>
    <font app:fontStyle="normal" app:fontWeight="500" app:font="@font/noto_sans_sc_medium"/>
    <font app:fontStyle="normal" app:fontWeight="600" app:font="@font/noto_sans_sc_semibold"/>
    <font app:fontStyle="normal" app:fontWeight="700" app:font="@font/noto_sans_sc_bold"/>
</font-family>
```

---

### 1.2 完整字型比例（Type Scale）

iOS 18 Dynamic Type 对应 Android sp 值如下表。所有数值基于 iOS 默认字号（Large Text 关闭状态）。

| 样式名称 | iOS pt | Android sp | 字重 | 行高 (sp) | 字间距 (em) | 用途说明 |
|---|---|---|---|---|---|---|
| Display | 34 | 34 | Semibold / 600 | 41 | -0.5 | 大数字、着重展示 |
| Large Title | 34 | 34 | Regular / 400 | 41 | 0.37 | 页面大标题（导航栏展开态） |
| Title 1 | 28 | 28 | Regular / 400 | 34 | 0.36 | 一级标题 |
| Title 2 | 22 | 22 | Regular / 400 | 28 | 0.35 | 二级标题 |
| Title 3 | 20 | 20 | Regular / 400 | 25 | 0.38 | 三级标题 |
| Headline | 17 | 17 | Semibold / 600 | 22 | -0.41 | 强调正文、列表标题 |
| Body | 17 | 17 | Regular / 400 | 22 | -0.41 | 主要正文 |
| Callout | 16 | 16 | Regular / 400 | 21 | -0.32 | 辅助说明、气泡文字 |
| Subheadline | 15 | 15 | Regular / 400 | 20 | -0.23 | 副标题、次级信息 |
| Footnote | 13 | 13 | Regular / 400 | 18 | -0.08 | 注脚、时间戳 |
| Caption 1 | 12 | 12 | Regular / 400 | 16 | 0 | 图片说明、小标签 |
| Caption 2 | 11 | 11 | Regular / 400 | 13 | 0.07 | 最小文字、badge |

**在 Android 中定义（`res/values/type.xml`）：**

```xml
<style name="Type.Display">
    <item name="android:textSize">34sp</item>
    <item name="android:fontFamily">@font/noto_sans_sc</item>
    <item name="android:textStyle">bold</item>
    <item name="android:lineHeight">41sp</item>
    <item name="android:letterSpacing">-0.015</item>
</style>

<style name="Type.LargeTitle">
    <item name="android:textSize">34sp</item>
    <item name="android:fontFamily">@font/noto_sans_sc</item>
    <item name="android:textStyle">normal</item>
    <item name="android:lineHeight">41sp</item>
    <item name="android:letterSpacing">0.011</item>
</style>

<style name="Type.Title1">
    <item name="android:textSize">28sp</item>
    <item name="android:lineHeight">34sp</item>
    <item name="android:letterSpacing">0.013</item>
</style>

<style name="Type.Title2">
    <item name="android:textSize">22sp</item>
    <item name="android:lineHeight">28sp</item>
    <item name="android:letterSpacing">0.016</item>
</style>

<style name="Type.Title3">
    <item name="android:textSize">20sp</item>
    <item name="android:lineHeight">25sp</item>
    <item name="android:letterSpacing">0.019</item>
</style>

<style name="Type.Headline">
    <item name="android:textSize">17sp</item>
    <item name="android:textStyle">bold</item>
    <item name="android:lineHeight">22sp</item>
    <item name="android:letterSpacing">-0.024</item>
</style>

<style name="Type.Body">
    <item name="android:textSize">17sp</item>
    <item name="android:lineHeight">22sp</item>
    <item name="android:letterSpacing">-0.024</item>
</style>

<style name="Type.Callout">
    <item name="android:textSize">16sp</item>
    <item name="android:lineHeight">21sp</item>
    <item name="android:letterSpacing">-0.020</item>
</style>

<style name="Type.Subheadline">
    <item name="android:textSize">15sp</item>
    <item name="android:lineHeight">20sp</item>
    <item name="android:letterSpacing">-0.015</item>
</style>

<style name="Type.Footnote">
    <item name="android:textSize">13sp</item>
    <item name="android:lineHeight">18sp</item>
    <item name="android:letterSpacing">-0.006</item>
</style>

<style name="Type.Caption1">
    <item name="android:textSize">12sp</item>
    <item name="android:lineHeight">16sp</item>
    <item name="android:letterSpacing">0.000</item>
</style>

<style name="Type.Caption2">
    <item name="android:textSize">11sp</item>
    <item name="android:lineHeight">13sp</item>
    <item name="android:letterSpacing">0.006</item>
</style>
```

---

### 1.3 中文文字调整

中文字符在相同 sp 值下视觉比西文更大，以下是中文场景的调整建议：

| 场景 | 英文 sp | 中文建议 sp | 原因 |
|---|---|---|---|
| 正文 Body | 17 | 17 | 保持不变，汉字紧凑 |
| 小标签 Caption | 12 | 13 | 汉字笔画复杂，12sp 可读性差 |
| 注脚 Footnote | 13 | 13 | 临界值，不建议更小 |
| 最小可用字号 | 11 | 12 | 11sp 汉字几乎不可辨认 |
| 标题行高 | 1.2× | 1.4× | 汉字无上升/下降部，行间距需增加 |

**中文字间距（letterSpacing）建议统一设为 `0.05em`，覆盖 iOS SF Pro 的负字间距设定，因汉字本身已足够紧凑。**

---

## 2. 色彩规范 (Colors)

### 2.1 iOS 18 系统色（Light / Dark）

| 颜色名称 | Light 模式 Hex | Dark 模式 Hex | Android 资源名 |
|---|---|---|---|
| System Blue | `#007AFF` | `#0A84FF` | `@color/ios_blue` |
| System Green | `#34C759` | `#30D158` | `@color/ios_green` |
| System Red | `#FF3B30` | `#FF453A` | `@color/ios_red` |
| System Orange | `#FF9500` | `#FF9F0A` | `@color/ios_orange` |
| System Yellow | `#FFCC00` | `#FFD60A` | `@color/ios_yellow` |
| System Pink | `#FF2D55` | `#FF375F` | `@color/ios_pink` |
| System Purple | `#AF52DE` | `#BF5AF2` | `@color/ios_purple` |
| System Teal | `#5AC8FA` | `#64D2FF` | `@color/ios_teal` |
| System Indigo | `#5856D6` | `#6E6EFF` | `@color/ios_indigo` |
| System Cyan | `#32ADE6` | `#32D4FF` | `@color/ios_cyan` |
| System Mint | `#00C7BE` | `#63E6E2` | `@color/ios_mint` |
| System Brown | `#A2845E` | `#AC8E68` | `@color/ios_brown` |

---

### 2.2 语义色（Semantic Colors）

#### 标签色（Label Colors）

| 名称 | Light Hex | Light Alpha | Dark Hex | Dark Alpha | 用途 |
|---|---|---|---|---|---|
| Label | `#000000` | 100% | `#FFFFFF` | 100% | 主要文字 |
| Secondary Label | `#3C3C43` | 60% → `#99999A` | `#EBEBF5` | 60% → `#9D9DA3` | 次要文字 |
| Tertiary Label | `#3C3C43` | 30% → `#C4C4C6` | `#EBEBF5` | 30% → `#636366` | 三级文字 |
| Quaternary Label | `#3C3C43` | 18% → `#D1D1D3` | `#EBEBF5` | 18% → `#3A3A3C` | 四级文字（最淡） |

**具体 Hex 值（直接使用，已含 Alpha 混合白/黑背景）：**

| 名称 | Light Hex | Dark Hex |
|---|---|---|
| Label | `#000000` | `#FFFFFF` |
| Secondary Label | `#3C3C43` @ 60% | `#EBEBF5` @ 60% |
| Tertiary Label | `#3C3C43` @ 30% | `#EBEBF5` @ 30% |
| Quaternary Label | `#3C3C43` @ 18% | `#EBEBF5` @ 18% |

在 Android 中用 `ColorStateList` 或 `?attr/` 主题属性实现：

```xml
<!-- res/values/colors.xml -->
<color name="label_primary">#FF000000</color>
<color name="label_secondary">#993C3C43</color>
<color name="label_tertiary">#4D3C3C43</color>
<color name="label_quaternary">#2E3C3C43</color>

<!-- res/values-night/colors.xml -->
<color name="label_primary">#FFFFFFFF</color>
<color name="label_secondary">#99EBEBF5</color>
<color name="label_tertiary">#4DEBEBF5</color>
<color name="label_quaternary">#2EEBEBF5</color>
```

---

### 2.3 背景色层级（Background Layers）

iOS 使用三层背景系统，对应如下：

| 层级名称 | Light Hex | Dark Hex | 用途 |
|---|---|---|---|
| System Background | `#FFFFFF` | `#000000` | 最底层背景，主页面 |
| Secondary System Background | `#F2F2F7` | `#1C1C1E` | 卡片、分组背景 |
| Tertiary System Background | `#FFFFFF` | `#2C2C2E` | 嵌套卡片、输入框 |
| System Grouped Background | `#F2F2F7` | `#000000` | grouped 列表页面背景 |
| Secondary Grouped Background | `#FFFFFF` | `#1C1C1E` | grouped 列表卡片 |
| Tertiary Grouped Background | `#F2F2F7` | `#2C2C2E` | 三级嵌套 |

```xml
<!-- res/values/colors.xml -->
<color name="bg_system">#FFFFFFFF</color>
<color name="bg_secondary">#FFF2F2F7</color>
<color name="bg_tertiary">#FFFFFFFF</color>
<color name="bg_grouped">#FFF2F2F7</color>
<color name="bg_grouped_secondary">#FFFFFFFF</color>

<!-- res/values-night/colors.xml -->
<color name="bg_system">#FF000000</color>
<color name="bg_secondary">#FF1C1C1E</color>
<color name="bg_tertiary">#FF2C2C2E</color>
<color name="bg_grouped">#FF000000</color>
<color name="bg_grouped_secondary">#FF1C1C1E</color>
```

---

### 2.4 填充色（Fill Colors）

填充色用于按钮背景、控件高亮等轻量容器，带透明度：

| 名称 | Light | Dark | 用途 |
|---|---|---|---|
| System Fill | `#78788033` (约 20% 灰) | `#7878805C` (约 36% 灰) | 输入框、次要按钮 |
| Secondary System Fill | `#78788028` (约 16%) | `#78788051` (约 32%) | 滑块轨道 |
| Tertiary System Fill | `#7676801E` (约 12%) | `#76768047` (约 28%) | 分隔块 |
| Quaternary System Fill | `#74748014` (约 8%) | `#7474803D` (约 24%) | 超淡背景 |

**直接可用的具体 Hex：**

| 名称 | Light Hex | Dark Hex |
|---|---|---|
| System Fill | `#33787880` | `#5C787880` |
| Secondary Fill | `#28787880` | `#51787880` |
| Tertiary Fill | `#1E767680` | `#47767680` |
| Quaternary Fill | `#14747480` | `#3D747480` |

---

### 2.5 分隔线色（Separator Colors）

| 名称 | Light Hex | Dark Hex | 用途 |
|---|---|---|---|
| Separator | `#3C3C4349` | `#54545899` | 列表分隔线（带透明） |
| Opaque Separator | `#C6C6C8FF` | `#38383AFF` | 不透明分隔线（性能优先） |

```xml
<color name="separator">#493C3C43</color>
<color name="separator_opaque">#FFC6C6C8</color>

<!-- night -->
<color name="separator">#99545458</color>
<color name="separator_opaque">#FF38383A</color>
```

---

### 2.6 遮罩与背景调暗（Overlay / Dim Levels）

| 场景 | 颜色 | 不透明度 | 用途 |
|---|---|---|---|
| Modal 背景遮罩 | `#000000` | 40%（`#66000000`） | 弹窗、底部表单后遮罩 |
| 轻量弹出遮罩 | `#000000` | 20%（`#33000000`） | Popover、菜单 |
| 全屏遮罩（Alert） | `#000000` | 50%（`#80000000`） | Alert 对话框 |
| 视频/图片覆层 | `#000000` | 30%（`#4D000000`） | 图片上的文字可读性保障 |
| 深色模式遮罩 | `#000000` | 60%（`#99000000`） | Dark 模式下 Modal 遮罩 |

---

## 3. 按钮规范 (Buttons)

### 3.1 按钮尺寸规格

| 尺寸类型 | 高度 | 最小宽度 | 圆角半径 | 水平内边距 | 字号 | 字重 |
|---|---|---|---|---|---|---|
| **Large（主操作）** | 56dp | 120dp | 14dp | 24dp | 17sp | Semibold / 600 |
| **Medium（次要操作）** | 44dp | 88dp | 12dp | 20dp | 15sp | Medium / 500 |
| **Small（辅助操作）** | 32dp | 64dp | 8dp | 14dp | 13sp | Medium / 500 |
| **Pill（胶囊，突出样式）** | 50dp | — | 25dp（全圆） | 24dp | 17sp | Semibold / 600 |

---

### 3.2 按钮颜色规格

#### 主要按钮（Filled）
```
背景色: @color/ios_blue (#007AFF / #0A84FF)
文字色: #FFFFFF
```

#### 次要按钮（Tinted）
```
背景色: @color/ios_blue @ 15% Alpha (#2600A3FF light / #1A0A84FF dark)
文字色: @color/ios_blue
```

#### 破坏性按钮（Destructive）
```
背景色: @color/ios_red (#FF3B30 / #FF453A)
文字色: #FFFFFF
圆角: 同 Large 按钮 14dp
```

#### 幽灵/描边按钮（Ghost / Outline）
```
背景色: 透明
边框: 1.5dp, @color/ios_blue
文字色: @color/ios_blue
```

#### 纯图标按钮（Icon-only）
```
最小点击区域: 44×44dp（HIG 强制要求）
图标尺寸: 24×24dp（居中于点击区域）
背景: 透明 或 @color/system_fill（32×32dp 容器）
```

---

### 3.3 按钮状态

| 状态 | 视觉变化 | 动画时长 |
|---|---|---|
| **默认** | 正常显示 | — |
| **按下（Pressed）** | 缩放至 0.97，透明度降至 85% | 缩小 100ms spring，松手 200ms spring 回弹 |
| **禁用（Disabled）** | 透明度降至 38%（`alpha = 0.38`） | 无动画 |
| **加载中** | 替换文字为 `ProgressIndicator`，按钮锁定 | fade 200ms |

**无 Ripple 效果**。iOS 风格使用缩放反馈，需在 Android 中关闭默认水波纹：
```xml
android:background="@drawable/btn_primary"  <!-- 自定义 drawable，不继承 Material -->
```

或在代码中：
```kotlin
button.isClickable = true
button.isFocusable = true
// 设置 OnTouchListener 实现 scale 动画，替代 ripple
button.setOnTouchListener { v, event ->
    when (event.action) {
        MotionEvent.ACTION_DOWN -> v.animate().scaleX(0.97f).scaleY(0.97f).alpha(0.85f).setDuration(100).start()
        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(200).start()
    }
    false
}
```

---

### 3.4 Pill（胶囊）按钮规格——iOS 17+ 突出样式

```
高度: 50dp
圆角: 25dp（完全圆角）
水平内边距: 28dp
字号: 17sp, Semibold
最小宽度: 屏幕宽度 - 32dp（全宽减去左右各 16dp margin）
背景渐变: 单色（系统蓝）或应用主题色
阴影: elevation 4dp 或自定义 shadow layer
```

---

## 4. 卡片与容器规范 (Cards & Containers)

### 4.1 圆角规格

| 容器类型 | 圆角半径 | 说明 |
|---|---|---|
| 标准卡片 | **20dp** | 通用内容卡片（iOS 实际约 13pt，但 20dp 在 Android 上视觉等效） |
| 大容器 / Modal Sheet | **28dp** | 全屏弹出层、大型底部表单 |
| 弹出菜单 / Popover | **16dp** | 上下文菜单、提示气泡 |
| 输入框 / TextField | **12dp** | 文本输入框 |
| 小型芯片 / Chip / Tag | **8dp** | 标签、筛选项 |
| Badge | **全圆（height/2）** | 徽标、数字角标 |
| 列表分组容器 | **12dp** | grouped 列表外层圆角 |
| 图片缩略图（小） | **8dp** | 列表内图片 |
| 图片缩略图（大） | **16dp** | 卡片内大图 |

---

### 4.2 卡片内边距

| 卡片尺寸 | 水平内边距 | 垂直内边距 | 内部元素间距 |
|---|---|---|---|
| 小型卡片（< 120dp 高） | 12dp | 10dp | 8dp |
| 标准卡片 | 16dp | 14dp | 10dp |
| 大型内容卡片 | 20dp | 18dp | 12dp |
| 全宽横幅卡片 | 16dp | 20dp | 16dp |

---

### 4.3 卡片阴影规格

iOS 不使用 Material Elevation 阴影，而是更柔和的漫反射阴影。Android 实现方案：

**方案 A：Paint.setShadowLayer（推荐，API 28+）**
```kotlin
// 标准卡片阴影
val shadowPaint = Paint().apply {
    setShadowLayer(
        8f,    // radius（模糊半径）
        0f,    // dx
        4f,    // dy
        Color.argb(20, 0, 0, 0)  // 黑色 8% 透明度
    )
}
```

**方案 B：CardView / MaterialCard elevation 降低值**
```xml
<!-- 标准卡片：elevation 2dp，但加自定义 background shadow -->
app:cardElevation="2dp"
app:cardPreventCornerOverlap="true"
```

**阴影参数对照表：**

| 卡片类型 | Blur Radius | Offset Y | 颜色 Alpha | Android Elevation 近似 |
|---|---|---|---|---|
| 轻量卡片 | 4dp | 2dp | 8% | 1dp |
| 标准卡片 | 8dp | 4dp | 12% | 2dp |
| 浮起卡片 | 16dp | 8dp | 18% | 4dp |
| Modal/Sheet | 24dp | 12dp | 25% | 8dp |

---

### 4.4 磨砂玻璃卡片配方

```
背景色: rgba(255, 255, 255, 0.72)  [Light]
       rgba(28, 28, 30, 0.72)     [Dark]
模糊半径: 40dp（RenderEffect 或 Glide blur）
边框: 1dp, rgba(255,255,255,0.5)  [Light]
     1dp, rgba(255,255,255,0.1)   [Dark]
```

详见第 11 节磨砂玻璃规范。

---

## 5. 间距规范 (Spacing)

### 5.1 基础网格

**所有间距值必须是 4dp 的倍数。**

| 间距级别 | dp 值 | 用途 |
|---|---|---|
| XXS | 2dp | 紧密元素内部（图标与徽标） |
| XS | 4dp | 最小间距单元 |
| S | 8dp | 图标与文字间距、紧凑列表 |
| M | 12dp | 组件内垂直内边距 |
| L | 16dp | 标准内边距、页面水平边距（紧凑屏） |
| XL | 20dp | 页面水平边距（标准屏）、组件间 |
| 2XL | 24dp | 主要区块间距 |
| 3XL | 32dp | 大区块间距 |
| 4XL | 40dp | 特大间距 |
| 5XL | 48dp | 顶部安全区近似值 |

---

### 5.2 页面级间距

| 规格项 | 数值 | 说明 |
|---|---|---|
| 页面水平边距（紧凑，< 375dp 屏宽） | **16dp** | iPhone SE 等小屏 |
| 页面水平边距（标准，≥ 375dp 屏宽） | **20dp** | iPhone 14 等标准屏 |
| 导航栏与内容间距 | **12dp** | 内容顶部与导航栏底部距离 |
| 主要区块间距 | **24dp** | 两个 Section 之间 |
| 次级区块间距 | **16dp** | 同一 Section 内子模块 |
| 底部安全区（Home Indicator） | **34dp** | iPhone X+ 底部预留 |
| 底部安全区（无 Home Indicator） | **20dp** | 老机型 / Android 导航栏 |

---

### 5.3 组件内部间距

| 组件 | 水平内边距 | 垂直内边距 | 子元素间距 |
|---|---|---|---|
| 列表项 | 16dp | — | 12dp（图标与文字） |
| 卡片 | 16dp | 14dp | 10dp |
| 输入框 | 14dp | 12dp | — |
| Alert 对话框 | 24dp | 20dp | 12dp |
| 工具提示 Tooltip | 12dp | 8dp | — |
| 导航栏 | 16dp | — | — |
| Tab Bar 图标区域 | — | 6dp（上）/ 4dp（下） | — |

---

### 5.4 关键最小尺寸（HIG 强制要求）

| 元素 | 最小点击区域 | 最小视觉尺寸 |
|---|---|---|
| 所有可点击元素 | **44×44dp** | — |
| 开关 Switch | 51×31dp | 51×31dp |
| 复选框 Checkbox | 44×44dp | 22×22dp |
| 导航栏按钮 | 44×44dp | 24×24dp 图标 |
| 列表行 | 全宽 × 44dp | — |

---

## 6. 导航规范 (Navigation)

### 6.1 导航栏（Navigation Bar）

| 规格项 | 数值 | 说明 |
|---|---|---|
| 标准导航栏内容区高度 | **44dp** | 不含状态栏 |
| 状态栏高度（近似） | **44dp** | Android 实际用 `WindowInsets` 获取 |
| 导航栏总高度（含状态栏） | **~88dp** | = 44 内容 + 44 状态栏 |
| 大标题导航栏总高度 | **~96dp** | Large Title 展开态 |
| 大标题字号 | **34sp** | Regular 字重 |
| 标准标题字号 | **17sp** | Semibold 字重 |
| 返回按钮区域 | **44×44dp** | 含文字时宽度可变 |
| 导航栏背景（滚动后） | 半透明磨砂，见第 11 节 | 等同 `UINavigationBarAppearance` |
| 导航栏背景（顶部时） | 完全透明 | Large Title 模式 |

**Large Title 折叠动画：**
- 滚动距离 0-44dp：Large Title 从 34sp 缩小至 17sp（标准导航栏标题出现）
- 背景同步淡入磨砂效果
- 折叠弹簧：`interpolator = FastOutSlowIn`，300ms

---

### 6.2 侧边抽屉（Side Drawer，如"以往灵感"面板）

| 规格项 | 数值 | 说明 |
|---|---|---|
| 抽屉宽度 | **屏幕宽度 × 0.85** | 最大不超过 320dp |
| 最大宽度上限 | **320dp** | 平板适配 |
| 圆角（抽屉右侧） | **0dp**（无圆角） | 从左侧滑出时左边贴边 |
| 圆角（从右侧滑出时左侧） | **0dp** | 同理 |
| 遮罩透明度 | `#66000000`（40%黑） | 抽屉打开时背景遮罩 |
| 打开动画 | 300ms ease-out，X 轴平移 | 从左侧外滑入 |
| 关闭动画 | 250ms ease-in，X 轴平移回 | |
| 抽屉内顶部内边距 | 状态栏高度 + 20dp | 避开状态栏 |
| 抽屉内水平内边距 | **20dp** | |
| 列表项高度（抽屉内） | **48dp** | |

---

### 6.3 Tab Bar（底部标签栏）

| 规格项 | 数值 |
|---|---|
| Tab Bar 内容区高度 | **49dp** |
| Tab Bar 总高度（含底部安全区） | **49dp + 底部安全区（≈34dp）= 83dp** |
| 图标尺寸 | **25×25dp** |
| 图标与文字间距 | **3dp** |
| 文字字号 | **10sp** |
| 文字字重（选中） | Medium / 500 |
| 文字字重（未选中） | Regular / 400 |
| 选中色 | `@color/ios_blue` |
| 未选中色 | `@color/label_tertiary` |
| 背景 | 磨砂玻璃，见第 11 节 |

---

### 6.4 返回按钮

```
图标: chevron.left（向左箭头，SF Symbols 等效）
图标尺寸: 17×17dp（视觉大小），点击区域 44×44dp
颜色: @color/ios_blue
文字（可选）: 前一页标题，Callout(16sp) 字重 Regular，颜色同上
左边距: 8dp（从屏幕左边缘）
```

---

## 7. 列表规范 (Lists)

### 7.1 行高规格

| 列表行类型 | 最小高度 | 推荐高度 | 说明 |
|---|---|---|---|
| 纯文本行 | **44dp** | 44dp | HIG 最小点击区域要求 |
| 图标 + 单行文字 | **44dp** | 56dp | 32dp 图标 + 内边距 |
| 图标 + 双行文字（标题+副标题） | 56dp | **64dp** | 标准双行 |
| 图片缩略图行（正方形小图） | 56dp | **68dp** | 48dp 图片 + 边距 |
| 联系人/头像行 | 56dp | **64dp** | 40dp 头像 + 边距 |
| 分节标题（Section Header） | 28dp | 28dp | 小号大写文字 |
| 分节尾注（Section Footer） | 自适应 | 28dp | 说明文字 |

---

### 7.2 分节标题（Section Header）规格

```
文字: 全大写（toUpperCase()）
字号: 13sp
字重: Regular
颜色: @color/label_secondary（次要标签色）
水平内边距左: 20dp（相对于屏幕）
垂直内边距: 上 20dp，下 6dp
```

---

### 7.3 列表分隔线（Separator）

```
颜色: @color/separator（见 2.5 节）
高度: 0.5dp（或 1px，物理像素）
左侧内缩: 16dp（Inset Separator，从图标/内容开始，不从屏幕边缘）
全宽分隔线: 0dp 内缩（章节间使用）
```

**iOS 风格使用 Inset Separator（缩进分隔线），分隔线从文字内容左边缘开始，不延伸到屏幕边缘**。Android RecyclerView 实现：

```kotlin
val divider = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
// 自定义 Drawable 实现 16dp 左内缩
```

---

### 7.4 滑动操作（Swipe Actions）

| 动作类型 | 背景色 | 图标颜色 | 标签色 |
|---|---|---|---|
| 删除（Delete） | `@color/ios_red` (#FF3B30) | `#FFFFFF` | `#FFFFFF` |
| 归档（Archive） | `@color/ios_orange` (#FF9500) | `#FFFFFF` | `#FFFFFF` |
| 置顶（Pin） | `@color/ios_yellow` (#FFCC00) | `#FFFFFF` | `#FFFFFF` |
| 标记（Flag） | `@color/ios_orange` | `#FFFFFF` | `#FFFFFF` |
| 更多（More） | `@color/label_tertiary` opacity 40% gray | `#FFFFFF` | `#FFFFFF` |

```
滑动操作按钮宽度: 80dp
图标尺寸: 22×22dp
文字字号: 12sp
文字字重: Regular
图标与文字间距: 4dp
圆角（最末尾按钮与卡片圆角一致）: 12dp
```

---

### 7.5 下拉刷新（Pull-to-Refresh）

```
触发距离: 64dp（向下拖拽 64dp 触发）
指示器: UIRefreshControl 等效——圆形进度弧，非 Material LinearProgressIndicator
指示器尺寸: 20dp 直径
颜色: @color/label_secondary
弹性效果: 使用 OverScrollDelegate 模拟 iOS 弹性滚动
```

---

## 8. 底部操作表规范 (Bottom Sheet / Action Sheet)

### 8.1 基础规格

| 规格项 | 数值 | 说明 |
|---|---|---|
| 顶部圆角半径 | **20dp** | 左上 + 右上圆角 |
| 拖拽指示条宽度 | **36dp** | Grab Handle |
| 拖拽指示条高度 | **4dp** | |
| 拖拽指示条圆角 | **2dp** | 全圆角 |
| 拖拽指示条颜色 | `#3C3C4349`（Light）/ `#54545899`（Dark）| 同 Separator 色 |
| 指示条距顶部 | **8dp** | 指示条上边距 |
| 底部表单背景 | 磨砂玻璃 Regular（见第 11 节）或 `@color/bg_secondary` | |
| 底部安全区内边距 | **34dp**（或动态获取）| 保证按钮不被 Home Indicator 遮挡 |

---

### 8.2 高度级别

| 档位 | 高度 | 用途 |
|---|---|---|
| Peek（窥视） | 屏幕高度 × 0.25（约 200dp） | 折叠状态，露出标题和操作 |
| Half（半展开） | 屏幕高度 × 0.5 | 中等内容 |
| Full（全屏） | 屏幕高度 - 状态栏高度 - 20dp | 完整内容，顶部留 20dp 可见上层 |

**弹性参数：**
```
展开动画: spring(dampingRatio=0.85, stiffness=300), 350ms
折叠动画: 250ms ease-in
snap 吸附: 速度阈值 800dp/s 触发跳档
```

---

### 8.3 按钮规格（Sheet 内）

| 按钮类型 | 高度 | 字号 | 字重 | 颜色 |
|---|---|---|---|---|
| 主要操作按钮 | **56dp** | 20sp | Semibold / 600 | `@color/ios_blue` |
| 普通操作按钮 | **56dp** | 20sp | Regular / 400 | `@color/ios_blue` |
| 破坏性操作按钮 | **56dp** | 20sp | Regular / 400 | `@color/ios_red` |
| 取消按钮 | **56dp** | 20sp | Semibold / 600 | `@color/ios_blue` |

**取消按钮（Cancel）与其他按钮之间有 8dp 间距**（iOS UIActionSheet 特征）。

---

### 8.4 Action Sheet vs Alert Sheet 区别

| 特征 | Action Sheet（操作表） | Alert（警告框） |
|---|---|---|
| 出现位置 | 从底部滑入 | 屏幕中央淡入 |
| 圆角 | 20dp 顶部 | 14dp 全部 |
| 最大宽度 | 全屏宽度 - 16dp 左右边距 | 270dp（固定） |
| 遮罩 | `#66000000`（40%） | `#80000000`（50%） |
| 取消按钮 | 有独立"取消"行，与主体分离 8dp | 按钮排列在底部 |

---

## 9. Segmented Control 规范

### 9.1 尺寸规格

| 规格项 | 数值 |
|---|---|
| 整体高度 | **32dp** |
| 外层圆角（背景） | **10dp** 或 `height/2`（Pill 形） |
| 内层选中指示器圆角 | **8dp** |
| 水平外边距（容器内） | **2dp** |
| 垂直外边距（容器内） | **2dp** |
| 每段最小宽度 | **32dp** |
| 每段水平内边距 | **16dp** |
| 文字字号 | **13sp** |
| 文字字重（选中） | Semibold / 600 |
| 文字字重（未选中） | Regular / 400 |

---

### 9.2 颜色规格

| 状态 | 背景色 | 文字色 |
|---|---|---|
| 容器背景 | `@color/system_fill`（见 2.4 节） | — |
| 选中指示器 | `#FFFFFFFF`（白色）+ 阴影 | `@color/label_primary` |
| 未选中 | 透明 | `@color/label_secondary` |
| 禁用 | — | `@color/label_quaternary` |

**选中指示器阴影：**
```
blur: 4dp
offsetY: 1dp
alpha: 25%（`#40000000`）
```

---

### 9.3 动画规格

```
切换动画类型: Spring
持续时间: 300ms
Spring 参数: dampingRatio = 0.75, stiffness = 400（MotionInterpolator）
动画内容: 选中指示器 X 轴平移，宽度微弹性缩放
```

Android 实现方案：用 `ValueAnimator` + `SpringForce` 驱动指示器 `translationX`。

---

## 10. 动画规范 (Animation)

### 10.1 时长基准

| 动画类型 | 时长 | 说明 |
|---|---|---|
| 快速交互反馈 | **100ms** | 按钮按下缩放 |
| 标准交互 | **200ms** | 状态切换、图标变换 |
| 页面内过渡 | **300ms** | 标准转场 |
| 页面推入/推出 | **350ms** | Navigation push/pop |
| Modal 呈现 | **300ms** | 底部弹出 |
| Modal 消失 | **250ms** | 略快于呈现 |
| 复杂布局变化 | **400ms** | 展开/折叠动画 |
| 长按激活 | **200ms** | 缩放至稳定状态 |

---

### 10.2 插值器（Interpolator）映射

| iOS 曲线 | Android 等效 | 用途 |
|---|---|---|
| `easeInOut` | `AccelerateDecelerateInterpolator` | 通用状态变化 |
| `easeOut` | `DecelerateInterpolator(2f)` | 弹出 Modal、页面推入 |
| `easeIn` | `AccelerateInterpolator(2f)` | 关闭 Modal、页面推出 |
| `spring(damping: 0.7)` | `SpringForce(dampingRatio=0.7, stiffness=MEDIUM)` | 弹性按钮回弹 |
| `spring(damping: 0.85)` | `SpringForce(dampingRatio=0.85, stiffness=HIGH)` | Bottom Sheet 档位吸附 |
| `linear` | `LinearInterpolator` | 进度条、加载动画 |

---

### 10.3 核心动画规格

#### 页面转场（Navigation Push/Pop）
```kotlin
// Push（推入新页面）
enterAnimation:  translationX: 100%→0, alpha: 0.0→1.0, 350ms, DecelerateInterpolator(2f)
exitAnimation:   translationX: 0→-30%, alpha: 1.0→0.7, 350ms, DecelerateInterpolator(2f)

// Pop（返回）
enterAnimation:  translationX: -30%→0, alpha: 0.7→1.0, 300ms, DecelerateInterpolator(2f)
exitAnimation:   translationX: 0→100%, alpha: 1.0→0.0, 300ms, AccelerateInterpolator(2f)
```

#### Modal 呈现/消失
```kotlin
// 呈现
translationY: 100%→0, 300ms, DecelerateInterpolator(2.5f)
同时: 背景遮罩 alpha 0→40%, 300ms, LinearInterpolator

// 消失
translationY: 0→100%, 250ms, AccelerateInterpolator(2f)
同时: 背景遮罩 alpha 40%→0, 250ms, LinearInterpolator
```

#### 按钮点击反馈
```kotlin
// 按下
scaleX/Y: 1.0→0.97, alpha: 1.0→0.85
duration: 100ms, interpolator: AccelerateInterpolator

// 松手回弹
scaleX/Y: 0.97→1.0, alpha: 0.85→1.0
duration: 200ms, interpolator: SpringForce(damping=0.5, stiffness=HIGH)
```

#### 长按激活（如拖拽排序）
```kotlin
scaleX/Y: 1.0→0.95
duration: 200ms, interpolator: DecelerateInterpolator
阴影: elevation 2dp→8dp（同步）
```

#### 列表项出现（Stagger）
```kotlin
每项延迟: index × 30ms（最多 10 项）
动画: translationY: 20dp→0, alpha: 0→1
duration: 300ms, DecelerateInterpolator(2f)
```

---

### 10.4 Spring 动画参数速查

| 场景 | dampingRatio | stiffness | 对应 SpringForce 常量 |
|---|---|---|---|
| 按钮回弹 | 0.5 | 800 | `STIFFNESS_MEDIUM` |
| Bottom Sheet 吸附 | 0.85 | 300 | `STIFFNESS_LOW` |
| Segmented Control | 0.75 | 400 | `STIFFNESS_MEDIUM_LOW` |
| 弹出卡片 | 0.7 | 600 | `STIFFNESS_MEDIUM` |
| 图标反弹 | 0.4 | 1200 | `STIFFNESS_HIGH` |

```kotlin
// Android SpringAnimation 示例
val springAnim = SpringAnimation(view, DynamicAnimation.SCALE_X, 1f).apply {
    spring.dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY  // ≈ 0.5
    spring.stiffness = SpringForce.STIFFNESS_MEDIUM  // ≈ 800
}
```

---

## 11. 磨砂玻璃效果规范 (Frosted Glass / Blur)

### 11.1 iOS 材质层级

iOS 定义了 5 种材质（Material），从薄到厚：

| 材质名称 | 模糊半径 | Light 背景色 | Dark 背景色 | 用途 |
|---|---|---|---|---|
| **Ultra Thin** | 20dp | `rgba(255,255,255,0.45)` | `rgba(28,28,30,0.45)` | 极轻量覆层，后方内容清晰可见 |
| **Thin** | 30dp | `rgba(255,255,255,0.55)` | `rgba(28,28,30,0.55)` | 轻量 HUD，导航栏 |
| **Regular** | 40dp | `rgba(255,255,255,0.72)` | `rgba(28,28,30,0.72)` | 标准：Tab Bar、Bottom Sheet |
| **Thick** | 50dp | `rgba(255,255,255,0.85)` | `rgba(28,28,30,0.85)` | 厚重覆盖，Alert 背景 |
| **Chrome** | 60dp | `rgba(242,242,247,0.92)` | `rgba(22,22,24,0.92)` | 导航栏完全不透明态 |

---

### 11.2 颜色配方（Hex + Alpha）

**Light 模式：**

| 材质 | 背景 Hex + Alpha | 边框 |
|---|---|---|
| Ultra Thin | `#73FFFFFF` | `#26FFFFFF` |
| Thin | `#8CFFFFFF` | `#33FFFFFF` |
| Regular | `#B8FFFFFF` | `#4DFFFFFF` |
| Thick | `#D9FFFFFF` | `#66FFFFFF` |

**Dark 模式：**

| 材质 | 背景 Hex + Alpha | 边框 |
|---|---|---|
| Ultra Thin | `#731C1C1E` | `#26FFFFFF` |
| Thin | `#8C1C1C1E` | `#1AFFFFFF` |
| Regular | `#B81C1C1E` | `#1AFFFFFF` |
| Thick | `#D91C1C1E` | `#33FFFFFF` |

---

### 11.3 Android 实现方案

#### 方案 A：RenderEffect API（推荐，Android 12 / API 31+）

```kotlin
// Kotlin：对目标 View 应用模糊效果
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    val blurEffect = RenderEffect.createBlurEffect(
        40f, 40f,  // radiusX, radiusY（Regular 材质）
        Shader.TileMode.MIRROR
    )
    targetView.setRenderEffect(blurEffect)
}

// 在模糊层上叠加半透明背景色
overlayView.setBackgroundColor(Color.parseColor("#B8FFFFFF"))  // Regular Light
```

**注意：** `RenderEffect` 作用于 View 自身，要实现背景模糊需要将背景 View 单独分层：
1. `BackgroundBlurView`：持有 `RenderEffect`，显示被模糊的背景内容截图
2. `ColorOverlayView`：叠加半透明颜色层
3. `ContentView`：前景内容

#### 方案 B：自定义 BlurDrawable（API 21+，兼容性方案）

```kotlin
class BlurDrawable(
    private val view: View,
    private val blurRadius: Float = 40f,
    private val overlayColor: Int = Color.parseColor("#B8FFFFFF")
) : Drawable() {

    override fun draw(canvas: Canvas) {
        // 1. 获取背景截图
        val bitmap = captureBackground(view)
        // 2. 用 RenderScript 或自定义 Kernel 模糊（已废弃于 API 31，但 API 21-30 可用）
        val blurred = stackBlur(bitmap, blurRadius.toInt())
        canvas.drawBitmap(blurred, 0f, 0f, null)
        // 3. 叠加颜色
        canvas.drawColor(overlayColor)
    }
}
```

#### 方案 C：简化近似方案（纯色半透明，无模糊，极端兼容）

当设备不支持 RenderEffect 时的降级方案：

```kotlin
// Light 模式降级
val fallbackLight = Color.parseColor("#E5F2F2F7")  // bg_secondary + 90% alpha

// Dark 模式降级
val fallbackDark = Color.parseColor("#E51C1C1E")
```

---

### 11.4 各组件材质使用规范

| 组件 | 使用材质 | 说明 |
|---|---|---|
| 导航栏（滚动后） | **Thin / Regular** | 根据内容密度选择 |
| Tab Bar | **Regular** | 标准底部栏 |
| Bottom Sheet | **Regular** | 标准底部表单 |
| Alert 对话框 | **Thick** | 高优先级遮罩 |
| Context Menu | **Regular** | 长按弹出菜单 |
| Tooltip / Popover | **Thin** | 轻量提示 |
| 悬浮工具条 | **Ultra Thin** | 文字选中工具条 |

---

## 12. 图标规范 (Icons)

### 12.1 尺寸规格

| 使用场景 | 图标尺寸 | 容器尺寸 | 点击区域 | 描边粗细 |
|---|---|---|---|---|
| **导航栏图标** | **24×24dp** | — | 44×44dp | 1.5dp |
| **Tab Bar 图标** | **25×25dp** | — | 44×44dp | 1.5dp（选中填充，未选中描边） |
| **列表行图标（带背景）** | **18×18dp** | **32×32dp** | 行高 × 行宽 | 1.5dp |
| **按钮内图标** | **17×17dp** | — | 44×44dp | 1.5dp |
| **大型功能图标** | **32×32dp** | **60×60dp** | — | 2dp |
| **超大图标（AppIcon 风格）** | **64×64dp** | **80×80dp** | — | — |
| **Badge / 微型图标** | **12×12dp** | — | — | 1dp |

---

### 12.2 SF Symbols 等效绘制规则

SF Symbols 使用可变描边（variable weight），Android 自定义图标应遵循以下规则以达到 iOS 视觉效果：

**描边规则：**
```
24dp 图标：描边 1.5dp（非矢量图用 1px @ 2x，1.5px @ 3x）
Navigation bar 图标：统一使用描边风格（非填充）
Tab Bar 图标：
  - 未选中: 描边风格（stroke）
  - 选中: 填充风格（filled）或描边加粗至 2dp
```

**圆角一致性：**
```
图标内所有圆角 ≥ 1dp
箭头、三角形等尖角可保持锐利
路径端点使用圆角 cap（round cap）
```

**视觉重量（Optical Weight）：**
- 图标实际面积应占容器的约 55-65%，不要撑满
- 避免图标过于"重"或过于"轻"，参考 SF Symbols 的 Regular weight

---

### 12.3 列表行图标（带彩色背景圆角方块）

iOS 设置列表中常见"彩色背景 + 白色图标"风格：

```
容器尺寸: 32×32dp
容器圆角: 8dp
容器背景色: 使用 iOS 系统色（每个功能不同颜色）
图标尺寸: 18×18dp（居中）
图标颜色: #FFFFFF
```

常用颜色分配参考（与 iOS 系统设置一致）：

| 颜色 | 建议用途 |
|---|---|
| `@color/ios_blue` | 信息类、通用功能 |
| `@color/ios_red` | 删除、危险操作 |
| `@color/ios_green` | 确认、成功、添加 |
| `@color/ios_orange` | 提醒、警告 |
| `@color/ios_purple` | 媒体、创意 |
| `@color/ios_teal` | 通信、分享 |

---

### 12.4 图标资源命名规范（Android）

```
ic_nav_[name]_24      → 导航栏 24dp 图标
ic_tab_[name]_25      → Tab Bar 25dp 图标（含 _selected 变体）
ic_list_[name]_18     → 列表行 18dp 图标
ic_btn_[name]_17      → 按钮内 17dp 图标
ic_feature_[name]_32  → 功能大图标 32dp
```

---

## 13. 本项目（灵感胶囊）应用规范摘要

> 本节为"灵感胶囊"应用的专属规范快速参考，基于深紫色主题定制。

### 13.1 品牌色系

| 颜色角色 | Hex 值 | 说明 |
|---|---|---|
| **主品牌色（Primary）** | `#7B5EA7` | 深紫，主要按钮、强调色 |
| **主品牌色浅（Primary Light）** | `#9B7FC7` | 浅紫，hover/选中态 |
| **主品牌色深（Primary Dark）** | `#5A3D87` | 暗紫，按压态 |
| **强调色（Accent）** | `#BF5AF2` | iOS Purple Dark，亮紫 |
| **渐变起点** | `#7B5EA7` | 深紫 |
| **渐变终点** | `#4A3C7E` | 偏蓝紫 |

---

### 13.2 语义色（应用内使用）

| 语义角色 | Light Hex | Dark Hex | 用途 |
|---|---|---|---|
| 主要文字 | `#000000` | `#F0EEF4` | 灵感标题、正文 |
| 次要文字 | `#6E6E73` | `#AEAEB2` | 时间戳、标签 |
| 三级文字 | `#AEAEB2` | `#636366` | 辅助说明 |
| 强调/链接 | `#7B5EA7` | `#BF5AF2` | 可点击文字 |
| 成功 | `#34C759` | `#30D158` | 保存成功提示 |
| 危险 | `#FF3B30` | `#FF453A` | 删除操作 |
| 警告 | `#FF9500` | `#FF9F0A` | 警告提示 |

---

### 13.3 背景色层级（灵感胶囊）

| 层级 | Light Hex | Dark Hex | 使用位置 |
|---|---|---|---|
| App 背景 | `#F2F0F7` | `#0E0B14` | 主页面背景（紫色调黑底） |
| 卡片背景 | `#FFFFFF` | `#1C1820` | 灵感卡片 |
| 卡片背景次级 | `#F7F5FB` | `#252030` | 嵌套内容区域 |
| 输入框背景 | `#F0EDF7` | `#201D2A` | 文本输入区域 |
| 导航栏背景 | 磨砂 Regular + `#F2F0F7` | 磨砂 Regular + `#0E0B14` | 顶部导航 |
| 侧边栏背景 | `#EDE9F5` | `#150F20` | "以往灵感"抽屉 |

---

### 13.4 组件快速参考表

| 组件 | 关键数值 |
|---|---|
| **灵感卡片圆角** | 20dp |
| **灵感卡片内边距** | 水平 16dp，垂直 14dp |
| **灵感卡片阴影** | blur 8dp, offsetY 4dp, `#201E2C` 12% |
| **主操作按钮（新建灵感）** | 高 56dp，圆角 14dp，背景 `#7B5EA7`，白色文字 |
| **Pill 浮动按钮（FAB 替代）** | 高 50dp，圆角 25dp，背景渐变 `#7B5EA7→#4A3C7E` |
| **标签/Tag 芯片** | 高 28dp，圆角 8dp，背景 `#7B5EA7` @ 15%，文字 `#7B5EA7` |
| **侧边抽屉宽度** | 屏幕宽度 × 0.85，最大 320dp |
| **底部操作表圆角** | 20dp |
| **输入框圆角** | 12dp |
| **列表行最小高度** | 44dp（纯文本），64dp（带副标题） |
| **页面水平边距** | 16dp（< 375dp 屏），20dp（≥ 375dp 屏） |
| **区块间距** | 24dp |
| **底部安全区** | 34dp |

---

### 13.5 字体使用（灵感胶囊）

| 文字场景 | 样式 | 颜色 |
|---|---|---|
| 灵感标题 | Title 2（22sp, Regular） | 主要文字色 |
| 灵感正文预览 | Body（17sp, Regular） | 次要文字色 |
| 灵感时间戳 | Caption 1（12sp, Regular） | 三级文字色 |
| 页面大标题 | Large Title（34sp, Regular） | 主要文字色 |
| 操作按钮文字 | Headline（17sp, Semibold） | `#FFFFFF` 或主品牌色 |
| 分节标题 | Footnote（13sp, Regular, 大写） | 次要文字色 |
| 标签/分类 | Caption 1（12sp, Medium） | 主品牌色 |

---

### 13.6 动画（灵感胶囊）

| 交互 | 规格 |
|---|---|
| 新建灵感卡片出现 | translationY 20dp→0, alpha 0→1, 300ms, DecelerateInterpolator |
| 删除灵感卡片 | translationX 0→-100%, alpha 1→0, 250ms, AccelerateInterpolator |
| 侧边栏打开 | translationX -100%→0, 300ms, DecelerateInterpolator(2f) |
| 底部表单弹出 | translationY 100%→0, 300ms, DecelerateInterpolator(2.5f) |
| 卡片点击缩放 | scale 1.0→0.97, 100ms; 回弹 0.97→1.0, 200ms spring |
| 标签切换（Segmented） | spring(damping=0.75, stiffness=400), 300ms |

---

### 13.7 磨砂玻璃（灵感胶囊，紫色调）

由于本应用背景色为深紫色，磨砂效果需叠加紫色调：

| 材质 | Light | Dark |
|---|---|---|
| 导航栏（Regular） | `rgba(242,240,247,0.85)` | `rgba(14,11,20,0.85)` |
| 侧边栏背景 | `rgba(237,233,245,0.90)` | `rgba(21,15,32,0.90)` |
| 底部表单 | `rgba(255,255,255,0.80)` | `rgba(28,24,32,0.80)` |
| 模糊半径 | 40dp | 40dp |

---

*文档结束。如需更新，请修改本文件并同步至开发团队。*

*最后更新：2026-04-05 | 适用 iOS 18 HIG + Android 14 实施规范*
