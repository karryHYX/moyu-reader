const $ = (selector, root = document) => root.querySelector(selector);
const boardType = new URLSearchParams(location.search).get("board") || "components";

const icon = (name, label = name) => `
  <div class="glyph-item"><div class="glyph ${name}"><i></i><b></b></div>${label}</div>`;

function boardHeader(kicker, title, lead, meta) {
  return `<header class="board-header">
    <div><div class="board-kicker">${kicker}</div><h1>${title}</h1><p class="board-lead">${lead}</p></div>
    <div class="board-meta">${meta}</div>
  </header>`;
}

function heading(title, code) {
  return `<div class="section-heading"><h2>${title}</h2><span>${code}</span></div>`;
}

function themeCards(compact = false) {
  const themes = [
    ["light", "浅色", "LIGHT"],
    ["dark", "深色", "DARK"],
    ["oled", "OLED", "OLED"],
    ["paper", "纸张", "PAPER"],
  ];
  return `<div class="theme-cards ${compact ? "compact" : ""}">${themes.map((t, i) => `
    <div class="theme-card ${t[0]} ${i === 0 ? "selected" : ""}" data-theme="${t[0]}">
      <div class="theme-preview"><strong>第十二章</strong><span></span><span></span></div>
      <label><span>${t[1]}</span><small>${t[2]}</small></label>
    </div>`).join("")}</div>`;
}

