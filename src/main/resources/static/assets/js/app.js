/**
 * 应用框架 - 路由 / 鉴权 / 通用组件
 */
const App = {
  routes: {
    dashboard: { title: '仪表盘', icon: 'bi-speedometer2' },
    accounts: { title: '我的账户', icon: 'bi-wallet2' },
    transactions: { title: '交易流水', icon: 'bi-arrow-left-right' },
    deposit: { title: '存款', icon: 'bi-cash-coin' },
    withdraw: { title: '取款', icon: 'bi-cash-stack' },
    transfer: { title: '转账', icon: 'bi-send-fill' },
    products: { title: '理财产品', icon: 'bi-box-seam' },
    holdings: { title: '我的持仓', icon: 'bi-graph-up-arrow' }
  },

  init() {
    // 鉴权检查
    const token = localStorage.getItem('accessToken');
    if (!token) { location.href = '/login.html'; return; }

    // 渲染用户信息
    this.renderUser();

    // 设置日期
    this.updateDate();
    setInterval(() => this.updateDate(), 60000);

    // 绑定导航
    document.querySelectorAll('#navMenu .nav-item').forEach(el => {
      el.addEventListener('click', () => this.navigate(el.dataset.route));
    });

    // 初始路由
    this.navigate('dashboard');
  },

  updateDate() {
    const d = new Date();
    const weekdays = ['周日','周一','周二','周三','周四','周五','周六'];
    document.getElementById('topDate').textContent =
      `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')} ${weekdays[d.getDay()]}`;
  },

  renderUser() {
    const info = JSON.parse(localStorage.getItem('userInfo') || '{}');
    document.getElementById('userName').textContent = info.realName || info.username || '--';
    document.getElementById('userAvatar').textContent = (info.realName || info.username || 'U').charAt(0).toUpperCase();
  },

  navigate(route) {
    const cfg = this.routes[route];
    if (!cfg) return;

    // 更新导航高亮
    document.querySelectorAll('#navMenu .nav-item').forEach(el => {
      el.classList.toggle('active', el.dataset.route === route);
    });

    // 更新面包屑
    document.getElementById('breadcrumb').textContent = cfg.title;

    // 渲染页面
    const container = document.getElementById('pageContent');
    container.innerHTML = '<div class="text-center py-5"><div class="spinner-border text-primary" role="status"></div><div class="mt-2 text-muted">加载中...</div></div>';

    const renderer = Pages[route];
    if (typeof renderer === 'function') {
      renderer(container).catch(err => {
        Toast.show(err.message || '加载失败', 'error');
        container.innerHTML = `<div class="empty"><i class="bi bi-exclamation-triangle"></i><div class="empty-tip">加载失败: ${err.message}</div></div>`;
      });
    } else {
      container.innerHTML = `<div class="empty"><i class="bi bi-tools"></i><div class="empty-tip">页面开发中</div></div>`;
    }
  },

  openModal({ title, body, footer }) {
    const modalEl = document.getElementById('appModal');
    const content = document.getElementById('appModalContent');
    content.innerHTML = `
      <div class="modal-header"><h5 class="modal-title">${title}</h5><button type="button" class="btn-close" data-bs-dismiss="modal"></button></div>
      <div class="modal-body">${body}</div>
      ${footer ? `<div class="modal-footer">${footer}</div>` : ''}
    `;
    const modal = new bootstrap.Modal(modalEl);
    modal.show();
    modal._instance = modal;
    return modal;
  },

  closeModal() {
    const modalEl = document.getElementById('appModal');
    const inst = bootstrap.Modal.getInstance(modalEl);
    if (inst) inst.hide();
  },

  formatMoney(n, currency = '¥') {
    if (n === null || n === undefined) return '--';
    const num = Number(n);
    return currency + num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  },

  formatDate(dt) {
    if (!dt) return '--';
    const d = new Date(dt);
    return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`;
  }
};

const Auth = {
  logout() {
    if (!confirm('确定退出登录?')) return;
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('userInfo');
    location.href = '/login.html';
  }
};

const Toast = {
  show(msg, type = '', delay = 2500) {
    const container = document.getElementById('toastContainer');
    const div = document.createElement('div');
    div.className = `toast align-items-center ${type === 'error' ? 'text-bg-danger' : type === 'success' ? 'text-bg-success' : 'text-bg-dark'} border-0`;
    div.setAttribute('role', 'alert');
    div.innerHTML = `<div class="d-flex"><div class="toast-body">${msg}</div><button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button></div>`;
    container.appendChild(div);
    const t = new bootstrap.Toast(div, { delay });
    t.show();
    div.addEventListener('hidden.bs.toast', () => div.remove());
  },
  success(msg) { this.show(msg, 'success'); },
  error(msg) { this.show(msg, 'error'); }
};
