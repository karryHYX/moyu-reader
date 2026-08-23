# Gate 3 — Figma 设计评审

- 视觉方向：C — 现代编辑部
- Figma：https://www.figma.com/design/3WSyghIHa0a3RCF9s3V9OJ
- 主验证尺寸：393 × 852
- 辅助验证尺寸：360 × 800、412 × 915

## 文件结构

Figma Starter 工作区采用 3 个物理页面承载 6 个正式逻辑区：

1. `00–01 — Cover & Foundations`
   - `00 — Cover`
   - `01 — Foundations`
2. `02 — Components`
3. `03–05 — Screens · Prototype · Handoff`
   - `03 — Screens`
   - `04 — Prototype`
   - `05 — Handoff`

## Foundations

- Light、Dark、OLED、Paper 四套同构语义色集合。
- 颜色角色覆盖 background、surface、surfaceVariant、text、divider、accent、selection、scrim、success、warning、error 与 Reader 专用色。
- Typography：Display、Title、Subtitle、UI Body、Label、Caption、Reader Body、Reader Chapter Title、Reader Header、Reader Footer。
- Spacing：4 / 8 / 12 / 16 / 20 / 24 / 32 / 40 / 48 / 64。
- Radius：8 / 12 / 16 / 20 / 24 / Full。
- Motion：Fast 150ms、Standard 240ms、Slow 340ms、Spring bounce .12、Emphasized 420ms。

## 主题设置

主题设置采用：

1. 顶部实时正文预览；
2. Light / Dark / OLED / Paper 四宫格；
3. 三色 palette 识别；
4. 跟随系统、夜间降低对比度、主题切换动效；
5. 选择标记沿主题卡上沿移动；
6. Reader 主题从触点向外扩散，正文不闪白、不重排。

## 按钮与图标动效

### 按钮“收笔”

- Pressed：容器缩放至 96.5%。
- 圆角从均匀 12dp 变为具有语义方向的不对称形态。
- 左侧 13dp 短线扩展为 30dp，表现出版排版中的收笔动作。
- Reduced Motion 只保留颜色、描边与状态变化。

### Editorial Glyphs

- 图书馆：三条书脊，中间强调笔画独立移动。
- 主题：半圆相位变化，而不是整枚图标旋转。
- 目录：当前章节的小方块完成一次横向定位。
- 书签：折角翻转约 35° 后主体加深。
- 返回：强调色箭头先移动，主轴线随后收拢。

## 高保真 Screens

共 35 个状态：

- Onboarding：3
- Library：6
- Import：7
- Book Details：1
- Reader：12
- Settings：5
- Paper Reader 补充验证：1

覆盖空状态、加载、成功、重复、失败、编码选择、Light/Dark/OLED/Paper Reader、控制层、目录、进度、搜索、书签、阅读设置、主题设置、字体、统计、备份与隐私。

## 视觉检查结论

- C 的网格、编辑红线、字体层级在书架与设置中保持一致。
- Reader 主体降低品牌装饰强度，正文层级始终最高。
- 四套主题共用布局和角色语义。
- Bottom Sheet、Drawer、Dialog 与 Android 手势区边界清晰。
- 主题设置、阅读设置与控制层不会触发正文版面跳动。
- 本地交互稿与 Figma Capture Board 内容一致。

## 本地评审入口

- Components：`http://127.0.0.1:4173/?board=components`
- Screens：`http://127.0.0.1:4173/?board=screens`

## Gate 3 决策点

确认项：整体视觉、主题设置、按钮/图标动效语言、Reader 排版与页面覆盖范围。

通过后进入 Gate 4 Android 工程开发。
