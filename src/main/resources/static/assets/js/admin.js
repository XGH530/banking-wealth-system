/* ============================================================
   后台管理控制台 · 前端逻辑
   - 与客户端页面共用同一个后端(Spring Boot),但走 /api/admin/** 一组管理接口
   - Token 存在 adminAccessToken,与普通客户登录的 accessToken 相互隔离,
     避免同一浏览器里"客户登录"和"管理员登录"互相顶掉
   ============================================================ */

/** 统一请求封装:返回 Result.data,失败抛出带 code 的 Error */
async function request(path, options = {}) {
  const token = localStorage.getItem('adminAccessToken');
  const headers = Object.assign({ 'Content-Type': 'application/json' }, options.headers || {});
  if (token) headers['Authorization'] = 'Bearer ' + token;

  const res = await fetch(path, Object.assign({}, options, { headers }));
  let json;
  try { json = await res.json(); } catch (e) { json = { code: res.status, message: '网络异常或服务不可用' }; }

  if (json.code === 200 || json.code === 201) return json.data;
  if (json.code === 401) localStorage.removeItem('adminAccessToken');
  const err = new Error(json.message || ('请求失败(' + json.code + ')'));
  err.code = json.code;
  throw err;
}

const Admin = {

  state: {
    tab: 'dashboard',
    pages: {},          // 每个列表页的分页状态
    charts: {},         // Chart 实例
    opsTimer: null,
    me: null
  },

  // ==================== 初始化 ====================
  init() {
    const now = new Date();
    document.getElementById('topDate').textContent =
      now.toLocaleDateString('zh-CN') + ' ' + now.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });

    document.querySelectorAll('#navMenu .nav-item').forEach(item => {
      item.addEventListener('click', () => Admin.switchTab(item.dataset.tab));
    });

    const token = localStorage.getItem('adminAccessToken');
    if (token) {
      Admin.enterConsole();
    } else {
      document.getElementById('loginUser').value = 'admin';
      document.getElementById('loginLayer').style.display = 'flex';
    }

    document.getElementById('loginPwd').addEventListener('keydown', e => {
      if (e.key === 'Enter') Admin.login();
    });
  },

  // ==================== 登录 ====================
  async login() {
    const username = document.getElementById('loginUser').value.trim();
    const password = document.getElementById('loginPwd').value;
    const hint = document.getElementById('loginHint');
    if (!username || !password) { hint.innerHTML = '<span style="color:#e53e3e">请输入账号和密码</span>'; return; }

    const btn = document.getElementById('loginBtn');
    btn.disabled = true;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm"></span> 登录中...';
    try {
      const data = await request('/api/auth/login', {
        method: 'POST',
        body: JSON.stringify({ username: username, password: password })
      });
      localStorage.setItem('adminAccessToken', data.accessToken);
      await Admin.enterConsole();
    } catch (e) {
      hint.innerHTML = '<span style="color:#e53e3e">' + esc(e.message) + '</span>';
      localStorage.removeItem('adminAccessToken');
    } finally {
      btn.disabled = false;
      btn.innerHTML = '<i class="bi bi-box-arrow-in-right"></i> 登 录';
    }
  },

  /** 校验管理员身份并进入控制台 */
  async enterConsole() {
    try {
      const me = await request('/api/admin/me');
      Admin.state.me = me;
      document.getElementById('adminName').textContent = me.realName || me.username;
      document.getElementById('adminAvatar').textContent = (me.realName || me.username).slice(0, 1).toUpperCase();
      document.getElementById('loginLayer').style.display = 'none';
      document.getElementById('appLayer').style.display = 'flex';
      Admin.switchTab('dashboard');
    } catch (e) {
      localStorage.removeItem('adminAccessToken');
      document.getElementById('appLayer').style.display = 'none';
      document.getElementById('loginLayer').style.display = 'flex';
      document.getElementById('loginHint').innerHTML =
        '<span style="color:#e53e3e">' + esc(e.message) + '</span><br>演示管理员：<b>admin / 123456</b>';
    }
  },

  logout() {
    localStorage.removeItem('adminAccessToken');
    location.reload();
  },

  // ==================== Tab 切换 ====================
  switchTab(tab) {
    Admin.state.tab = tab;
    if (Admin.state.opsTimer) { clearInterval(Admin.state.opsTimer); Admin.state.opsTimer = null; }

    document.querySelectorAll('#navMenu .nav-item').forEach(item => {
      item.classList.toggle('active', item.dataset.tab === tab);
    });
    const titles = {
      dashboard: '运营概览', users: '用户管理', accounts: '账户管理',
      txns: '交易流水', products: '理财产品', holdings: '理财持仓', ops: '运行监控'
    };
    document.getElementById('breadcrumb').textContent = titles[tab] || tab;

    const content = document.getElementById('content');
    content.innerHTML = '<div class="empty">加载中...</div>';

    const renderers = {
      dashboard: renderDashboard, users: renderUsers, accounts: renderAccounts,
      txns: renderTxns, products: renderProducts, holdings: renderHoldings, ops: renderOps
    };
    (renderers[tab] || renderDashboard)(content);
  },

  /** 供各页面调用的分页跳转 */
  go(tab, current) {
    Admin.state.pages[tab] = Object.assign({}, Admin.state.pages[tab], { current: current });
    Admin.switchTab(tab);
  }
};
/* ============================================================
   工具函数
   ============================================================ */

