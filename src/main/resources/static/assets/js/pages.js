/**
 * 所有页面渲染
 */
const Pages = {

  // ==================== 仪表盘 ====================
  async dashboard(container) {
    const [accounts, products, holdings, txns] = await Promise.all([
      API.accounts.list(1, 20),
      API.products.list(1, 20),
      API.holdings.list().catch(() => []),
      API.transactions.list({ current: 1, size: 6 })
    ]);

    const accts = accounts.records || accounts || [];
    const prods = products.records || products || [];
    const holds = holdings.records || holdings || [];
    const txList = txns.records || txns || [];

    const totalBalance = accts.reduce((s, a) => s + Number(a.balance || 0), 0);
    const totalHolding = holds.reduce((s, h) => s + Number(h.amount || 0), 0);
    const totalAssets = totalBalance + totalHolding;

    container.innerHTML = `
      <div class="stat-grid">
        <div class="stat-card"><div class="stat-label">账户总余额</div><div class="stat-value">${App.formatMoney(totalBalance)}</div><div class="stat-sub">${accts.length} 个账户</div></div>
        <div class="stat-card green"><div class="stat-label">理财持仓金额</div><div class="stat-value">${App.formatMoney(totalHolding)}</div><div class="stat-sub">${holds.length} 笔持仓</div></div>
        <div class="stat-card orange"><div class="stat-label">总资产</div><div class="stat-value">${App.formatMoney(totalAssets)}</div><div class="stat-sub">账户 + 理财</div></div>
        <div class="stat-card purple"><div class="stat-label">在售理财产品</div><div class="stat-value">${prods.length}</div><div class="stat-sub">可申购</div></div>
      </div>

      <div class="row g-4">
        <div class="col-lg-7">
          <div class="card">
            <div class="card-title"><i class="bi bi-list-check"></i> 最近交易流水</div>
            ${txList.length === 0 ? `
              <div class="empty"><i class="bi bi-inbox"></i><div class="empty-tip">暂无交易记录</div></div>
            ` : `
            <div class="table-wrap">
              <table class="table-custom">
                <thead><tr><th>流水号</th><th>类型</th><th>金额</th><th>账户</th><th>时间</th></tr></thead>
                <tbody>
                  ${txList.map(t => `
                    <tr>
                      <td style="font-family:monospace;color:#8a94a6">${t.txnNo}</td>
                      <td><span class="badge badge-${t.txnType<=2?'info':t.txnType<=4?'warning':'purple'}">${t.txnTypeDesc}</span></td>
                      <td style="font-weight:600;color:${[3,5,7].includes(t.txnType)?'#38a169':'#e53e3e'}">${[3,5,7].includes(t.txnType)?'+':'-'}${App.formatMoney(t.amount)}</td>
                      <td style="font-family:monospace;font-size:13px">${t.accountNo}</td>
                      <td style="font-size:13px;color:#8a94a6">${App.formatDate(t.txnTime)}</td>
                    </tr>
                  `).join('')}
                </tbody>
              </table>
            </div>`}
          </div>
        </div>

        <div class="col-lg-5">
          <div class="card">
            <div class="card-title"><i class="bi bi-wallet2"></i> 我的账户概览</div>
            ${accts.length === 0 ? `
              <div class="empty"><i class="bi bi-inbox"></i><div class="empty-tip">暂无账户</div></div>
            ` : `
            <div class="account-grid">
              ${accts.map(a => `
                <div class="account-card ${a.status===0?'frozen':''}">
                  <div class="account-status">${a.statusDesc}</div>
                  <div class="account-type">${a.accountTypeDesc}</div>
                  <div class="account-no">${a.accountNo}</div>
                  <div class="account-balance-label">可用余额</div>
                  <div class="account-balance">${App.formatMoney(a.balance)}</div>
                </div>
              `).join('')}
            </div>`}
          </div>
        </div>
      </div>
    `;
  },

  // ==================== 我的账户 ====================
  async accounts(container) {
    const res = await API.accounts.list(1, 50);
    const accts = res.records || res || [];

    container.innerHTML = `
      <div class="card mb-4">
        <div class="card-title"><i class="bi bi-wallet2"></i> 我的账户 (${accts.length})</div>
        ${accts.length === 0 ? `
          <div class="empty"><i class="bi bi-inbox"></i><div class="empty-tip">暂无账户</div></div>
        ` : `
        <div class="account-grid">
          ${accts.map(a => `
            <div class="account-card ${a.status===0?'frozen':''}" onclick="App.navigate('transactions')">
              <div class="account-status">${a.statusDesc}</div>
              <div class="account-type">${a.accountTypeDesc} · ${a.currency}</div>
              <div class="account-no">${a.accountNo}</div>
              <div class="account-balance-label">可用余额</div>
              <div class="account-balance">${App.formatMoney(a.balance)}</div>
              <div style="font-size:12px;opacity:.7;margin-top:10px">开户日: ${a.openDate || '--'}</div>
            </div>
          `).join('')}
        </div>`}
      </div>

      ${accts.length > 0 ? `
      <div class="card">
        <div class="card-title"><i class="bi bi-list-columns"></i> 账户明细表</div>
        <div class="table-wrap">
          <table class="table-custom">
            <thead><tr><th>账号</th><th>户名</th><th>类型</th><th>余额</th><th>币种</th><th>状态</th><th>操作</th></tr></thead>
            <tbody>
              ${accts.map(a => `
                <tr>
                  <td style="font-family:monospace">${a.accountNo}</td>
                  <td>${a.realName}</td>
                  <td>${a.accountTypeDesc}</td>
                  <td style="font-weight:600">${App.formatMoney(a.balance)}</td>
                  <td>${a.currency}</td>
                  <td><span class="badge ${a.status===1?'badge-success':a.status===0?'badge-warning':'badge-gray'}">${a.statusDesc}</span></td>
                  <td>
                    ${a.status===1 ? `<button class="btn btn-sm btn-outline" onclick="Pages._freeze(${a.id},0)" style="background:#fff7ed;color:#b7791f;border:1px solid #fed7aa">冻结</button>` : a.status===0 ? `<button class="btn btn-sm btn-outline" onclick="Pages._freeze(${a.id},1)" style="background:#c6f6d5;color:#22543d;border:1px solid #9ae6b4">解冻</button>` : `<span class="badge badge-gray">已销户</span>`}
                  </td>
                </tr>
              `).join('')}
            </tbody>
          </table>
        </div>
      </div>` : ''}
    `;
  },

  async _freeze(id, status) {
    const label = status === 1 ? '解冻' : '冻结';
    if (!confirm(`确定要${label}该账户吗?`)) return;
    try {
      await API.accounts.updateStatus(id, status);
      Toast.success(`${label}成功`);
      App.navigate('accounts');
    } catch(e) { Toast.error(e.message); }
  },

  // ==================== 交易流水 ====================
  async transactions(container) {
    const res = await API.transactions.list({ current: 1, size: 50 });
    const txns = res.records || res || [];

    container.innerHTML = `
      <div class="card">
        <div class="card-title"><i class="bi bi-arrow-left-right"></i> 交易流水 (${txns.length})</div>
        ${txns.length === 0 ? `
          <div class="empty"><i class="bi bi-inbox"></i><div class="empty-tip">暂无交易记录,去做几笔存款/取款吧</div></div>
        ` : `
        <div class="table-wrap">
          <table class="table-custom">
            <thead><tr><th>流水号</th><th>账号</th><th>类型</th><th>金额</th><th>前余额</th><th>后余额</th><th>对方</th><th>时间</th></tr></thead>
            <tbody>
              ${txns.map(t => `
                <tr>
                  <td style="font-family:monospace;color:#8a94a6;font-size:13px">${t.txnNo}</td>
                  <td style="font-family:monospace;font-size:13px">${t.accountNo}</td>
                  <td><span class="badge ${[1,3,5,7].includes(t.txnType)?'badge-success':'badge-warning'}">${t.txnTypeDesc}</span></td>
                  <td style="font-weight:700;color:${[1,3,5,7].includes(t.txnType)?'#38a169':'#e53e3e'}">${[1,3,5,7].includes(t.txnType)?'+':'-'}${App.formatMoney(t.amount)}</td>
                  <td style="color:#8a94a6;font-size:13px">${App.formatMoney(t.balanceBefore)}</td>
                  <td style="font-weight:600">${App.formatMoney(t.balanceAfter)}</td>
                  <td>${t.counterpartyAccount ? `${t.counterpartyName||''}(${t.counterpartyAccount})` : '-'}</td>
                  <td style="color:#8a94a6;font-size:13px">${App.formatDate(t.txnTime)}</td>
                </tr>
              `).join('')}
            </tbody>
          </table>
        </div>`}
      </div>
    `;
  },

  // ==================== 存款 ====================
  async deposit(container) {
    const res = await API.accounts.list(1, 50);
    const accts = (res.records || res || []).filter(a => a.status === 1);

    container.innerHTML = `
      <div class="row g-4">
        <div class="col-lg-6">
          <div class="card">
            <div class="card-title"><i class="bi bi-cash-coin"></i> 存款</div>
            <form id="depositForm" onsubmit="return Pages._submitDeposit(event)">
              <div class="mb-3">
                <label class="form-label-custom">选择账户</label>
                <select class="form-control form-control-custom" id="depositAccount" required>
                  <option value="">-- 请选择账户 --</option>
                  ${accts.map(a => `<option value="${a.accountNo}">${a.accountNo} (${a.accountTypeDesc}) - ${App.formatMoney(a.balance)}</option>`).join('')}
                </select>
              </div>
              <div class="mb-3">
                <label class="form-label-custom">存款金额</label>
                <input type="number" step="0.01" min="0.01" class="form-control form-control-custom" id="depositAmount" placeholder="请输入存款金额" required>
              </div>
              <div class="mb-4">
                <label class="form-label-custom">备注</label>
                <input type="text" class="form-control form-control-custom" id="depositRemark" placeholder="选填" maxlength="100">
              </div>
              <button type="submit" class="btn btn-primary w-100">确认存款</button>
            </form>
          </div>
        </div>
        <div class="col-lg-6">
          <div class="card">
            <div class="card-title"><i class="bi bi-info-circle"></i> 存款须知</div>
            <ul style="line-height:2;color:#4a5568;font-size:14px">
              <li>存款实时到账,交易即时生效</li>
              <li>请仔细核对存款账户号</li>
              <li>每笔存款都会生成独立流水号</li>
              <li>定期账户存款按定期利率计息</li>
            </ul>
          </div>
        </div>
      </div>
    `;
  },
  async _submitDeposit(e) {
    e.preventDefault();
    const data = {
      accountNo: document.getElementById('depositAccount').value,
      amount: Number(document.getElementById('depositAmount').value),
      remark: document.getElementById('depositRemark').value
    };
    if (!data.amount || data.amount <= 0) { Toast.error('金额须大于0'); return false; }
    try {
      await API.transactions.deposit(data);
      Toast.success('存款成功');
      document.getElementById('depositForm').reset();
      setTimeout(() => App.navigate('transactions'), 800);
    } catch(err) { Toast.error(err.message); }
    return false;
  },

  // ==================== 取款 ====================
  async withdraw(container) {
    const res = await API.accounts.list(1, 50);
    const accts = (res.records || res || []).filter(a => a.status === 1);

    container.innerHTML = `
      <div class="row g-4">
        <div class="col-lg-6">
          <div class="card">
            <div class="card-title"><i class="bi bi-cash-stack"></i> 取款</div>
            <form id="withdrawForm" onsubmit="return Pages._submitWithdraw(event)">
              <div class="mb-3">
                <label class="form-label-custom">选择账户</label>
                <select class="form-control form-control-custom" id="withdrawAccount" required>
                  <option value="">-- 请选择账户 --</option>
                  ${accts.map(a => `<option value="${a.accountNo}">${a.accountNo} (${a.accountTypeDesc}) - 可用 ${App.formatMoney(a.balance)}</option>`).join('')}
                </select>
              </div>
              <div class="mb-3">
                <label class="form-label-custom">取款金额</label>
                <input type="number" step="0.01" min="0.01" class="form-control form-control-custom" id="withdrawAmount" placeholder="请输入取款金额" required>
              </div>
              <div class="mb-4">
                <label class="form-label-custom">备注</label>
                <input type="text" class="form-control form-control-custom" id="withdrawRemark" placeholder="选填" maxlength="100">
              </div>
              <button type="submit" class="btn btn-danger w-100">确认取款</button>
            </form>
          </div>
        </div>
        <div class="col-lg-6">
          <div class="card">
            <div class="card-title"><i class="bi bi-info-circle"></i> 取款须知</div>
            <ul style="line-height:2;color:#4a5568;font-size:14px">
              <li>取款金额不能超过账户可用余额</li>
              <li>定期账户取款可能产生罚息</li>
              <li>取款成功后余额实时更新</li>
              <li>每笔取款都会生成独立流水号</li>
            </ul>
          </div>
        </div>
      </div>
    `;
  },
  async _submitWithdraw(e) {
    e.preventDefault();
    const data = {
      accountNo: document.getElementById('withdrawAccount').value,
      amount: Number(document.getElementById('withdrawAmount').value),
      remark: document.getElementById('withdrawRemark').value
    };
    if (!data.amount || data.amount <= 0) { Toast.error('金额须大于0'); return false; }
    try {
      await API.transactions.withdraw(data);
      Toast.success('取款成功');
      document.getElementById('withdrawForm').reset();
      setTimeout(() => App.navigate('transactions'), 800);
    } catch(err) { Toast.error(err.message); }
    return false;
  },

  // ==================== 转账 ====================
  async transfer(container) {
    const res = await API.accounts.list(1, 50);
    const accts = (res.records || res || []).filter(a => a.status === 1);

    container.innerHTML = `
      <div class="row g-4">
        <div class="col-lg-6">
          <div class="card">
            <div class="card-title"><i class="bi bi-send-fill"></i> 转账</div>
            <form id="transferForm" onsubmit="return Pages._submitTransfer(event)">
              <div class="mb-3">
                <label class="form-label-custom">转出账户</label>
                <select class="form-control form-control-custom" id="fromAccount" required>
                  <option value="">-- 请选择账户 --</option>
                  ${accts.map(a => `<option value="${a.accountNo}">${a.accountNo} (${a.accountTypeDesc}) - 可用 ${App.formatMoney(a.balance)}</option>`).join('')}
                </select>
              </div>
              <div class="mb-3">
                <label class="form-label-custom">转入账户</label>
                <input type="text" class="form-control form-control-custom" id="toAccount" placeholder="请输入对方账户号" required>
              </div>
              <div class="mb-3">
                <label class="form-label-custom">对方户名</label>
                <input type="text" class="form-control form-control-custom" id="toName" placeholder="请输入对方户名(选填)">
              </div>
              <div class="mb-3">
                <label class="form-label-custom">转账金额</label>
                <input type="number" step="0.01" min="0.01" class="form-control form-control-custom" id="transferAmount" placeholder="请输入转账金额" required>
              </div>
              <div class="mb-4">
                <label class="form-label-custom">备注</label>
                <input type="text" class="form-control form-control-custom" id="transferRemark" placeholder="选填" maxlength="100">
              </div>
              <button type="submit" class="btn btn-primary w-100">确认转账</button>
            </form>
          </div>
        </div>
        <div class="col-lg-6">
          <div class="card">
            <div class="card-title"><i class="bi bi-info-circle"></i> 转账须知</div>
            <ul style="line-height:2;color:#4a5568;font-size:14px">
              <li>请仔细核对转入账户号和户名</li>
              <li>转账金额不能超过转出账户可用余额</li>
              <li>本系统为模拟转账,即时到账</li>
              <li>双向流水号会同时生成</li>
            </ul>
          </div>
        </div>
      </div>
    `;
  },
  async _submitTransfer(e) {
    e.preventDefault();
    const data = {
      fromAccountNo: document.getElementById('fromAccount').value,
      toAccountNo: document.getElementById('toAccount').value.trim(),
      toName: document.getElementById('toName').value.trim(),
      amount: Number(document.getElementById('transferAmount').value),
      remark: document.getElementById('transferRemark').value.trim()
    };
    if (!data.amount || data.amount <= 0) { Toast.error('金额须大于0'); return false; }
    if (!data.toAccountNo) { Toast.error('请输入转入账户'); return false; }
    try {
      await API.transactions.transfer(data);
      Toast.success('转账成功');
      document.getElementById('transferForm').reset();
      setTimeout(() => App.navigate('transactions'), 800);
    } catch(err) { Toast.error(err.message); }
    return false;
  },

  // ==================== 理财产品 ====================
  async products(container) {
    const res = await API.products.list(1, 100);
    const products = res.records || res || [];
    const holdings = await API.holdings.list().catch(() => []);

    const riskMap = { 1: '低', 2: '低', 3: '中', 4: '中高', 5: '高' };
    const riskClass = { 1: 'low', 2: 'low', 3: '', 4: 'high', 5: 'high' };

    container.innerHTML = `
      <div class="card">
        <div class="card-title"><i class="bi bi-box-seam"></i> 在售理财产品 (${products.length})</div>
        ${products.length === 0 ? `
          <div class="empty"><i class="bi bi-inbox"></i><div class="empty-tip">暂无可申购产品</div></div>
        ` : `
        <div class="product-grid">
          ${products.map(p => `
            <div class="product-card" onclick="Pages._buyProduct(${p.id})">
              <div class="d-flex justify-content-between align-items-start">
                <div class="product-rate">${p.annualRate}<span>%</span></div>
                <span class="product-risk ${riskClass[p.riskLevel]}">${p.riskLevelDesc} · ${riskMap[p.riskLevel]}</span>
              </div>
              <div class="product-name">${p.productName}</div>
              <div class="product-code">${p.productCode} · ${p.productTypeDesc}</div>
              <div class="product-meta">
                <div class="product-meta-item"><div class="product-meta-value">${App.formatMoney(p.minAmount,'¥')}</div><div class="product-meta-label">最低申购</div></div>
                <div class="product-meta-item"><div class="product-meta-value">${p.termDays||'活期'}</div><div class="product-meta-label">期限(天)</div></div>
                <div class="product-meta-item"><div class="product-meta-value">${App.formatMoney(p.remainingQuota,'¥')}</div><div class="product-meta-label">剩余额度</div></div>
              </div>
              <div class="mt-3">
                <button class="btn btn-primary btn-sm w-100" onclick="event.stopPropagation();Pages._buyProduct(${p.id})">立即申购</button>
              </div>
            </div>
          `).join('')}
        </div>`}
      </div>
    `;
  },
  async _buyProduct(productId) {
    const product = await API.products.get(productId);
    const res = await API.accounts.list(1, 50);
    const accts = (res.records || res || []).filter(a => a.status === 1 && a.balance >= product.minAmount);

    const body = `
      <div style="margin-bottom:16px">
        <div style="font-size:22px;font-weight:700;color:#1e3c72">${product.annualRate}% <span style="font-size:12px;color:#8a94a6">年化收益率</span></div>
        <div style="font-size:16px;font-weight:600;margin:8px 0 4px">${product.productName}</div>
        <div style="font-size:12px;color:#8a94a6">${product.productCode} · ${product.productTypeDesc} · ${product.riskLevelDesc}</div>
      </div>
      <div class="form-grid" style="margin-top:16px">
        <div>
          <label class="form-label-custom">扣款账户</label>
          <select class="form-control form-control-custom" id="buyAccount" required>
            <option value="">-- 请选择账户 --</option>
            ${accts.map(a => `<option value="${a.accountNo}">${a.accountNo} (${a.accountTypeDesc}) - 余额 ${App.formatMoney(a.balance)}</option>`).join('')}
          </select>
        </div>
        <div>
          <label class="form-label-custom">申购金额 (最低 ${App.formatMoney(product.minAmount,'¥')})</label>
          <input type="number" step="0.01" min="${product.minAmount}" class="form-control form-control-custom" id="buyAmount" placeholder="请输入申购金额" required>
        </div>
      </div>
    `;
    const footer = `<button class="btn btn-outline" onclick="App.closeModal()">取消</button><button class="btn btn-primary" id="buyBtn">确认申购</button>`;
    const modal = App.openModal({ title: '申购理财产品', body, footer });

    document.getElementById('buyBtn').onclick = async () => {
      const accountNo = document.getElementById('buyAccount').value;
      const amount = Number(document.getElementById('buyAmount').value);
      if (!accountNo) { Toast.error('请选择扣款账户'); return; }
      if (!amount || amount < product.minAmount) { Toast.error(`金额不能低于 ${product.minAmount}`); return; }
      try {
        await API.holdings.purchase({ accountNo, productId, amount });
        Toast.success('申购成功');
        App.closeModal();
        setTimeout(() => App.navigate('holdings'), 800);
      } catch(err) { Toast.error(err.message); }
    };
  },

  // ==================== 我的持仓 ====================
  async holdings(container) {
    const res = await API.holdings.list();
    const holds = res.records || res || [];

    const totalAmount = holds.reduce((s, h) => s + Number(h.amount || 0), 0);
    const totalExpected = holds.reduce((s, h) => s + Number(h.expectedIncome || 0), 0);

    container.innerHTML = `
      <div class="stat-grid">
        <div class="stat-card"><div class="stat-label">持仓总金额</div><div class="stat-value">${App.formatMoney(totalAmount)}</div><div class="stat-sub">${holds.length} 笔持仓</div></div>
        <div class="stat-card green"><div class="stat-label">预期总收益</div><div class="stat-value">${App.formatMoney(totalExpected)}</div><div class="stat-sub">年化 4.2%-6.5%</div></div>
      </div>

      <div class="card">
        <div class="card-title"><i class="bi bi-graph-up-arrow"></i> 我的持仓明细</div>
        ${holds.length === 0 ? `
          <div class="empty"><i class="bi bi-inbox"></i><div class="empty-tip">暂无持仓,去 <a href="javascript:void(0)" onclick="App.navigate('products')" style="color:#667eea">理财产品</a> 申购一笔吧</div></div>
        ` : `
        <div class="table-wrap">
          <table class="table-custom">
            <thead><tr><th>持仓编号</th><th>产品</th><th>类型</th><th>收益率</th><th>持有金额</th><th>预期收益</th><th>申购日</th><th>到期日</th><th>状态</th><th>操作</th></tr></thead>
            <tbody>
              ${holds.map(h => `
                <tr>
                  <td style="font-family:monospace;color:#8a94a6">${h.holdingNo}</td>
                  <td style="font-weight:600">${h.productName}</td>
                  <td>${h.productTypeDesc}</td>
                  <td style="color:#2a5298;font-weight:600">${h.annualRate}%</td>
                  <td style="font-weight:700">${App.formatMoney(h.amount)}</td>
                  <td style="color:#38a169;font-weight:600">${App.formatMoney(h.expectedIncome)}</td>
                  <td style="font-size:13px;color:#8a94a6">${h.purchaseDate}</td>
                  <td style="font-size:13px;color:#8a94a6">${h.maturityDate||'活期'}</td>
                  <td><span class="badge ${h.status===1?'badge-success':h.status===2?'badge-gray':'badge-warning'}">${h.statusDesc}</span></td>
                  <td>
                    ${h.status===1 ? `<button class="btn btn-sm btn-outline" onclick="Pages._redeem('${h.holdingNo}')" style="background:#fed7d7;color:#742a2a;border:1px solid #feb2b2">赎回</button>` : `<span class="badge badge-gray">已结束</span>`}
                  </td>
                </tr>
              `).join('')}
            </tbody>
          </table>
        </div>`}
      </div>
    `;
  },
  async _redeem(holdingNo) {
    if (!confirm('确定赎回该持仓吗?赎回后将直接变现到扣款账户。')) return;
    try {
      await API.holdings.redeem({ holdingNo });
      Toast.success('赎回成功');
      App.navigate('holdings');
    } catch(err) { Toast.error(err.message); }
  }
};
