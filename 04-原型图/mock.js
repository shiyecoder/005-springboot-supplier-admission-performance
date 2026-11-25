/* 原型共用外壳：侧边栏 / 顶栏 / 图标精灵。真实实现里由 Vue 布局组件 + Element Plus 图标承担。 */
const SPRITE = `
<svg xmlns="http://www.w3.org/2000/svg" style="display:none">
<symbol id="i-dashboard" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="9" rx="1.5"/><rect x="14" y="3" width="7" height="5" rx="1.5"/><rect x="14" y="12" width="7" height="9" rx="1.5"/><rect x="3" y="16" width="7" height="5" rx="1.5"/></symbol>
<symbol id="i-supplier" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M3 21h18"/><path d="M5 21V8l7-4 7 4v13"/><path d="M9 21v-5h6v5"/><path d="M9 11h.01M15 11h.01"/></symbol>
<symbol id="i-apply" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8z"/><path d="M14 3v5h5"/><path d="M9 13h6M9 17h4"/></symbol>
<symbol id="i-cert" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="13" rx="2"/><path d="M7 8h6M7 12h4"/><circle cx="16.5" cy="11" r="2.2"/><path d="M15.2 13.1 14.6 17l1.9-1 1.9 1-.6-3.9"/></symbol>
<symbol id="i-performance" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M3 20h18"/><path d="M6 20v-6M11 20V8M16 20v-9M21 20V5"/></symbol>
<symbol id="i-rectify" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M12 3 2.5 20h19z"/><path d="M12 9v5M12 17h.01"/></symbol>
<symbol id="i-model" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="5" r="2.4"/><circle cx="5" cy="19" r="2.4"/><circle cx="19" cy="19" r="2.4"/><path d="M10.6 7.1 6.4 16.8M13.4 7.1l4.2 9.7M7.4 19h9.2"/></symbol>
<symbol id="i-report" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M4 19V5M4 19h16"/><path d="M8 15l3-4 3 2 4-6"/></symbol>
<symbol id="i-system" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.7 1.7 0 0 0 .3 1.9l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-2.9 1.2 2 2 0 1 1-4 0 1.7 1.7 0 0 0-2.9-1.2l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1A1.7 1.7 0 0 0 3 15a2 2 0 1 1 0-4 1.7 1.7 0 0 0 1.4-2.9l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1A1.7 1.7 0 0 0 10 4a2 2 0 1 1 4 0 1.7 1.7 0 0 0 2.9 1.4l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1A1.7 1.7 0 0 0 21 11a2 2 0 1 1 0 4z"/></symbol>
<symbol id="i-bell" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M18 8a6 6 0 1 0-12 0c0 6-2 7-2 7h16s-2-1-2-7"/><path d="M10.3 20a2 2 0 0 0 3.4 0"/></symbol>
<symbol id="i-user" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="8" r="3.6"/><path d="M4.5 20a7.5 7.5 0 0 1 15 0"/></symbol>
<symbol id="i-search" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><circle cx="11" cy="11" r="6.5"/><path d="m16 16 4 4"/></symbol>
<symbol id="i-plus" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><path d="M12 5v14M5 12h14"/></symbol>
<symbol id="i-export" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M12 3v12"/><path d="m8 11 4 4 4-4"/><path d="M4 17v3h16v-3"/></symbol>
<symbol id="i-refresh" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M20 11a8 8 0 1 0-2.3 5.7"/><path d="M20 4v7h-7"/></symbol>
<symbol id="i-check" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m4 12 5 5L20 6"/></symbol>
<symbol id="i-close" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><path d="M6 6l12 12M18 6 6 18"/></symbol>
<symbol id="i-upload" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M12 16V4"/><path d="m7 9 5-5 5 5"/><path d="M4 17v3h16v-3"/></symbol>
<symbol id="i-star" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="m12 3 2.7 5.9 6.3.7-4.7 4.3 1.3 6.1L12 17l-5.6 3 1.3-6.1L3 9.6l6.3-.7z"/></symbol>
</svg>`;

const MENU = [
  { g: "业务工作台" },
  { k: "dashboard", t: "风险看板", i: "i-dashboard", h: "02-admin-dashboard.html" },
  { k: "supplier", t: "供应商名录", i: "i-supplier", h: "03-supplier-list.html" },
  { k: "admission", t: "准入申请", i: "i-apply", h: "05-admission-list.html", b: "4" },
  { k: "cert", t: "证照与预警", i: "i-cert", h: "07-cert-alert.html", b: "3" },
  { g: "绩效与退出" },
  { k: "performance", t: "绩效评价", i: "i-performance", h: "08-performance.html" },
  { k: "rectify", t: "整改管理", i: "i-rectify", h: "09-rectify.html" },
  { k: "model", t: "动态分层模型", i: "i-model", h: "10-model.html" },
  { g: "统计与配置" },
  { k: "report", t: "统计分析", i: "i-report", h: "11-report.html" },
  { k: "system", t: "系统管理", i: "i-system", h: "12-system.html" },
  { k: "profile", t: "个人中心", i: "i-user", h: "13-profile.html" },
];

function renderShell(active) {
  document.body.insertAdjacentHTML("afterbegin", SPRITE);
  const side = document.getElementById("sidebar");
  if (side) {
    side.className = "sidebar";
    side.innerHTML = `
      <div class="brand"><div class="logo">供</div><div><b>供应商准入与绩效</b><small>SUPPLIER GOVERNANCE</small></div></div>
      <nav class="menu">${MENU.map(m => m.g ? `<div class="menu-group">${m.g}</div>`
        : `<a href="${m.h}" class="${m.k === active ? "on" : ""}"><svg class="i"><use href="#${m.i}"/></svg>${m.t}${m.b ? `<span class="badge">${m.b}</span>` : ""}</a>`).join("")}</nav>`;
  }
  const top = document.getElementById("topbar");
  if (top) {
    top.className = "topbar";
    top.innerHTML = `
      <div class="crumb">供应链管理 / <b id="crumb-last">${document.body.dataset.crumb || "风险看板"}</b></div>
      <div class="spacer"></div>
      <div class="icon-btn" title="站内消息"><svg class="i"><use href="#i-bell"/></svg><i class="dot"></i></div>
      <div class="icon-btn" title="帮助"><svg class="i"><use href="#i-user"/></svg></div>
      <div class="who">
        <img src="../avatar.png" alt="头像" onerror="this.style.visibility='hidden'">
        <div><div class="n">${document.body.dataset.user || "周敏"}</div><div class="r">${document.body.dataset.role || "采购管理员"}</div></div>
      </div>`;
  }
}