function renderComponents() {
  document.title = "墨屿阅读 · Components";
  $("#app").innerHTML = `<div class="board">
    ${boardHeader("02 — COMPONENTS", "编辑部组件库", "Material 3 触控底座 × 精准排版 × 收笔动效", "FOUNDATION → COMPONENTS<br>LIGHT · DARK · OLED · PAPER")}

    ${heading("Buttons", "STATE / SHAPE / INK STROKE")}
    <div class="component-grid">
      <article class="component-panel wide">
        <h3 class="component-title">Primary Button</h3><p class="component-desc">按下时容器收缩，左侧短线像铅字排版中的“收笔”一样向右生长。</p>
        <div class="component-stage">
          <div><div class="state-label">DEFAULT</div><button class="moyu-button">开始阅读</button></div>
          <div><div class="state-label">PRESSED</div><button class="moyu-button pressed">开始阅读</button></div>
          <div><div class="state-label">DISABLED</div><button class="moyu-button disabled">开始阅读</button></div>
        </div>
      </article>
      <article class="component-panel wide">
        <h3 class="component-title">Secondary Button</h3><p class="component-desc">无阴影；边框在按下时从 1dp 加深至 2dp，并改变不对称圆角。</p>
        <div class="component-stage">
          <button class="moyu-button secondary">稍后再说</button>
          <button class="moyu-button secondary pressed">重新识别</button>
          <button class="moyu-button secondary disabled">导出备份</button>
        </div>
      </article>
    </div>

    ${heading("Editorial Glyphs", "ORIGINAL 24dp ICON FAMILY")}
    <div class="component-panel full">
      <p class="component-desc">图标内部保留一条强调色“完成笔画”。交互不是整枚图标旋转，而是让语义部分单独完成动作。</p>
      <div class="glyph-row">
        ${icon("search", "Search")}${icon("library", "Library")}${icon("import", "Import")}${icon("settings", "Settings")}${icon("theme", "Theme")}${icon("directory", "Directory")}${icon("bookmark", "Bookmark")}${icon("back", "Back")}${icon("more", "More")}
      </div>
    </div>

    ${heading("Inputs & Selection", "FOCUS / SELECTED / PRESSED")}
    <div class="component-grid">
      <article class="component-panel wide"><h3 class="component-title">Search Field</h3><p class="component-desc">焦点出现时不发光，只让 2dp 编辑红线接管边框。</p><div class="component-stage"><div class="search-field">⌕　搜索书名或作者</div><div class="search-field focused">⌕　山海</div></div></article>
      <article class="component-panel wide"><h3 class="component-title">Segmented Control</h3><p class="component-desc">选中项使用白底与短下划线，避免胶囊堆叠。</p><div class="component-stage"><div class="segmented"><button class="segment active">全部</button><button class="segment">在读</button><button class="segment">已读</button></div></div></article>
      <article class="component-panel"><h3 class="component-title">Slider</h3><p class="component-desc">刻度线只在拖动时出现。</p><div class="slider"><i></i></div></article>
      <article class="component-panel"><h3 class="component-title">Switch</h3><p class="component-desc">拇指末端带轻微“收笔”位移。</p><div class="component-stage"><div class="switch"><i></i></div><div class="switch on"><i></i></div></div></article>
      <article class="component-panel"><h3 class="component-title">Radio Row</h3><div class="radio-list"><div class="radio-row"><i class="radio on"></i>分页阅读</div><div class="radio-row"><i class="radio"></i>纵向滚动</div></div></article>
      <article class="component-panel"><h3 class="component-title">Progress</h3><p class="component-desc">只显示一条出版式细线。</p><div class="progress" style="--p:68%"><i></i></div></article>
    </div>

    ${heading("Theme Selection", "LIVE PREVIEW / FOUR PALETTES")}
    <article class="component-panel full"><h3 class="component-title">Theme Card</h3><p class="component-desc">选择标记沿卡片上沿滑动；阅读页主题从当前触点以柔和遮罩向外扩散。</p>${themeCards()}</article>

    ${heading("Content Components", "BOOK / CHAPTER / RESULT / STATUS")}
    <div class="component-grid">
      <article class="component-panel"><h3 class="component-title">Book Card</h3><div style="width:118px"><div class="book-cover"><strong>山海之间</strong><small>林舟</small></div><div class="book-meta"><strong>山海之间</strong><span>已读 48%</span></div></div></article>
      <article class="component-panel wide"><h3 class="component-title">Book Row</h3><div class="book-row"><div class="mini-cover"></div><div><h4>山海之间</h4><p>林舟 · 第十二章 雨夜来信</p><div class="progress" style="--p:48%"><i></i></div></div><span>48%</span></div></article>
      <article class="component-panel"><h3 class="component-title">Chapter Row</h3><div class="chapter-row active"><span>第十二章　雨夜来信</span><b>▮</b></div><div class="chapter-row"><span>第十三章　晨光</span><span>›</span></div></article>
      <article class="component-panel wide"><h3 class="component-title">Search Result</h3><div class="search-result"><header><span>第十二章 · 雨夜来信</span><span>48%</span></header><p>远处的<mark>灯火</mark>隔着薄雾，一盏一盏亮起来。</p></div></article>
      <article class="component-panel"><h3 class="component-title">Import Status</h3><div class="task-card"><header><strong>山海之间.txt</strong><span>78%</span></header><p>正在建立全文索引 · 78 / 100 章</p><div class="progress" style="--p:78%"><i></i></div></div></article>
      <article class="component-panel"><h3 class="component-title">Snackbar</h3><div style="background:var(--ink-950);color:var(--paper-50);padding:14px 16px;display:flex;justify-content:space-between"><span>已添加书签</span><b style="color:var(--red-200)">查看</b></div></article>
    </div>

    ${heading("Motion Storyboard", "150 / 240 / 340ms")}
    <div class="motion-story">
      <div class="motion-frame"><header><span>01</span><span>0ms</span></header><div class="demo"><button class="moyu-button">主题</button></div><footer>静止：短线保持 13dp。</footer></div>
      <div class="motion-frame"><header><span>02</span><span>70ms</span></header><div class="demo"><button class="moyu-button pressed">主题</button></div><footer>按压：容器缩至 96.5%，圆角向动作方向偏移。</footer></div>
      <div class="motion-frame"><header><span>03</span><span>150ms</span></header><div class="demo">${icon("theme", "")}</div><footer>图标内部半圆先完成 12° 相位移动。</footer></div>
      <div class="motion-frame"><header><span>04</span><span>240ms</span></header><div class="demo"><div class="theme-card paper selected" style="width:150px"><div class="theme-preview"><strong>第十二章</strong><span></span><span></span></div></div></div><footer>注册标记沿上沿停靠。</footer></div>
      <div class="motion-frame"><header><span>05</span><span>340ms</span></header><div class="demo" style="background:var(--paper-50);font:18px 'Noto Serif SC';">雨夜来信</div><footer>主题遮罩由触点扩散，正文不闪白、不跳版。</footer></div>
    </div>

    <div class="handoff-board">
      <div class="handoff-card"><h3>Compose Motion</h3><p>Pressed 使用 graphicsLayer 缩放；内部笔画独立 animateDpAsState。</p><code>duration = 150ms\nscale = .965f\nshape = 8/15/15/8</code></div>
      <div class="handoff-card"><h3>Reduced Motion</h3><p>保留颜色与描边反馈，停用位移、扩散遮罩和弹簧。</p><code>if (reducedMotion)\n  crossfade(90ms)\nelse animateInkStroke()</code></div>
      <div class="handoff-card"><h3>Touch & Semantics</h3><p>视觉图标 24dp，点击区域至少 48dp；每个图标均提供中文语义。</p><code>Modifier.minimumInteractiveComponentSize()\ncontentDescription = "打开目录"</code></div>
    </div>
  </div>`;
  wireInteractions();
}

