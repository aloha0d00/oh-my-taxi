/* ============================================
   Campus Carpool - Application Script
   ============================================ */
window.onerror = function(m,s,l,c,e){document.title='[ERR] '+m;alert('JS Error: '+m+' at line '+l);return false;};
console.log('=== app.js LOADED ===');
document.title = '[OK] 校园拼车';

const $ = (id) => { var el = document.getElementById(id); return el; };
const API = (function() {
  const base = (window.CONTEXT_PATH || '');
  const token = () => localStorage.getItem('token');
  return {
    async request(url, method, data) {
      const fullUrl = base + url;
      const opts = {
        method,
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include'
      };
      const t = token();
      if (t) opts.headers['Authorization'] = 'Bearer ' + t;
      if (data && (method === 'POST' || method === 'PUT')) opts.body = JSON.stringify(data);

      const resp = await fetch(fullUrl, opts);
      const text = await resp.text();
      let body;
      try { body = JSON.parse(text); } catch(e) { body = { message: text }; }

      if (!resp.ok) {
        var msg = body.message || body.error || '';
        if (!msg) {
          if (resp.status === 401) msg = '登录已过期，请重新登录';
          else if (resp.status === 403) msg = '没有权限执行此操作';
          else if (resp.status === 404) msg = '请求的资源不存在';
          else if (resp.status === 400) msg = '请求参数有误';
          else msg = '服务器错误(' + resp.status + ')';
        }
        const err = new Error(msg);
        err.status = resp.status;
        throw err;
      }
      return body;
    },
    get(url) { return this.request(url, 'GET'); },
    post(url, data) { return this.request(url, 'POST', data); },
    put(url, data) { return this.request(url, 'PUT', data); },
    del(url) { return this.request(url, 'DELETE'); }
  };
})();

// Toast notification
function toast(msg, type) {
  type = type || 'success';
  if (!msg) return;
  // Also alert errors for visibility
  if (type === 'error') alert(msg);
  var c = document.getElementById('toastContainer');
  if (!c) { console.error('toastContainer missing'); return; }
  var el = document.createElement('div');
  el.className = 'toast toast-' + type;
  el.textContent = msg;
  el.style.position = 'relative';
  c.appendChild(el);
  setTimeout(function(){ el.style.opacity = '0'; setTimeout(function(){ if(el.parentNode) el.parentNode.removeChild(el); }, 300); }, 3000);
}

// Modal helpers
function openModal(id) { console.log('openModal:', id); const el = $(id); if(el) { el.classList.remove('d-none'); console.log('modal opened, has d-none:', el.classList.contains('d-none')); } else { console.error('Modal not found:', id); } }
function closeModal(id) { const el = $(id); if(el) el.classList.add('d-none'); }

// Format helpers
function fmtDate(ts) {
  if (!ts) return '-';
  const d = new Date(ts);
  return d.toLocaleDateString('zh-CN') + ' ' + d.toLocaleTimeString('zh-CN', {hour:'2-digit',minute:'2-digit'});
}
function fmtDateShort(ts) {
  if (!ts) return '-';
  return new Date(ts).toLocaleDateString('zh-CN');
}
function esc(str) {
  if (!str) return '';
  return String(str).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}
function statusBadge(s) {
  const m = { pending: ['待处理','status-pending'], ongoing: ['进行中','status-ongoing'],
    completed: ['已完成','status-completed'], cancelled: ['已取消','status-cancelled'],
    accepted: ['已同意','status-accepted'], rejected: ['已拒绝','status-rejected'] };
  const v = m[s] || [s, 'status-pending'];
  return '<span class="status-badge ' + v[1] + '">' + v[0] + '</span>';
}

