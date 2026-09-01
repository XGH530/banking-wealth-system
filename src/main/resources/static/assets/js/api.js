/**
 * API 封装 - 所有后端接口 + JWT 拦截
 */
const API = {
  BASE: '/api',

  // ==================== 基础 fetch 封装 ====================
  async request(url, options = {}) {
    const token = localStorage.getItem('accessToken');
    const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) };
    if (token) headers['Authorization'] = 'Bearer ' + token;

    const res = await fetch(this.BASE + url, { ...options, headers });
    const json = await res.json().catch(() => ({ code: res.status, message: '网络错误' }));

    if (json.code === 200 || json.code === 201) return json.data;
    if (json.code === 401 || json.code === 403) {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('userInfo');
      location.href = '/login.html';
      throw new Error('登录已过期');
    }
    throw new Error(json.message || '请求失败');
  },

  // ==================== 鉴权 ====================
  auth: {
    login(data) { return API.request('/auth/login', { method: 'POST', body: JSON.stringify(data) }); },
    register(data) { return API.request('/auth/register', { method: 'POST', body: JSON.stringify(data) }); },
    refresh(token) { return API.request('/auth/refresh', { method: 'POST', body: JSON.stringify({ refreshToken: token }) }); },
    logout() { return API.request('/auth/logout', { method: 'POST' }); }
  },

  // ==================== 账户 ====================
  accounts: {
    list(page = 1, size = 10) { return API.request(`/accounts?current=${page}&size=${size}`); },
    get(accountNo) { return API.request(`/accounts/${accountNo}`); },
    open(data) { return API.request('/accounts', { method: 'POST', body: JSON.stringify(data) }); },
    updateStatus(id, status) { return API.request(`/accounts/${id}/status`, { method: 'PUT', body: JSON.stringify({ status }) }); },
    close(id) { return API.request(`/accounts/${id}`, { method: 'DELETE' }); }
  },

  // ==================== 交易 ====================
  transactions: {
    list(params = {}) {
      const qs = new URLSearchParams(params).toString();
      return API.request('/transactions' + (qs ? '?' + qs : ''));
    },
    deposit(data) { return API.request('/transactions/deposit', { method: 'POST', body: JSON.stringify(data) }); },
    withdraw(data) { return API.request('/transactions/withdraw', { method: 'POST', body: JSON.stringify(data) }); },
    transfer(data) { return API.request('/transactions/transfer', { method: 'POST', body: JSON.stringify(data) }); }
  },

  // ==================== 理财产品 ====================
  products: {
    list(page = 1, size = 100) { return API.request(`/products?current=${page}&size=${size}`); },
    get(id) { return API.request(`/products/${id}`); }
  },

  // ==================== 持仓 ====================
  holdings: {
    list() { return API.request('/holdings'); },
    get(holdingNo) { return API.request(`/holdings/${holdingNo}`); },
    purchase(data) { return API.request('/holdings/purchase', { method: 'POST', body: JSON.stringify(data) }); },
    redeem(data) { return API.request('/holdings/redeem', { method: 'POST', body: JSON.stringify(data) }); }
  }
};