const statusBar = () => `<div class="status-bar"><span>10:30</span><div class="status-icons"><i></i><i></i><strong>100%</strong></div></div>`;
const appBar = (title, back = false, more = true) => `<div class="appbar">${back ? '<button class="icon-btn back"></button>' : ""}<h3 class="${back ? "small-title" : ""}">${title}</h3>${more ? '<button class="icon-btn"></button>' : ""}</div>`;
const bottomNav = (active = "书架") => `<nav class="bottom-nav">${["书架","导入","设置"].map(x => `<div class="nav-item ${x===active?"active":""}"><b></b>${x}</div>`).join("")}</nav>`;
const progress = (p) => `<div class="progress" style="--p:${p}%"><i></i></div>`;
const libraryBooks = ["山海之间","长安旧事","浮生记","风起陇西","人间烟火","云深不知处"];
const bookGrid = () => `<div class="book-grid">${libraryBooks.map((b,i)=>`<div><div class="book-cover"><strong>${b}</strong><small>${["林舟","北渡","沈复","马伯庸","温书意","许墨"][i]}</small></div><div class="book-meta"><strong>${b}</strong><span>${i<4?`已读 ${48-i*9}%`:"未读"}</span></div></div>`).join("")}</div>`;
const bookRows = () => `<div class="book-list">${libraryBooks.slice(0,5).map((b,i)=>`<div class="book-row"><div class="mini-cover"></div><div><h4>${b}</h4><p>${["林舟","北渡","沈复","马伯庸","温书意"][i]} · ${i?"最近阅读 3 天前":"第十二章 雨夜来信"}</p>${progress(48-i*7)}</div><span>${48-i*7}%</span></div>`).join("")}</div>`;
const readerText = () => `<div class="reader-copy"><p>雨落在旧城的屋檐上，像有人翻动一本很厚的书。远处的灯火隔着薄雾，一盏一盏亮起来。</p><p>他站在廊下，手里握着那封迟来的信，纸角已经被雨水浸得微微发软。</p><p>信封上没有署名，只有一枚淡淡的邮印，日期模糊。</p></div>`;
const settingRows = (rows) => `<div class="settings-group">${rows.map(r=>`<div class="setting-row"><div><strong>${r[0]}</strong>${r[1]?`<br><span>${r[1]}</span>`:""}</div>${r[2]||"›"}</div>`).join("")}</div>`;

function phone(inner, cls = "", nav = "") {
  return `<div class="phone ${cls}">${statusBar()}${inner}${nav ? bottomNav(nav) : ""}<div style="position:absolute;bottom:6px;left:50%;transform:translateX(-50%);width:108px;height:4px;border-radius:3px;background:currentColor;opacity:.35"></div></div>`;
}

function onboarding(kind) {
  if (kind === "welcome") return phone(`<div class="phone-content" style="padding-top:70px"><div class="eyebrow">MOYU READER</div><div class="hero-title">把一本书，<br>安静地留在这里。</div><p class="body-copy">为本地 TXT 与 EPUB 打造的中文小说阅读器。没有账户，也没有云端。</p><div style="height:270px;display:grid;place-items:center"><div class="empty-symbol" style="transform:rotate(-3deg)"></div></div><button class="primary-cta">开始使用</button></div>`);
  if (kind === "privacy") return phone(`${appBar("只在你的设备上", true, false)}<div class="phone-content"><div class="hero-title" style="font-size:30px">离线，是默认状态。</div><div class="privacy-lines">${[["不需要账户","打开即可阅读"],["不上传小说","文件复制到应用私有空间"],["不包含统计 SDK","阅读数据只属于你"]].map(x=>`<div class="privacy-line"><i class="privacy-mark"></i><div><strong>${x[0]}</strong><p class="body-copy" style="margin:3px 0">${x[1]}</p></div></div>`).join("")}</div><button class="primary-cta">继续</button><button class="secondary-cta" style="margin-top:12px">查看隐私说明</button></div>`);
  return phone(`${appBar("导入第一本书", true, false)}<div class="phone-content"><div class="hero-title" style="font-size:30px">从设备开始。</div><p class="body-copy">支持 TXT 与 DRM-Free EPUB。</p>${importOptions()}<button class="secondary-cta" style="margin-top:28px">暂时跳过</button></div>`);
}