// ========== App State ==========
const App = {
  user: null,

  init() {
    // Visible debug
    var d = document.getElementById('breadcrumb');
    if (d) d.innerHTML = '<b style=\"color:green\">JS已加载...</b>';
    try { this.bindEvents(); if(d) d.innerHTML += ' 绑定OK'; } catch(e) { if(d) d.innerHTML += ' 绑定失败:'+e.message; }
    try { this.checkAuth(); if(d) d.innerHTML += ' 认证OK'; } catch(e) { if(d) d.innerHTML += ' 认证失败:'+e.message; }
    try { this.loadTrips(); if(d) d.innerHTML += ' 加载OK'; } catch(e) { if(d) d.innerHTML += ' 加载失败:'+e.message; }
  },

  checkAuth() {
    const t = localStorage.getItem('token');
    if (t) { this.fetchUser(); } else { this.updateUI(); }
  },

  async fetchUser() {
    try {
      this.user = await API.get('/api/users/me');
      this.updateUI();
    } catch(e) {
      localStorage.removeItem('token');
      this.user = null;
      this.updateUI();
    }
  },

  showSection(id) {
    $('homeDashboard').classList.add('d-none');
    const sections = ['tripListSection','driverTripsSection','myAppsSection','vehiclesSection',
      'passengersSection','adminDashboardSection','adminUsersSection','adminVehiclesSection','adminTripsSection'];
    sections.forEach(s => { const el = $(s); if(el) el.classList.add('d-none'); });
    const target = $(id);
    if(target) target.classList.remove('d-none');
    // Update breadcrumb
    var bc = { tripListSection:'行程列表', driverTripsSection:'我的行程', myAppsSection:'我的申请',
      vehiclesSection:'我的车辆', passengersSection:'乘客信息', adminDashboardSection:'仪表盘',
      adminUsersSection:'用户管理', adminVehiclesSection:'车辆管理', adminTripsSection:'行程管理' };
    var name = bc[id] || id;
    var b = $('breadcrumb');
    if (b) {
      if (id.indexOf('admin') === 0) b.innerHTML = '<a href="#" onclick="App.goHome();return false" style="color:var(--primary)">首页</a> &rsaquo; <a href="#" onclick="App.adminShowDashboard();return false" style="color:var(--primary)">管理后台</a> &rsaquo; ' + name;
      else if (id === 'passengersSection') b.innerHTML = '<a href="#" onclick="App.goHome();return false" style="color:var(--primary)">首页</a> &rsaquo; <a href="#" onclick="App.showSection(\'driverTripsSection\');App.loadDriverTrips();return false" style="color:var(--primary)">我的行程</a> &rsaquo; ' + name;
      else b.innerHTML = '<a href="#" onclick="App.goHome();return false" style="color:var(--primary)">首页</a> &rsaquo; ' + name;
    }
  },

  goHome() {
    if (!this.user) { this.showSection('tripListSection'); this.loadTrips(); return; }
    if (this.user.role === 'admin') { this.adminShowDashboard(); return; }
    this.showHomeDashboard();
  },

  showHomeDashboard() {
    var sections = ['tripListSection','driverTripsSection','myAppsSection','vehiclesSection',
      'passengersSection','adminDashboardSection','adminUsersSection','adminVehiclesSection','adminTripsSection'];
    sections.forEach(function(s) { var el = $(s); if(el) el.classList.add('d-none'); });
    $('homeDashboard').classList.remove('d-none');
    $('breadcrumb').innerHTML = '<span style=\"color:var(--gray-500)\">首页</span>';
    // Restore hero for logged-in users
    var hero = document.querySelector('.hero');
    if (hero && this.user) hero.innerHTML = '<h2>校园拼车，出行无忧</h2><p>安全、便捷、经济的校园出行平台</p>';
  },

  updateUI() {
    const u = this.user;
    const authed = !!u;
    $('btnLogin').classList.toggle('d-none', authed);
    $('btnRegister').classList.toggle('d-none', authed);
    $('userArea').classList.toggle('d-none', !authed);
    $('actionCards').classList.toggle('d-none', !authed);

    if (authed) {
      $('userGreeting').textContent = u.name;
      const rb = $('userRoleBadge');
      rb.textContent = u.role;
      rb.className = 'role-badge role-' + u.role;

      const isDriver = u.role === 'driver';
      const isStudent = u.role === 'student';
      const isAdmin = u.role === 'admin';

      // Build action cards
      var cards = [];
      if (!isAdmin) cards.push({id:'cardSearch',icon:'#eef2ff',icolor:'var(--primary)',label:'查找行程',desc:'搜索可用拼车',click:'App.showSection(&quot;tripListSection&quot;);App.loadTrips()'});
      if (isStudent) cards.push({id:'cardHistory',icon:'#ecfdf5',icolor:'var(--success)',label:'历史行程',desc:'查看出行记录',click:'App.showSection(\"myAppsSection\");App.loadMyApplications()'});
      if (isDriver) { cards.push({id:'cardMyTrips',icon:'#ecfdf5',icolor:'var(--success)',label:'我的行程',desc:'管理发布的行程',click:'App.showSection(\"driverTripsSection\");App.loadDriverTrips()'}); cards.push({id:'cardMyVehicles',icon:'#eef2ff',icolor:'var(--primary)',label:'我的车辆',desc:'管理车辆信息',click:'App.showSection(\"vehiclesSection\");App.loadVehicles()'}); }
      if (isStudent) cards.push({id:'cardMyApps',icon:'#fffbeb',icolor:'var(--warning)',label:'我的申请',desc:'查看申请状态',click:'App.showSection(\"myAppsSection\");App.loadMyApplications()'});
      if (isAdmin) cards.push({id:'cardAdmin',icon:'#fef2f2',icolor:'var(--danger)',label:'管理后台',desc:'系统管理',click:'App.adminShowDashboard()'});
      $('actionGrid').innerHTML = cards.map(function(c){return '<div class=\"action-card\" onclick=\"'+c.click.replace(/\"/g,'&quot;')+'\"><div class=\"ac-icon\" style=\"background:'+c.icon+';color:'+c.icolor+'\">'+(c.label.charAt(0))+'</div><div class=\"ac-label\">'+c.label+'</div><div class=\"ac-desc\">'+c.desc+'</div></div>';}).join('');

      if (isAdmin) { this.adminShowDashboard(); }
      else { this.showHomeDashboard(); }
    } else {
      // Unauthenticated: show welcome/login page
      var sections = ['tripListSection','driverTripsSection','myAppsSection','vehiclesSection',
        'passengersSection','adminDashboardSection','adminUsersSection','adminVehiclesSection','adminTripsSection'];
      sections.forEach(function(s) { var el = $(s); if(el) el.classList.add('d-none'); });
      $('homeDashboard').classList.remove('d-none');
      $('actionCards').classList.add('d-none');
      $('breadcrumb').innerHTML = '<span style=\"color:var(--gray-500)\">首页</span>';
      // Replace hero content with welcome message
      var hero = document.querySelector('.hero');
      if (hero) { hero.innerHTML = '<h2>欢迎使用校园拼车</h2><p>登录或注册以开始使用</p><div style=\"margin-top:16px\"><button class=\"btn btn-lg\" onclick=\"openModal(&quot;loginModal&quot;)\" style=\"background:#fff;color:var(--primary);margin-right:8px;font-weight:600\">登录</button><button class=\"btn btn-lg\" onclick=\"openModal(&quot;registerModal&quot;)\" style=\"background:rgba(255,255,255,.2);color:#fff;font-weight:600\">注册</button></div>'; }
    }
  },

  // ====== Event Binding ======
  bindEvents() {
    const on = (id, evt, fn) => { const el = document.getElementById(id); if (el) el.addEventListener(evt, fn); };

    on('btnLogin', 'click', () => openModal('loginModal'));
    on('btnRegister', 'click', () => openModal('registerModal'));
    on('btnLogout', 'click', () => this.logout());

    // Home action cards
    on('cardSearchTrips', 'click', () => this.showHomeDashboard());
    on('cardHistory', 'click', () => { this.showSection('myAppsSection'); this.loadMyApplications(); });
    on('cardMyTrips', 'click', () => { this.showSection('driverTripsSection'); this.loadDriverTrips(); });
    on('cardMyVehicles', 'click', () => { this.showSection('vehiclesSection'); this.loadVehicles(); });
    on('cardMyApps', 'click', () => { this.showSection('myAppsSection'); this.loadMyApplications(); });
    on('cardAdmin', 'click', () => this.adminShowDashboard());
    on('btnSearch', 'click', () => this.searchTrips());
    on('btnLoginSubmit', 'click', () => this.login());
    on('btnRegSubmit', 'click', () => this.register());
    on('switchToRegister', 'click', (e) => { e.preventDefault(); closeModal('loginModal'); openModal('registerModal'); });
    on('switchToLogin', 'click', (e) => { e.preventDefault(); closeModal('registerModal'); openModal('loginModal'); });

    on('btnCreateTrip', 'click', () => this.openCreateTrip());
    on('btnCreateTrip2', 'click', () => this.openCreateTrip());
    on('btnVehicles', 'click', () => this.showVehicles());
    on('btnAddVehicle', 'click', () => openModal('addVehicleModal'));
    on('btnAddVehicleSubmit', 'click', () => this.addVehicle());
    on('btnCreateTripSubmit', 'click', () => this.createTrip());
    on('btnEditVehicleSubmit', 'click', () => this.editVehicle());
    on('btnBackToDriverTrips', 'click', () => { this.showSection('driverTripsSection'); this.loadDriverTrips(); });

    on('btnMyApps', 'click', () => this.loadMyApplications());
    on('btnBackToTrips', 'click', () => this.goHome());
    on('btnBackToTrips2', 'click', () => this.goHome());

    on('btnAdminDashboard', 'click', () => this.adminShowDashboard());
    on('btnAdminUsers', 'click', () => this.adminLoadUsers());
    on('btnAdminVehicles', 'click', () => this.adminLoadVehicles());
    on('btnAdminTrips', 'click', () => this.adminLoadTrips());
    on('btnAdminAddUser', 'click', () => openModal('adminAddUserModal'));
    on('btnAdminAddUserSubmit', 'click', () => this.adminAddUser());
    on('btnAdminEditUserSubmit', 'click', () => this.adminEditUser());
    on('btnAdminEditTripSubmit', 'click', () => this.adminEditTrip());
    on('btnProfile', 'click', () => this.openEditProfile());
    on('btnEditProfileSubmit', 'click', () => this.editProfile());
    on('btnRatingSubmit', 'click', () => this.submitRating());
    on('btnAdminAddVehicle', 'click', () => openModal('adminAddVehicleModal'));
    on('btnAdminAddVehicleSubmit', 'click', () => this.adminAddVehicle());
    on('btnAdminEditVehicleSubmit', 'click', () => this.adminEditVehicle());

    // Login role toggle
    on('loginRole', 'change', () => this.toggleLoginRole());
    this.toggleLoginRole();
  },

  toggleLoginRole() {
    const r = $('loginRole').value;
    $('loginStudentIdGroup').classList.toggle('d-none', r !== 'student');
    $('loginPhoneGroup').classList.toggle('d-none', r === 'student');
  },

  // ====== Auth ======
  async login() {
    const role = $('loginRole').value;
    const pw = $('loginPassword').value;
    let data = { role, password: pw };
    if (role === 'student') data.studentId = $('loginStudentId').value;
    else data.phone = $('loginPhone').value;
    try {
      const r = await API.post('/api/users/login', data);
      localStorage.setItem('token', r.token);
      closeModal('loginModal');
      toast('登录成功');
      await this.fetchUser();
    } catch(e) { toast(e.message || '登录失败', 'error'); }
  },

  async register() {
    const data = {
      role: $('regRole').value,
      name: $('regName').value,
      phone: $('regPhone').value,
      password: $('regPassword').value
    };
    if (data.role === 'student') data.studentId = $('regStudentId').value;
    try {
      await API.post('/api/users/register', data);
      closeModal('registerModal');
      toast('注册成功，请登录');
      openModal('loginModal');
    } catch(e) { toast(e.message || '注册失败', 'error'); }
  },

  logout() {
    localStorage.removeItem('token');
    this.user = null;
    this.updateUI();
    this.showSection('tripListSection');
    this.loadTrips();
    toast('已退出登录');
  },

  // ====== Trip Search & Display ======
  async loadTrips() {
    try {
      const trips = await API.get('/api/trips');
      this.renderTrips(trips, 'tripGrid', 'public');
    } catch(e) { $('tripGrid').innerHTML = '<div class="empty-state"><h3>加载失败</h3><p>请稍后重试</p></div>'; }
  },

  async searchTrips() {
    const d = $('sDeparture').value, a = $('sArrival').value, dt = $('sDate').value;
    try {
      let url = '/api/trips?departureLocation=' + encodeURIComponent(d) + '&arrivalLocation=' + encodeURIComponent(a);
      if (dt) url += '&departureDate=' + dt;
      const trips = await API.get(url);
      this.renderTrips(trips, 'tripGrid', 'public');
    } catch(e) { toast('搜索失败', 'error'); }
  },

  renderTrips(trips, gridId, mode) {
    const grid = $(gridId);
    if (!trips || trips.length === 0) {
      grid.innerHTML = '<div class="empty-state"><div class="empty-icon">--</div><h3>' + (mode==='driver'?'还没有发布行程':'暂无可用行程') + '</h3><p>' + (mode==='driver'?'点击发布行程开始':'换个搜索条件试试') + '</p></div>';
      return;
    }
    grid.innerHTML = trips.map(t => this.tripCard(t, mode)).join('');
  },

  tripCard(t, mode) {
    const price = t.pricePerSeat || 0;
    const vehicle = (t.vehicle && t.vehicle.brand) ? (t.vehicle.brand + ' ' + (t.vehicle.model||'')) : '未知车辆';
    const driverName = (t.driver && t.driver.name) ? t.driver.name : '未知';
    const isDriverMode = mode === 'driver';
    const isPublic = mode === 'public';
    const tripId = t.id;

    let actions = '';
    if (isPublic) {
      actions = '<button class="btn btn-primary btn-sm" onclick="App.applyTrip(' + tripId + ')">申请拼车</button>';
    } else if (isDriverMode) {
      actions = '<button class="btn btn-outline btn-sm" onclick="App.viewApplications(' + tripId + ')">查看申请</button>' +
        '<button class="btn btn-outline btn-sm" onclick="App.viewPassengers(' + tripId + ')">查看乘客</button>';
      if (t.tripStatus === 'pending' || t.tripStatus === 'ongoing') {
        actions += '<button class="btn btn-warning btn-sm" onclick="App.endTrip(' + tripId + ',\'completed\')">结束行程</button>';
      }
      if (t.tripStatus === 'pending') {
        actions += '<button class="btn btn-danger btn-sm" onclick="App.endTrip(' + tripId + ',\'cancelled\')">取消行程</button>';
      }
    }

    return '<div class="trip-card">' +
      '<div class="trip-card-header"><div class="route">' + esc(t.departureLocation) + ' <span class="arrow">→</span> ' + esc(t.arrivalLocation) + '</div>' +
      (isDriverMode ? statusBadge(t.tripStatus) : '') + '</div>' +
      '<div class="trip-card-body">' +
      '<div class="info-row"><span class="ic">&#x1F4C5;</span>' + fmtDate(t.departureTime) + '</div>' +
      '<div class="info-row"><span class="ic">&#x1F464;</span>' + esc(driverName) + '</div>' +
      '<div class="info-row"><span class="ic">&#x1F698;</span>' + esc(vehicle) + '</div>' +
      (t.description ? '<div class="info-row"><span style="font-size:12px;color:var(--gray-400)">' + esc(t.description) + '</span></div>' : '') +
      '</div>' +
      '<div class="trip-card-footer"><div class="price">' + price + '<small> 元/人</small></div>' +
      '<div class="seats">剩余 <strong>' + (t.availableSeats||0) + '</strong> 座</div></div>' +
      (actions ? '<div class="trip-card-actions">' + actions + '</div>' : '') +
      '</div>';
  },

  // ====== Student ======
  async applyTrip(tripId) {
    if (!this.user) { toast('请先登录', 'warning'); openModal('loginModal'); return; }
    if (this.user.role !== 'student') { toast('只有学生可以申请拼车', 'warning'); return; }
    try {
      await API.post('/api/participants/trip/' + tripId, { seatsBooked: 1 });
      toast('申请成功，等待司机确认');
    } catch(e) { toast(e.message || '申请失败', 'error'); }
  },

  async loadMyApplications() {
    if (!this.user || this.user.role !== 'student') return;
    try {
      const apps = await API.get('/api/participants/student');
      this.showSection('myAppsSection');
      const grid = $('myAppsGrid');
      if (!apps || apps.length === 0) {
        grid.innerHTML = '<div class="empty-state"><h3>暂无申请</h3><p>去搜索行程并申请吧</p></div>';
        return;
      }
      grid.innerHTML = apps.map(a => {
        const trip = a.trip || {};
        const vehicle = (trip.vehicle && trip.vehicle.brand) ? (trip.vehicle.brand + ' ' + (trip.vehicle.model||'')) : '未知车辆';
        const driverName = (a.driver && a.driver.name) ? a.driver.name : '未知';
        return '<div class="trip-card">' +
          '<div class="trip-card-header"><div class="route">' + esc(trip.departureLocation) + ' <span class="arrow">→</span> ' + esc(trip.arrivalLocation) + '</div>' +
          statusBadge(a.participantStatus) + '</div>' +
          '<div class="trip-card-body">' +
          '<div class="info-row"><span class="ic">&#x1F4C5;</span>' + fmtDate(trip.departureTime) + '</div>' +
          '<div class="info-row"><span class="ic">&#x1F464;</span>' + esc(driverName) + '</div>' +
          '<div class="info-row"><span class="ic">&#x1F698;</span>' + esc(vehicle) + '</div>' +
          '</div>' +
          '<div class="trip-card-footer"><div class="price">' + (trip.pricePerSeat||0) + '<small> 元/人</small></div>' +
          '<div class="seats">预订 <strong>' + (a.seatsBooked||1) + '</strong> 座</div></div>' +
          (function(){
            var btns = '';
            var ps = a.paymentStatus || 'unpaid';
            var pLabel = ps==='paid'?'已支付':'未支付';
            var pCls = ps==='paid'?'status-completed':'status-pending';
            var pBadge = '<span class=\"status-badge '+pCls+'\">'+pLabel+'</span> ';
            if (a.participantStatus === 'pending' || a.participantStatus === 'accepted') {
              btns = '<button class=\"btn btn-danger btn-sm\" onclick=\"App.cancelApplication('+a.id+')\">取消申请</button>';
            } else if (a.participantStatus === 'completed') {
              btns = pBadge;
              if (ps !== 'paid') btns += '<button class=\"btn btn-success btn-sm\" onclick=\"App.payApplication('+a.id+')\">支付</button> ';
              btns += '<button class=\"btn btn-outline btn-sm\" onclick=\"App.rateTrip('+a.tripId+','+a.id+')\">评价</button>';
            } else if (a.participantStatus === 'cancelled') {
              btns = pBadge;
            }
            return btns ? '<div class=\"trip-card-actions\">'+btns+'</div>' : '';
          })() +
          '</div>';
      }).join('');
    } catch(e) { toast('加载申请失败', 'error'); }
  },

  async cancelApplication(appId) {
    if (!confirm('确定取消申请吗？')) return;
    try {
      await API.del('/api/participants/' + appId);
      toast('已取消');
      this.loadMyApplications();
    } catch(e) { toast(e.message || '取消失败', 'error'); }
  },

  async payApplication(appId) {
    try {
      await API.post('/api/participants/' + appId + '/pay');
      toast('支付成功');
      this.loadMyApplications();
    } catch(e) { toast(e.message || '支付失败', 'error'); }
  },

  rateTrip(tripId, appId) {
    var score = prompt('请给本次行程打分 (1-5):', '5');
    if (!score) return;
    score = parseInt(score);
    if (score < 1 || score > 5) { toast('评分需在1-5之间', 'warning'); return; }
    toast('感谢您的评分: ' + score + '分');
  },

  // ====== Driver: Vehicle Management ======
  async showVehicles() {
    this.showSection('vehiclesSection');
    await this.loadVehicles();
  },

  async loadVehicles() {
    try {
      const vehicles = await API.get('/api/vehicles/driver');
      const grid = $('vehicleGrid');
      if (!vehicles || vehicles.length === 0) {
        grid.innerHTML = '<div class="empty-state"><h3>还没有添加车辆</h3><p>点击上方按钮添加您的第一辆车</p></div>';
        return;
      }
      grid.innerHTML = vehicles.map(v =>
        '<div class="vehicle-card">' +
        '<div class="plate-box">' + esc(v.plateNumber) + '</div>' +
        '<div class="v-info">品牌: ' + esc(v.brand) + ' ' + esc(v.model) + '</div>' +
        '<div class="v-info">颜色: ' + esc(v.color) + ' | 座位: ' + (v.seats||5) + '座 | 年份: ' + (v.year||'-') + '</div>' +
        '<div class="v-info" style="font-size:12px;color:var(--gray-400)">添加于: ' + fmtDate(v.createdAt) + '</div>' +
        '<div class="v-actions">' +
        '<button class="btn btn-outline btn-sm" onclick="App.openEditVehicle(' + v.id + ', \'' + esc(v.plateNumber) + '\', \'' + esc(v.brand) + '\', \'' + esc(v.model) + '\', \'' + esc(v.color) + '\', ' + (v.seats||5) + ', ' + (v.year||0) + ')">编辑</button>' +
        '<button class="btn btn-danger btn-sm" onclick="App.deleteVehicle(' + v.id + ')">删除</button>' +
        '</div></div>'
      ).join('');
    } catch(e) { toast('加载车辆失败', 'error'); }
  },

  openEditVehicle(id, plate, brand, model, color, seats, year) {
    $('evId').value = id;
    $('evPlate').value = plate || '';
    $('evBrand').value = brand || '';
    $('evModel').value = model || '';
    $('evColor').value = color || '';
    $('evSeats').value = seats || 5;
    $('evYear').value = year || '';
    openModal('editVehicleModal');
  },

  async addVehicle() {
    const data = {
      plateNumber: $('avPlate').value,
      brand: $('avBrand').value,
      model: $('avModel').value,
      color: $('avColor').value,
      seats: parseInt($('avSeats').value) || 5,
      year: parseInt($('avYear').value) || null
    };
    try {
      await API.post('/api/vehicles', data);
      closeModal('addVehicleModal');
      toast('车辆添加成功');
      this.loadVehicles();
      // Reset form
      ['avPlate','avBrand','avModel','avColor'].forEach(id => $(id).value = '');
      $('avSeats').value = '5'; $('avYear').value = '';
    } catch(e) { toast(e.message || '添加失败', 'error'); }
  },

  async editVehicle() {
    const id = $('evId').value;
    const data = {
      plateNumber: $('evPlate').value,
      brand: $('evBrand').value,
      model: $('evModel').value,
      color: $('evColor').value,
      seats: parseInt($('evSeats').value) || null,
      year: parseInt($('evYear').value) || null
    };
    try {
      await API.put('/api/vehicles/' + id, data);
      closeModal('editVehicleModal');
      toast('车辆更新成功');
      this.loadVehicles();
    } catch(e) { toast(e.message || '更新失败', 'error'); }
  },

  async deleteVehicle(id) {
    if (!confirm('确定删除这辆车吗？相关行程也会被删除。')) return;
    try {
      await API.del('/api/vehicles/' + id);
      toast('车辆已删除');
      this.loadVehicles();
    } catch(e) { toast(e.message || '删除失败', 'error'); }
  },

  // ====== Driver: Trip Management ======
  async openCreateTrip() {
    if (!this.user || this.user.role !== 'driver') { toast('只有司机可以发布行程', 'warning'); return; }
    // Load vehicles for selection
    try {
      const vehicles = await API.get('/api/vehicles/driver');
      const sel = $('ctVehicle');
      if (!vehicles || vehicles.length === 0) {
        sel.innerHTML = '<option value="">请先添加车辆</option>';
        toast('请先添加车辆', 'warning');
        return;
      }
      sel.innerHTML = vehicles.map(v => '<option value="' + v.id + '">' + esc(v.plateNumber) + ' - ' + esc(v.brand) + ' ' + esc(v.model) + '</option>').join('');
      openModal('createTripModal');
    } catch(e) { toast('加载车辆失败', 'error'); }
  },

  async createTrip() {
    const data = {
      vehicleId: parseInt($('ctVehicle').value) || null,
      departureLocation: $('ctDeparture').value,
      arrivalLocation: $('ctArrival').value,
      departureTime: $('ctTime').value,
      availableSeats: parseInt($('ctSeats').value) || 1,
      pricePerSeat: parseFloat($('ctPrice').value) || 0,
      description: $('ctDesc').value || null
    };
    try {
      await API.post('/api/trips', data);
      closeModal('createTripModal');
      toast('行程发布成功');
      this.loadDriverTrips();
      // Reset
      ['ctDeparture','ctArrival','ctTime','ctDesc'].forEach(id => $(id).value = '');
      $('ctSeats').value = '3'; $('ctPrice').value = '10';
    } catch(e) { toast(e.message || '发布失败', 'error'); }
  },

  async loadDriverTrips() {
    try {
      const trips = await API.get('/api/trips/driver');
      this.renderTrips(trips, 'driverTripGrid', 'driver');
    } catch(e) { $('driverTripGrid').innerHTML = '<div class="empty-state"><h3>加载失败</h3></div>'; }
  },

  async endTrip(tripId, status) {
    const label = status === 'completed' ? '结束' : '取消';
    if (!confirm('确定' + label + '这个行程吗？')) return;
    try {
      await API.put('/api/trips/' + tripId, { tripStatus: status });
      toast('行程已' + label);
      this.loadDriverTrips();
    } catch(e) { toast(e.message || '操作失败', 'error'); }
  },

  // ====== Driver: Applications & Passengers ======
  async viewApplications(tripId) {
    if (!tripId) { toast('请从行程卡片中查看申请', 'warning'); return; }
    try {
      const apps = await API.get('/api/participants/trips/' + tripId + '/applications');
      $('appModalTitle').textContent = '行程申请 (ID: ' + tripId + ')';
      const list = $('applicationsList');
      if (!apps || apps.length === 0) {
        list.innerHTML = '<div class="empty-state"><h3>暂无申请</h3></div>';
      } else {
        list.innerHTML = '<div class="application-list">' + apps.map(a => {
          const s = a.student || {};
          return '<div class="application-item"><div class="app-header"><div class="student-info"><h4>' + esc(s.name) + '</h4><div class="meta">学号: ' + esc(s.studentId) + ' | 电话: ' + esc(s.phone) + '</div></div>' +
            '<div class="app-actions">' +
            '<button class="btn btn-success btn-sm" onclick="App.handleApplication(' + a.id + ',\'accepted\')">同意</button>' +
            '<button class="btn btn-danger btn-sm" onclick="App.handleApplication(' + a.id + ',\'rejected\')">拒绝</button>' +
            '</div></div>' +
            '<div style="font-size:13px;color:var(--gray-500)">预订 ' + (a.seatsBooked||1) + ' 座 | ' + fmtDate(a.createdAt) + '</div></div>';
        }).join('') + '</div>';
      }
      openModal('applicationsModal');
    } catch(e) { toast(e.message || '查看申请失败', 'error'); }
  },

  async handleApplication(appId, status) {
    try {
      await API.put('/api/participants/' + appId + '/status', { status: status });
      toast(status === 'accepted' ? '已同意' : '已拒绝');
      closeModal('applicationsModal');
      this.loadDriverTrips();
    } catch(e) { toast(e.message || '操作失败', 'error'); }
  },

  async viewPassengers(tripId) {
    if (!tripId) return;
    try {
      const data = await API.get('/api/participants/trips/' + tripId + '/passengers');
      this.showSection('passengersSection');
      $('totalIncome').textContent = '总收入: Y' + (data.totalIncome || 0).toFixed(2) + ' (单价: Y' + (data.pricePerSeat || 0) + ')';
      const list = $('passengersList');
      const pax = data.passengers || [];
      if (pax.length === 0) {
        list.innerHTML = '<div class="empty-state"><h3>暂无乘客</h3></div>';
      } else {
        list.innerHTML = pax.map(p => {
          const s = p.student || {};
          return '<div class="application-item"><div class="app-header"><div><h4>' + esc(s.name) + '</h4><div style="font-size:13px;color:var(--gray-500)">学号: ' + esc(s.studentId) + ' | 电话: ' + esc(s.phone) + '</div></div>' +
            '<div style="text-align:right"><span class="status-badge ' + (p.paymentStatus === 'paid' ? 'status-completed' : 'status-pending') + '">' + (p.paymentStatus === 'paid' ? '已支付' : '未支付') + '</span></div></div>' +
            '<div style="font-size:13px;color:var(--gray-500)">座位: ' + (p.seatsBooked||1) + ' 座 | 时间: ' + fmtDate(p.createdAt) + '</div></div>';
        }).join('');
      }
    } catch(e) { toast(e.message || '加载乘客失败', 'error'); this.showSection('driverTripsSection'); this.loadDriverTrips(); }
  },

  // ====== Admin ======
  async adminShowDashboard() {
    this.showSection('adminDashboardSection');
    try {
      const stats = await API.get('/api/admin/stats');
      $('statsGrid').innerHTML =
        '<div class="stat-card" onclick="App.adminLoadUsers()"><div class="stat-label">总用户数</div><div class="stat-value">' + (stats.totalUsers||0) + '</div></div>' +
        '<div class="stat-card" onclick="App.adminLoadVehicles()"><div class="stat-label">总车辆数</div><div class="stat-value">' + (stats.totalVehicles||0) + '</div></div>' +
        '<div class="stat-card" onclick="App.adminLoadTrips()"><div class="stat-label">总行程数</div><div class="stat-value">' + (stats.totalTrips||0) + '</div></div>' +
        '<div class="stat-card"><div class="stat-label">今日新用户</div><div class="stat-value">' + (stats.todayNewUsers||0) + '</div></div>';
    } catch(e) { toast('加载仪表盘失败', 'error'); }
  },

  // ====== Admin Data Cache ======
  _adminUsers: [],
  _adminVehicles: [],
  _adminTrips: [],

  async adminShowDashboard() {
    this.showSection("adminDashboardSection");
    try {
      const stats = await API.get("/api/admin/stats");
      var h = "";
      h += "<div class='stat-card' onclick='App.adminLoadUsers()'><div class='stat-label'>总用户数</div><div class='stat-value'>" + (stats.totalUsers||0) + "</div></div>";
      h += "<div class='stat-card' onclick='App.adminLoadVehicles()'><div class='stat-label'>总车辆数</div><div class='stat-value'>" + (stats.totalVehicles||0) + "</div></div>";
      h += "<div class='stat-card' onclick='App.adminLoadTrips()'><div class='stat-label'>总行程数</div><div class='stat-value'>" + (stats.totalTrips||0) + "</div></div>";
      h += "<div class='stat-card'><div class='stat-label'>今日新用户</div><div class='stat-value'>" + (stats.todayNewUsers||0) + "</div></div>";
      var el = document.getElementById("statsGrid"); if(el) el.innerHTML = h;
    } catch(e) { toast("加载仪表盘失败", "error"); }
  },

  async adminLoadUsers() {
    this.showSection("adminUsersSection");
    try {
      var users = await API.get("/api/admin/users");
      this._adminUsers = users || [];
      var h = "";
      for (var i = 0; i < this._adminUsers.length; i++) {
        var u = this._adminUsers[i];
        h += "<tr><td>" + u.id + "</td><td>" + esc(u.name) + "</td><td>" + esc(u.studentId || u.phone || "-") + "</td>";
        h += "<td><span class='role-badge role-" + (u.role||"student") + "'>" + (u.role||"student") + "</span></td>";
        h += "<td>" + fmtDateShort(u.createdAt) + "</td>";
        h += "<td><button class='btn btn-outline btn-sm' onclick='App.adminOpenEditUser(" + i + ")'>编辑</button> ";
        h += "<button class='btn btn-danger btn-sm' onclick='App.adminDeleteUser(" + u.id + ")'>删除</button></td></tr>";
      }
      if (!h) h = "<tr><td colspan='6' class='text-center text-muted'>暂无用户</td></tr>";
      var el = document.getElementById("adminUsersTable"); if(el) el.innerHTML = h;
    } catch(e) { toast("加载用户失败", "error"); }
  },

  adminOpenEditUser(idx) {
    var u = this._adminUsers[idx]; if (!u) return;
    $("euId").value = u.id; $("euName").value = u.name || "";
    $("euRole").value = u.role || "student";
    $("euStudentId").value = u.studentId || "";
    $("euPhone").value = u.phone || "";
    openModal("adminEditUserModal");
  },

  async adminAddUser() {
    var pw = $("auPassword").value;
    var data = { name: $("auName").value, role: $("auRole").value, studentId: $("auStudentId").value || null, phone: $("auPhone").value || null, password: pw || null };
    try { await API.post("/api/admin/users", data); closeModal("adminAddUserModal"); toast("用户创建成功"); this.adminLoadUsers();
      $("auName").value = ""; $("auStudentId").value = ""; $("auPhone").value = ""; $("auPassword").value = "";
    } catch(e) { toast(e.message || "创建失败", "error"); }
  },

  async adminEditUser() {
    var id = $("euId").value;
    var pw = $("euPassword").value;
    var data = { name: $("euName").value, role: $("euRole").value, studentId: $("euStudentId").value || null, phone: $("euPhone").value || null, password: pw || null };
    try { await API.put("/api/admin/users/" + id, data); closeModal("adminEditUserModal"); toast("用户更新成功"); this.adminLoadUsers();
    } catch(e) { toast(e.message || "更新失败", "error"); }
  },

  async adminDeleteUser(id) {
    if (!confirm("确定删除该用户？")) return;
    try { await API.del("/api/admin/users/" + id); toast("已删除"); this.adminLoadUsers();
    } catch(e) { toast(e.message || "删除失败", "error"); }
  },

  async adminLoadVehicles() {
    this.showSection("adminVehiclesSection");
    try {
      var vehicles = await API.get("/api/admin/vehicles");
      this._adminVehicles = vehicles || [];
      var h = "";
      for (var i = 0; i < this._adminVehicles.length; i++) {
        var v = this._adminVehicles[i];
        h += "<tr><td>" + v.id + "</td><td>" + esc(v.plateNumber) + "</td><td>" + esc(v.brand) + "</td><td>" + esc(v.model) + "</td>";
        h += "<td>" + (v.seats||5) + "</td><td>" + esc((v.driver && v.driver.name) || "-") + "</td>";
        h += "<td><button class='btn btn-outline btn-sm' onclick='App.adminOpenEditVehicle(" + i + ")'>编辑</button> ";
        h += "<button class='btn btn-danger btn-sm' onclick='App.adminDeleteVehicle(" + v.id + ")'>删除</button></td></tr>";
      }
      if (!h) h = "<tr><td colspan='7' class='text-center text-muted'>暂无车辆</td></tr>";
      var el = document.getElementById("adminVehiclesTable"); if(el) el.innerHTML = h;
    } catch(e) { toast("加载车辆失败", "error"); }
  },

  async adminDeleteVehicle(id) {
    if (!confirm("确定删除该车辆？关联行程也会删除。")) return;
    try { await API.del("/api/admin/vehicles/" + id); toast("已删除"); this.adminLoadVehicles();
    } catch(e) { toast(e.message || "删除失败", "error"); }
  },

  async adminLoadTrips() {
    this.showSection("adminTripsSection");
    try {
      var trips = await API.get("/api/admin/trips");
      this._adminTrips = trips || [];
      var h = "";
      for (var i = 0; i < this._adminTrips.length; i++) {
        var t = this._adminTrips[i];
        h += "<tr><td>" + t.id + "</td><td>" + esc(t.departureLocation) + " &rarr; " + esc(t.arrivalLocation) + "</td>";
        h += "<td>" + fmtDate(t.departureTime) + "</td><td>" + (t.pricePerSeat||0) + "</td><td>" + (t.availableSeats||0) + "</td>";
        h += "<td>" + statusBadge(t.tripStatus) + "</td>";
        h += "<td><button class='btn btn-outline btn-sm' onclick='App.adminOpenEditTrip(" + i + ")'>编辑</button> ";
        h += "<button class='btn btn-danger btn-sm' onclick='App.adminDeleteTrip(" + t.id + ")'>删除</button></td></tr>";
      }
      if (!h) h = "<tr><td colspan='7' class='text-center text-muted'>暂无行程</td></tr>";
      var el = document.getElementById("adminTripsTable"); if(el) el.innerHTML = h;
    } catch(e) { toast("加载行程失败", "error"); }
  },

  adminOpenEditTrip(idx) {
    var t = this._adminTrips[idx]; if (!t) return;
    $("etId").value = t.id;
    $("etDeparture").value = t.departureLocation || "";
    $("etArrival").value = t.arrivalLocation || "";
    if (t.departureTime) { var d = new Date(t.departureTime); $("etTime").value = d.toISOString().slice(0,16); }
    $("etSeats").value = t.availableSeats || 0;
    $("etPrice").value = t.pricePerSeat || 0;
    $("etStatus").value = t.tripStatus || "pending";
    openModal("adminEditTripModal");
  },

  async adminEditTrip() {
    var id = $("etId").value;
    var data = { departureLocation: $("etDeparture").value, arrivalLocation: $("etArrival").value,
      departureTime: $("etTime").value, availableSeats: parseInt($("etSeats").value) || null,
      pricePerSeat: parseFloat($("etPrice").value) || null, tripStatus: $("etStatus").value };
    try { await API.put("/api/admin/trips/" + id, data); closeModal("adminEditTripModal"); toast("行程更新成功"); this.adminLoadTrips();
    } catch(e) { toast(e.message || "更新失败", "error"); }
  },

  async adminDeleteTrip(id) {
    if (!confirm("确定删除该行程？")) return;
    try { await API.del("/api/admin/trips/" + id); toast("已删除"); this.adminLoadTrips();
    } catch(e) { toast(e.message || "删除失败", "error"); }
  }
,

  // ====== Profile ======
  openEditProfile() {
    if (!this.user) return;
    $('epUserId').value = this.user.id;
    $('epName').value = this.user.name || '';
    $('epPhone').value = this.user.phone || '';
    openModal('editProfileModal');
  },

  async editProfile() {
    const userId = $('epUserId').value;
    const data = {
      name: $('epName').value,
      phone: $('epPhone').value
    };
    try {
      await API.put('/api/users/' + userId, data);
      closeModal('editProfileModal');
      toast('资料更新成功');
      // 重新加载用户信息
      this.user = await API.get('/api/users/me');
      this.updateUI();
    } catch(e) {
      toast(e.message || '更新失败', 'error');
    }
  },

  // ====== Rating ======
  openRatingModal(tripId, ratedUserId, userName) {
    $('ratingTripId').value = tripId;
    $('ratingRatedUserId').value = ratedUserId;
    $('ratingUserName').value = userName;
    $('ratingScore').value = 5;
    $('ratingComment').value = '';
    // 重置星星状态
    document.querySelectorAll('.rating-stars .star').forEach((s, i) => {
      s.classList.toggle('active', i < 5);
    });
    openModal('ratingModal');
  },

  async submitRating() {
    const data = {
      tripId: parseInt($('ratingTripId').value),
      ratedUserId: parseInt($('ratingRatedUserId').value),
      ratingScore: parseInt($('ratingScore').value),
      comment: $('ratingComment').value
    };
    try {
      await API.post('/api/ratings/trip', data);
      closeModal('ratingModal');
      toast('评价成功');
      // 刷新当前列表
      if (this.user && this.user.role === 'student') {
        this.loadMyApplications();
      } else if (this.user && this.user.role === 'driver') {
        this.loadDriverTrips();
      }
    } catch(e) {
      toast(e.message || '评价失败', 'error');
    }
  },

  async loadTripRatings(tripId) {
    try {
      const result = await API.get('/api/ratings/trip/' + tripId);
      return result.ratings || [];
    } catch(e) {
      return [];
    }
  },

  async deleteTrip(tripId) {
    if (!confirm('确定要删除该行程吗？')) return;
    try {
      await API.del('/api/trips/' + tripId);
      toast('行程已删除');
      this.loadDriverTrips();
    } catch(e) {
      toast(e.message || '删除失败', 'error');
    }
  },

  // ====== Admin Vehicle Management ======
  async adminAddVehicle() {
    const data = {
      userId: parseInt($('aavUserId').value) || null,
      plateNumber: $('aavPlate').value,
      brand: $('aavBrand').value,
      model: $('aavModel').value,
      color: $('aavColor').value,
      seats: parseInt($('aavSeats').value) || 5,
      year: parseInt($('aavYear').value) || null
    };
    try {
      await API.post('/api/admin/vehicles', data);
      closeModal('adminAddVehicleModal');
      toast('车辆添加成功');
      this.adminLoadVehicles();
    } catch(e) {
      toast(e.message || '添加失败', 'error');
    }
  },

  adminOpenEditVehicle(idx) {
    const v = this._adminVehicles[idx];
    if (!v) return;
    $('aevId').value = v.id;
    $('aevPlate').value = v.plateNumber || '';
    $('aevBrand').value = v.brand || '';
    $('aevModel').value = v.model || '';
    $('aevColor').value = v.color || '';
    $('aevSeats').value = v.seats || 5;
    $('aevYear').value = v.year || '';
    openModal('adminEditVehicleModal');
  },

  async adminEditVehicle() {
    const id = $('aevId').value;
    const data = {
      plateNumber: $('aevPlate').value,
      brand: $('aevBrand').value,
      model: $('aevModel').value,
      color: $('aevColor').value,
      seats: parseInt($('aevSeats').value) || 5,
      year: parseInt($('aevYear').value) || null
    };
    try {
      await API.put('/api/admin/vehicles/' + id, data);
      closeModal('adminEditVehicleModal');
      toast('车辆更新成功');
      this.adminLoadVehicles();
    } catch(e) {
      toast(e.message || '更新失败', 'error');
    }
  }

};

// Initialize
document.addEventListener("DOMContentLoaded", function() { App.init(); });
