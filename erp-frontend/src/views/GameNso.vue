<template>
  <div class="game-nso-page">
    <!-- Compact Header -->
    <div class="page-header-bar">
      <div class="header-title-block">
        <span class="title-icon-badge">🎮</span>
        <div class="title-texts">
          <div class="title-line">
            <h1 class="page-title">Trung tâm Giải trí &amp; Quản trị Game NSO</h1>
            <el-tag :type="overview.serverOnline ? 'success' : 'danger'" size="small" effect="dark">
              {{ overview.serverOnline ? 'SERVER ONLINE (14444)' : 'SERVER OFFLINE' }}
            </el-tag>
          </div>
          <span class="page-subtitle">Khởi chạy game Ninja School Online, quản lý tài khoản, nhân vật, tiền tệ &amp; vật phẩm.</span>
        </div>
      </div>

      <div class="header-actions">
        <el-button size="small" :loading="statusLoading" @click="fetchOverview">
          <el-icon><Refresh /></el-icon>
          <span>Làm mới</span>
        </el-button>
        <el-button
          v-if="!overview.serverOnline && canManage"
          size="small"
          type="warning"
          :loading="startingServer"
          @click="handleStartServer"
        >
          <span>⚡ Bật Server Game</span>
        </el-button>
        <el-button
          size="small"
          type="primary"
          :loading="launchingClient"
          @click="handleLaunchClient"
        >
          <span>🚀 Mở Game NSO</span>
        </el-button>
      </div>
    </div>

    <!-- Tabs -->
    <div class="tabs-panel">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <!-- TAB 1: TỔNG QUAN & KHỞI CHẠY -->
        <el-tab-pane label="🎮 Khởi chạy & Trạng thái" name="overview">
          <div class="kpi-grid">
            <div class="kpi-card">
              <div class="kpi-label">Trạng thái Máy chủ</div>
              <div class="kpi-value" :class="overview.serverOnline ? 'text-emerald' : 'text-red'">
                {{ overview.serverOnline ? 'Đang chạy' : 'Đang tắt' }}
              </div>
              <div class="kpi-sub">{{ overview.serverHost }}:{{ overview.serverPort }}</div>
            </div>
            <div class="kpi-card">
              <div class="kpi-label">Tài khoản Game</div>
              <div class="kpi-value text-blue">{{ formatNumber(overview.totalUsers) }}</div>
              <div class="kpi-sub">Đang Online: {{ overview.onlineUsers ?? 0 }}</div>
            </div>
            <div class="kpi-card">
              <div class="kpi-label">Nhân vật đã tạo</div>
              <div class="kpi-value text-slate">{{ formatNumber(overview.totalPlayers) }}</div>
              <div class="kpi-sub">Database: nsoz</div>
            </div>
            <div class="kpi-card">
              <div class="kpi-label">Danh mục Vật phẩm</div>
              <div class="kpi-value text-purple">{{ formatNumber(overview.totalItems) }}</div>
              <div class="kpi-sub">Trang bị, Đá, Nguyên liệu</div>
            </div>
          </div>

          <div class="launch-guide-box">
            <div class="guide-left">
              <h3>Hướng dẫn vào game nhanh</h3>
              <ul>
                <li><strong>Bước 1:</strong> Đảm bảo trạng thái Server phía trên hiển thị <code>SERVER ONLINE (14444)</code>. Nếu đang tắt, bấm <strong>⚡ Bật Server Game</strong> và đợi ~10 giây.</li>
                <li><strong>Bước 2:</strong> Bấm nút <strong>🚀 Mở Game NSO</strong> để bật trình giả lập <code>AngelChipEmulator</code>.</li>
                <li><strong>Bước 3:</strong> Trên giả lập, chọn file <code>Local.jar</code> và đăng nhập bằng tài khoản mặc định: <code>admin</code> / <code>112233</code> (hoặc tạo tài khoản mới ở tab bên cạnh).</li>
              </ul>
            </div>
            <div class="guide-right">
              <el-button type="primary" size="large" :loading="launchingClient" @click="handleLaunchClient">
                🎮 VÀO CHƠI NGAY (MỞ CLIENT)
              </el-button>
            </div>
          </div>
        </el-tab-pane>

        <!-- TAB 2: QUẢN LÝ TÀI KHOẢN -->
        <el-tab-pane v-if="canManage" label="👤 Tài khoản Game" name="users">
          <div class="tab-toolbar">
            <el-input
              v-model="userSearch"
              size="small"
              placeholder="Tìm theo username..."
              clearable
              style="width: 240px"
              @keyup.enter="fetchUsers(true)"
              @clear="fetchUsers(true)"
            >
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
            <el-button size="small" type="primary" plain @click="fetchUsers(true)">Tìm</el-button>
            <el-button size="small" type="success" @click="openCreateUserModal">+ Tạo tài khoản</el-button>
          </div>

          <el-table v-loading="usersLoading" :data="usersList" size="small" stripe height="calc(100vh - 340px)">
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="username" label="Tên đăng nhập" min-width="140">
              <template #default="{ row }">
                <strong>{{ row.username }}</strong>
              </template>
            </el-table-column>
            <el-table-column label="Trạng thái" width="120">
              <template #default="{ row }">
                <el-tag :type="row.lock === 1 ? 'danger' : 'success'" size="small">
                  {{ row.lock === 1 ? 'Đang khóa' : 'Hoạt động' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="Online" width="95">
              <template #default="{ row }">
                <el-tag :type="row.online === 1 ? 'success' : 'info'" size="small" effect="plain">
                  {{ row.online === 1 ? 'Online' : 'Offline' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="Lượng" width="120">
              <template #default="{ row }">
                <span class="text-blue fw-bold">{{ formatNumber(row.luong) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="Coin (Web)" width="120">
              <template #default="{ row }">
                <span>{{ formatNumber(row.coin) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="Thao tác" width="210" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="openTopupModal(row)">Nạp Lượng/Coin</el-button>
                <el-button link :type="row.lock === 1 ? 'success' : 'danger'" size="small" @click="handleToggleLock(row)">
                  {{ row.lock === 1 ? 'Mở khóa' : 'Khóa' }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-bar">
            <span class="total-info">Tổng: <strong>{{ usersTotal }}</strong> tài khoản</span>
            <el-pagination
              v-model:current-page="usersPage"
              size="small"
              :page-size="15"
              :total="usersTotal"
              layout="prev, pager, next"
              @current-change="fetchUsers(false)"
            />
          </div>
        </el-tab-pane>

        <!-- TAB 3: NHÂN VẬT & ĐỒ VẬT -->
        <el-tab-pane v-if="canManage" label="🥷 Nhân vật & Hành trang" name="players">
          <div class="tab-toolbar">
            <el-input
              v-model="playerSearch"
              size="small"
              placeholder="Tìm tên nhân vật..."
              clearable
              style="width: 240px"
              @keyup.enter="fetchPlayers(true)"
              @clear="fetchPlayers(true)"
            >
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
            <el-button size="small" type="primary" plain @click="fetchPlayers(true)">Tìm</el-button>
          </div>

          <el-table v-loading="playersLoading" :data="playersList" size="small" stripe height="calc(100vh - 340px)">
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="name" label="Tên nhân vật" min-width="140">
              <template #default="{ row }">
                <strong>{{ row.name }}</strong>
              </template>
            </el-table-column>
            <el-table-column prop="username" label="Tài khoản" width="130" />
            <el-table-column label="Hệ / Phái" width="120">
              <template #default="{ row }">
                <span>{{ formatClassLabel(row.charClass) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="Xu" width="130">
              <template #default="{ row }">
                <span class="text-amber fw-bold">{{ formatNumber(row.xu) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="Yên" width="130">
              <template #default="{ row }">
                <span class="text-emerald fw-bold">{{ formatNumber(row.yen) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="Online" width="90">
              <template #default="{ row }">
                <el-tag :type="row.online === 1 ? 'success' : 'info'" size="small" effect="plain">
                  {{ row.online === 1 ? 'Online' : 'Offline' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="Thao tác" width="210" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="openBuffModal(row)">⚡ Buff Toàn diện</el-button>
                <el-button link type="info" size="small" @click="openBagModal(row)">🎒 Xem Túi đồ</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-bar">
            <span class="total-info">Tổng: <strong>{{ playersTotal }}</strong> nhân vật</span>
            <el-pagination
              v-model:current-page="playersPage"
              size="small"
              :page-size="15"
              :total="playersTotal"
              layout="prev, pager, next"
              @current-change="fetchPlayers(false)"
            />
          </div>
        </el-tab-pane>

        <!-- TAB 4: DANH MỤC VẬT PHẨM / ITEM -->
        <el-tab-pane v-if="canManage" label="🎒 Danh mục Vật phẩm (Item)" name="items">
          <div class="tab-toolbar">
            <el-input
              v-model="itemSearch"
              size="small"
              placeholder="Tìm theo ID hoặc tên vật phẩm..."
              clearable
              style="width: 240px"
              @keyup.enter="fetchItems(true)"
              @clear="fetchItems(true)"
            >
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
            <el-select
              v-model="itemTypeFilter"
              size="small"
              placeholder="Tất cả loại (Type)"
              clearable
              style="width: 190px"
              @change="fetchItems(true)"
            >
              <el-option v-for="t in itemTypeOptions" :key="t.value" :label="t.label" :value="t.value" />
            </el-select>
            <el-button size="small" type="primary" plain @click="fetchItems(true)">Tìm kiếm</el-button>
          </div>

          <el-table v-loading="itemsLoading" :data="itemsList" size="small" stripe height="calc(100vh - 340px)">
            <el-table-column prop="id" label="ID" width="72" />
            <el-table-column label="Ảnh" width="68" align="center">
              <template #default="{ row }">
                <div class="item-icon-frame">
                  <img
                    v-if="row.icon !== null && row.icon !== undefined"
                    :src="getItemIconUrl(row.icon)"
                    alt=""
                    class="item-icon-img"
                    loading="lazy"
                    @error="onIconError"
                  />
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="name" label="Tên vật phẩm" min-width="200">
              <template #default="{ row }">
                <strong>{{ row.name }}</strong>
              </template>
            </el-table-column>
            <el-table-column prop="type" label="Loại (Type)" width="100" />
            <el-table-column prop="level" label="Cấp yêu cầu" width="110" />
            <el-table-column prop="description" label="Mô tả" min-width="240" show-overflow-tooltip />
            <el-table-column label="Thao tác" width="160" fixed="right">
              <template #default="{ row }">
                <el-button link type="success" size="small" @click="openSendGiftFromItemModal(row)">🎁 Tặng quà</el-button>
                <el-button link type="primary" size="small" @click="openEditItemModal(row)">Sửa</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-bar">
            <span class="total-info">Tổng: <strong>{{ itemsTotal }}</strong> vật phẩm</span>
            <el-pagination
              v-model:current-page="itemsPage"
              size="small"
              :page-size="20"
              :total="itemsTotal"
              layout="prev, pager, next"
              @current-change="fetchItems(false)"
            />
          </div>
        </el-tab-pane>

        <!-- TAB 5: TIỆN ÍCH GM & BANG HỘI -->
        <el-tab-pane v-if="canManage" label="⚡ Tiện ích GM & Bang hội" name="gm-tools">
          <div class="gm-grid">
            <!-- CỘT TRÁI: BUFF CHỈ SỐ CHO 1 NHÂN VẬT CỤ THỂ -->
            <div class="gm-card">
              <div class="gm-card-head">
                <h3>⚡ Buff Chỉ Số Nhân Vật</h3>
                <span class="gm-hint">Chọn nhân vật và nhập giá trị cộng thêm</span>
              </div>

              <el-form label-position="top" size="small">
                <el-form-item label="Chọn Nhân vật cần Buff">
                  <el-select
                    v-model="gmBuffForm.playerId"
                    filterable
                    placeholder="Chọn nhân vật..."
                    style="width: 100%"
                  >
                    <el-option
                      v-for="p in playersList"
                      :key="p.id"
                      :label="`${p.name} (@${p.username || 'N/A'}) - ID #${p.id}`"
                      :value="p.id"
                    />
                  </el-select>
                </el-form-item>

                <el-row :gutter="10">
                  <el-col :span="12">
                    <el-form-item label="Chuyển Hệ / Phái">
                      <el-select v-model="gmBuffForm.charClass" clearable placeholder="Giữ nguyên" style="width: 100%">
                        <el-option :value="0" label="0 - Chưa nhập học" />
                        <el-option :value="1" label="1 - Kiếm" />
                        <el-option :value="2" label="2 - Tiêu" />
                        <el-option :value="3" label="3 - Kunai" />
                        <el-option :value="4" label="4 - Cung" />
                        <el-option :value="5" label="5 - Đao" />
                        <el-option :value="6" label="6 - Quạt" />
                      </el-select>
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="Ghi đè Tiềm năng [STR,AGI,VIT,ENE]">
                      <el-input v-model="gmBuffForm.potentialOverride" placeholder="VD: [5000,5000,5000,5000]" />
                    </el-form-item>
                  </el-col>
                </el-row>

                <el-row :gutter="10">
                  <el-col :span="8">
                    <el-form-item label="+ Tiềm năng">
                      <el-input-number v-model="gmBuffForm.pointDelta" :step="100" style="width: 100%" />
                    </el-form-item>
                  </el-col>
                  <el-col :span="8">
                    <el-form-item label="+ Kỹ năng">
                      <el-input-number v-model="gmBuffForm.spointDelta" :step="10" style="width: 100%" />
                    </el-form-item>
                  </el-col>
                  <el-col :span="8">
                    <el-form-item label="+ Uy danh">
                      <el-input-number v-model="gmBuffForm.uydanhDelta" :step="500" style="width: 100%" />
                    </el-form-item>
                  </el-col>
                </el-row>

                <el-row :gutter="10">
                  <el-col :span="8">
                    <el-form-item label="+ Xu">
                      <el-input-number v-model="gmBuffForm.xuDelta" :step="1000000" style="width: 100%" />
                    </el-form-item>
                  </el-col>
                  <el-col :span="8">
                    <el-form-item label="+ Yên">
                      <el-input-number v-model="gmBuffForm.yenDelta" :step="1000000" style="width: 100%" />
                    </el-form-item>
                  </el-col>
                  <el-col :span="8">
                    <el-form-item label="+ Lượng">
                      <el-input-number v-model="gmBuffForm.luongDelta" :step="1000" style="width: 100%" />
                    </el-form-item>
                  </el-col>
                </el-row>

                <el-form-item label="+ Kinh nghiệm (EXP)">
                  <el-input-number v-model="gmBuffForm.expDelta" :step="1000000" style="width: 100%" />
                </el-form-item>

                <el-button type="primary" :loading="gmBuffSaving" style="width: 100%" @click="submitGmBuffStats">
                  ⚡ Thực thi Buff Chỉ Số Ngay
                </el-button>
              </el-form>
            </div>

            <!-- CỘT PHẢI: QUẢN LÝ & TẠO BANG HỘI -->
            <div class="gm-card">
              <div class="gm-card-head-row">
                <div>
                  <h3>🏰 Quản lý &amp; Tạo Bang Hội</h3>
                  <span class="gm-hint">Thành lập bang hội mới hoặc buff cấp độ, ngân quỹ bang</span>
                </div>
                <el-button size="small" type="success" @click="openCreateClanModal">+ Tạo Bang Hội</el-button>
              </div>

              <div class="tab-toolbar">
                <el-input
                  v-model="clanSearch"
                  size="small"
                  placeholder="Tìm tên bang hoặc bang chủ..."
                  clearable
                  style="width: 220px"
                  @keyup.enter="fetchClans(true)"
                  @clear="fetchClans(true)"
                >
                  <template #prefix><el-icon><Search /></el-icon></template>
                </el-input>
                <el-button size="small" type="primary" plain @click="fetchClans(true)">Tìm</el-button>
              </div>

              <el-table v-loading="clansLoading" :data="clansList" size="small" stripe height="340px">
                <el-table-column prop="id" label="ID" width="55" />
                <el-table-column prop="name" label="Tên Bang" min-width="120">
                  <template #default="{ row }">
                    <strong>{{ row.name }}</strong>
                  </template>
                </el-table-column>
                <el-table-column prop="main_name" label="Bang Chủ" width="110" />
                <el-table-column prop="level" label="Cấp" width="60" />
                <el-table-column label="Ngân quỹ" width="105">
                  <template #default="{ row }">{{ formatNumber(row.coin) }}</template>
                </el-table-column>
                <el-table-column prop="memberCount" label="TV" width="50" />
                <el-table-column label="Thao tác" width="90" fixed="right">
                  <template #default="{ row }">
                    <el-button link type="primary" size="small" @click="openBuffClanModal(row)">Buff Bang</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </div>
        </el-tab-pane>

        <!-- TAB 6: THEO DÕI LOG SERVER & CLIENT -->
        <el-tab-pane v-if="canManage" label="📜 Theo dõi Log Game" name="logs">
          <div class="log-toolbar">
            <div class="log-toolbar-left">
              <el-radio-group v-model="logSource" size="small" @change="fetchGameLogs">
                <el-radio-button value="server">Log Server (nso.log)</el-radio-button>
                <el-radio-button value="console">Console Server</el-radio-button>
                <el-radio-button value="client">Log Client</el-radio-button>
              </el-radio-group>
              <el-input
                v-model="logSearch"
                size="small"
                placeholder="Lọc dòng log..."
                clearable
                style="width: 190px"
              >
                <template #prefix><el-icon><Search /></el-icon></template>
              </el-input>
              <el-switch
                v-model="logAutoRefresh"
                size="small"
                active-text="Realtime (2s)"
                @change="handleToggleAutoRefresh"
              />
            </div>
            <el-button size="small" :loading="logsLoading" @click="fetchGameLogs">
              <el-icon><Refresh /></el-icon>
              <span>Tải lại Log</span>
            </el-button>
          </div>

          <div ref="logBoxRef" class="terminal-log-box">
            <div v-if="filteredGameLogs.length === 0" class="log-empty">Chưa có dữ liệu log khớp bộ lọc.</div>
            <div v-for="(line, idx) in filteredGameLogs" :key="idx" class="log-line">{{ line }}</div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- Modal Tạo Bang Hội -->
    <el-dialog v-model="createClanVisible" title="Thành Lập Bang Hội Mới" width="440px" destroy-on-close>
      <el-form label-position="top" size="small">
        <el-form-item label="Tên Bang Hội">
          <el-input v-model="createClanForm.name" placeholder="Ví dụ: Hỏa Quốc, Akatsuki..." maxlength="30" />
        </el-form-item>
        <el-form-item label="Tên Nhân vật làm Bang Chủ (phải chưa vào bang)">
          <el-select v-model="createClanForm.leaderName" filterable allow-create placeholder="Chọn hoặc nhập tên nhân vật..." style="width: 100%">
            <el-option v-for="p in playersList" :key="p.id" :label="`${p.name} (Clan: ${p.clan || 0})`" :value="p.name" />
          </el-select>
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="Cấp độ khởi tạo">
              <el-input-number v-model="createClanForm.level" :min="1" :max="50" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Ngân quỹ (Xu)">
              <el-input-number v-model="createClanForm.coin" :min="0" :step="1000000" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="Thông báo Bang">
          <el-input v-model="createClanForm.alert" placeholder="Chào mừng anh em vào bang!" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button size="small" @click="createClanVisible = false">Hủy</el-button>
        <el-button size="small" type="primary" @click="submitCreateClan">Tạo Bang Hội</el-button>
      </template>
    </el-dialog>

    <!-- Modal Buff Bang Hội -->
    <el-dialog v-model="buffClanVisible" :title="'Buff Bang Hội: ' + (buffClanTarget?.name || '')" width="420px" destroy-on-close>
      <el-form label-position="top" size="small">
        <el-form-item label="+ Ngân quỹ (Coin)">
          <el-input-number v-model="buffClanForm.coinDelta" :step="1000000" style="width: 100%" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="+ Cấp độ (Level)">
              <el-input-number v-model="buffClanForm.levelDelta" :step="1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="+ Kinh nghiệm Bang">
              <el-input-number v-model="buffClanForm.expDelta" :step="10000" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="Cập nhật Thông báo Bang (để trống nếu giữ nguyên)">
          <el-input v-model="buffClanForm.alert" placeholder="Nhập thông báo mới..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button size="small" @click="buffClanVisible = false">Hủy</el-button>
        <el-button size="small" type="primary" @click="submitBuffClan">Xác nhận Buff</el-button>
      </template>
    </el-dialog>

    <!-- Modal Tạo Tài khoản -->
    <el-dialog v-model="createUserVisible" title="Tạo Tài khoản Game NSO Mới" width="420px" destroy-on-close>
      <el-form label-position="top" size="small">
        <el-form-item label="Tên đăng nhập">
          <el-input v-model="createUserForm.username" placeholder="Ví dụ: ninja_vip" />
        </el-form-item>
        <el-form-item label="Mật khẩu">
          <el-input v-model="createUserForm.password" placeholder="Nhập mật khẩu" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="Lượng ban đầu">
              <el-input-number v-model="createUserForm.luong" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Coin ban đầu">
              <el-input-number v-model="createUserForm.coin" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button size="small" @click="createUserVisible = false">Hủy</el-button>
        <el-button size="small" type="primary" @click="submitCreateUser">Tạo tài khoản</el-button>
      </template>
    </el-dialog>

    <!-- Modal Nạp Lượng / Coin -->
    <el-dialog v-model="topupVisible" title="Nạp Lượng / Coin cho Tài khoản" width="400px" destroy-on-close>
      <el-form label-position="top" size="small">
        <el-form-item label="Tài khoản">
          <el-input :value="topupTarget?.username" disabled />
        </el-form-item>
        <el-form-item label="Số Lượng cộng thêm (có thể nhập số âm để trừ)">
          <el-input-number v-model="topupForm.luongDelta" :step="1000" style="width: 100%" />
        </el-form-item>
        <el-form-item label="Số Coin cộng thêm">
          <el-input-number v-model="topupForm.coinDelta" :step="10000" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button size="small" @click="topupVisible = false">Hủy</el-button>
        <el-button size="small" type="primary" @click="submitTopup">Xác nhận nạp</el-button>
      </template>
    </el-dialog>

    <!-- Modal Buff Toàn diện Nhân vật -->
    <el-dialog v-model="buffVisible" :title="'⚡ Buff Toàn diện cho: ' + (buffTarget?.name || '')" width="520px" destroy-on-close>
      <el-form label-position="top" size="small">
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="+ Xu">
              <el-input-number v-model="buffForm.xuDelta" :step="1000000" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="+ Yên">
              <el-input-number v-model="buffForm.yenDelta" :step="1000000" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="+ Lượng (Tài khoản)">
              <el-input-number v-model="buffForm.luongDelta" :step="5000" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="+ Điểm Tiềm năng">
              <el-input-number v-model="buffForm.pointDelta" :step="100" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="+ Điểm Kỹ năng">
              <el-input-number v-model="buffForm.spointDelta" :step="10" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="+ Điểm Uy danh">
              <el-input-number v-model="buffForm.uydanhDelta" :step="500" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="+ Kinh nghiệm (EXP)">
          <el-input-number v-model="buffForm.expDelta" :step="1000000" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button size="small" @click="buffVisible = false">Hủy</el-button>
        <el-button size="small" type="primary" :loading="buffSaving" @click="submitBuff">⚡ Xác nhận Buff</el-button>
      </template>
    </el-dialog>

    <!-- Modal Xem Túi đồ Trực quan -->
    <el-dialog v-model="bagVisible" :title="'🎒 Hành trang nhân vật: ' + (bagTarget?.name || '')" width="780px" destroy-on-close>
      <div class="give-item-bar">
        <el-input-number v-model="giveItemForm.itemId" :min="0" size="small" placeholder="ID Item" style="width: 120px" />
        <el-input-number v-model="giveItemForm.quantity" :min="1" :max="9999" size="small" placeholder="SL" style="width: 105px" />
        <el-checkbox v-model="giveItemForm.isLock" size="small">Khóa đồ</el-checkbox>
        <el-button size="small" type="success" :loading="giveItemSaving" @click="submitGiveItem">
          + Gửi Item vào túi
        </el-button>
      </div>

      <div v-loading="bagLoading" class="bag-grid-container">
        <div v-if="!bagLoading && bagItems.length === 0" class="bag-empty">
          Túi đồ hiện đang trống.
        </div>
        <div v-for="(slot, idx) in bagItems" :key="idx" class="bag-slot-card" :title="slot.description || slot.name || ('Item #' + slot.id)">
          <div class="slot-top-row">
            <span class="slot-idx">#{{ slot.index ?? idx }}</span>
            <span v-if="slot.isLock" class="slot-lock" title="Vật phẩm đã khóa">🔒</span>
          </div>
          <div class="slot-icon-wrap">
            <img
              v-if="slot.icon !== null && slot.icon !== undefined"
              :src="getItemIconUrl(slot.icon)"
              alt=""
              class="slot-icon-img"
              @error="onIconError"
            />
            <span v-else class="slot-fallback-id">{{ slot.id }}</span>
            <span class="slot-qty-badge">x{{ slot.quantity ?? 1 }}</span>
          </div>
          <div class="slot-name">{{ slot.name || ('Item #' + slot.id) }}</div>
          <div class="slot-meta">ID: {{ slot.id }} <span v-if="slot.upgrade">• +{{ slot.upgrade }}</span></div>
        </div>
      </div>

      <template #footer>
        <el-button size="small" @click="bagVisible = false">Đóng</el-button>
      </template>
    </el-dialog>

    <!-- Modal Sửa Vật phẩm -->
    <el-dialog v-model="editItemVisible" :title="'Chỉnh sửa Vật phẩm #' + (editItemForm.id ?? '')" width="460px" destroy-on-close>
      <el-form label-position="top" size="small">
        <el-form-item label="Tên vật phẩm">
          <el-input v-model="editItemForm.name" />
        </el-form-item>
        <el-form-item label="Cấp độ yêu cầu">
          <el-input-number v-model="editItemForm.level" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="Mô tả">
          <el-input v-model="editItemForm.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button size="small" @click="editItemVisible = false">Hủy</el-button>
        <el-button size="small" type="primary" @click="submitEditItem">Lưu thay đổi</el-button>
      </template>
    </el-dialog>

    <!-- Modal Tặng Quà từ Danh mục Vật phẩm -->
    <el-dialog v-model="sendGiftVisible" title="🎁 Tặng Vật phẩm cho Nhân vật" width="440px" destroy-on-close>
      <div v-if="sendGiftTargetItem" class="gift-item-preview">
        <div class="item-icon-frame">
          <img
            v-if="sendGiftTargetItem.icon !== null && sendGiftTargetItem.icon !== undefined"
            :src="getItemIconUrl(sendGiftTargetItem.icon)"
            alt=""
            class="item-icon-img"
            @error="onIconError"
          />
        </div>
        <div>
          <div class="gift-item-name">{{ sendGiftTargetItem.name }}</div>
          <div class="gift-item-id">ID: {{ sendGiftTargetItem.id }} • Cấp {{ sendGiftTargetItem.level }}</div>
        </div>
      </div>

      <el-form label-position="top" size="small">
        <el-form-item label="Chọn Nhân vật nhận quà">
          <el-select v-model="sendGiftForm.playerId" filterable placeholder="Chọn nhân vật..." style="width: 100%">
            <el-option
              v-for="p in playersList"
              :key="p.id"
              :label="`${p.name} (@${p.username || 'N/A'}) - ID #${p.id}`"
              :value="p.id"
            />
          </el-select>
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="Số lượng">
              <el-input-number v-model="sendGiftForm.quantity" :min="1" :max="9999" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Trạng thái khóa">
              <el-checkbox v-model="sendGiftForm.isLock">Khóa vật phẩm</el-checkbox>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button size="small" @click="sendGiftVisible = false">Hủy</el-button>
        <el-button size="small" type="success" :loading="sendGiftSaving" @click="submitSendGiftFromItem">
          🎁 Gửi Quà Ngay
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import { useAuthStore } from '../store/auth'
import api from '../api'

// Helpers khai báo đầu script setup (tránh TDZ ReferenceError)
function getItemIconUrl(iconId) {
  const baseUrl = api.defaults?.baseURL || '/api'
  const cleanBase = baseUrl.endsWith('/') ? baseUrl.slice(0, -1) : baseUrl
  return `${cleanBase}/game-nso/icons/${iconId}`
}

function onIconError(event) {
  if (event?.target) {
    event.target.style.display = 'none'
  }
}

function formatNumber(val) {
  return Number(val ?? 0).toLocaleString('vi-VN')
}

function formatClassLabel(cls) {
  const map = {
    0: 'Chưa nhập học',
    1: 'Kiếm',
    2: 'Tiêu',
    3: 'Kunai',
    4: 'Cung',
    5: 'Đao',
    6: 'Quạt'
  }
  return map[cls] ?? ('Phái ' + cls)
}

function formatBagJson(raw) {
  if (!raw || !String(raw).trim()) return '(Túi đồ trống)'
  try {
    return JSON.stringify(JSON.parse(raw), null, 2)
  } catch {
    return String(raw)
  }
}

const authStore = useAuthStore()
const route = useRoute()
const canManage = computed(() => authStore.isAdmin || authStore.user?.role === 'MANAGER')

const activeTab = ref('overview')
const statusLoading = ref(false)
const launchingClient = ref(false)
const startingServer = ref(false)

const overview = reactive({
  serverOnline: false,
  serverHost: '127.0.0.1',
  serverPort: 14444,
  totalUsers: 0,
  onlineUsers: 0,
  totalPlayers: 0,
  totalItems: 0
})

// Users tab
const usersLoading = ref(false)
const usersList = ref([])
const usersTotal = ref(0)
const usersPage = ref(1)
const userSearch = ref('')
const createUserVisible = ref(false)
const createUserForm = reactive({ username: '', password: '', luong: 10000, coin: 100000 })
const topupVisible = ref(false)
const topupTarget = ref(null)
const topupForm = reactive({ luongDelta: 5000, coinDelta: 0 })

// Players tab
const playersLoading = ref(false)
const playersList = ref([])
const playersTotal = ref(0)
const playersPage = ref(1)
const playerSearch = ref('')
const buffVisible = ref(false)
const buffSaving = ref(false)
const buffTarget = ref(null)
const buffForm = reactive({
  xuDelta: 1000000,
  yenDelta: 1000000,
  luongDelta: 5000,
  pointDelta: 0,
  spointDelta: 0,
  uydanhDelta: 0,
  expDelta: 0
})
const bagVisible = ref(false)
const bagLoading = ref(false)
const bagTarget = ref(null)
const bagItems = ref([])
const giveItemSaving = ref(false)
const giveItemForm = reactive({ itemId: 0, quantity: 1, isLock: false })

// Items tab
const itemsLoading = ref(false)
const itemsList = ref([])
const itemsTotal = ref(0)
const itemsPage = ref(1)
const itemSearch = ref('')
const itemTypeFilter = ref(null)
const editItemVisible = ref(false)
const editItemForm = reactive({ id: null, name: '', description: '', level: 1 })
const sendGiftVisible = ref(false)
const sendGiftSaving = ref(false)
const sendGiftTargetItem = ref(null)
const sendGiftForm = reactive({ playerId: null, quantity: 1, isLock: false })

const itemTypeOptions = [
  { value: 0, label: '0 - Nón' },
  { value: 1, label: '1 - Vũ khí' },
  { value: 2, label: '2 - Áo' },
  { value: 3, label: '3 - Dây chuyền' },
  { value: 4, label: '4 - Găng tay' },
  { value: 5, label: '5 - Nhẫn' },
  { value: 6, label: '6 - Quần' },
  { value: 7, label: '7 - Ngọc bội' },
  { value: 8, label: '8 - Giày' },
  { value: 9, label: '9 - Bùa' },
  { value: 10, label: '10 - Thú nuôi' },
  { value: 11, label: '11 - Mặt nạ' },
  { value: 16, label: '16 - HP / MP' },
  { value: 17, label: '17 - Thức ăn' },
  { value: 26, label: '26 - Đá nâng cấp' },
  { value: 27, label: '27 - Vật phẩm khác / SK' }
]

// GM Tools & Clan tab
const gmBuffSaving = ref(false)
const gmBuffForm = reactive({
  playerId: null,
  charClass: null,
  potentialOverride: '',
  pointDelta: 0,
  spointDelta: 0,
  uydanhDelta: 0,
  xuDelta: 0,
  yenDelta: 0,
  luongDelta: 0,
  expDelta: 0
})

const clansLoading = ref(false)
const clansList = ref([])
const clanSearch = ref('')
const createClanVisible = ref(false)
const createClanForm = reactive({ name: '', leaderName: '', level: 1, coin: 10000000, alert: '' })
const buffClanVisible = ref(false)
const buffClanTarget = ref(null)
const buffClanForm = reactive({ coinDelta: 5000000, levelDelta: 1, expDelta: 0, alert: '' })

// Logs tab
const logSource = ref('server')
const logSearch = ref('')
const logsLoading = ref(false)
const logAutoRefresh = ref(true)
const gameLogs = ref([])
const logBoxRef = ref(null)
let logTimer = null

const filteredGameLogs = computed(() => {
  const kw = logSearch.value?.trim().toLowerCase()
  if (!kw) return gameLogs.value
  return gameLogs.value.filter(line => String(line).toLowerCase().includes(kw))
})

async function fetchOverview() {
  statusLoading.value = true
  try {
    const res = await api.get('/game-nso/status')
    if (res?.data) Object.assign(overview, res.data)
  } catch (err) {
    ElMessage.error('Không thể kiểm tra trạng thái Game Server NSO!')
  } finally {
    statusLoading.value = false
  }
}

async function handleLaunchClient() {
  if (launchingClient.value) return
  launchingClient.value = true
  try {
    const res = await api.post('/game-nso/launch-client')
    ElMessage.success(res?.data?.message || 'Đã khởi chạy Client Game NSO!')
    if (canManage.value) {
      activeTab.value = 'logs'
      fetchGameLogs()
      handleToggleAutoRefresh(logAutoRefresh.value)
    }
  } catch (err) {
    ElMessage.error(err?.response?.data?.message || 'Không thể mở giả lập Game NSO!')
  } finally {
    launchingClient.value = false
  }
}

async function handleStartServer() {
  startingServer.value = true
  try {
    const res = await api.post('/game-nso/start-server')
    ElMessage.success(res?.data?.message || 'Đang khởi động Game Server!')
    setTimeout(fetchOverview, 4000)
  } catch (err) {
    ElMessage.error(err?.response?.data?.message || 'Không thể khởi động Server!')
  } finally {
    startingServer.value = false
  }
}

async function fetchUsers(resetPage = false) {
  if (resetPage) usersPage.value = 1
  usersLoading.value = true
  try {
    const res = await api.get('/game-nso/users', {
      params: { keyword: userSearch.value || undefined, page: usersPage.value - 1, size: 15 }
    })
    usersList.value = res?.data?.content ?? []
    usersTotal.value = res?.data?.totalElements ?? 0
  } catch (err) {
    ElMessage.error('Lỗi tải danh sách tài khoản game!')
  } finally {
    usersLoading.value = false
  }
}

function openCreateUserModal() {
  createUserForm.username = ''
  createUserForm.password = ''
  createUserForm.luong = 10000
  createUserForm.coin = 100000
  createUserVisible.value = true
}

async function submitCreateUser() {
  try {
    const res = await api.post('/game-nso/users', createUserForm)
    ElMessage.success(res?.data?.message || 'Đã tạo tài khoản!')
    createUserVisible.value = false
    fetchUsers(true)
  } catch (err) {
    ElMessage.error(err?.response?.data?.message || 'Lỗi tạo tài khoản!')
  }
}

function openTopupModal(row) {
  topupTarget.value = row
  topupForm.luongDelta = 5000
  topupForm.coinDelta = 0
  topupVisible.value = true
}

async function submitTopup() {
  try {
    const res = await api.post(`/game-nso/users/${topupTarget.value.id}/topup`, topupForm)
    ElMessage.success(res?.data?.message || 'Đã nạp thành công!')
    topupVisible.value = false
    fetchUsers(false)
  } catch (err) {
    ElMessage.error('Lỗi nạp Lượng/Coin!')
  }
}

async function handleToggleLock(row) {
  try {
    const res = await api.patch(`/game-nso/users/${row.id}/toggle-lock`)
    ElMessage.success(res?.data?.message || 'Đã cập nhật trạng thái!')
    fetchUsers(false)
  } catch (err) {
    ElMessage.error('Lỗi thay đổi trạng thái khóa!')
  }
}

async function fetchPlayers(resetPage = false) {
  if (resetPage) playersPage.value = 1
  playersLoading.value = true
  try {
    const res = await api.get('/game-nso/players', {
      params: { keyword: playerSearch.value || undefined, page: playersPage.value - 1, size: 15 }
    })
    playersList.value = res?.data?.content ?? []
    playersTotal.value = res?.data?.totalElements ?? 0
  } catch (err) {
    ElMessage.error('Lỗi tải danh sách nhân vật!')
  } finally {
    playersLoading.value = false
  }
}

function openBuffModal(row) {
  buffTarget.value = row
  buffForm.xuDelta = 1000000
  buffForm.yenDelta = 1000000
  buffForm.luongDelta = 5000
  buffForm.pointDelta = 0
  buffForm.spointDelta = 0
  buffForm.uydanhDelta = 0
  buffForm.expDelta = 0
  buffVisible.value = true
}

async function submitBuff() {
  buffSaving.value = true
  try {
    const res = await api.post(`/game-nso/players/${buffTarget.value.id}/buff-stats`, buffForm)
    ElMessage.success(res?.data?.message || 'Đã buff thành công!')
    buffVisible.value = false
    fetchPlayers(false)
  } catch (err) {
    ElMessage.error(err?.response?.data?.message || 'Lỗi buff nhân vật!')
  } finally {
    buffSaving.value = false
  }
}

async function openBagModal(row) {
  bagTarget.value = row
  bagItems.value = []
  bagVisible.value = true
  bagLoading.value = true
  try {
    const res = await api.get(`/game-nso/players/${row.id}/bag`)
    bagItems.value = Array.isArray(res?.data) ? res.data : []
  } catch (err) {
    ElMessage.error('Không thể tải chi tiết túi đồ!')
  } finally {
    bagLoading.value = false
  }
}

async function submitGiveItem() {
  if (giveItemForm.itemId === null || giveItemForm.itemId === undefined) return
  giveItemSaving.value = true
  try {
    const res = await api.post(`/game-nso/players/${bagTarget.value.id}/give-item`, giveItemForm)
    ElMessage.success(res?.data?.message || 'Đã gửi vật phẩm!')
    await openBagModal(bagTarget.value)
  } catch (err) {
    ElMessage.error(err?.response?.data?.message || 'Không thể gửi vật phẩm!')
  } finally {
    giveItemSaving.value = false
  }
}

async function fetchItems(resetPage = false) {
  if (resetPage) itemsPage.value = 1
  itemsLoading.value = true
  try {
    const res = await api.get('/game-nso/items', {
      params: {
        keyword: itemSearch.value || undefined,
        type: (itemTypeFilter.value !== null && itemTypeFilter.value !== '') ? itemTypeFilter.value : undefined,
        page: itemsPage.value - 1,
        size: 20
      }
    })
    itemsList.value = res?.data?.content ?? []
    itemsTotal.value = res?.data?.totalElements ?? 0
  } catch (err) {
    ElMessage.error('Lỗi tải danh sách vật phẩm!')
  } finally {
    itemsLoading.value = false
  }
}

function openEditItemModal(row) {
  editItemForm.id = row.id
  editItemForm.name = row.name
  editItemForm.description = row.description
  editItemForm.level = row.level
  editItemVisible.value = true
}

async function submitEditItem() {
  try {
    const res = await api.put(`/game-nso/items/${editItemForm.id}`, {
      name: editItemForm.name,
      description: editItemForm.description,
      level: editItemForm.level
    })
    ElMessage.success(res?.data?.message || 'Đã cập nhật vật phẩm!')
    editItemVisible.value = false
    fetchItems(false)
  } catch (err) {
    ElMessage.error('Lỗi cập nhật vật phẩm!')
  }
}

async function openSendGiftFromItemModal(row) {
  sendGiftTargetItem.value = row
  sendGiftForm.quantity = 1
  sendGiftForm.isLock = false
  if (playersList.value.length === 0) {
    await fetchPlayers(true)
  }
  if (!sendGiftForm.playerId && playersList.value.length > 0) {
    sendGiftForm.playerId = playersList.value[0].id
  }
  sendGiftVisible.value = true
}

async function submitSendGiftFromItem() {
  if (!sendGiftForm.playerId) {
    ElMessage.warning('Vui lòng chọn nhân vật nhận quà!')
    return
  }
  sendGiftSaving.value = true
  try {
    const res = await api.post(`/game-nso/players/${sendGiftForm.playerId}/give-item`, {
      itemId: sendGiftTargetItem.value.id,
      quantity: sendGiftForm.quantity,
      isLock: sendGiftForm.isLock
    })
    ElMessage.success(res?.data?.message || 'Đã gửi quà thành công!')
    sendGiftVisible.value = false
  } catch (err) {
    ElMessage.error(err?.response?.data?.message || 'Không thể gửi quà!')
  } finally {
    sendGiftSaving.value = false
  }
}

function handleTabChange(name) {
  if (name === 'users' && usersList.value.length === 0) fetchUsers(true)
  if (name === 'players' && playersList.value.length === 0) fetchPlayers(true)
  if (name === 'items' && itemsList.value.length === 0) fetchItems(true)
  if (name === 'gm-tools') {
    if (playersList.value.length === 0) fetchPlayers(true)
    if (clansList.value.length === 0) fetchClans(true)
  }
  if (name === 'logs') {
    fetchGameLogs()
    handleToggleAutoRefresh(logAutoRefresh.value)
  } else {
    stopLogTimer()
  }
}

async function fetchGameLogs() {
  logsLoading.value = true
  try {
    const res = await api.get('/game-nso/logs', {
      params: { source: logSource.value, lines: 150 }
    })
    gameLogs.value = res?.data?.lines ?? []
    await nextTick()
    if (logBoxRef.value) {
      logBoxRef.value.scrollTop = logBoxRef.value.scrollHeight
    }
  } catch (err) {
    console.error('Lỗi tải log game:', err)
  } finally {
    logsLoading.value = false
  }
}

function stopLogTimer() {
  if (logTimer) {
    clearInterval(logTimer)
    logTimer = null
  }
}

function handleToggleAutoRefresh(enabled) {
  stopLogTimer()
  if (enabled && activeTab.value === 'logs') {
    logTimer = setInterval(fetchGameLogs, 2000)
  }
}

onBeforeUnmount(() => {
  stopLogTimer()
})

async function submitGmBuffStats() {
  if (!gmBuffForm.playerId) {
    ElMessage.warning('Vui lòng chọn nhân vật cần Buff!')
    return
  }
  gmBuffSaving.value = true
  try {
    const res = await api.post(`/game-nso/players/${gmBuffForm.playerId}/buff-stats`, {
      xuDelta: gmBuffForm.xuDelta || 0,
      yenDelta: gmBuffForm.yenDelta || 0,
      luongDelta: gmBuffForm.luongDelta || 0,
      pointDelta: gmBuffForm.pointDelta || 0,
      spointDelta: gmBuffForm.spointDelta || 0,
      charClass: gmBuffForm.charClass,
      potentialOverride: gmBuffForm.potentialOverride || null,
      uydanhDelta: gmBuffForm.uydanhDelta || 0,
      expDelta: gmBuffForm.expDelta || 0
    })
    ElMessage.success(res?.data?.message || 'Đã buff chỉ số thành công!')
    fetchPlayers(false)
  } catch (err) {
    ElMessage.error(err?.response?.data?.message || 'Lỗi buff chỉ số nhân vật!')
  } finally {
    gmBuffSaving.value = false
  }
}

async function fetchClans() {
  clansLoading.value = true
  try {
    const res = await api.get('/game-nso/clans', {
      params: { keyword: clanSearch.value || undefined, page: 0, size: 50 }
    })
    clansList.value = res?.data?.content ?? []
  } catch (err) {
    ElMessage.error('Lỗi tải danh sách bang hội!')
  } finally {
    clansLoading.value = false
  }
}

function openCreateClanModal() {
  createClanForm.name = ''
  createClanForm.leaderName = ''
  createClanForm.level = 1
  createClanForm.coin = 10000000
  createClanForm.alert = ''
  createClanVisible.value = true
}

async function submitCreateClan() {
  try {
    const res = await api.post('/game-nso/clans', createClanForm)
    ElMessage.success(res?.data?.message || 'Đã thành lập bang hội!')
    createClanVisible.value = false
    fetchClans()
    fetchPlayers(false)
  } catch (err) {
    ElMessage.error(err?.response?.data?.message || 'Lỗi tạo bang hội!')
  }
}

function openBuffClanModal(row) {
  buffClanTarget.value = row
  buffClanForm.coinDelta = 5000000
  buffClanForm.levelDelta = 1
  buffClanForm.expDelta = 0
  buffClanForm.alert = ''
  buffClanVisible.value = true
}

async function submitBuffClan() {
  try {
    const res = await api.post(`/game-nso/clans/${buffClanTarget.value.id}/buff`, buffClanForm)
    ElMessage.success(res?.data?.message || 'Đã buff bang hội thành công!')
    buffClanVisible.value = false
    fetchClans()
  } catch (err) {
    ElMessage.error('Lỗi buff bang hội!')
  }
}

watch(() => route.query.tab, (tab) => {
  if (tab && canManage.value) {
    activeTab.value = String(tab)
    handleTabChange(String(tab))
  }
})

onMounted(() => {
  fetchOverview()
  if (route.query.tab && canManage.value) {
    activeTab.value = String(route.query.tab)
    handleTabChange(String(route.query.tab))
  }
})
</script>

<style scoped>
.game-nso-page {
  padding-bottom: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.page-header-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 10px 16px;
}

.header-title-block {
  flex: 1 1 280px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.title-icon-badge {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  background: #f0fdf4;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
}

.title-texts {
  display: flex;
  flex-direction: column;
}

.title-line {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.page-title {
  margin: 0;
  font-size: 15.5px;
  font-weight: 700;
  color: #0f172a;
}

.page-subtitle {
  font-size: 12px;
  color: #64748b;
}

.header-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tabs-panel {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 10px 16px;
  min-width: 0;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 14px;
}

@media (max-width: 960px) {
  .kpi-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

.kpi-card {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 10px 14px;
}

.kpi-label {
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
}

.kpi-value {
  font-size: 20px;
  font-weight: 700;
  margin: 2px 0;
}

.kpi-sub {
  font-size: 11.5px;
  color: #94a3b8;
}

.text-blue { color: #2563eb; }
.text-red { color: #dc2626; }
.text-emerald { color: #059669; }
.text-amber { color: #d97706; }
.text-purple { color: #7c3aed; }
.text-slate { color: #0f172a; }
.fw-bold { font-weight: 700; }

.launch-guide-box {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 16px 20px;
}

.guide-left h3 {
  margin: 0 0 8px;
  font-size: 14.5px;
  color: #0f172a;
}

.guide-left ul {
  margin: 0;
  padding-left: 18px;
  font-size: 13px;
  color: #334155;
  line-height: 1.7;
}

.tab-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
}

.pagination-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 10px;
  padding-top: 8px;
  border-top: 1px solid #f1f5f9;
}

.total-info {
  font-size: 12.5px;
  color: #64748b;
}

.json-viewer {
  margin: 0;
  padding: 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 12px;
  max-height: 360px;
  overflow: auto;
  white-space: pre-wrap;
}

.gm-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

@media (max-width: 1024px) {
  .gm-grid {
    grid-template-columns: 1fr;
  }
}

.gm-card {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 14px 16px;
}

.gm-card-head {
  margin-bottom: 12px;
}

.gm-card-head-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 10px;
}

.gm-card-head h3,
.gm-card-head-row h3 {
  margin: 0;
  font-size: 14.5px;
  font-weight: 700;
  color: #0f172a;
}

.gm-hint {
  font-size: 12px;
  color: #64748b;
}

.log-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 10px;
}

.log-toolbar-left {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}

.terminal-log-box {
  background: #0f172a;
  color: #e2e8f0;
  border-radius: 10px;
  padding: 12px 14px;
  height: calc(100vh - 320px);
  min-height: 260px;
  overflow: auto;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  line-height: 1.55;
}

.log-line {
  white-space: pre-wrap;
  word-break: break-word;
}

.log-empty {
  color: #94a3b8;
  font-style: italic;
}

.item-icon-frame {
  width: 34px;
  height: 34px;
  border-radius: 6px;
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.item-icon-img {
  max-width: 28px;
  max-height: 28px;
  object-fit: contain;
  image-rendering: pixelated;
}

.give-item-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  padding: 10px 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
}

.bag-grid-container {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(128px, 1fr));
  gap: 10px;
  max-height: 400px;
  overflow-y: auto;
  padding: 4px;
}

.bag-empty {
  grid-column: 1 / -1;
  text-align: center;
  padding: 32px;
  color: #94a3b8;
}

.bag-slot-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 4px;
  transition: border-color 0.15s;
}

.bag-slot-card:hover {
  border-color: #3b82f6;
}

.slot-top-row {
  width: 100%;
  display: flex;
  justify-content: space-between;
  font-size: 10.5px;
  color: #94a3b8;
}

.slot-icon-wrap {
  position: relative;
  width: 44px;
  height: 44px;
  border-radius: 8px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.slot-icon-img {
  max-width: 34px;
  max-height: 34px;
  object-fit: contain;
  image-rendering: pixelated;
}

.slot-fallback-id {
  font-size: 11px;
  font-weight: 700;
  color: #64748b;
}

.slot-qty-badge {
  position: absolute;
  right: -4px;
  bottom: -4px;
  background: #0f172a;
  color: #ffffff;
  font-size: 10px;
  font-weight: 700;
  padding: 1px 5px;
  border-radius: 99px;
}

.slot-name {
  font-size: 12px;
  font-weight: 600;
  color: #0f172a;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.3;
}

.slot-meta {
  font-size: 10.5px;
  color: #64748b;
}

.gift-item-preview {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  margin-bottom: 14px;
}

.gift-item-name {
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
}

.gift-item-id {
  font-size: 12px;
  color: #64748b;
}
</style>