function importOptions() {
  return `<div style="margin-top:28px"><div class="import-option"><i class="import-icon"></i><div><h4>选择文件</h4><p>导入单个 TXT 或 EPUB</p></div></div><div class="import-option"><i class="import-icon"></i><div><h4>选择多个文件</h4><p>一次导入多本小说</p></div></div><div class="import-option"><i class="import-icon"></i><div><h4>扫描文件夹</h4><p>授权目录后仅扫描支持格式</p></div></div></div>`;
}

function library(kind) {
  const top = `${appBar("墨屿阅读")}<div class="phone-content"><div class="library-headline"><h2>${kind==="empty"?"书架":"读书如登山"}</h2><div>⌕　☷</div></div>`;
  if (kind === "empty") return phone(`${top}<div class="empty-state"><div><div class="empty-symbol"></div><h3>书架还是空的</h3><p class="body-copy">导入 TXT 或 EPUB，阅读内容只保存在本机。</p><button class="moyu-button">导入书籍</button></div></div></div>`, "", "书架");
  if (kind === "grid") return phone(`${top}<div class="tabs"><span class="active">全部</span><span>在读</span><span>已读</span><span>收藏</span></div><div style="height:18px"></div>${bookGrid()}</div>`, "", "书架");
  if (kind === "list") return phone(`${top}<div class="tabs"><span class="active">全部</span><span>在读</span><span>已读</span><span>收藏</span></div>${bookRows()}</div>`, "", "书架");
  if (kind === "search") return phone(`${appBar("搜索书架", true, false)}<div class="phone-content"><div class="search-field focused">⌕　山海</div><div style="margin-top:22px">${bookRows().replaceAll("book-list","book-list search-list")}</div></div>`);
  if (kind === "sort") return phone(`${top}${bookGrid()}</div><div class="scrim"></div><div class="sheet"><div class="sheet-handle"></div><h3>排序与筛选</h3><div class="radio-list"><div class="radio-row"><i class="radio on"></i>最近阅读</div><div class="radio-row"><i class="radio"></i>添加时间</div><div class="radio-row"><i class="radio"></i>书名</div><div class="radio-row"><i class="radio"></i>阅读进度</div></div><button class="primary-cta" style="margin-top:16px">应用</button></div>`);
  return phone(`${appBar("已选择 2 本", true)}<div class="phone-content"><div class="tabs"><span class="active">全部</span><span>在读</span><span>已读</span></div><div style="height:18px"></div>${bookGrid()}</div><div class="bottom-nav" style="height:82px"><div class="nav-item active"><b></b>删除</div><div class="nav-item"><b></b>已读</div><div class="nav-item"><b></b>重置</div><div class="nav-item"><b></b>编辑</div></div>`);
}