function esc(v) {
  if (v === null || v === undefined) return '';
  return String(v).replace(/[&<>"']/g, function (c) {
    return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c];
  });
}

function money(v) {
  const n = Number(v || 0);
  return n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function int(v) { return Number(v || 0).toLocaleString('zh-CN'); }

function mask(text, head, tail) {
  const s = String(text || '');
  if (s.length <= head + tail) return s;
  return s.slice(0, head) + '****' + s.slice(s.length - tail);
}

function toast(msg, type) {
  const wrap = document.getElementById('toastContainer');
  const el = document.createElement('div');
  el.style.cssText = 'padding:12px 22px;border-radius:12px;color:#fff;margin-bottom:10px;font-size:13.5px;' +
    'box-shadow:0 10px 30px rgba(0,0,0,.2);background:' + (type === 'error' ? '#e53e3e' : type === 'success' ? '#38a169' : '#1a2238');
  el.textContent = msg;
  wrap.appendChild(el);
  setTimeout(function () { el.remove(); }, 2600);
}

/** 生成分页控件 HTML */
function pagerHtml(tab, page) {
  const total = Number(page.total || 0);
  const current = Number(page.current || 1);
  const pages = Number(page.pages || 0);
  let buttons = '';
  const from = Math.max(1, current - 2);
  const to = Math.min(pages, from + 4);
  for (let i = from; i <= to; i++) {
    buttons += '<button class="pg ' + (i === current ? 'on' : '') + '" onclick="Admin.go(\'' + tab + '\',' + i + ')">' + i + '</button>';
  }
  return '<div class="pager">' +
    '<span class="info">共 ' + int(total) + ' 条 · 第 ' + current + '/' + (pages || 1) + ' 页</span>' +
    '<button class="pg" ' + (current <= 1 ? 'disabled' : '') + ' onclick="Admin.go(\'' + tab + '\',' + (current - 1) + ')">上一页</button>' +
    buttons +
    '<button class="pg" ' + (current >= pages ? 'disabled' : '') + ' onclick="Admin.go(\'' + tab + '\',' + (current + 1) + ')">下一页</button>' +
    '</div>';
}

/* ============================================================
   ① 运营概览
   ============================================================ */

async function renderDashboard(root) {
  let d;
  try {
    d = await request('/api/admin/overview');
  } catch (e) {
    root.innerHTML = '<div class="empty">加载失败：' + esc(e.message) + '</div>'; return;
  }

  root.innerHTML =
    '<div class="grid grid-4 mb-3">' +
      kpi('bi-people', '客户总数', int(d.userCount), '注册用户', '') +
      kpi('bi-wallet2', '账户总数', int(d.accountCount), '含活期 / 定期 / 理财账户', 'g') +
      kpi('bi-cash-stack', '存款余额合计', '¥' + money(d.totalBalance), '全部账户余额之和', 'o') +
      kpi('bi-arrow-left-right', '交易流水', int(d.txnCount), '今日 ' + int(d.todayTxnCount) + ' 笔', 'p') +
    '</div>' +
    '<div class="grid grid-4 mb-3">' +
      kpi('bi-graph-up-arrow', '理财产品', int(d.productCount), '在售产品数', '') +
      kpi('bi-briefcase', '持仓笔数', int(d.holdingCount), '客户理财持仓', 'p') +
      kpi('bi-piggy-bank', '持仓金额', '¥' + money(d.holdingAmount), '客户理财投入', 'o') +
      kpi('bi-calendar-check', '今日交易额', '¥' + money(d.todayTxnAmount), '今日 ' + int(d.todayTxnCount) + ' 笔', 'g') +
    '</div>' +
    '<div class="grid grid-2 mb-3">' +
      '<div class="card"><div class="card-title"><i class="bi bi-activity"></i> 近 7 日交易趋势</div>' +
        '<div style="height:230px"><canvas id="chartTrend"></canvas></div></div>' +
      '<div class="card"><div class="card-title"><i class="bi bi-pie-chart"></i> 交易类型分布</div>' +
        '<div style="height:230px"><canvas id="chartType"></canvas></div></div>' +
    '</div>' +
    '<div class="grid grid-2">' +
      '<div class="card"><div class="card-title"><i class="bi bi-trophy"></i> 余额 Top 5 账户</div>' +
        '<table class="admin-table"><thead><tr><th>账号</th><th>户名</th><th>类型</th><th class="num">余额</th></tr></thead><tbody>' +
        (d.topAccounts || []).map(function (a) {
          return '<tr><td class="mono">' + esc(a.accountNo) + '</td><td>' + esc(a.userName) + '</td><td>' +
            esc(a.accountTypeText) + '</td><td class="num"><b>¥' + money(a.balance) + '</b></td></tr>';
        }).join('') +
        '</tbody></table></div>' +
      '<div class="card"><div class="card-title"><i class="bi bi-diagram-3"></i> 账户状态分布</div>' +
        '<table class="admin-table"><thead><tr><th>状态</th><th class="num">账户数</th></tr></thead><tbody>' +
        (d.accountStatus || []).map(function (s) {
          return '<tr><td>' + esc(s.name) + '</td><td class="num">' + int(s.value) + '</td></tr>';
        }).join('') +
        '</tbody></table>' +
        '<div class="muted mt-3" style="font-size:12.5px">统计时间：' + esc(d.serverTime) + '</div></div>' +
    '</div>';

  drawTrendChart(d.trend || []);
  drawTypeChart(d.txnType || []);
}

function kpi(icon, label, value, sub, color) {
  return '<div class="card kpi">' +
    '<div class="ico ' + (color || '') + '"><i class="bi ' + icon + '"></i></div>' +
    '<div><div class="k-label">' + esc(label) + '</div><div class="k-value">' + value + '</div>' +
    '<div class="k-sub">' + esc(sub) + '</div></div></div>';
}

function drawTrendChart(trend) {
  const el = document.getElementById('chartTrend');
  if (!el) return;
  if (Admin.state.charts.trend) Admin.state.charts.trend.destroy();
  Admin.state.charts.trend = new Chart(el, {
    type: 'line',
    data: {
      labels: trend.map(function (t) { return t.date; }),
      datasets: [
        { label: '交易笔数', data: trend.map(function (t) { return Number(t.count || 0); }), borderColor: '#1e3c72',
          backgroundColor: 'rgba(30,60,114,.10)', fill: true, tension: .35, borderWidth: 2, pointRadius: 3 },
        { label: '交易金额', data: trend.map(function (t) { return Number(t.amount || 0); }), borderColor: '#dd6b20',
          backgroundColor: 'rgba(221,107,32,.08)', fill: false, tension: .35, borderWidth: 2, pointRadius: 3, yAxisID: 'y1' }
      ]
    },
    options: {
      responsive: true, maintainAspectRatio: false,
      plugins: { legend: { labels: { boxWidth: 12, font: { size: 12 } } } },
      scales: {
        x: { grid: { display: false }, ticks: { font: { size: 11 } } },
        y: { beginAtZero: true, ticks: { font: { size: 11 }, precision: 0 }, grid: { color: '#f1f4f9' } },
        y1: { position: 'right', beginAtZero: true, grid: { display: false }, ticks: { font: { size: 11 } } }
      }
    }
  });
}

function drawTypeChart(rows) {
  const el = document.getElementById('chartType');
  if (!el) return;
  if (Admin.state.charts.type) Admin.state.charts.type.destroy();
  const palette = ['#1e3c72', '#667eea', '#38a169', '#dd6b20', '#805ad5', '#e53e3e', '#0987a0'];
  Admin.state.charts.type = new Chart(el, {
    type: 'doughnut',
    data: {
      labels: rows.map(function (r) { return r.name; }),
      datasets: [{ data: rows.map(function (r) { return Number(r.count || 0); }), backgroundColor: palette, borderWidth: 2, borderColor: '#fff' }]
    },
    options: {
      responsive: true, maintainAspectRatio: false, cutout: '58%',
      plugins: { legend: { position: 'right', labels: { boxWidth: 12, font: { size: 12 } } } }
    }
  });
}
/* ============================================================
   ② 用户管理
   ============================================================ */

async function renderUsers(root) {
  const st = Admin.state.pages.users || (Admin.state.pages.users = { current: 1, keyword: '', status: '' });
  let data;
  try {
    const qs = new URLSearchParams({ current: st.current, size: 10 });
    if (st.keyword) qs.set('keyword', st.keyword);
    if (st.status !== '' && st.status !== null) qs.set('status', st.status);
    data = await request('/api/admin/users?' + qs.toString());
  } catch (e) {
    root.innerHTML = '<div class="empty">加载失败：' + esc(e.message) + '</div>'; return;
  }

  root.innerHTML =
    '<div class="toolbar">' +
      '<input class="inp" id="uKeyword" style="width:240px" placeholder="用户名 / 姓名 / 手机号" value="' + esc(st.keyword) + '">' +
      '<select class="inp" id="uStatus">' +
        '<option value=""' + (st.status === '' ? ' selected' : '') + '>全部状态</option>' +
        '<option value="1"' + (st.status === 1 ? ' selected' : '') + '>正常</option>' +
        '<option value="0"' + (st.status === 0 ? ' selected' : '') + '>已禁用</option>' +
      '</select>' +
      '<button class="btn btn-primary" onclick="searchUsers()"><i class="bi bi-search"></i> 查询</button>' +
      '<button class="btn btn-ghost" onclick="resetUsers()"><i class="bi bi-arrow-counterclockwise"></i> 重置</button>' +
      '<div class="spacer"></div><span class="muted" style="font-size:12.5px">共 ' + int(data.total) + ' 位客户</span>' +
    '</div>' +
    '<div class="table-wrap"><table class="admin-table">' +
      '<thead><tr><th>ID</th><th>用户名</th><th>姓名</th><th>身份证</th><th>手机号</th><th>邮箱</th><th>角色</th><th>状态</th><th>注册时间</th><th>操作</th></tr></thead>' +
      '<tbody>' + (data.records || []).map(function (u) {
        return '<tr><td>' + u.id + '</td><td><b>' + esc(u.username) + '</b></td><td>' + esc(u.realName) + '</td>' +
          '<td class="mono">' + esc(mask(u.idCard, 6, 4)) + '</td><td class="mono">' + esc(mask(u.phone, 3, 4)) + '</td>' +
          '<td class="muted">' + esc(u.email) + '</td>' +
          '<td>' + (u.role === 'ADMIN' ? '<span class="badge-soft badge-info">管理员</span>' : '<span class="badge-soft badge-gray">普通用户</span>') + '</td>' +
          '<td>' + (u.status === 1 ? '<span class="badge-soft badge-on">正常</span>' : '<span class="badge-soft badge-off">已禁用</span>') + '</td>' +
          '<td class="muted" style="font-size:12.5px">' + esc(u.createTime) + '</td>' +
          '<td>' + (u.status === 1
            ? '<button class="btn btn-ghost btn-sm" onclick="setUserStatus(' + u.id + ',0)">禁用</button>'
            : '<button class="btn btn-primary btn-sm" onclick="setUserStatus(' + u.id + ',1)">启用</button>') + '</td></tr>';
      }).join('') + '</tbody>' +
    '</table>' + (data.records && data.records.length ? pagerHtml('users', data) : '<div class="empty">暂无数据</div>') + '</div>';

  const kw = document.getElementById('uKeyword');
  if (kw) kw.addEventListener('keydown', function (e) { if (e.key === 'Enter') searchUsers(); });
}

function searchUsers() {
  const st = Admin.state.pages.users || (Admin.state.pages.users = {});
  st.keyword = document.getElementById('uKeyword').value.trim();
  st.status = document.getElementById('uStatus').value;
  st.current = 1;
  Admin.switchTab('users');
}

function resetUsers() {
  Admin.state.pages.users = { current: 1, keyword: '', status: '' };
  Admin.switchTab('users');
}

async function setUserStatus(id, status) {
  if (!confirm(status === 0 ? '确定禁用该用户？禁用后无法登录。' : '确定启用该用户？')) return;
  try {
    await request('/api/admin/users/' + id + '/status?status=' + status, { method: 'PUT' });
    toast('用户状态已更新', 'success');
    Admin.switchTab('users');
  } catch (e) { toast(e.message, 'error'); }
}

/* ============================================================
   ③ 账户管理
   ============================================================ */

async function renderAccounts(root) {
  const st = Admin.state.pages.accounts || (Admin.state.pages.accounts = { current: 1, keyword: '', status: '' });
  let data;
  try {
    const qs = new URLSearchParams({ current: st.current, size: 10 });
    if (st.keyword) qs.set('keyword', st.keyword);
    if (st.status !== '') qs.set('status', st.status);
    data = await request('/api/admin/accounts?' + qs.toString());
  } catch (e) {
    root.innerHTML = '<div class="empty">加载失败：' + esc(e.message) + '</div>'; return;
  }

  root.innerHTML =
    '<div class="toolbar">' +
      '<input class="inp" id="aKeyword" style="width:240px" placeholder="账号模糊查询" value="' + esc(st.keyword) + '">' +
      '<select class="inp" id="aStatus">' +
        '<option value=""' + (st.status === '' ? ' selected' : '') + '>全部状态</option>' +
        '<option value="1"' + (st.status === 1 ? ' selected' : '') + '>正常</option>' +
        '<option value="0"' + (st.status === 0 ? ' selected' : '') + '>冻结</option>' +
        '<option value="2"' + (st.status === 2 ? ' selected' : '') + '>销户</option>' +
      '</select>' +
      '<button class="btn btn-primary" onclick="searchAccounts()"><i class="bi bi-search"></i> 查询</button>' +
      '<button class="btn btn-ghost" onclick="resetAccounts()"><i class="bi bi-arrow-counterclockwise"></i> 重置</button>' +
      '<div class="spacer"></div><span class="muted" style="font-size:12.5px">共 ' + int(data.total) + ' 个账户</span>' +
    '</div>' +
    '<div class="table-wrap"><table class="admin-table">' +
      '<thead><tr><th>账号</th><th>户名</th><th>用户名</th><th>类型</th><th class="num">余额</th><th>币种</th><th>状态</th><th>开户日期</th><th>操作</th></tr></thead>' +
      '<tbody>' + (data.records || []).map(function (a) {
        const cls = a.status === 1 ? 'badge-on' : a.status === 0 ? 'badge-warn' : 'badge-gray';
        return '<tr><td class="mono"><b>' + esc(a.accountNo) + '</b></td><td>' + esc(a.userRealName) + '</td>' +
          '<td class="muted">' + esc(a.userName) + '</td><td>' + esc(a.accountTypeText) + '</td>' +
          '<td class="num"><b>¥' + money(a.balance) + '</b></td><td class="muted">' + esc(a.currency) + '</td>' +
          '<td><span class="badge-soft ' + cls + '">' + esc(a.statusText) + '</span></td>' +
          '<td class="muted" style="font-size:12.5px">' + esc(a.openDate) + '</td>' +
          '<td>' + (a.status === 2 ? '<span class="muted">—</span>' :
            (a.status === 1
              ? '<button class="btn btn-ghost btn-sm" onclick="setAccountStatus(' + a.id + ',0)">冻结</button>'
              : '<button class="btn btn-primary btn-sm" onclick="setAccountStatus(' + a.id + ',1)">解冻</button>')) + '</td></tr>';
      }).join('') + '</tbody>' +
    '</table>' + (data.records && data.records.length ? pagerHtml('accounts', data) : '<div class="empty">暂无数据</div>') + '</div>';

  const kw = document.getElementById('aKeyword');
  if (kw) kw.addEventListener('keydown', function (e) { if (e.key === 'Enter') searchAccounts(); });
}

function searchAccounts() {
  const st = Admin.state.pages.accounts || (Admin.state.pages.accounts = {});
  st.keyword = document.getElementById('aKeyword').value.trim();
  st.status = document.getElementById('aStatus').value;
  st.current = 1;
  Admin.switchTab('accounts');
}

function resetAccounts() {
  Admin.state.pages.accounts = { current: 1, keyword: '', status: '' };
  Admin.switchTab('accounts');
}

async function setAccountStatus(id, status) {
  if (!confirm(status === 0 ? '确定冻结该账户？冻结后不能发生交易。' : '确定解冻该账户？')) return;
  try {
    await request('/api/admin/accounts/' + id + '/status?status=' + status, { method: 'PUT' });
    toast('账户状态已更新', 'success');
    Admin.switchTab('accounts');
  } catch (e) { toast(e.message, 'error'); }
}
/* ============================================================
   ④ 交易流水
   ============================================================ */

async function renderTxns(root) {
  const st = Admin.state.pages.txns || (Admin.state.pages.txns = { current: 1, accountNo: '', txnType: '', startDate: '', endDate: '' });
  let data;
  try {
    const qs = new URLSearchParams({ current: st.current, size: 10 });
    if (st.accountNo) qs.set('accountNo', st.accountNo);
    if (st.txnType !== '') qs.set('txnType', st.txnType);
    if (st.startDate) qs.set('startDate', st.startDate);
    if (st.endDate) qs.set('endDate', st.endDate);
    data = await request('/api/admin/transactions?' + qs.toString());
  } catch (e) {
    root.innerHTML = '<div class="empty">加载失败：' + esc(e.message) + '</div>'; return;
  }

  const types = [['', '全部类型'], ['1', '存入'], ['2', '支取'], ['3', '转入'], ['4', '转出'], ['5', '利息'], ['6', '理财申购'], ['7', '理财赎回']];
  root.innerHTML =
    '<div class="toolbar">' +
      '<input class="inp" id="tAccountNo" style="width:190px" placeholder="账号" value="' + esc(st.accountNo) + '">' +
      '<select class="inp" id="tType">' + types.map(function (t) {
        return '<option value="' + t[0] + '"' + (String(st.txnType) === t[0] ? ' selected' : '') + '>' + t[1] + '</option>';
      }).join('') + '</select>' +
      '<input class="inp" type="date" id="tStart" value="' + esc(st.startDate) + '">' +
      '<span class="muted">至</span>' +
      '<input class="inp" type="date" id="tEnd" value="' + esc(st.endDate) + '">' +
      '<button class="btn btn-primary" onclick="searchTxns()"><i class="bi bi-search"></i> 查询</button>' +
      '<button class="btn btn-ghost" onclick="resetTxns()"><i class="bi bi-arrow-counterclockwise"></i> 重置</button>' +
      '<div class="spacer"></div><span class="muted" style="font-size:12.5px">共 ' + int(data.total) + ' 笔</span>' +
    '</div>' +
    '<div class="table-wrap"><table class="admin-table">' +
      '<thead><tr><th>流水号</th><th>账号</th><th>类型</th><th class="num">金额</th><th class="num">交易前</th><th class="num">交易后</th><th>对方</th><th>备注</th><th>时间</th></tr></thead>' +
      '<tbody>' + (data.records || []).map(function (t) {
        const cls = (t.txnType === 1 || t.txnType === 3 || t.txnType === 5 || t.txnType === 7) ? 'badge-on'
          : t.txnType === 6 ? 'badge-info' : 'badge-warn';
        return '<tr><td class="mono" style="font-size:12px">' + esc(t.txnNo) + '</td><td class="mono">' + esc(t.accountNo) + '</td>' +
          '<td><span class="badge-soft ' + cls + '">' + esc(t.txnTypeText) + '</span></td>' +
          '<td class="num"><b>¥' + money(t.amount) + '</b></td>' +
          '<td class="num muted">' + money(t.balanceBefore) + '</td><td class="num">' + money(t.balanceAfter) + '</td>' +
          '<td class="muted">' + esc(t.counterpartyName || t.counterpartyAccount || '—') + '</td>' +
          '<td class="muted">' + esc(t.remark || '—') + '</td>' +
          '<td class="muted" style="font-size:12.5px">' + esc(t.txnTime) + '</td></tr>';
      }).join('') + '</tbody>' +
    '</table>' + (data.records && data.records.length ? pagerHtml('txns', data) : '<div class="empty">暂无数据</div>') + '</div>';
}

function searchTxns() {
  const st = Admin.state.pages.txns || (Admin.state.pages.txns = {});
  st.accountNo = document.getElementById('tAccountNo').value.trim();
  st.txnType = document.getElementById('tType').value;
  st.startDate = document.getElementById('tStart').value;
  st.endDate = document.getElementById('tEnd').value;
  st.current = 1;
  Admin.switchTab('txns');
}

function resetTxns() {
  Admin.state.pages.txns = { current: 1, accountNo: '', txnType: '', startDate: '', endDate: '' };
  Admin.switchTab('txns');
}

/* ============================================================
   ⑤ 理财产品
   ============================================================ */

async function renderProducts(root) {
  const st = Admin.state.pages.products || (Admin.state.pages.products = { current: 1, keyword: '', status: '' });
  let data;
  try {
    const qs = new URLSearchParams({ current: st.current, size: 10 });
    if (st.keyword) qs.set('keyword', st.keyword);
    if (st.status !== '') qs.set('status', st.status);
    data = await request('/api/admin/products?' + qs.toString());
  } catch (e) {
    root.innerHTML = '<div class="empty">加载失败：' + esc(e.message) + '</div>'; return;
  }

  root.innerHTML =
    '<div class="toolbar">' +
      '<input class="inp" id="pKeyword" style="width:240px" placeholder="产品名称 / 代码" value="' + esc(st.keyword) + '">' +
      '<select class="inp" id="pStatus">' +
        '<option value=""' + (st.status === '' ? ' selected' : '') + '>全部状态</option>' +
        '<option value="0"' + (st.status === 0 ? ' selected' : '') + '>待售</option>' +
        '<option value="1"' + (st.status === 1 ? ' selected' : '') + '>募集中</option>' +
        '<option value="2"' + (st.status === 2 ? ' selected' : '') + '>运作中</option>' +
        '<option value="3"' + (st.status === 3 ? ' selected' : '') + '>已结束</option>' +
      '</select>' +
      '<button class="btn btn-primary" onclick="searchProducts()"><i class="bi bi-search"></i> 查询</button>' +
      '<button class="btn btn-ghost" onclick="resetProducts()"><i class="bi bi-arrow-counterclockwise"></i> 重置</button>' +
    '</div>' +
    '<div class="table-wrap"><table class="admin-table">' +
      '<thead><tr><th>代码</th><th>产品名称</th><th class="num">年化</th><th class="num">起投</th><th class="num">期限</th><th>风险</th>' +
      '<th class="num">总额度</th><th class="num">剩余额度</th><th>募集期</th><th>状态</th><th>调整状态</th></tr></thead>' +
      '<tbody>' + (data.records || []).map(function (p) {
        const cls = p.status === 1 ? 'badge-on' : p.status === 2 ? 'badge-info' : p.status === 0 ? 'badge-warn' : 'badge-gray';
        const riskMap = { 1: ['低风险', 'badge-on'], 2: ['中低', 'badge-info'], 3: ['中风险', 'badge-warn'], 4: ['中高', 'badge-warn'], 5: ['高风险', 'badge-off'] };
        const risk = riskMap[p.riskLevel] || ['—', 'badge-gray'];
        return '<tr><td class="mono">' + esc(p.productCode) + '</td><td><b>' + esc(p.productName) + '</b></td>' +
          '<td class="num">' + (Number(p.annualRate) * 100).toFixed(2) + '%</td>' +
          '<td class="num">¥' + money(p.minAmount) + '</td>' +
          '<td class="num">' + (p.termDays ? p.termDays + ' 天' : '活期') + '</td>' +
          '<td><span class="badge-soft ' + risk[1] + '">R' + p.riskLevel + ' ' + risk[0] + '</span></td>' +
          '<td class="num muted">' + money(p.totalQuota) + '</td>' +
          '<td class="num"><b>' + money(p.remainingQuota) + '</b></td>' +
          '<td class="muted" style="font-size:12px">' + esc(p.raiseStartDate) + ' ~ ' + esc(p.raiseEndDate) + '</td>' +
          '<td><span class="badge-soft ' + cls + '">' + esc(p.statusText) + '</span></td>' +
          '<td><select class="inp" style="height:30px;font-size:12.5px;width:104px" onchange="setProductStatus(' + p.id + ', this.value)">' +
            [['0', '待售'], ['1', '募集中'], ['2', '运作中'], ['3', '已结束']].map(function (s) {
              return '<option value="' + s[0] + '"' + (String(p.status) === s[0] ? ' selected' : '') + '>' + s[1] + '</option>';
            }).join('') +
          '</select></td></tr>';
      }).join('') + '</tbody>' +
    '</table>' + (data.records && data.records.length ? pagerHtml('products', data) : '<div class="empty">暂无数据</div>') + '</div>';

  const kw = document.getElementById('pKeyword');
  if (kw) kw.addEventListener('keydown', function (e) { if (e.key === 'Enter') searchProducts(); });
}

function searchProducts() {
  const st = Admin.state.pages.products || (Admin.state.pages.products = {});
  st.keyword = document.getElementById('pKeyword').value.trim();
  st.status = document.getElementById('pStatus').value;
  st.current = 1;
  Admin.switchTab('products');
}

function resetProducts() {
  Admin.state.pages.products = { current: 1, keyword: '', status: '' };
  Admin.switchTab('products');
}

async function setProductStatus(id, status) {
  try {
    await request('/api/admin/products/' + id + '/status?status=' + status, { method: 'PUT' });
    toast('产品状态已更新', 'success');
  } catch (e) { toast(e.message, 'error'); }
  Admin.switchTab('products');
}

/* ============================================================
   ⑥ 理财持仓
   ============================================================ */

async function renderHoldings(root) {
  const st = Admin.state.pages.holdings || (Admin.state.pages.holdings = { current: 1, status: '' });
  let data;
  try {
    const qs = new URLSearchParams({ current: st.current, size: 10 });
    if (st.status !== '') qs.set('status', st.status);
    data = await request('/api/admin/holdings?' + qs.toString());
  } catch (e) {
    root.innerHTML = '<div class="empty">加载失败：' + esc(e.message) + '</div>'; return;
  }

  root.innerHTML =
    '<div class="toolbar">' +
      '<select class="inp" id="hStatus">' +
        '<option value=""' + (st.status === '' ? ' selected' : '') + '>全部状态</option>' +
        '<option value="1"' + (st.status === 1 ? ' selected' : '') + '>持有中</option>' +
        '<option value="2"' + (st.status === 2 ? ' selected' : '') + '>已赎回</option>' +
        '<option value="3"' + (st.status === 3 ? ' selected' : '') + '>已到期</option>' +
      '</select>' +
      '<button class="btn btn-primary" onclick="searchHoldings()"><i class="bi bi-search"></i> 查询</button>' +
      '<div class="spacer"></div><span class="muted" style="font-size:12.5px">共 ' + int(data.total) + ' 笔持仓</span>' +
    '</div>' +
    '<div class="table-wrap"><table class="admin-table">' +
      '<thead><tr><th>持仓编号</th><th>客户</th><th>产品</th><th class="num">持有金额</th><th class="num">预期收益</th>' +
      '<th class="num">实际收益</th><th>申购日</th><th>到期日</th><th>状态</th></tr></thead>' +
      '<tbody>' + (data.records || []).map(function (h) {
        const cls = h.status === 1 ? 'badge-on' : h.status === 2 ? 'badge-gray' : 'badge-info';
        return '<tr><td class="mono" style="font-size:12px">' + esc(h.holdingNo) + '</td><td>' + esc(h.userName) + '</td>' +
          '<td><b>' + esc(h.productName) + '</b><div class="muted" style="font-size:12px">' + esc(h.productCode) + '</div></td>' +
          '<td class="num"><b>¥' + money(h.amount) + '</b></td>' +
          '<td class="num">' + money(h.expectedIncome) + '</td><td class="num">' + money(h.actualIncome) + '</td>' +
          '<td class="muted" style="font-size:12.5px">' + esc(h.purchaseDate) + '</td>' +
          '<td class="muted" style="font-size:12.5px">' + esc(h.maturityDate || '—') + '</td>' +
          '<td><span class="badge-soft ' + cls + '">' + esc(h.statusText) + '</span></td></tr>';
      }).join('') + '</tbody>' +
    '</table>' + (data.records && data.records.length ? pagerHtml('holdings', data) : '<div class="empty">暂无数据</div>') + '</div>';
}

function searchHoldings() {
  const st = Admin.state.pages.holdings || (Admin.state.pages.holdings = {});
  st.status = document.getElementById('hStatus').value;
  st.current = 1;
  Admin.switchTab('holdings');
}
/* ============================================================
   ⑦ 运行监控(健康检查)
   ============================================================ */

async function renderOps(root) {
  let h;
  try {
    h = await request('/api/admin/health');
  } catch (e) {
    root.innerHTML = '<div class="empty">加载失败：' + esc(e.message) + '</div>'; return;
  }

  const jvm = h.jvm || {};
  const usedPct = jvm.maxMb ? Math.min(100, Math.round(jvm.usedMb / jvm.maxMb * 100)) : 0;
  const dbOk = h.db && h.db.ok, redisOk = h.redis && h.redis.ok;

  root.innerHTML =
    '<div class="grid grid-3 mb-3">' +
      '<div class="card"><div class="card-title"><i class="bi bi-database-check"></i> 数据库</div>' +
        '<div class="health-row"><span class="dot ' + (dbOk ? 'ok' : 'bad') + '"></span>' +
        '<b>' + (dbOk ? '连接正常' : '连接异常') + '</b>' +
        '<span class="muted" style="margin-left:auto">' + (h.db ? h.db.latencyMs : '-') + ' ms</span></div>' +
        '<div class="muted" style="font-size:12.5px">' +
          esc(h.db && h.db.product ? h.db.product + ' ' + (h.db.version || '') : (h.db && h.db.error) || '') + '</div>' +
      '</div>' +
      '<div class="card"><div class="card-title"><i class="bi bi-lightning-charge"></i> Redis 缓存</div>' +
        '<div class="health-row"><span class="dot ' + (redisOk ? 'ok' : 'bad') + '"></span>' +
        '<b>' + (redisOk ? '连接正常' : '连接异常') + '</b>' +
        '<span class="muted" style="margin-left:auto">' + (h.redis ? h.redis.latencyMs : '-') + ' ms</span></div>' +
        '<div class="muted" style="font-size:12.5px">' +
          esc((h.redis && h.redis.error) || '用于 JWT 黑名单与热点账户缓存') + '</div>' +
      '</div>' +
      '<div class="card"><div class="card-title"><i class="bi bi-cpu"></i> 应用进程</div>' +
        '<div class="health-row"><span class="dot ok"></span><b>运行中</b>' +
        '<span class="muted" style="margin-left:auto">已运行 ' + esc(jvm.uptimeText) + '</span></div>' +
        '<div class="muted" style="font-size:12.5px">' + esc(h.os) + ' · JDK ' + esc(jvm.javaVersion) +
          ' · ' + jvm.processors + ' 核</div>' +
      '</div>' +
    '</div>' +
    '<div class="card" id="jvmCard"><div class="card-title"><i class="bi bi-memory"></i> JVM 堆内存</div>' +
      '<div class="d-flex justify-content-between" style="font-size:13.5px">' +
        '<span id="jvmText">已用 <b>' + jvm.usedMb + ' MB</b> / 上限 ' + jvm.maxMb + ' MB</span>' +
        '<span class="muted" id="jvmPct">提交 ' + jvm.committedMb + ' MB · 使用率 ' + usedPct + '%</span>' +
      '</div>' +
      '<div class="bar"><i id="jvmBar" style="width:' + usedPct + '%"></i></div>' +
      '<div class="muted mt-3" style="font-size:12.5px">服务端时间：' + esc(h.serverTime) + '　·　每 10 秒自动刷新</div>' +
    '</div>';

  Admin.state.opsTimer = setInterval(function () {
    if (Admin.state.tab !== 'ops') { clearInterval(Admin.state.opsTimer); Admin.state.opsTimer = null; return; }
    request('/api/admin/health').then(function (latest) {
      const j = latest.jvm || {};
      const pct = j.maxMb ? Math.min(100, Math.round(j.usedMb / j.maxMb * 100)) : 0;
      const bar = document.getElementById('jvmBar');
      const txt = document.getElementById('jvmText');
      const pc = document.getElementById('jvmPct');
      const barWrap = document.querySelector('.bar > i');
      if (bar) bar.style.width = pct + '%'; else if (barWrap) barWrap.style.width = pct + '%';
      if (txt) txt.innerHTML = '已用 <b>' + j.usedMb + ' MB</b> / 上限 ' + j.maxMb + ' MB';
      if (pc) pc.textContent = '提交 ' + j.committedMb + ' MB · 使用率 ' + pct + '%';
    }).catch(function () { /* 静默失败 */ });
  }, 10000);
}