function importScreen(kind) {
  const wrap = (content) => phone(`${appBar(kind==="entry"?"导入书籍":"导入", kind!=="entry", false)}<div class="phone-content">${content}</div>`, "", kind==="entry"?"导入":"");
  if (kind === "entry") return wrap(`<div class="hero-title" style="font-size:30px">把书带进来。</div><p class="body-copy">系统文件选择器负责授权，墨屿只复制你确认的内容。</p>${importOptions()}`);
  if (kind === "parsing") return wrap(`<div class="eyebrow">IMPORTING 01 / 03</div><h2 class="hero-title" style="font-size:28px">正在解析</h2><div class="task-card"><header><strong>山海之间.txt</strong><span>78%</span></header><p>GB18030 · 正在建立章节索引</p>${progress(78)}</div><div class="task-card"><header><strong>长安旧事.epub</strong><span>等待</span></header><p>将在当前任务完成后开始</p>${progress(0)}</div><p class="body-copy">离开此页面后，任务会在后台继续。</p><button class="secondary-cta">在后台继续</button>`);
  if (kind === "batch") return wrap(`<div class="eyebrow">BATCH IMPORT</div><h2 class="hero-title" style="font-size:28px">发现 12 个文件</h2>${[["可导入","9"],["重复","2"],["不支持","1"]].map(x=>`<div class="setting-row"><strong>${x[0]}</strong><b>${x[1]}</b></div>`).join("")}<div style="height:30px"></div><button class="primary-cta">导入 9 本</button><button class="secondary-cta" style="margin-top:12px">查看文件</button>`);
  if (kind === "success") return wrap(`<div class="success-mark"></div><h2 style="text-align:center;font:650 28px 'Noto Serif SC'">导入完成</h2><p class="body-copy" style="text-align:center">3 本成功 · 1 本跳过 · 0 本失败</p><div style="height:40px"></div><button class="primary-cta">查看书架</button>`);
  if (kind === "duplicate") return wrap(`<div class="hero-title" style="font-size:28px">这本书已经在书架里</div><div class="book-row"><div class="mini-cover"></div><div><h4>山海之间</h4><p>现有版本 · 48% · 2026/08/20</p></div><span>12 MB</span></div><div class="book-row"><div class="mini-cover"></div><div><h4>山海之间</h4><p>准备导入 · 文件内容一致</p></div><span>12 MB</span></div><button class="primary-cta" style="margin-top:28px">查看已有书籍</button><button class="secondary-cta" style="margin-top:12px">仍然导入</button>`);
  if (kind === "failure") return wrap(`<div class="success-mark error-mark"></div><h2 style="text-align:center;font:650 26px 'Noto Serif SC'">解析没有完成</h2><p class="body-copy" style="text-align:center">EPUB 的目录文件已损坏，现有书库没有受到影响。</p><button class="primary-cta" style="margin-top:36px">重新选择文件</button><button class="secondary-cta" style="margin-top:12px">查看详情</button>`);
  return wrap(`<div class="eyebrow">CHARSET DETECTION · 62%</div><h2 class="hero-title" style="font-size:28px">请选择正确编码</h2><div class="charset-preview">雨落在旧城的屋檐上，像有人翻动一本很厚的书。</div><div class="radio-list">${["GB18030（推荐）","UTF-8","GBK","Big5"].map((x,i)=>`<div class="radio-row"><i class="radio ${i===0?"on":""}"></i>${x}</div>`).join("")}</div><button class="primary-cta" style="margin-top:22px">使用此编码重新解析</button>`);
}

function bookDetail() {
  return phone(`${appBar("书籍详情", true)}<div class="phone-content"><div class="book-detail-hero"><div class="detail-cover"></div><div class="detail-info"><div class="eyebrow">TXT · GB18030</div><h1>山海之间</h1><p>林舟<br>最近阅读：今天 09:42</p>${progress(48)}</div></div><div class="stats-row"><div class="stat"><strong>48%</strong><span>阅读进度</span></div><div class="stat"><strong>32.6万</strong><span>字数</span></div><div class="stat"><strong>86</strong><span>章节</span></div></div><button class="primary-cta">继续阅读</button><div style="height:22px"></div>${settingRows([["目录","86 章"],["全文搜索","索引已完成"],["书签","12 条"],["重新解析","编码与章节识别"]])}</div>`);
}

function reader(kind) {
  const cls = kind === "dark" ? "reader dark" : kind === "oled" ? "reader oled" : "reader";
  const base = `<div class="reader-page"><div class="eyebrow" style="color:inherit;opacity:.55">山海之间</div><div class="reader-chapter">第十二章　雨夜来信</div>${readerText()}<div class="reader-footer"><span>12:40</span><span>48%</span></div></div>`;
  if (["light","dark","oled"].includes(kind)) return phone(base, cls);
  if (kind === "controls") return phone(`${base}<div class="reader-controls"><div class="control-top"><button class="icon-btn back"></button><strong>山海之间</strong><button class="icon-btn"></button></div><div class="control-bottom"><div style="display:flex;align-items:center;gap:12px"><span>上一章</span><div class="slider" style="flex:1"><i></i></div><span>下一章</span></div><div class="control-actions">${["目录","搜索","主题","设置"].map(x=>`<div class="control-action"><i></i>${x}</div>`).join("")}</div></div></div>`,"reader");
  if (kind === "drawer") return phone(`${base}<div class="scrim"></div><aside class="drawer"><h3>目录</h3><p class="body-copy">山海之间 · 86 章</p><div class="chapter-list">${["第九章　夜航","第十章　故人","第十一章　雾起","第十二章　雨夜来信","第十三章　晨光","第十四章　归去来"].map((x,i)=>`<div class="chapter-row ${i===3?"active":""}">${x}<span>${i===3?"▮":""}</span></div>`).join("")}</div></aside>`,`reader`);
  if (kind === "progress") return phone(`${base}<div class="scrim"></div><div class="sheet"><div class="sheet-handle"></div><h3>阅读进度</h3><div style="display:flex;justify-content:space-between;color:var(--text-3);font-size:11px"><span>第十二章</span><span>48%</span></div><div class="slider" style="margin:18px 0"><i></i></div><div class="stats-row"><div class="stat"><strong>3h 42m</strong><span>已阅读</span></div><div class="stat"><strong>4h 08m</strong><span>预计剩余</span></div><div class="stat"><strong>86</strong><span>总章节</span></div></div></div>`,`reader`);
  if (kind === "search") return phone(`${appBar("全书搜索", true, false)}<div class="phone-content"><div class="search-field focused">⌕　灯火</div><p class="body-copy">在《山海之间》中搜索</p></div>`);
  if (kind === "results") return phone(`${appBar("“灯火”的结果", true, false)}<div class="phone-content"><div class="eyebrow">12 RESULTS</div>${["远处的灯火隔着薄雾，一盏一盏亮起来。","那盏灯火仍在河岸尽头，像一句未说完的话。","城门外最后一处灯火，也在风里熄灭。"].map((x,i)=>`<div class="search-result"><header><span>第${12+i*9}章</span><span>${48+i*11}%</span></header><p>${x.replace("灯火","<mark>灯火</mark>")}</p></div>`).join("")}</div>`);
  if (kind === "bookmarks") return phone(`${appBar("书签", true, false)}<div class="phone-content"><div class="tabs"><span class="active">全部 12</span><span>本章 2</span></div>${["远处的灯火隔着薄雾，一盏一盏亮起来。","他没有回头，只把那封信重新折好。","山风穿过松林，带来潮湿的气味。"].map((x,i)=>`<div class="bookmark-row"><strong>第${12+i*5}章 · ${["雨夜来信","潮汐","山中"][i]}</strong><p>${x}</p></div>`).join("")}</div>`);
  if (kind === "reading-settings") return phone(`${base}<div class="scrim"></div><div class="sheet"><div class="sheet-handle"></div><h3>阅读设置</h3><div class="setting-row"><strong>字号</strong><div style="display:flex;gap:16px;align-items:center"><button class="icon-btn">－</button><b>18</b><button class="icon-btn">＋</button></div></div><div class="setting-row"><strong>行距</strong><div class="segmented"><button class="segment">紧</button><button class="segment active">舒适</button><button class="segment">松</button></div></div><div class="setting-row"><strong>页边距</strong><div class="segmented"><button class="segment">窄</button><button class="segment active">中</button><button class="segment">宽</button></div></div><div class="setting-row"><strong>翻页方式</strong><span>覆盖　›</span></div></div>`,`reader`);
  if (kind === "theme-settings") return phone(`${appBar("主题设置", true, false)}<div class="phone-content"><div class="theme-setting-hero"><p>雨落在旧城的屋檐上，像有人翻动一本很厚的书。远处的灯火隔着薄雾，一盏一盏亮起来。</p></div><div class="theme-setting-grid">${[["浅色","#fcfbf7","#151412","#d83a2e"],["深色","#11110f","#f8f5ed","#f2c9c3"],["OLED","#000","#f8f5ed","#a9d5ce"],["纸张","#f8f5ed","#34312d","#b92d24"]].map((x,i)=>`<div class="theme-setting-option ${i===0?"selected":""}" data-reader-theme="${i}"><strong>${x[0]}</strong><div class="palette-dots"><i style="background:${x[1]}"></i><i style="background:${x[2]}"></i><i style="background:${x[3]}"></i></div></div>`).join("")}</div><div style="height:20px"></div>${settingRows([["跟随系统","关闭",'<div class="switch"><i></i></div>'],["夜间降低对比度","开启",'<div class="switch on"><i></i></div>'],["主题切换动效","墨色扩散"]])}</div>`);
  return phone(`${appBar("字体设置", true, false)}<div class="phone-content"><div class="theme-setting-hero"><p style="font-family:'Noto Serif SC'">山川异域，风月同天。中文正文需要稳定的字面、重心和标点挤压。</p></div><div class="radio-list"><div class="radio-row"><i class="radio on"></i><div><strong>思源宋体</strong><br><span>系统字体 · 当前使用</span></div></div><div class="radio-row"><i class="radio"></i><div><strong>系统默认</strong><br><span>跟随设备字体</span></div></div><div class="radio-row"><i class="radio"></i><div><strong>导入自定义字体</strong><br><span>TTF / OTF</span></div></div></div>${settingRows([["字体粗细","常规"],["标点挤压","自动"],["删除当前字体","使用默认回退"]])}</div>`);
}

function settings(kind) {
  const wrap = (title, content) => phone(`${appBar(title, kind!=="general", false)}<div class="phone-content">${content}</div>`, "", kind==="general"?"设置":"");
  if (kind === "general") return wrap("设置", `<div class="hero-title" style="font-size:30px">阅读，按你的习惯。</div>${settingRows([["阅读设置","字号、行距、翻页"],["字体管理","2 个字体"],["主题与外观","浅色"],["阅读统计","本周 4 小时 12 分"],["备份与恢复","最近备份：昨天"],["关于与隐私","完全离线"]])}`);
  if (kind === "fonts") return wrap("字体管理", `<div class="import-option"><i class="import-icon"></i><div><h4>导入字体</h4><p>支持 TTF 与 OTF，文件保存在本机</p></div></div>${[["思源宋体","系统 · 当前使用"],["霞鹜文楷","4.8 MB · 可删除"],["系统默认","设备字体"]].map((x,i)=>`<div class="book-row"><div class="mini-cover" style="display:grid;place-items:center;font:24px 'Noto Serif SC'">字</div><div><h4>${x[0]}</h4><p>${x[1]}</p></div><span>${i===0?"✓":"⋮"}</span></div>`).join("")}`);
  if (kind === "stats") return wrap("阅读统计", `<div class="eyebrow">THIS WEEK</div><div class="hero-title">4 小时 12 分</div><p class="body-copy">比上周多 38 分钟</p><div class="chart">${[32,66,48,82,55,94,70].map((h,i)=>`<div class="bar ${i===5?"active":""}" style="--h:${h}%"><span>${["一","二","三","四","五","六","日"][i]}</span></div>`).join("")}</div><div style="height:55px"></div><div class="stats-row"><div class="stat"><strong>156</strong><span>本周翻页</span></div><div class="stat"><strong>42m</strong><span>日均阅读</span></div><div class="stat"><strong>3</strong><span>连续天数</span></div></div>`);
  if (kind === "backup") return wrap("备份与恢复", `<div class="hero-title" style="font-size:28px">把阅读状态留一份副本。</div><p class="body-copy">备份通过系统文件选择器写入你指定的位置，全程本地。</p>${settingRows([["包含小说源文件","预计 324 MB",'<div class="switch"><i></i></div>'],["包含字体","2 个字体",'<div class="switch on"><i></i></div>'],["备份文件名","MoyuBackup-20260822.zip"]])}<button class="primary-cta" style="margin-top:28px">创建备份</button><button class="secondary-cta" style="margin-top:12px">从备份恢复</button>`);
  return wrap("关于与隐私", `<div class="eyebrow">MOYU READER · 1.0.0</div><div class="hero-title" style="font-size:32px">墨屿阅读</div><p class="body-copy">一个安静、离线、以中文排版为先的本地阅读器。</p><div class="privacy-card"><p><span>账户</span><strong>不需要</strong></p><p><span>小说上传</span><strong>从不</strong></p><p><span>统计 SDK</span><strong>没有</strong></p><p><span>广告</span><strong>没有</strong></p><p><span>网络权限</span><strong>未申请</strong></p></div>${settingRows([["开源许可","查看第三方许可"],["数据位置","设备本地"],["版本信息","1.0.0 (100)"]])}`);
}

const screenDefs = [
  ["01","欢迎",()=>onboarding("welcome")],["02","离线说明",()=>onboarding("privacy")],["03","首次导入",()=>onboarding("import")],
  ["04","空书架",()=>library("empty")],["05","有书书架 / Grid",()=>library("grid")],["06","书架 / List",()=>library("list")],["07","书架搜索",()=>library("search")],["08","排序与筛选",()=>library("sort")],["09","Selection Mode",()=>library("selection")],
  ["10","导入入口",()=>importScreen("entry")],["11","导入解析中",()=>importScreen("parsing")],["12","批量导入",()=>importScreen("batch")],["13","导入成功",()=>importScreen("success")],["14","重复文件",()=>importScreen("duplicate")],["15","解析失败",()=>importScreen("failure")],["16","编码选择",()=>importScreen("charset")],
  ["17","Book Details",bookDetail],
  ["18","Light Reader",()=>reader("light")],["19","Dark Reader",()=>reader("dark")],["20","OLED Reader",()=>reader("oled")],["21","Reader Controls",()=>reader("controls")],["22","Chapter Drawer",()=>reader("drawer")],["23","Progress Panel",()=>reader("progress")],["24","Book Search",()=>reader("search")],["25","Search Results",()=>reader("results")],["26","Bookmark Panel",()=>reader("bookmarks")],["27","Reading Settings",()=>reader("reading-settings")],["28","Theme Settings",()=>reader("theme-settings")],["29","Font Settings",()=>reader("font-settings")],
  ["30","General Settings",()=>settings("general")],["31","Font Manager",()=>settings("fonts")],["32","Reading Statistics",()=>settings("stats")],["33","Backup & Restore",()=>settings("backup")],["34","About / Privacy",()=>settings("about")],
  ["35","Paper Reader",()=>phone(`<div class="reader-page"><div class="eyebrow" style="color:inherit;opacity:.55">山海之间</div><div class="reader-chapter">第十二章　雨夜来信</div>${readerText()}<div class="reader-footer"><span>12:40</span><span>48%</span></div></div>`,`reader paper`)]
];

function renderScreens() {
  document.title = "墨屿阅读 · Screens Prototype Handoff";
  $("#app").innerHTML = `<div class="board screens-board">
    ${boardHeader("03–05 — SCREENS · PROTOTYPE · HANDOFF", "完整产品流程", "35 个高保真状态，覆盖首次使用、书架、导入、详情、阅读器与设置。", "393 × 852 PRIMARY<br>360 / 412 RESPONSIVE CHECK")}
    <div class="screens-grid">${screenDefs.map(([id,title,render])=>`<article class="screen-cell"><div class="screen-label"><strong>${title}</strong><span>${id}</span></div>${render()}</article>`).join("")}</div>
    ${heading("Prototype Notes", "04 — INTERACTION")}
    <div class="motion-story">
      <div class="motion-frame"><header><span>BOOK CARD</span><span>240ms</span></header><div class="demo"><div class="book-cover" style="width:96px"><strong>山海之间</strong></div></div><footer>封面下沿成为 Detail 的标题基线，避免整卡漂浮。</footer></div>
      <div class="motion-frame"><header><span>READER BAR</span><span>220ms</span></header><div class="demo"><button class="moyu-button secondary pressed">显示控制层</button></div><footer>正文保持原位；控制层在内容上方淡入并轻移 8dp。</footer></div>
      <div class="motion-frame"><header><span>THEME</span><span>340ms</span></header><div class="demo">${icon("theme","")}</div><footer>触点向外扩散主题遮罩，背景与正文颜色同时插值。</footer></div>
      <div class="motion-frame"><header><span>BOOKMARK</span><span>180ms</span></header><div class="demo">${icon("bookmark","")}</div><footer>书签折角先翻 35°，随后主体加深；无需弹跳。</footer></div>
      <div class="motion-frame"><header><span>GRID / LIST</span><span>280ms</span></header><div class="demo"><div class="segmented"><button class="segment">Grid</button><button class="segment active">List</button></div></div><footer>封面保持 identity，元数据沿共同基线重排。</footer></div>
    </div>
    ${heading("Handoff", "05 — COMPOSE CONTRACT")}
    <div class="handoff-board">
      <div class="handoff-card"><h3>Reader Anchor</h3><p>主题、字体、方向变化后，以 chapterId + characterOffset 恢复语义位置。</p><code>ReaderAnchor(\n bookId, chapterId,\n characterOffset, percentage\n)</code></div>
      <div class="handoff-card"><h3>Theme Contract</h3><p>四套主题共享角色命名。Starter 文件采用四个同构变量集合。</p><code>background · surface\nreaderBackground · readerText\naccent · selection</code></div>
      <div class="handoff-card"><h3>Motion Contract</h3><p>Fast 150ms、Standard 240ms、Slow 340ms。Reduced Motion 保留状态，移除位移。</p><code>Fast = 150\nStandard = 240\nSlow = 340\nSpringBounce = .12</code></div>
    </div>
  </div>`;
  wireInteractions();
}

function wireInteractions() {
  document.querySelectorAll(".theme-card").forEach(card => card.addEventListener("click", () => {
    card.parentElement.querySelectorAll(".theme-card").forEach(c => c.classList.remove("selected"));
    card.classList.add("selected");
  }));
  document.querySelectorAll(".segment").forEach(seg => seg.addEventListener("click", () => {
    seg.parentElement.querySelectorAll(".segment").forEach(s => s.classList.remove("active"));
    seg.classList.add("active");
  }));
  document.querySelectorAll(".switch").forEach(sw => sw.addEventListener("click", () => sw.classList.toggle("on")));
}

if (boardType === "screens") renderScreens(); else renderComponents();
