<template>
  <div class="calendar-page-container">
    <!-- TOP HEADER: TIÊU ĐỀ & ĐIỀU HƯỚNG LỊCH BIỂU (THIẾT KẾ COMPACT) -->
    <header class="calendar-header-card">
      <div class="header-left-col">
        <div class="header-icon-box">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect>
            <line x1="16" y1="2" x2="16" y2="6"></line>
            <line x1="8" y1="2" x2="8" y2="6"></line>
            <line x1="3" y1="10" x2="21" y2="10"></line>
          </svg>
        </div>
        <div>
          <div class="title-with-pill">
            <h2 class="header-title">Lịch biểu & Công việc</h2>
            <span class="pro-tag-badge">Lập lịch trực quan</span>
          </div>
          <p class="header-subtitle">Quản lý lịch họp, sự kiện doanh nghiệp và deadline dự án • Kéo thả chuột để chọn khoảng ngày lập lịch</p>
        </div>
      </div>

      <div class="header-right-col">
        <!-- Nút Bật/Tắt Bảng Công việc & To-do -->
        <button
          type="button"
          class="btn-toggle-task-board"
          :class="{ 'is-active': isTaskBoardOpen }"
          @click="isTaskBoardOpen = !isTaskBoardOpen"
        >
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M9 11l3 3L22 4"></path>
            <path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"></path>
          </svg>
          <span>Bảng Công việc</span>
          <span v-if="pendingTasksList.length > 0" class="task-badge-count">{{ pendingTasksList.length }}</span>
        </button>

        <!-- Nút Tạo sự kiện mới -->
        <button type="button" class="btn-create-event" @click="openCreateModal()">
          <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2.5">
            <line x1="12" y1="5" x2="12" y2="19"></line>
            <line x1="5" y1="12" x2="19" y2="12"></line>
          </svg>
          <span>Tạo sự kiện mới</span>
        </button>
      </div>
    </header>

    <!-- THANH THỐNG KÊ NHANH (METRICS BAR COMPACT) -->
    <section class="calendar-metrics-grid">
      <div class="metric-card">
        <div class="metric-icon-circle blue-theme">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"></circle>
            <polyline points="12 6 12 12 16 14"></polyline>
          </svg>
        </div>
        <div class="metric-info">
          <span class="metric-val">{{ stats.todayEventsCount }}</span>
          <span class="metric-label">Sự kiện hôm nay</span>
        </div>
      </div>

      <div class="metric-card">
        <div class="metric-icon-circle purple-theme">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect>
            <line x1="16" y1="2" x2="16" y2="6"></line>
            <line x1="8" y1="2" x2="8" y2="6"></line>
            <line x1="3" y1="10" x2="21" y2="10"></line>
          </svg>
        </div>
        <div class="metric-info">
          <span class="metric-val">{{ stats.thisWeekEventsCount }}</span>
          <span class="metric-label">Sự kiện trong tuần</span>
        </div>
      </div>

      <div class="metric-card">
        <div class="metric-icon-circle green-theme">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
            <circle cx="9" cy="7" r="4"></circle>
            <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
            <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
          </svg>
        </div>
        <div class="metric-info">
          <span class="metric-val">{{ stats.myMeetingsCount }}</span>
          <span class="metric-label">Cuộc họp tuần này</span>
        </div>
      </div>

      <div class="metric-card">
        <div class="metric-icon-circle red-theme">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"></circle>
            <line x1="12" y1="8" x2="12" y2="12"></line>
            <line x1="12" y1="16" x2="12.01" y2="16"></line>
          </svg>
        </div>
        <div class="metric-info">
          <span class="metric-val">{{ stats.pendingDeadlinesCount }}</span>
          <span class="metric-label">Deadline sắp tới</span>
        </div>
      </div>
    </section>

    <!-- THANH CÔNG CỤ & ĐIỀU HƯỚNG THỜI GIAN (CALENDAR CONTROLS VỚI BỘ CHỌN THÁNG/NĂM TRỰC TIẾP) -->
    <div class="calendar-toolbar-card">
      <div class="toolbar-nav-left">
        <!-- Nhóm nút điều hướng lùi/tiến -->
        <div class="date-navigator-group">
          <button type="button" class="btn-nav-arrow" title="Khoảng thời gian trước" @click="navigatePeriod(-1)">
            <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2.5">
              <polyline points="15 18 9 12 15 6"></polyline>
            </svg>
          </button>
          <button type="button" class="btn-today-pill" @click="goToToday">Hôm nay</button>
          <button type="button" class="btn-nav-arrow" title="Khoảng thời gian kế tiếp" @click="navigatePeriod(1)">
            <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2.5">
              <polyline points="9 18 15 12 9 6"></polyline>
            </svg>
          </button>
        </div>

        <!-- BỘ CHỌN THÁNG & NĂM TRỰC TIẾP (1 CLICK NHẢY ĐẾN BẤT KỲ THÁNG/NĂM NÀO) -->
        <div class="period-quick-selector-group">
          <el-date-picker
            v-model="currentFocusDate"
            :type="currentViewMode === 'year' ? 'year' : 'month'"
            :format="currentViewMode === 'year' ? '[Năm] YYYY' : '[Tháng] M, YYYY'"
            :clearable="false"
            size="small"
            class="toolbar-month-year-picker"
            placeholder="Chọn tháng, năm"
            @change="fetchEvents"
          />
        </div>

        <!-- Báo hiệu chọn khoảng ngày khi đang kéo chuột -->
        <transition name="fade">
          <div v-if="isSelectingRange && rangeDisplaySummary" class="drag-range-indicator-pill">
            <span class="drag-pulse-dot"></span>
            <span>Đang chọn: <strong>{{ rangeDisplaySummary }}</strong> (Thả chuột để tạo sự kiện)</span>
          </div>
        </transition>
      </div>

      <div class="toolbar-actions-right">
        <!-- Ô tìm kiếm sự kiện -->
        <div class="search-event-input-wrapper">
          <svg class="search-icon" viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="11" cy="11" r="8"></circle>
            <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
          </svg>
          <input
            v-model="searchQuery"
            type="text"
            placeholder="Tìm kiếm sự kiện, họp..."
            class="search-event-input"
            @input="debouncedFetch"
          />
          <button v-if="searchQuery" class="search-clear-btn" @click="searchQuery = ''; fetchEvents()">✕</button>
        </div>

        <!-- Bộ lọc loại sự kiện -->
        <el-select
          v-model="selectedTypeFilter"
          placeholder="Tất cả loại sự kiện"
          clearable
          size="small"
          class="event-type-select"
          @change="fetchEvents"
        >
          <el-option label="Tất cả loại sự kiện" value="" />
          <el-option label="👥 Cuộc họp nội bộ (Meeting)" value="MEETING" />
          <el-option label="🤝 Gặp khách hàng / Đối tác" value="CLIENT_MEETING" />
          <el-option label="⏰ Deadline dự án (Deadline)" value="DEADLINE" />
          <el-option label="📋 Nhiệm vụ công việc (Task)" value="TASK" />
          <el-option label="🎓 Đào tạo / Workshop (Training)" value="TRAINING" />
          <el-option label="💼 Phỏng vấn tuyển dụng (Interview)" value="INTERVIEW" />
          <el-option label="✈️ Đi công tác (Business Trip)" value="BUSINESS_TRIP" />
          <el-option label="📊 Đánh giá KPI (Review)" value="REVIEW" />
          <el-option label="🎉 Team Building (Team Building)" value="TEAM_BUILDING" />
          <el-option label="🛠️ Bảo trì hệ thống (Maintenance)" value="MAINTENANCE" />
          <el-option label="🏖️ Nghỉ lễ / Nghỉ phép (Holiday)" value="HOLIDAY" />
          <el-option label="📌 Sự kiện khác (Event)" value="EVENT" />
        </el-select>

        <!-- Chuyển đổi View: Năm / Quý / Tháng / Tuần / Ngày / Danh sách -->
        <div class="view-mode-tabs">
          <button
            type="button"
            class="btn-view-tab"
            :class="{ 'is-active': currentViewMode === 'year' }"
            @click="switchViewMode('year')"
          >
            Năm
          </button>
          <button
            type="button"
            class="btn-view-tab"
            :class="{ 'is-active': currentViewMode === 'quarter' }"
            @click="switchViewMode('quarter')"
          >
            Quý
          </button>
          <button
            type="button"
            class="btn-view-tab"
            :class="{ 'is-active': currentViewMode === 'month' }"
            @click="switchViewMode('month')"
          >
            Tháng
          </button>
          <button
            type="button"
            class="btn-view-tab"
            :class="{ 'is-active': currentViewMode === 'week' }"
            @click="switchViewMode('week')"
          >
            Tuần
          </button>
          <button
            type="button"
            class="btn-view-tab"
            :class="{ 'is-active': currentViewMode === 'day' }"
            @click="switchViewMode('day')"
          >
            Ngày
          </button>
          <button
            type="button"
            class="btn-view-tab"
            :class="{ 'is-active': currentViewMode === 'agenda' }"
            @click="switchViewMode('agenda')"
          >
            Danh sách
          </button>
        </div>
      </div>
    </div>

    <!-- KHÔNG GIAN LÀM VIỆC LỊCH BIỂU & CÔNG VIỆC (WORKSPACE LAYOUT) -->
    <div class="calendar-workspace-layout" :class="{ 'with-task-board': isTaskBoardOpen }">
      <!-- NỘI DUNG CHÍNH CỦA LỊCH (MAIN CALENDAR VIEWS COMPACT) -->
      <div v-loading="isLoading" class="calendar-content-card">
      <!-- ================================================================= -->
      <!-- 1. CHẾ ĐỘ XEM NĂM (YEAR VIEW - TỔNG QUAN 12 THÁNG)                 -->
      <!-- ================================================================= -->
      <div v-if="currentViewMode === 'year'" class="year-view-container">
        <div class="year-header-bar">
          <div class="year-title-block">
            <h3 class="year-main-title">Tổng quan năm {{ currentFocusDate.getFullYear() }}</h3>
            <span class="year-subtitle">Bao quát toàn bộ sự kiện và hoạt động qua 12 tháng (Nhấn vào tên tháng để xem chi tiết)</span>
          </div>
          <div class="year-stats-pills">
            <span class="stat-pill">📅 Tổng sự kiện năm: <strong>{{ eventsList.length }}</strong></span>
          </div>
        </div>

        <div class="year-grid-12months">
          <div
            v-for="m in yearMonthsList"
            :key="m.monthIndex"
            class="mini-month-card"
          >
            <div class="mini-month-header" @click="jumpToMonthView(m.monthIndex, m.year)">
              <span class="mini-month-name">{{ m.monthName }}</span>
              <span v-if="m.totalEvents > 0" class="mini-events-badge">{{ m.totalEvents }} sự kiện</span>
            </div>

            <!-- Hàng tiêu đề 7 thứ trong tuần -->
            <div class="mini-days-row">
              <span v-for="w in ['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN']" :key="w">{{ w }}</span>
            </div>

            <!-- Ma trận ngày mini -->
            <div class="mini-grid-cells">
              <div
                v-for="(cell, cIdx) in m.cells"
                :key="cIdx"
                class="mini-day-cell"
                :class="{
                  'is-empty': !cell.dayNumber,
                  'is-today': cell.isToday,
                  'has-events': cell.events && cell.events.length > 0
                }"
                :title="cell.events && cell.events.length > 0 ? `${cell.dayNumber}/${m.monthNumber}: ${cell.events.length} sự kiện` : ''"
                @click="cell.dayNumber && jumpToDayView(cell.date)"
              >
                <span class="mini-day-num">{{ cell.dayNumber }}</span>
                <span v-if="cell.events && cell.events.length > 0" class="mini-event-dot" :style="{ backgroundColor: cell.events[0].colorHex || '#2563eb' }"></span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- ================================================================= -->
      <!-- 2. CHẾ ĐỘ XEM QUÝ (QUARTER VIEW - HIỂN THỊ RỘNG 3 THÁNG)           -->
      <!-- ================================================================= -->
      <div v-else-if="currentViewMode === 'quarter'" class="quarter-view-container">
        <!-- Thanh chọn nhanh 4 Quý trong năm -->
        <div class="quarter-header-bar">
          <div class="quarter-title-block">
            <h3 class="quarter-main-title">Kế hoạch Quý {{ currentQuarter }}, {{ currentFocusDate.getFullYear() }}</h3>
            <span class="quarter-subtitle">Hiển thị song song 3 tháng liên tiếp • Xem toàn cảnh tiến độ & deadline quý</span>
          </div>

          <div class="quarter-nav-tabs">
            <button
              v-for="q in [1, 2, 3, 4]"
              :key="q"
              type="button"
              class="quarter-tab-pill"
              :class="{ 'is-active': currentQuarter === q }"
              @click="selectQuarter(q)"
            >
              Quý {{ q }} (T{{ (q-1)*3 + 1 }} - T{{ q*3 }})
            </button>
          </div>
        </div>

        <!-- Lưới 3 tháng song song trong Quý -->
        <div class="quarter-3months-grid">
          <div
            v-for="m in quarterMonthsList"
            :key="m.monthIndex"
            class="quarter-month-col"
          >
            <div class="quarter-month-header" @click="jumpToMonthView(m.monthIndex, currentFocusDate.getFullYear())">
              <h4>{{ m.monthName }}</h4>
              <span class="badge-count">{{ m.events.length }} sự kiện</span>
            </div>

            <div class="month-days-header-row mini-weekday-row">
              <div v-for="d in ['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN']" :key="d" class="weekday-header-cell">
                {{ d }}
              </div>
            </div>

            <div class="quarter-month-cells">
              <div
                v-for="cell in m.cells"
                :key="cell.dateKey"
                class="quarter-day-cell"
                :class="{
                  'is-today': cell.isToday,
                  'is-other-month': !cell.isCurrentMonth,
                  'has-events': cell.events.length > 0
                }"
                @click="jumpToDayView(cell.date)"
              >
                <div class="cell-day-header">
                  <span class="day-number-badge" :class="{ 'today-highlight': cell.isToday }">{{ cell.dayNumber }}</span>
                </div>
                <div class="cell-events-list">
                  <div
                    v-for="evt in cell.events.slice(0, 2)"
                    :key="evt.id"
                    class="event-chip-item"
                    :style="{ backgroundColor: evt.colorHex ? evt.colorHex + '22' : '#2563eb22', borderLeftColor: evt.colorHex || '#2563eb' }"
                    @click.stop="openDetailModal(evt)"
                  >
                    <span class="event-chip-title" :title="evt.title">{{ evt.title }}</span>
                  </div>
                  <span v-if="cell.events.length > 2" class="event-more-pill">+{{ cell.events.length - 2 }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Danh sách sự kiện nổi bật trong Quý -->
        <div class="quarter-events-agenda-card">
          <h4 class="agenda-section-title">📋 Lộ trình sự kiện & Deadline trong Quý {{ currentQuarter }} ({{ quarterEventsList.length }} mục)</h4>
          <div v-if="quarterEventsList.length === 0" class="empty-quarter-events">
            Chưa có sự kiện nào được lên lịch trong Quý này. Nhấn "Tạo sự kiện mới" để thêm lịch trình!
          </div>
          <div v-else class="quarter-events-table">
            <div
              v-for="evt in quarterEventsList"
              :key="evt.id"
              class="quarter-event-row"
              :style="{ borderLeftColor: evt.colorHex || '#2563eb' }"
              @click="openDetailModal(evt)"
            >
              <div class="row-date-badge">
                <span class="date-num">{{ new Date(evt.startTime).getDate() }}</span>
                <span class="month-num">Th{{ new Date(evt.startTime).getMonth() + 1 }}</span>
              </div>
              <div class="row-type-badge">
                <span class="event-type-badge-mini" :style="{ color: evt.colorHex || '#2563eb', borderColor: evt.colorHex || '#2563eb' }">
                  {{ getTypeName(evt.eventType) }}
                </span>
              </div>
              <div class="row-title-desc">
                <h5 class="row-event-title">{{ evt.title }}</h5>
                <span v-if="evt.description" class="row-event-desc">{{ evt.description }}</span>
              </div>
              <div class="row-meta">
                <span v-if="evt.location" class="meta-tag">📍 {{ evt.location }}</span>
                <span class="meta-tag">🕒 {{ formatTime(evt.startTime) }} - {{ formatTime(evt.endTime) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- ================================================================= -->
      <!-- 3. CHẾ ĐỘ XEM THÁNG (MONTH GRID VIEW KÈM KÉO THẢ CHỌN KHOẢNG NGÀY) -->
      <!-- ================================================================= -->
      <div v-else-if="currentViewMode === 'month'" class="month-view-container" @mouseleave="handleMouseLeaveGrid">
        <!-- 7 Tiêu đề các thứ trong tuần -->
        <div class="month-days-header-row">
          <div v-for="dayName in ['Thứ Hai', 'Thứ Ba', 'Thứ Tư', 'Thứ Năm', 'Thứ Sáu', 'Thứ Bảy', 'Chủ Nhật']" :key="dayName" class="weekday-header-cell">
            {{ dayName }}
          </div>
        </div>

        <!-- Lưới các ngày trong tháng (Compact 76px) -->
        <div class="month-grid-cells">
          <div
            v-for="cell in monthCalendarCells"
            :key="cell.dateKey"
            class="month-day-cell"
            :class="{
              'is-today': cell.isToday,
              'is-other-month': !cell.isCurrentMonth,
              'has-events': cell.events.length > 0,
              'is-in-drag-range': isCellInRange(cell.dateKey),
              'is-drag-start': isRangeStart(cell.dateKey),
              'is-drag-end': isRangeEnd(cell.dateKey),
              'is-drop-hover': dragOverDateKey === cell.dateKey
            }"
            @mousedown="handleCellMouseDown(cell, $event)"
            @mouseenter="handleCellMouseEnter(cell)"
            @dragover.prevent="handleDragOverCell(cell, $event)"
            @dragleave="handleDragLeaveCell(cell, $event)"
            @drop="handleDropOnCell(cell, $event)"
            @click="handleDayCellClick(cell)"
          >
            <!-- Header ngày: số ngày & nút thêm nhanh -->
            <div class="cell-day-header">
              <span class="day-number-badge" :class="{ 'today-highlight': cell.isToday }">{{ cell.dayNumber }}</span>
              <button
                type="button"
                class="btn-quick-add-event"
                title="Thêm sự kiện vào ngày này"
                @click.stop="openCreateModal(cell.date)"
              >
                +
              </button>
            </div>

            <!-- Danh sách sự kiện trong ngày -->
            <div class="cell-events-list">
              <div
                v-for="evt in cell.events.slice(0, 2)"
                :key="evt.id"
                class="event-chip-item"
                :class="{ 'is-dragging': draggingEventId === evt.id }"
                :style="{ backgroundColor: evt.colorHex ? evt.colorHex + '22' : '#2563eb22', borderLeftColor: evt.colorHex || '#2563eb' }"
                draggable="true"
                @dragstart.stop="handleDragStart(evt, $event)"
                @dragend="handleDragEnd"
                @click.stop="openDetailModal(evt)"
              >
                <span class="event-chip-time">{{ formatChipTime(evt) }}</span>
                <span class="event-chip-title" :title="evt.title">{{ evt.title }}</span>
              </div>

              <!-- Chỉ báo có thêm sự kiện -->
              <div
                v-if="cell.events.length > 2"
                class="event-more-pill"
                @click.stop="openDayAgenda(cell)"
              >
                +{{ cell.events.length - 2 }} sự kiện khác
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- ================================================================= -->
      <!-- 4. CHẾ ĐỘ XEM TUẦN (WEEK VIEW COMPACT)                             -->
      <!-- ================================================================= -->
      <div v-else-if="currentViewMode === 'week'" class="week-view-container">
        <div class="week-header-row">
          <div class="time-col-header">Giờ</div>
          <div
            v-for="day in weekDaysList"
            :key="day.dateKey"
            class="week-day-header-col"
            :class="{ 'is-today': day.isToday }"
          >
            <span class="week-day-name">{{ day.dayName }}</span>
            <span class="week-day-num" :class="{ 'today-num': day.isToday }">{{ day.dayNumber }}</span>
          </div>
        </div>

        <div class="week-time-grid">
          <div v-for="hour in hoursOfDay" :key="hour" class="week-hour-row">
            <div class="time-label-cell">{{ hour }}:00</div>
            <div
              v-for="day in weekDaysList"
              :key="day.dateKey + '-' + hour"
              class="week-hour-slot"
              @click="openCreateModal(day.date, hour)"
            >
              <!-- Các sự kiện rơi vào giờ này -->
              <div
                v-for="evt in getEventsForSlot(day.dateKey, hour)"
                :key="evt.id"
                class="week-event-card"
                :style="{ backgroundColor: evt.colorHex ? evt.colorHex + '22' : '#2563eb22', borderLeft: `3px solid ${evt.colorHex || '#2563eb'}` }"
                @click.stop="openDetailModal(evt)"
              >
                <div class="week-event-time">{{ formatTime(evt.startTime) }} - {{ formatTime(evt.endTime) }}</div>
                <div class="week-event-title" :title="evt.title">{{ evt.title }}</div>
                <div v-if="evt.location" class="week-event-location">📍 {{ evt.location }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- ================================================================= -->
      <!-- 5. CHẾ ĐỘ XEM NGÀY (DAY VIEW COMPACT)                              -->
      <!-- ================================================================= -->
      <div v-else-if="currentViewMode === 'day'" class="day-view-container">
        <div class="day-header-summary">
          <h4 class="day-summary-title">{{ formatFullDate(currentFocusDate) }}</h4>
          <span class="day-events-count">{{ dayViewEvents.length }} sự kiện trong ngày</span>
        </div>

        <div class="day-timeline-scroll">
          <div v-for="hour in hoursOfDay" :key="hour" class="day-timeline-row">
            <div class="day-hour-marker">{{ hour }}:00</div>
            <div class="day-slot-content" @click="openCreateModal(currentFocusDate, hour)">
              <div
                v-for="evt in getEventsForDayAndHour(currentFocusDate, hour)"
                :key="evt.id"
                class="day-event-banner"
                :style="{ borderLeft: `4px solid ${evt.colorHex || '#2563eb'}`, backgroundColor: evt.colorHex ? evt.colorHex + '15' : '#2563eb15' }"
                @click.stop="openDetailModal(evt)"
              >
                <div class="day-event-banner-header">
                  <span class="day-event-type-tag" :style="{ backgroundColor: evt.colorHex || '#2563eb' }">
                    {{ getTypeName(evt.eventType) }}
                  </span>
                  <span class="day-event-banner-time">{{ formatTime(evt.startTime) }} - {{ formatTime(evt.endTime) }}</span>
                </div>
                <h5 class="day-event-banner-title">{{ evt.title }}</h5>
                <p v-if="evt.description" class="day-event-banner-desc">{{ evt.description }}</p>
                <div class="day-event-banner-footer">
                  <span v-if="evt.location" class="day-footer-item">📍 {{ evt.location }}</span>
                  <a v-if="evt.meetingUrl" :href="evt.meetingUrl" target="_blank" class="day-footer-link" @click.stop>
                    🔗 Tham gia họp trực tuyến
                  </a>
                  <span class="day-footer-item">👤 {{ evt.creatorFullName || evt.creatorUsername }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- ================================================================= -->
      <!-- 6. CHẾ ĐỘ XEM DANH SÁCH (AGENDA VIEW COMPACT)                      -->
      <!-- ================================================================= -->
      <div v-else-if="currentViewMode === 'agenda'" class="agenda-view-container">
        <div v-if="agendaGroupedEvents.length === 0" class="agenda-empty-card">
          <div class="empty-icon">📅</div>
          <h4>Không có sự kiện nào</h4>
          <p>Không tìm thấy lịch họp hay công việc nào trong khoảng thời gian đã chọn</p>
          <button type="button" class="btn-create-event" @click="openCreateModal()">+ Thêm sự kiện ngay</button>
        </div>

        <div v-else class="agenda-groups-list">
          <div v-for="grp in agendaGroupedEvents" :key="grp.dateKey" class="agenda-date-group">
            <div class="agenda-date-header">
              <div class="date-badge-box">
                <span class="date-badge-day">{{ grp.dayNumber }}</span>
                <span class="date-badge-month">Tháng {{ grp.monthNumber }}</span>
              </div>
              <div class="date-weekday-title">
                <h4>{{ grp.weekdayName }}, {{ grp.dateFormatted }}</h4>
                <span class="events-badge-pill">{{ grp.events.length }} sự kiện</span>
              </div>
            </div>

            <div class="agenda-events-cards">
              <div
                v-for="evt in grp.events"
                :key="evt.id"
                class="agenda-card-item"
                :style="{ borderLeft: `4px solid ${evt.colorHex || '#2563eb'}` }"
                @click="openDetailModal(evt)"
              >
                <div class="agenda-card-left">
                  <div class="agenda-time-range">
                    <span class="start-time">{{ formatTime(evt.startTime) }}</span>
                    <span class="separator">-</span>
                    <span class="end-time">{{ formatTime(evt.endTime) }}</span>
                  </div>
                  <span class="event-type-badge-mini" :style="{ color: evt.colorHex || '#2563eb', borderColor: evt.colorHex || '#2563eb' }">
                    {{ getTypeName(evt.eventType) }}
                  </span>
                </div>

                <div class="agenda-card-body">
                  <h4 class="agenda-item-title">{{ evt.title }}</h4>
                  <p v-if="evt.description" class="agenda-item-desc">{{ evt.description }}</p>
                  <div class="agenda-item-meta">
                    <span v-if="evt.location" class="meta-tag">📍 {{ evt.location }}</span>
                    <span v-if="evt.department" class="meta-tag">🏢 {{ evt.department }}</span>
                    <span class="meta-tag">👤 Tạo bởi: {{ evt.creatorFullName || evt.creatorUsername }}</span>
                    <span v-if="evt.attendees && evt.attendees.length > 0" class="meta-tag">
                      👥 {{ evt.attendees.length }} người tham gia
                    </span>
                  </div>
                </div>

                <div class="agenda-card-right">
                  <a
                    v-if="evt.meetingUrl"
                    :href="evt.meetingUrl"
                    target="_blank"
                    class="btn-join-meet"
                    @click.stop
                  >
                    Vào họp 🔗
                  </a>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

      <!-- BẢNG CÔNG VIỆC, TO-DO, TIẾN ĐỘ & DEADLINE (TASK BOARD SIDEBAR) -->
      <aside v-if="isTaskBoardOpen" class="task-board-panel">
        <div class="task-panel-header">
          <div class="panel-header-title">
            <span class="panel-header-icon">📋</span>
            <div class="panel-titles">
              <h3 class="panel-main-title">Công việc & Deadline</h3>
              <span class="panel-sub-title">Kéo thẻ việc thả vào ngày trên Lịch để dời hạn chót</span>
            </div>
          </div>
          <button type="button" class="btn-panel-close" title="Thu gọn bảng công việc" @click="isTaskBoardOpen = false">✕</button>
        </div>

        <!-- KPI TIẾN ĐỘ HOÀN THÀNH CÔNG VIỆC -->
        <div class="task-kpi-card">
          <div class="kpi-header">
            <span class="kpi-label">Tiến độ công việc</span>
            <span class="kpi-rate">{{ completedTasksCount }}/{{ allTasksList.length }} việc ({{ taskCompletionRate }}%)</span>
          </div>
          <div class="kpi-progress-track">
            <div class="kpi-progress-bar" :style="{ width: taskCompletionRate + '%' }"></div>
          </div>
        </div>

        <!-- BỘ LỌC CÔNG VIỆC NHANH (Tất cả, Của tôi, Hôm nay, Chờ xử lý, Đã xong) -->
        <div class="task-filter-pills">
          <button
            type="button"
            class="task-filter-pill"
            :class="{ 'is-active': taskFilter === 'all' }"
            @click="taskFilter = 'all'"
          >
            Tất cả ({{ allTasksList.length }})
          </button>
          <button
            type="button"
            class="task-filter-pill"
            :class="{ 'is-active': taskFilter === 'my' }"
            @click="taskFilter = 'my'"
          >
            Của tôi ({{ myTasksList.length }})
          </button>
          <button
            type="button"
            class="task-filter-pill"
            :class="{ 'is-active': taskFilter === 'today' }"
            @click="taskFilter = 'today'"
          >
            Hôm nay ({{ todayTasksList.length }})
          </button>
          <button
            type="button"
            class="task-filter-pill"
            :class="{ 'is-active': taskFilter === 'pending' }"
            @click="taskFilter = 'pending'"
          >
            Chưa xong ({{ pendingTasksList.length }})
          </button>
          <button
            type="button"
            class="task-filter-pill"
            :class="{ 'is-active': taskFilter === 'completed' }"
            @click="taskFilter = 'completed'"
          >
            Đã xong ({{ completedTasksList.length }})
          </button>
        </div>

        <!-- Ô NHẬP TẠO NHANH CÔNG VIỆC (QUICK TASK INPUT) -->
        <div class="quick-task-input-box">
          <input
            v-model="quickTaskTitle"
            type="text"
            placeholder="+ Thêm công việc nhanh (Enter để lưu)..."
            class="quick-task-input"
            @keyup.enter="handleCreateQuickTask"
          />
          <button
            v-if="quickTaskTitle.trim()"
            type="button"
            class="btn-submit-quick-task"
            @click="handleCreateQuickTask"
          >
            Lưu
          </button>
        </div>

        <!-- DANH SÁCH THẺ CÔNG VIỆC (TASK CARDS) -->
        <div class="task-list-scroll">
          <div v-if="filteredTasksList.length === 0" class="task-list-empty">
            <span class="empty-icon">🎉</span>
            <p>Không có công việc nào trong mục này</p>
          </div>

          <div
            v-for="task in filteredTasksList"
            :key="task.id"
            class="task-card-item"
            :class="{
              'is-completed': task.status === 'COMPLETED' || (task.progress >= 100),
              'is-dragging': draggingEventId === task.id
            }"
            :style="{ borderLeftColor: task.colorHex || '#2563eb' }"
            draggable="true"
            @dragstart="handleDragStart(task, $event)"
            @dragend="handleDragEnd"
            @click="openDetailModal(task)"
          >
            <div class="task-card-top-row">
              <!-- Checkbox hoàn thành nhanh -->
              <button
                type="button"
                class="task-checkbox-btn"
                :class="{ 'is-checked': task.status === 'COMPLETED' || (task.progress >= 100) }"
                :title="task.status === 'COMPLETED' ? 'Đánh dấu chưa hoàn thành' : 'Đánh dấu hoàn thành'"
                @click.stop="toggleTaskStatus(task)"
              >
                <svg v-if="task.status === 'COMPLETED' || (task.progress >= 100)" viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="3">
                  <polyline points="20 6 9 17 4 12"></polyline>
                </svg>
              </button>

              <div class="task-card-main-info">
                <h4 class="task-card-title" :class="{ 'completed-strikethrough': task.status === 'COMPLETED' || (task.progress >= 100) }">
                  {{ task.title }}
                </h4>
                
                <div class="task-badges-row">
                  <span class="task-type-badge-mini" :style="{ backgroundColor: (task.colorHex || '#2563eb') + '18', color: task.colorHex || '#2563eb' }">
                    {{ getTypeName(task.eventType) }}
                  </span>
                  <span v-if="task.priority" class="task-priority-badge-mini" :class="getPriorityClass(task.priority)">
                    {{ getPriorityName(task.priority) }}
                  </span>
                </div>
              </div>
            </div>

            <!-- THANH TIẾN ĐỘ % & SUBTASKS -->
            <div class="task-card-progress-section">
              <div class="progress-labels">
                <span class="subtask-count-text">
                  <span v-if="task.subtasks && task.subtasks.length > 0">
                    ☑️ {{ countCompletedSubtasks(task.subtasks) }}/{{ task.subtasks.length }} việc con
                  </span>
                  <span v-else>Tiến độ</span>
                </span>
                <span class="progress-percent-val">{{ task.progress || 0 }}%</span>
              </div>
              <div class="mini-progress-track">
                <div
                  class="mini-progress-fill"
                  :style="{
                    width: (task.progress || 0) + '%',
                    backgroundColor: (task.progress >= 100 || task.status === 'COMPLETED') ? '#10b981' : (task.colorHex || '#2563eb')
                  }"
                ></div>
              </div>
            </div>

            <!-- DÒNG META: PHỤ TRÁCH & DEADLINE -->
            <div class="task-card-footer-meta">
              <div class="task-assignee-box" :title="'Người phụ trách: ' + (task.assigneeFullName || task.assigneeUsername || 'Chưa phân công')">
                <span class="assignee-avatar-mini">{{ getAvatarInitial(task.assigneeFullName || task.assigneeUsername) }}</span>
                <span class="assignee-name">{{ task.assigneeFullName || (task.assigneeUsername ? '@' + task.assigneeUsername : 'Chưa phân công') }}</span>
              </div>

              <div class="task-deadline-tag" :class="{ 'is-overdue': isTaskOverdue(task) }">
                <svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="12" r="10"></circle>
                  <polyline points="12 6 12 12 16 14"></polyline>
                </svg>
                <span>{{ formatTaskDeadline(task.endTime) }}</span>
              </div>
            </div>
          </div>
        </div>
      </aside>
    </div>

    <!-- MODAL 1: THÊM MỚI / CHỈNH SỬA SỰ KIỆN -->
    <Teleport to="body">
      <div v-if="showEventModal" class="modal-backdrop-overlay" @click.self="showEventModal = false">
        <div class="custom-modal-card calendar-modal-card">
          <div class="modal-card-header">
            <div class="header-icon-title">
              <div class="modal-header-icon" :style="{ backgroundColor: form.colorHex || '#2563eb', color: '#fff' }">
                📅
              </div>
              <div class="modal-title-group">
                <h3 class="modal-card-title">{{ isEditing ? 'Chỉnh sửa sự kiện' : 'Tạo sự kiện mới' }}</h3>
                <span class="modal-card-desc">Thiết lập thời gian, người tham gia và phòng họp</span>
              </div>
            </div>
            <button type="button" class="btn-close-modal" @click="showEventModal = false">✕</button>
          </div>

          <form @submit.prevent="handleSaveEvent">
            <div class="modal-card-body modal-form-scroll">
              <!-- Tiêu đề sự kiện -->
              <div class="form-group-field">
                <label class="field-label">Tiêu đề sự kiện / Cuộc họp <span class="required-star">*</span></label>
                <input
                  v-model="form.title"
                  type="text"
                  required
                  placeholder="Ví dụ: Họp bàn giao Sprint 4, Phỏng vấn ứng viên..."
                  class="form-text-input"
                />
              </div>

              <!-- Loại sự kiện & Mức độ ưu tiên -->
              <div class="form-row-2col">
                <div class="form-group-field">
                  <label class="field-label">Loại sự kiện</label>
                  <el-select v-model="form.eventType" style="width: 100%" @change="handleTypeChange">
                    <el-option label="👥 Cuộc họp nội bộ (Meeting)" value="MEETING" />
                    <el-option label="🤝 Gặp khách hàng / Đối tác (Client Meeting)" value="CLIENT_MEETING" />
                    <el-option label="⏰ Deadline / Bàn giao dự án (Deadline)" value="DEADLINE" />
                    <el-option label="📋 Nhiệm vụ / Công việc (Task)" value="TASK" />
                    <el-option label="🎓 Đào tạo / Workshop (Training)" value="TRAINING" />
                    <el-option label="💼 Phỏng vấn / Tuyển dụng (Interview)" value="INTERVIEW" />
                    <el-option label="✈️ Đi công tác / Thị trường (Business Trip)" value="BUSINESS_TRIP" />
                    <el-option label="📊 Đánh giá KPI / Hiệu suất (Review)" value="REVIEW" />
                    <el-option label="🎉 Team Building / Sinh hoạt (Team Building)" value="TEAM_BUILDING" />
                    <el-option label="🛠️ Bảo trì hệ thống / Hạ tầng (Maintenance)" value="MAINTENANCE" />
                    <el-option label="🏖️ Nghỉ lễ / Nghỉ phép (Holiday)" value="HOLIDAY" />
                    <el-option label="📌 Sự kiện chung khác (Event)" value="EVENT" />
                  </el-select>
                </div>

                <div class="form-group-field">
                  <label class="field-label">Mức độ ưu tiên</label>
                  <el-select v-model="form.priority" style="width: 100%">
                    <el-option label="🔴 Tối khẩn (Critical)" value="CRITICAL" />
                    <el-option label="🟠 Khẩn cấp (Urgent)" value="URGENT" />
                    <el-option label="🟡 Quan trọng / Cao (High)" value="HIGH" />
                    <el-option label="🔵 Bình thường (Medium)" value="MEDIUM" />
                    <el-option label="🟢 Thấp (Low)" value="LOW" />
                    <el-option label="⚪ Tùy chọn / Không bắt buộc (Optional)" value="OPTIONAL" />
                  </el-select>
                </div>
              </div>

              <!-- Thời gian bắt đầu & kết thúc -->
              <div class="form-row-2col">
                <div class="form-group-field">
                  <label class="field-label">Bắt đầu <span class="required-star">*</span></label>
                  <el-date-picker
                    v-model="form.startTime"
                    type="datetime"
                    placeholder="Chọn ngày giờ bắt đầu"
                    format="DD/MM/YYYY HH:mm"
                    value-format="YYYY-MM-DDTHH:mm:ss"
                    style="width: 100%"
                  />
                </div>

                <div class="form-group-field">
                  <label class="field-label">Kết thúc <span class="required-star">*</span></label>
                  <el-date-picker
                    v-model="form.endTime"
                    type="datetime"
                    placeholder="Chọn ngày giờ kết thúc"
                    format="DD/MM/YYYY HH:mm"
                    value-format="YYYY-MM-DDTHH:mm:ss"
                    style="width: 100%"
                  />
                </div>
              </div>

              <!-- Checkbox Cả ngày -->
              <div class="form-group-field">
                <el-checkbox v-model="form.isAllDay">Sự kiện diễn ra cả ngày</el-checkbox>
              </div>

              <!-- Địa điểm & Đường link cuộc họp -->
              <div class="form-row-2col">
                <div class="form-group-field">
                  <label class="field-label">Địa điểm / Phòng họp</label>
                  <input
                    v-model="form.location"
                    type="text"
                    placeholder="Ví dụ: Phòng họp tầng 3, Kho tổng..."
                    class="form-text-input"
                  />
                </div>

                <div class="form-group-field">
                  <label class="field-label">Link phòng họp trực tuyến</label>
                  <input
                    v-model="form.meetingUrl"
                    type="url"
                    placeholder="https://meet.google.com/..."
                    class="form-text-input"
                  />
                </div>
              </div>

              <!-- Người tham gia (Attendees) & Phòng ban -->
              <div class="form-row-2col">
                <div class="form-group-field">
                  <label class="field-label">Đồng nghiệp tham gia</label>
                  <el-select
                    v-model="form.attendees"
                    multiple
                    filterable
                    placeholder="Chọn người tham gia"
                    style="width: 100%"
                  >
                    <el-option
                      v-for="user in colleagueOptions"
                      :key="user.username"
                      :label="user.fullName ? `${user.fullName} (@${user.username})` : `@${user.username}`"
                      :value="user.username"
                    />
                  </el-select>
                </div>

                <div class="form-group-field">
                  <label class="field-label">Phòng ban liên quan</label>
                  <input
                    v-model="form.department"
                    type="text"
                    placeholder="Toàn công ty, Kỹ thuật, Kinh doanh..."
                    class="form-text-input"
                  />
                </div>
              </div>

              <!-- Người phụ trách chính (Assignee) & Tiến độ % -->
              <div class="form-row-2col">
                <div class="form-group-field">
                  <label class="field-label">Người phụ trách chính (Assignee)</label>
                  <el-select
                    v-model="form.assigneeUsername"
                    clearable
                    filterable
                    placeholder="Chọn người chịu trách nhiệm"
                    style="width: 100%"
                  >
                    <el-option
                      v-for="user in colleagueOptions"
                      :key="user.username"
                      :label="user.fullName ? `${user.fullName} (@${user.username})` : `@${user.username}`"
                      :value="user.username"
                    />
                  </el-select>
                </div>

                <div class="form-group-field">
                  <div class="field-label-with-val">
                    <label class="field-label">Tiến độ công việc</label>
                    <span class="field-val-badge">{{ form.progress || 0 }}%</span>
                  </div>
                  <el-slider
                    v-model="form.progress"
                    :min="0"
                    :max="100"
                    :step="5"
                    style="margin-top: 4px"
                  />
                </div>
              </div>

              <!-- Checklist việc con (Subtasks) -->
              <div class="form-group-field">
                <label class="field-label">
                  Checklist việc con (Subtasks)
                  <span v-if="form.subtasks && form.subtasks.length > 0" class="subtask-header-counter">
                    ({{ countCompletedSubtasks(form.subtasks) }}/{{ form.subtasks.length }} việc xong)
                  </span>
                </label>
                <div class="subtask-input-row">
                  <input
                    v-model="subtaskDraftText"
                    type="text"
                    placeholder="Nhập việc con cần làm (ví dụ: Soạn thảo hợp đồng, gửi email...)"
                    class="form-text-input subtask-text-input"
                    @keyup.enter.prevent="addSubtaskToForm"
                  />
                  <button type="button" class="btn-add-subtask" @click="addSubtaskToForm">+ Thêm</button>
                </div>

                <!-- Danh sách việc con trong form -->
                <div v-if="form.subtasks && form.subtasks.length > 0" class="form-subtasks-list">
                  <div
                    v-for="(st, idx) in form.subtasks"
                    :key="st.id || idx"
                    class="subtask-list-item"
                  >
                    <el-checkbox
                      v-model="st.completed"
                      @change="onSubtaskFormCheckChange"
                    >
                      <span :class="{ 'subtask-done-text': st.completed }">{{ st.title }}</span>
                    </el-checkbox>
                    <button type="button" class="btn-remove-subtask" title="Xóa việc con này" @click="removeSubtaskFromForm(idx)">✕</button>
                  </div>
                </div>
              </div>

              <!-- Tùy chọn dải màu sắc sự kiện (Color Picker & Dải màu tùy chỉnh) -->
              <div class="form-group-field">
                <div class="field-label-with-val">
                  <label class="field-label">Màu sắc đánh dấu trên lịch (Kéo dải màu hoặc chọn nhanh)</label>
                  <span class="field-val-badge" :style="{ color: form.colorHex, backgroundColor: (form.colorHex || '#2563eb') + '18' }">
                    {{ form.colorHex || '#2563eb' }}
                  </span>
                </div>

                <div class="custom-color-picker-row">
                  <!-- Dải màu Element Plus Color Picker (kéo gradient & dải quang phổ Hue) -->
                  <div class="el-picker-container" title="Kéo dải màu tùy chỉnh">
                    <el-color-picker
                      v-model="form.colorHex"
                      :predefine="colorPresets"
                      size="default"
                    />
                  </div>

                  <!-- Ô nhập mã màu HEX có chấm chỉ báo màu -->
                  <div class="color-hex-input-wrapper" title="Nhập trực tiếp mã màu HEX">
                    <span class="color-dot-indicator" :style="{ backgroundColor: form.colorHex || '#2563eb' }"></span>
                    <input
                      v-model="form.colorHex"
                      type="text"
                      placeholder="#2563eb"
                      class="color-hex-text-input"
                      maxlength="7"
                    />
                  </div>

                  <span class="color-divider-line"></span>

                  <!-- Dải màu gợi ý phổ biến (Click chọn nhanh) -->
                  <div class="color-preset-picker" title="Bảng màu gợi ý">
                    <button
                      v-for="color in colorPresets"
                      :key="color"
                      type="button"
                      class="color-dot-btn"
                      :style="{ backgroundColor: color }"
                      :class="{ 'is-selected': form.colorHex === color }"
                      @click="form.colorHex = color"
                    ></button>
                  </div>
                </div>
              </div>

              <!-- Mô tả chi tiết -->
              <div class="form-group-field">
                <label class="field-label">Nội dung / Ghi chú cuộc họp</label>
                <textarea
                  v-model="form.description"
                  rows="3"
                  placeholder="Ghi chú tóm tắt nội dung cuộc họp, tài liệu đính kèm..."
                  class="form-textarea-input"
                ></textarea>
              </div>
            </div>

            <div class="modal-card-footer">
              <button type="button" class="btn-modal-cancel" @click="showEventModal = false">Hủy</button>
              <button type="submit" class="btn-modal-primary" :disabled="isSaving">
                {{ isSaving ? 'Đang lưu...' : (isEditing ? 'Cập nhật' : 'Tạo sự kiện') }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </Teleport>

    <!-- MODAL 2: CHI TIẾT SỰ KIỆN (DETAIL MODAL) -->
    <Teleport to="body">
      <div v-if="showDetailModal && selectedEvent" class="modal-backdrop-overlay" @click.self="showDetailModal = false">
        <div class="custom-modal-card calendar-detail-card">
          <div class="modal-card-header" :style="{ borderTop: `4px solid ${selectedEvent.colorHex || '#2563eb'}` }">
            <div class="header-icon-title">
              <span class="detail-type-tag" :style="{ backgroundColor: selectedEvent.colorHex || '#2563eb' }">
                {{ getTypeName(selectedEvent.eventType) }}
              </span>
              <span v-if="selectedEvent.priority" class="detail-priority-tag" :class="getPriorityClass(selectedEvent.priority)">
                {{ getPriorityName(selectedEvent.priority) }}
              </span>
              <h3 class="modal-card-title">{{ selectedEvent.title }}</h3>
            </div>
            <button type="button" class="btn-close-modal" @click="showDetailModal = false">✕</button>
          </div>

          <div class="modal-card-body detail-body-scroll">
            <!-- Khung thời gian -->
            <div class="detail-info-row">
              <span class="detail-row-icon">🕒</span>
              <div class="detail-row-content">
                <strong>Thời gian:</strong>
                <div>{{ formatFullDate(selectedEvent.startTime) }}</div>
                <div class="detail-time-range">{{ formatTime(selectedEvent.startTime) }} - {{ formatTime(selectedEvent.endTime) }} ({{ getDurationText(selectedEvent.startTime, selectedEvent.endTime) }})</div>
              </div>
            </div>

            <!-- Địa điểm -->
            <div v-if="selectedEvent.location" class="detail-info-row">
              <span class="detail-row-icon">📍</span>
              <div class="detail-row-content">
                <strong>Địa điểm:</strong>
                <div>{{ selectedEvent.location }}</div>
              </div>
            </div>

            <!-- Link họp trực tuyến -->
            <div v-if="selectedEvent.meetingUrl" class="detail-info-row">
              <span class="detail-row-icon">🎥</span>
              <div class="detail-row-content">
                <strong>Phòng họp trực tuyến:</strong>
                <div>
                  <a :href="selectedEvent.meetingUrl" target="_blank" class="detail-meet-link">
                    {{ selectedEvent.meetingUrl }}
                    <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2.5">
                      <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"></path>
                      <polyline points="15 3 21 3 21 9"></polyline>
                      <line x1="10" y1="14" x2="21" y2="3"></line>
                    </svg>
                  </a>
                </div>
              </div>
            </div>

            <!-- Người tạo & phòng ban -->
            <div class="detail-info-row">
              <span class="detail-row-icon">👤</span>
              <div class="detail-row-content">
                <strong>Người tổ chức:</strong>
                <div>{{ selectedEvent.creatorFullName || selectedEvent.creatorUsername }} • Phòng ban: {{ selectedEvent.department || 'Toàn công ty' }}</div>
              </div>
            </div>

            <!-- Người tham gia -->
            <div v-if="selectedEvent.attendees && selectedEvent.attendees.length > 0" class="detail-info-row">
              <span class="detail-row-icon">👥</span>
              <div class="detail-row-content">
                <strong>Người tham gia ({{ selectedEvent.attendees.length }}):</strong>
                <div class="attendees-tags-wrap">
                  <span v-for="username in selectedEvent.attendees" :key="username" class="attendee-pill">
                    @{{ username }}
                  </span>
                </div>
              </div>
            </div>

            <!-- Người phụ trách chính -->
            <div v-if="selectedEvent.assigneeUsername || selectedEvent.assigneeFullName" class="detail-info-row">
              <span class="detail-row-icon">🎯</span>
              <div class="detail-row-content">
                <strong>Người phụ trách chính:</strong>
                <div class="assignee-detail-badge">
                  <span class="assignee-avatar-mini">{{ getAvatarInitial(selectedEvent.assigneeFullName || selectedEvent.assigneeUsername) }}</span>
                  <span>{{ selectedEvent.assigneeFullName || ('@' + selectedEvent.assigneeUsername) }}</span>
                </div>
              </div>
            </div>

            <!-- Tiến độ công việc -->
            <div class="detail-info-row">
              <span class="detail-row-icon">📊</span>
              <div class="detail-row-content" style="width: 100%">
                <div class="detail-progress-header">
                  <strong>Tiến độ hoàn thành:</strong>
                  <span class="progress-val-text">{{ selectedEvent.progress || 0 }}%</span>
                </div>
                <div class="detail-progress-track">
                  <div
                    class="detail-progress-fill"
                    :style="{
                      width: (selectedEvent.progress || 0) + '%',
                      backgroundColor: (selectedEvent.progress >= 100 || selectedEvent.status === 'COMPLETED') ? '#10b981' : (selectedEvent.colorHex || '#2563eb')
                    }"
                  ></div>
                </div>
              </div>
            </div>

            <!-- Checklist việc con tương tác trực tiếp (Subtasks in Detail) -->
            <div v-if="selectedEvent.subtasks && selectedEvent.subtasks.length > 0" class="detail-subtasks-box">
              <div class="detail-subtasks-title">
                <strong>Checklist công việc con:</strong>
                <span class="subtask-ratio">({{ countCompletedSubtasks(selectedEvent.subtasks) }}/{{ selectedEvent.subtasks.length }} hoàn thành)</span>
              </div>
              <div class="detail-subtasks-checklist">
                <div
                  v-for="(st, idx) in selectedEvent.subtasks"
                  :key="st.id || idx"
                  class="detail-subtask-item"
                >
                  <label class="subtask-checkbox-label">
                    <input
                      type="checkbox"
                      :checked="st.completed"
                      class="subtask-real-check"
                      @change="handleToggleSubtaskInDetail(idx)"
                    />
                    <span :class="{ 'subtask-done-text': st.completed }">{{ st.title }}</span>
                  </label>
                </div>
              </div>
            </div>

            <!-- Mô tả chi tiết -->
            <div v-if="selectedEvent.description" class="detail-desc-box">
              <strong>Nội dung cuộc họp:</strong>
              <p>{{ selectedEvent.description }}</p>
            </div>
          </div>

          <div class="modal-card-footer detail-modal-footer">
            <div class="detail-footer-left">
              <button
                v-if="canModifyEvent(selectedEvent)"
                type="button"
                class="btn-detail-delete"
                @click="handleDeleteEvent(selectedEvent)"
              >
                Xóa sự kiện
              </button>
            </div>
            <div class="detail-footer-right">
              <!-- Nút Hoàn thành nhanh / Mở lại công việc -->
              <button
                type="button"
                class="btn-detail-toggle-status"
                :class="{ 'is-completed': selectedEvent.status === 'COMPLETED' || selectedEvent.progress >= 100 }"
                @click="handleToggleStatusInDetail"
              >
                {{ (selectedEvent.status === 'COMPLETED' || selectedEvent.progress >= 100) ? '↩️ Mở lại công việc' : '✅ Đánh dấu hoàn thành' }}
              </button>

              <button
                v-if="canModifyEvent(selectedEvent)"
                type="button"
                class="btn-detail-edit"
                @click="editEvent(selectedEvent)"
              >
                Chỉnh sửa
              </button>
              <a
                v-if="selectedEvent.meetingUrl"
                :href="selectedEvent.meetingUrl"
                target="_blank"
                class="btn-detail-join"
              >
                Tham gia họp ngay
              </a>
            </div>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'
import { useAuthStore } from '../store/auth'

const authStore = useAuthStore()

// State điều hướng lịch
const currentFocusDate = ref(new Date())
const currentViewMode = ref('month') // 'year' | 'quarter' | 'month' | 'week' | 'day' | 'agenda'
const searchQuery = ref('')
const selectedTypeFilter = ref('')
const isLoading = ref(false)
const isSaving = ref(false)

// Dữ liệu sự kiện & thống kê
const eventsList = ref([])
const stats = ref({
  todayEventsCount: 0,
  thisWeekEventsCount: 0,
  myMeetingsCount: 0,
  pendingDeadlinesCount: 0
})
const colleagueOptions = ref([])

// State Bảng Công việc (Task Board Sidebar)
const isTaskBoardOpen = ref(true)
const taskFilter = ref('all') // 'all' | 'my' | 'today' | 'pending' | 'completed'
const quickTaskTitle = ref('')
const subtaskDraftText = ref('')

// State Modal Thêm / Sửa
const showEventModal = ref(false)
const isEditing = ref(false)
const editingEventId = ref(null)

const colorPresets = ['#2563eb', '#0284c7', '#10b981', '#059669', '#f59e0b', '#ea580c', '#ef4444', '#dc2626', '#8b5cf6', '#06b6d4', '#ec4899', '#64748b']

const form = ref({
  title: '',
  description: '',
  startTime: '',
  endTime: '',
  isAllDay: false,
  location: '',
  meetingUrl: '',
  eventType: 'MEETING',
  priority: 'MEDIUM',
  status: 'SCHEDULED',
  colorHex: '#2563eb',
  department: 'Toàn công ty',
  attendees: [],
  reminderMinutesBefore: 15,
  progress: 0,
  assigneeUsername: '',
  subtasks: [],
  isPublic: true
})

// State Modal Chi tiết
const showDetailModal = ref(false)
const selectedEvent = ref(null)

// State Kéo thả khoảng ngày & kéo thả sự kiện
const isSelectingRange = ref(false)
const isMouseDown = ref(false)
const rangeStartDateKey = ref(null)
const rangeHoverDateKey = ref(null)

const draggingEventId = ref(null)
const dragOverDateKey = ref(null)

const hoursOfDay = [7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19]

// Helper chuyển Date sang chuỗi khóa 'YYYY-MM-DD'
const toDateKey = (date) => {
  if (!date) return ''
  const d = date instanceof Date ? date : new Date(date)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}
const formatDateKey = toDateKey

// =========================================================================
//                           COMPUTED PROPERTIES
// =========================================================================

// Quý hiện tại (1..4)
const currentQuarter = computed(() => {
  return Math.floor(currentFocusDate.value.getMonth() / 3) + 1
})

const currentPeriodTitle = computed(() => {
  const d = currentFocusDate.value
  const year = d.getFullYear()
  const month = d.getMonth() + 1
  if (currentViewMode.value === 'year') {
    return `Năm ${year}`
  }
  if (currentViewMode.value === 'quarter') {
    const q = currentQuarter.value
    return `Quý ${q}, ${year} (Tháng ${(q-1)*3 + 1} - ${q*3})`
  }
  if (currentViewMode.value === 'month') {
    return `Tháng ${month}, ${year}`
  }
  if (currentViewMode.value === 'week') {
    const startWeek = getStartOfWeek(d)
    const endWeek = new Date(startWeek)
    endWeek.setDate(endWeek.getDate() + 6)
    return `${startWeek.getDate()} Th${startWeek.getMonth() + 1} - ${endWeek.getDate()} Th${endWeek.getMonth() + 1}, ${year}`
  }
  if (currentViewMode.value === 'day') {
    return `${d.getDate()} Tháng ${month}, ${year}`
  }
  return `Lịch trình Tháng ${month}, ${year}`
})

// Tính toán tóm tắt khoảng ngày đang chọn
const rangeDisplaySummary = computed(() => {
  if (!rangeStartDateKey.value || !rangeHoverDateKey.value) return ''
  const startK = rangeStartDateKey.value <= rangeHoverDateKey.value ? rangeStartDateKey.value : rangeHoverDateKey.value
  const endK = rangeStartDateKey.value <= rangeHoverDateKey.value ? rangeHoverDateKey.value : rangeStartDateKey.value

  const startD = new Date(startK)
  const endD = new Date(endK)
  const diffDays = Math.round((endD - startD) / (1000 * 60 * 60 * 24)) + 1

  const formatShort = (d) => `${String(d.getDate()).padStart(2, '0')}/${String(d.getMonth() + 1).padStart(2, '0')}`
  if (startK === endK) {
    return `${formatShort(startD)} (1 ngày)`
  }
  return `${formatShort(startD)} → ${formatShort(endD)} (${diffDays} ngày)`
})

// =========================================================================
//                   COMPUTED CHO BẢNG CÔNG VIỆC & TIẾN ĐỘ
// =========================================================================
const allTasksList = computed(() => {
  return eventsList.value || []
})

const myTasksList = computed(() => {
  const currentUsername = authStore.user?.username || ''
  return allTasksList.value.filter(e => {
    return (e.assigneeUsername && e.assigneeUsername.toLowerCase() === currentUsername.toLowerCase()) ||
           (e.creatorUsername && e.creatorUsername.toLowerCase() === currentUsername.toLowerCase()) ||
           (Array.isArray(e.attendees) && e.attendees.map(a => a.toLowerCase()).includes(currentUsername.toLowerCase()))
  })
})

const todayTasksList = computed(() => {
  const todayStr = formatDateKey(new Date())
  return allTasksList.value.filter(e => {
    const startStr = e.startTime ? e.startTime.substring(0, 10) : ''
    const endStr = e.endTime ? e.endTime.substring(0, 10) : ''
    return startStr === todayStr || endStr === todayStr
  })
})

const pendingTasksList = computed(() => {
  return allTasksList.value.filter(e => e.status !== 'COMPLETED' && (!e.progress || e.progress < 100))
})

const completedTasksList = computed(() => {
  return allTasksList.value.filter(e => e.status === 'COMPLETED' || (e.progress && e.progress >= 100))
})

const completedTasksCount = computed(() => completedTasksList.value.length)

const taskCompletionRate = computed(() => {
  if (allTasksList.value.length === 0) return 0
  return Math.round((completedTasksCount.value / allTasksList.value.length) * 100)
})

const filteredTasksList = computed(() => {
  let list = []
  if (taskFilter.value === 'my') {
    list = myTasksList.value
  } else if (taskFilter.value === 'today') {
    list = todayTasksList.value
  } else if (taskFilter.value === 'pending') {
    list = pendingTasksList.value
  } else if (taskFilter.value === 'completed') {
    list = completedTasksList.value
  } else {
    list = allTasksList.value
  }

  // Sắp xếp: việc chưa xong lên trước, theo deadline gần nhất
  return [...list].sort((a, b) => {
    const aDone = a.status === 'COMPLETED' || (a.progress >= 100)
    const bDone = b.status === 'COMPLETED' || (b.progress >= 100)
    if (aDone !== bDone) return aDone ? 1 : -1
    return new Date(a.endTime || a.startTime) - new Date(b.endTime || b.startTime)
  })
})

const countCompletedSubtasks = (subtasks) => {
  if (!Array.isArray(subtasks)) return 0
  return subtasks.filter(s => s && (s.completed === true || s.completed === 'true')).length
}

const getAvatarInitial = (name) => {
  if (!name) return '?'
  return name.trim().charAt(0).toUpperCase()
}

const isTaskOverdue = (task) => {
  if (!task || task.status === 'COMPLETED' || (task.progress && task.progress >= 100)) return false
  if (!task.endTime) return false
  return new Date(task.endTime) < new Date()
}

const formatTaskDeadline = (dateStr) => {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const pad = (n) => String(n).padStart(2, '0')
  return `${pad(d.getHours())}:${pad(d.getMinutes())}, ${pad(d.getDate())}/${pad(d.getMonth() + 1)}`
}

const isCellInRange = (dateKey) => {
  if (!isSelectingRange.value || !rangeStartDateKey.value || !rangeHoverDateKey.value) return false
  const minK = rangeStartDateKey.value <= rangeHoverDateKey.value ? rangeStartDateKey.value : rangeHoverDateKey.value
  const maxK = rangeStartDateKey.value <= rangeHoverDateKey.value ? rangeHoverDateKey.value : rangeStartDateKey.value
  return dateKey >= minK && dateKey <= maxK
}

const isRangeStart = (dateKey) => {
  if (!isSelectingRange.value || !rangeStartDateKey.value || !rangeHoverDateKey.value) return false
  const minK = rangeStartDateKey.value <= rangeHoverDateKey.value ? rangeStartDateKey.value : rangeHoverDateKey.value
  return dateKey === minK
}

const isRangeEnd = (dateKey) => {
  if (!isSelectingRange.value || !rangeStartDateKey.value || !rangeHoverDateKey.value) return false
  const maxK = rangeStartDateKey.value <= rangeHoverDateKey.value ? rangeHoverDateKey.value : rangeStartDateKey.value
  return dateKey === maxK
}

// =========================================================================
//            TÍNH TOÁN DỮ LIỆU CHO 12 THÁNG (CHẾ ĐỘ XEM NĂM)
// =========================================================================
const yearMonthsList = computed(() => {
  const year = currentFocusDate.value.getFullYear()
  const todayStr = toDateKey(new Date())
  const months = []

  for (let m = 0; m < 12; m++) {
    const firstDay = new Date(year, m, 1)
    const lastDay = new Date(year, m + 1, 0)

    let startDayOfWeek = firstDay.getDay() - 1
    if (startDayOfWeek === -1) startDayOfWeek = 6

    const cells = []
    // Ô trống padding trước ngày mùng 1
    for (let i = 0; i < startDayOfWeek; i++) {
      cells.push({ dayNumber: null, dateKey: null, events: [] })
    }
    // Ngày thực trong tháng
    for (let d = 1; d <= lastDay.getDate(); d++) {
      const curDate = new Date(year, m, d)
      const dateKey = toDateKey(curDate)
      const evts = getEventsForDateKey(dateKey)
      cells.push({
        date: curDate,
        dateKey,
        dayNumber: d,
        isToday: dateKey === todayStr,
        events: evts
      })
    }

    // Đếm sự kiện trong tháng
    const monthEvtCount = eventsList.value.filter(e => {
      if (!e.startTime) return false
      const eD = new Date(e.startTime)
      return eD.getFullYear() === year && eD.getMonth() === m
    }).length

    months.push({
      monthIndex: m,
      monthNumber: m + 1,
      monthName: `Tháng ${m + 1}`,
      year,
      cells,
      totalEvents: monthEvtCount
    })
  }

  return months
})

// =========================================================================
//            TÍNH TOÁN DỮ LIỆU CHO 3 THÁNG (CHẾ ĐỘ XEM QUÝ)
// =========================================================================
const quarterMonthsList = computed(() => {
  const year = currentFocusDate.value.getFullYear()
  const q = currentQuarter.value
  const startMonthIndex = (q - 1) * 3
  const todayStr = toDateKey(new Date())
  const months = []

  for (let m = startMonthIndex; m < startMonthIndex + 3; m++) {
    const firstDay = new Date(year, m, 1)
    const lastDay = new Date(year, m + 1, 0)

    let startDayOfWeek = firstDay.getDay() - 1
    if (startDayOfWeek === -1) startDayOfWeek = 6

    const cells = []
    for (let i = startDayOfWeek - 1; i >= 0; i--) {
      const padDate = new Date(year, m, -i)
      const dateKey = toDateKey(padDate)
      cells.push({
        date: padDate,
        dateKey,
        dayNumber: padDate.getDate(),
        isCurrentMonth: false,
        isToday: dateKey === todayStr,
        events: getEventsForDateKey(dateKey)
      })
    }
    for (let d = 1; d <= lastDay.getDate(); d++) {
      const curDate = new Date(year, m, d)
      const dateKey = toDateKey(curDate)
      cells.push({
        date: curDate,
        dateKey,
        dayNumber: d,
        isCurrentMonth: true,
        isToday: dateKey === todayStr,
        events: getEventsForDateKey(dateKey)
      })
    }
    const totalSlots = cells.length <= 35 ? 35 : 42
    const remaining = totalSlots - cells.length
    for (let d = 1; d <= remaining; d++) {
      const padDate = new Date(year, m + 1, d)
      const dateKey = toDateKey(padDate)
      cells.push({
        date: padDate,
        dateKey,
        dayNumber: d,
        isCurrentMonth: false,
        isToday: dateKey === todayStr,
        events: getEventsForDateKey(dateKey)
      })
    }

    const monthEvents = eventsList.value.filter(e => {
      if (!e.startTime) return false
      const eD = new Date(e.startTime)
      return eD.getFullYear() === year && eD.getMonth() === m
    })

    months.push({
      monthIndex: m,
      monthNumber: m + 1,
      monthName: `Tháng ${m + 1}, ${year}`,
      cells,
      events: monthEvents
    })
  }

  return months
})

// Danh sách sự kiện đầy đủ trong Quý
const quarterEventsList = computed(() => {
  const year = currentFocusDate.value.getFullYear()
  const q = currentQuarter.value
  const startM = (q - 1) * 3
  const endM = startM + 2

  return eventsList.value.filter(e => {
    if (!e.startTime) return false
    const d = new Date(e.startTime)
    return d.getFullYear() === year && d.getMonth() >= startM && d.getMonth() <= endM
  }).sort((a, b) => new Date(a.startTime) - new Date(b.startTime))
})

// Tính toán các ô lịch cho chế độ xem Tháng (Month Grid)
const monthCalendarCells = computed(() => {
  const d = currentFocusDate.value
  const year = d.getFullYear()
  const month = d.getMonth()

  const firstDayOfMonth = new Date(year, month, 1)
  const lastDayOfMonth = new Date(year, month + 1, 0)

  let startDayOfWeek = firstDayOfMonth.getDay() - 1
  if (startDayOfWeek === -1) startDayOfWeek = 6

  const todayStr = toDateKey(new Date())
  const cells = []

  for (let i = startDayOfWeek - 1; i >= 0; i--) {
    const padDate = new Date(year, month, -i)
    const dateKey = toDateKey(padDate)
    cells.push({
      date: padDate,
      dateKey,
      dayNumber: padDate.getDate(),
      isCurrentMonth: false,
      isToday: dateKey === todayStr,
      events: getEventsForDateKey(dateKey)
    })
  }

  for (let day = 1; day <= lastDayOfMonth.getDate(); day++) {
    const curDate = new Date(year, month, day)
    const dateKey = toDateKey(curDate)
    cells.push({
      date: curDate,
      dateKey,
      dayNumber: day,
      isCurrentMonth: true,
      isToday: dateKey === todayStr,
      events: getEventsForDateKey(dateKey)
    })
  }

  const totalSlots = cells.length <= 35 ? 35 : 42
  const remaining = totalSlots - cells.length
  for (let day = 1; day <= remaining; day++) {
    const padDate = new Date(year, month + 1, day)
    const dateKey = toDateKey(padDate)
    cells.push({
      date: padDate,
      dateKey,
      dayNumber: day,
      isCurrentMonth: false,
      isToday: dateKey === todayStr,
      events: getEventsForDateKey(dateKey)
    })
  }

  return cells
})

const weekDaysList = computed(() => {
  const start = getStartOfWeek(currentFocusDate.value)
  const todayStr = toDateKey(new Date())
  const dayNames = ['Thứ 2', 'Thứ 3', 'Thứ 4', 'Thứ 5', 'Thứ 6', 'Thứ 7', 'Chủ Nhật']

  const list = []
  for (let i = 0; i < 7; i++) {
    const cur = new Date(start)
    cur.setDate(cur.getDate() + i)
    const dateKey = toDateKey(cur)
    list.push({
      date: cur,
      dateKey,
      dayName: dayNames[i],
      dayNumber: cur.getDate(),
      isToday: dateKey === todayStr
    })
  }
  return list
})

const dayViewEvents = computed(() => {
  const key = toDateKey(currentFocusDate.value)
  return getEventsForDateKey(key)
})

const agendaGroupedEvents = computed(() => {
  const groups = {}
  eventsList.value.forEach(evt => {
    if (!evt.startTime) return
    const d = new Date(evt.startTime)
    const key = toDateKey(d)
    if (!groups[key]) {
      const weekdays = ['Chủ Nhật', 'Thứ Hai', 'Thứ Ba', 'Thứ Tư', 'Thứ Năm', 'Thứ Sáu', 'Thứ Bảy']
      groups[key] = {
        dateKey: key,
        date: d,
        dayNumber: d.getDate(),
        monthNumber: d.getMonth() + 1,
        weekdayName: weekdays[d.getDay()],
        dateFormatted: `${d.getDate()}/${d.getMonth() + 1}/${d.getFullYear()}`,
        events: []
      }
    }
    groups[key].events.push(evt)
  })

  return Object.values(groups).sort((a, b) => a.dateKey.localeCompare(b.dateKey))
})

// =========================================================================
//                             API CALLS
// =========================================================================

const fetchEvents = async () => {
  isLoading.value = true
  try {
    const focus = currentFocusDate.value
    const year = focus.getFullYear()
    const month = focus.getMonth()

    let start, end

    if (currentViewMode.value === 'year') {
      start = new Date(year, 0, 1, 0, 0, 0).toISOString()
      end = new Date(year, 11, 31, 23, 59, 59).toISOString()
    } else if (currentViewMode.value === 'quarter') {
      const q = Math.floor(month / 3)
      start = new Date(year, q * 3, 1, 0, 0, 0).toISOString()
      const lastDayOfQ = new Date(year, q * 3 + 3, 0)
      end = new Date(year, q * 3 + 2, lastDayOfQ.getDate(), 23, 59, 59).toISOString()
    } else {
      start = new Date(year, month - 1, 20).toISOString()
      end = new Date(year, month + 2, 10).toISOString()
    }

    const params = {
      start,
      end,
      search: searchQuery.value || undefined,
      type: selectedTypeFilter.value || undefined
    }

    const res = await api.get('/calendar/events', { params })
    eventsList.value = res.data || []
  } catch (err) {
    console.error('Lỗi tải sự kiện calendar:', err)
    ElMessage.error('Không thể tải dữ liệu lịch biểu!')
  } finally {
    isLoading.value = false
  }
}

const fetchStats = async () => {
  try {
    const res = await api.get('/calendar/stats')
    if (res.data) {
      stats.value = res.data
    }
  } catch (err) {
    console.warn('Lỗi tải thống kê calendar:', err)
  }
}

const fetchColleagues = async () => {
  try {
    const res = await api.get('/chat/colleagues')
    colleagueOptions.value = res.data || []
  } catch (err) {
    try {
      const userRes = await api.get('/users')
      colleagueOptions.value = userRes.data || []
    } catch (e) {}
  }
}

let debounceTimer = null
const debouncedFetch = () => {
  clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => {
    fetchEvents()
  }, 350)
}

// =========================================================================
//               XỬ LÝ KÉO THẢ KHOẢNG NGÀY & KÉO THẢ SỰ KIỆN
// =========================================================================

const handleCellMouseDown = (cell, e) => {
  if (e.target.closest('.btn-quick-add-event') || e.target.closest('.event-chip-item') || e.target.closest('.event-more-pill')) {
    return
  }
  isMouseDown.value = true
  isSelectingRange.value = true
  rangeStartDateKey.value = cell.dateKey
  rangeHoverDateKey.value = cell.dateKey
}

const handleCellMouseEnter = (cell) => {
  if (isMouseDown.value && isSelectingRange.value) {
    rangeHoverDateKey.value = cell.dateKey
  }
}

const handleMouseLeaveGrid = () => {}

const handleGlobalMouseUp = () => {
  if (isMouseDown.value && isSelectingRange.value) {
    isMouseDown.value = false
    const startK = rangeStartDateKey.value <= rangeHoverDateKey.value ? rangeStartDateKey.value : rangeHoverDateKey.value
    const endK = rangeStartDateKey.value <= rangeHoverDateKey.value ? rangeHoverDateKey.value : rangeStartDateKey.value

    openCreateModalWithRange(startK, endK)

    setTimeout(() => {
      isSelectingRange.value = false
      rangeStartDateKey.value = null
      rangeHoverDateKey.value = null
    }, 100)
  } else {
    isMouseDown.value = false
    isSelectingRange.value = false
    rangeStartDateKey.value = null
    rangeHoverDateKey.value = null
  }
}

const handleDragStart = (evt, e) => {
  draggingEventId.value = evt.id
  e.dataTransfer.setData('text/plain', JSON.stringify({
    eventId: evt.id,
    originalStartTime: evt.startTime,
    originalEndTime: evt.endTime,
    title: evt.title
  }))
  e.dataTransfer.effectAllowed = 'move'
}

const handleDragEnd = () => {
  draggingEventId.value = null
  dragOverDateKey.value = null
}

const handleDragOverCell = (cell, e) => {
  if (draggingEventId.value) {
    e.dataTransfer.dropEffect = 'move'
    dragOverDateKey.value = cell.dateKey
  }
}

const handleDragLeaveCell = (cell, e) => {
  if (dragOverDateKey.value === cell.dateKey) {
    dragOverDateKey.value = null
  }
}

const handleDropOnCell = async (targetCell, e) => {
  dragOverDateKey.value = null
  const data = e.dataTransfer.getData('text/plain')
  if (!data) return

  try {
    const parsed = JSON.parse(data)
    const eventId = parsed.eventId
    const foundEvent = eventsList.value.find(ev => ev.id === eventId)
    if (!foundEvent) return

    if (!canModifyEvent(foundEvent)) {
      ElMessage.warning('Bạn không có quyền thay đổi sự kiện này!')
      return
    }

    const oldStart = new Date(foundEvent.startTime)
    const oldEnd = new Date(foundEvent.endTime)
    const targetDate = new Date(targetCell.date)

    const newStart = new Date(targetDate)
    newStart.setHours(oldStart.getHours(), oldStart.getMinutes(), oldStart.getSeconds())

    const durationMs = oldEnd.getTime() - oldStart.getTime()
    const newEnd = new Date(newStart.getTime() + durationMs)

    const payload = {
      ...foundEvent,
      startTime: formatToIsoString(newStart),
      endTime: formatToIsoString(newEnd)
    }

    await api.put(`/calendar/events/${eventId}`, payload)
    ElMessage.success(`Đã dời sự kiện "${foundEvent.title}" sang ngày ${targetCell.dayNumber}/${targetCell.date.getMonth() + 1}!`)
    await fetchEvents()
    fetchStats()
  } catch (err) {
    console.error('Lỗi dời sự kiện:', err)
    ElMessage.error('Không thể dời sự kiện!')
  } finally {
    draggingEventId.value = null
  }
}

// =========================================================================
//                        ACTIONS & HANDLERS
// =========================================================================

const navigatePeriod = (step) => {
  const d = new Date(currentFocusDate.value)
  if (currentViewMode.value === 'year') {
    d.setFullYear(d.getFullYear() + step)
  } else if (currentViewMode.value === 'quarter') {
    d.setMonth(d.getMonth() + step * 3)
  } else if (currentViewMode.value === 'month' || currentViewMode.value === 'agenda') {
    d.setMonth(d.getMonth() + step)
  } else if (currentViewMode.value === 'week') {
    d.setDate(d.getDate() + step * 7)
  } else if (currentViewMode.value === 'day') {
    d.setDate(d.getDate() + step)
  }
  currentFocusDate.value = d
  fetchEvents()
}

const goToToday = () => {
  currentFocusDate.value = new Date()
  fetchEvents()
}

const switchViewMode = (mode) => {
  currentViewMode.value = mode
  fetchEvents()
}

const selectQuarter = (qNumber) => {
  const year = currentFocusDate.value.getFullYear()
  currentFocusDate.value = new Date(year, (qNumber - 1) * 3, 1)
  fetchEvents()
}

const jumpToMonthView = (monthIndex, year) => {
  currentFocusDate.value = new Date(year, monthIndex, 1)
  currentViewMode.value = 'month'
  fetchEvents()
}

const jumpToDayView = (date) => {
  currentFocusDate.value = new Date(date)
  currentViewMode.value = 'day'
  fetchEvents()
}

const handleDayCellClick = (cell) => {
  currentFocusDate.value = cell.date
}

const openDayAgenda = (cell) => {
  currentFocusDate.value = cell.date
  currentViewMode.value = 'day'
}

const openCreateModal = (targetDate = null, targetHour = null) => {
  isEditing.value = false
  editingEventId.value = null
  subtaskDraftText.value = ''

  const d = targetDate ? new Date(targetDate) : new Date()
  const hour = targetHour !== null ? targetHour : 9
  d.setHours(hour, 0, 0, 0)

  const endD = new Date(d)
  endD.setHours(hour + 1, 0, 0, 0)

  form.value = {
    title: '',
    description: '',
    startTime: formatToIsoString(d),
    endTime: formatToIsoString(endD),
    isAllDay: false,
    location: '',
    meetingUrl: '',
    eventType: 'MEETING',
    priority: 'MEDIUM',
    status: 'SCHEDULED',
    colorHex: '#2563eb',
    department: 'Toàn công ty',
    attendees: [],
    reminderMinutesBefore: 15,
    progress: 0,
    assigneeUsername: '',
    subtasks: [],
    isPublic: true
  }

  showEventModal.value = true
}

const openCreateModalWithRange = (startDateKey, endDateKey) => {
  isEditing.value = false
  editingEventId.value = null
  subtaskDraftText.value = ''

  const startD = new Date(startDateKey)
  startD.setHours(9, 0, 0, 0)

  const endD = new Date(endDateKey)
  if (startDateKey === endDateKey) {
    endD.setHours(10, 0, 0, 0)
  } else {
    endD.setHours(18, 0, 0, 0)
  }

  const isMultiDay = startDateKey !== endDateKey

  form.value = {
    title: '',
    description: '',
    startTime: formatToIsoString(startD),
    endTime: formatToIsoString(endD),
    isAllDay: isMultiDay,
    location: '',
    meetingUrl: '',
    eventType: isMultiDay ? 'EVENT' : 'MEETING',
    priority: 'MEDIUM',
    status: 'SCHEDULED',
    colorHex: isMultiDay ? '#8b5cf6' : '#2563eb',
    department: 'Toàn công ty',
    attendees: [],
    reminderMinutesBefore: 15,
    progress: 0,
    assigneeUsername: '',
    subtasks: [],
    isPublic: true
  }

  showEventModal.value = true
}

const editEvent = (evt) => {
  showDetailModal.value = false
  isEditing.value = true
  editingEventId.value = evt.id
  subtaskDraftText.value = ''

  form.value = {
    title: evt.title || '',
    description: evt.description || '',
    startTime: evt.startTime ? evt.startTime.substring(0, 19) : '',
    endTime: evt.endTime ? evt.endTime.substring(0, 19) : '',
    isAllDay: Boolean(evt.isAllDay),
    location: evt.location || '',
    meetingUrl: evt.meetingUrl || '',
    eventType: evt.eventType || 'MEETING',
    priority: evt.priority || 'MEDIUM',
    status: evt.status || 'SCHEDULED',
    colorHex: evt.colorHex || '#2563eb',
    department: evt.department || 'Toàn công ty',
    attendees: Array.isArray(evt.attendees) ? [...evt.attendees] : [],
    reminderMinutesBefore: evt.reminderMinutesBefore || 15,
    progress: evt.progress !== undefined ? evt.progress : 0,
    assigneeUsername: evt.assigneeUsername || '',
    subtasks: Array.isArray(evt.subtasks) ? JSON.parse(JSON.stringify(evt.subtasks)) : [],
    isPublic: evt.isPublic !== undefined ? evt.isPublic : true
  }

  showEventModal.value = true
}

const handleSaveEvent = async () => {
  if (!form.value.title.trim()) {
    ElMessage.warning('Vui lòng nhập tiêu đề sự kiện!')
    return
  }
  if (!form.value.startTime || !form.value.endTime) {
    ElMessage.warning('Vui lòng chọn thời gian bắt đầu và kết thúc!')
    return
  }
  if (new Date(form.value.endTime) < new Date(form.value.startTime)) {
    ElMessage.error('Thời gian kết thúc phải diễn ra sau thời gian bắt đầu!')
    return
  }

  isSaving.value = true
  try {
    const payload = { ...form.value }
    if (isEditing.value && editingEventId.value) {
      await api.put(`/calendar/events/${editingEventId.value}`, payload)
      ElMessage.success('Đã cập nhật sự kiện thành công!')
    } else {
      await api.post('/calendar/events', payload)
      ElMessage.success('Đã thêm sự kiện mới vào lịch!')
    }
    showEventModal.value = false
    await fetchEvents()
    fetchStats()
  } catch (err) {
    console.error('Lỗi lưu sự kiện:', err)
    ElMessage.error(err.response?.data?.message || 'Không thể lưu sự kiện!')
  } finally {
    isSaving.value = false
  }
}

const openDetailModal = (evt) => {
  selectedEvent.value = evt
  showDetailModal.value = true
}

const handleDeleteEvent = async (evt) => {
  try {
    await ElMessageBox.confirm(
      `Bạn có chắc chắn muốn xóa sự kiện "${evt.title}" không?`,
      'Xác nhận xóa sự kiện',
      {
        confirmButtonText: 'Xóa',
        cancelButtonText: 'Hủy',
        type: 'warning'
      }
    )
    await api.delete(`/calendar/events/${evt.id}`)
    ElMessage.success('Đã xóa sự kiện thành công!')
    showDetailModal.value = false
    await fetchEvents()
    fetchStats()
  } catch (e) {}
}

const handleTypeChange = (type) => {
  const colorMap = {
    MEETING: '#2563eb',
    CLIENT_MEETING: '#0284c7',
    DEADLINE: '#ef4444',
    TASK: '#f59e0b',
    TRAINING: '#8b5cf6',
    INTERVIEW: '#06b6d4',
    BUSINESS_TRIP: '#0d9488',
    REVIEW: '#ea580c',
    TEAM_BUILDING: '#ec4899',
    MAINTENANCE: '#64748b',
    HOLIDAY: '#10b981',
    EVENT: '#6366f1'
  }
  if (colorMap[type]) {
    form.value.colorHex = colorMap[type]
  }
}

// =========================================================================
//            ACTION METHODS CHO BẢNG CÔNG VIỆC & SUBTASKS
// =========================================================================

// Đổi trạng thái hoàn thành nhanh (PATCH /toggle-status)
const toggleTaskStatus = async (task) => {
  try {
    const res = await api.patch(`/calendar/events/${task.id}/toggle-status`)
    const updated = res.data
    const idx = eventsList.value.findIndex(e => e.id === task.id)
    if (idx !== -1) {
      eventsList.value[idx] = updated
    }
    if (selectedEvent.value && selectedEvent.value.id === task.id) {
      selectedEvent.value = updated
    }
    if (updated.status === 'COMPLETED' || (updated.progress && updated.progress >= 100)) {
      ElMessage.success(`🎉 Đã hoàn thành công việc "${updated.title}"!`)
    } else {
      ElMessage.info(`Đã mở lại công việc "${updated.title}"`)
    }
    fetchStats()
  } catch (err) {
    console.error('Lỗi đổi trạng thái công việc:', err)
    ElMessage.error('Không thể cập nhật trạng thái công việc!')
  }
}

// Tạo công việc nhanh (Quick Task Input)
const handleCreateQuickTask = async () => {
  const title = quickTaskTitle.value.trim()
  if (!title) return

  const now = new Date()
  const end = new Date(now)
  end.setHours(17, 30, 0, 0)
  if (end <= now) {
    end.setDate(end.getDate() + 1)
  }

  try {
    const payload = {
      title,
      description: 'Công việc tạo nhanh từ Bảng nhiệm vụ',
      startTime: formatToIsoString(now),
      endTime: formatToIsoString(end),
      isAllDay: false,
      eventType: 'TASK',
      priority: 'MEDIUM',
      status: 'SCHEDULED',
      colorHex: '#f59e0b',
      progress: 0,
      department: 'Toàn công ty',
      attendees: [],
      subtasks: []
    }
    await api.post('/calendar/events', payload)
    ElMessage.success(`Đã thêm nhanh công việc: "${title}"`)
    quickTaskTitle.value = ''
    await fetchEvents()
    fetchStats()
  } catch (err) {
    console.error('Lỗi tạo nhanh công việc:', err)
    ElMessage.error('Không thể tạo nhanh công việc!')
  }
}

// Quản lý Subtasks trong Modal Thêm / Sửa
const addSubtaskToForm = () => {
  const text = subtaskDraftText.value.trim()
  if (!text) return
  if (!Array.isArray(form.value.subtasks)) {
    form.value.subtasks = []
  }
  form.value.subtasks.push({
    id: Date.now(),
    title: text,
    completed: false
  })
  subtaskDraftText.value = ''
  onSubtaskFormCheckChange()
}

const removeSubtaskFromForm = (idx) => {
  if (Array.isArray(form.value.subtasks)) {
    form.value.subtasks.splice(idx, 1)
    onSubtaskFormCheckChange()
  }
}

const onSubtaskFormCheckChange = () => {
  if (!form.value.subtasks || form.value.subtasks.length === 0) return
  const done = countCompletedSubtasks(form.value.subtasks)
  form.value.progress = Math.round((done / form.value.subtasks.length) * 100)
}

// Toggle Subtasks trong Detail Modal
const handleToggleSubtaskInDetail = async (idx) => {
  if (!selectedEvent.value || !Array.isArray(selectedEvent.value.subtasks)) return
  const subtasks = JSON.parse(JSON.stringify(selectedEvent.value.subtasks))
  subtasks[idx].completed = !subtasks[idx].completed

  try {
    const res = await api.patch(`/calendar/events/${selectedEvent.value.id}/subtasks`, { subtasks })
    const updated = res.data
    selectedEvent.value = updated
    const listIdx = eventsList.value.findIndex(e => e.id === updated.id)
    if (listIdx !== -1) {
      eventsList.value[listIdx] = updated
    }
    fetchStats()
  } catch (err) {
    console.error('Lỗi cập nhật subtasks:', err)
    ElMessage.error('Không thể cập nhật danh sách việc con!')
  }
}

const handleToggleStatusInDetail = async () => {
  if (!selectedEvent.value) return
  await toggleTaskStatus(selectedEvent.value)
}

const canModifyEvent = (evt) => {
  if (!evt) return false
  if (authStore.user?.role === 'ADMIN') return true
  return evt.creatorUsername === authStore.user?.username
}

// =========================================================================
//                         UTILITY FUNCTIONS
// =========================================================================

const getStartOfWeek = (date) => {
  const d = new Date(date)
  const day = d.getDay()
  const diff = d.getDate() - day + (day === 0 ? -6 : 1)
  d.setDate(diff)
  d.setHours(0, 0, 0, 0)
  return d
}

const getEventsForDateKey = (dateKey) => {
  return eventsList.value.filter(e => {
    if (!e.startTime) return false
    const dStr = e.startTime.substring(0, 10)
    return dStr === dateKey
  })
}

const getEventsForSlot = (dateKey, hour) => {
  return eventsList.value.filter(e => {
    if (!e.startTime) return false
    const dStr = e.startTime.substring(0, 10)
    if (dStr !== dateKey) return false
    const h = new Date(e.startTime).getHours()
    return h === hour
  })
}

const getEventsForDayAndHour = (date, hour) => {
  const key = toDateKey(date)
  return getEventsForSlot(key, hour)
}

const formatChipTime = (evt) => {
  if (evt.isAllDay) return 'Cả ngày'
  if (!evt.startTime) return ''
  const d = new Date(evt.startTime)
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

const formatTime = (timeStr) => {
  if (!timeStr) return ''
  const d = new Date(timeStr)
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

const formatFullDate = (dateObjOrStr) => {
  if (!dateObjOrStr) return ''
  const d = new Date(dateObjOrStr)
  const weekdays = ['Chủ Nhật', 'Thứ Hai', 'Thứ Ba', 'Thứ Tư', 'Thứ Năm', 'Thứ Sáu', 'Thứ Bảy']
  return `${weekdays[d.getDay()]}, ngày ${d.getDate()}/${d.getMonth() + 1}/${d.getFullYear()}`
}

const formatToIsoString = (d) => {
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:00`
}

const getDurationText = (startStr, endStr) => {
  if (!startStr || !endStr) return ''
  const diffMs = new Date(endStr) - new Date(startStr)
  const diffMins = Math.round(diffMs / (1000 * 60))
  if (diffMins < 60) return `${diffMins} phút`
  const hours = Math.floor(diffMins / 60)
  const mins = diffMins % 60
  return mins > 0 ? `${hours} giờ ${mins} phút` : `${hours} giờ`
}

const getTypeName = (type) => {
  const names = {
    MEETING: 'Họp nội bộ',
    CLIENT_MEETING: 'Gặp khách hàng / Đối tác',
    DEADLINE: 'Deadline dự án',
    TASK: 'Nhiệm vụ',
    TRAINING: 'Đào tạo / Workshop',
    INTERVIEW: 'Phỏng vấn ứng viên',
    BUSINESS_TRIP: 'Đi công tác',
    REVIEW: 'Đánh giá KPI',
    TEAM_BUILDING: 'Team Building',
    MAINTENANCE: 'Bảo trì hệ thống',
    HOLIDAY: 'Nghỉ lễ / Nghỉ phép',
    EVENT: 'Sự kiện khác'
  }
  return names[type] || 'Sự kiện'
}

const getPriorityName = (priority) => {
  const names = {
    CRITICAL: 'Tối khẩn',
    URGENT: 'Khẩn cấp',
    HIGH: 'Quan trọng',
    MEDIUM: 'Bình thường',
    LOW: 'Thấp',
    OPTIONAL: 'Tùy chọn'
  }
  return names[priority] || 'Bình thường'
}

const getPriorityClass = (priority) => {
  const map = {
    CRITICAL: 'priority-critical',
    URGENT: 'priority-urgent',
    HIGH: 'priority-high',
    MEDIUM: 'priority-medium',
    LOW: 'priority-low',
    OPTIONAL: 'priority-optional'
  }
  return map[priority] || 'priority-medium'
}

onMounted(() => {
  window.addEventListener('mouseup', handleGlobalMouseUp)
  fetchEvents()
  fetchStats()
  fetchColleagues()
})

onBeforeUnmount(() => {
  window.removeEventListener('mouseup', handleGlobalMouseUp)
})
</script>

<style scoped>
/* =========================================================================
   CONTAINER & HEADER (COMPACT & MODERN)
   ========================================================================= */
.calendar-page-container {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 10px 16px 20px;
  background: #f8fafc;
  min-height: calc(100vh - 64px);
  user-select: none;
}

.calendar-header-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #ffffff;
  padding: 10px 16px;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.03);
}

.header-left-col {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-icon-box {
  width: 36px;
  height: 36px;
  border-radius: 9px;
  background: linear-gradient(135deg, #eff6ff, #dbeafe);
  color: #2563eb;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.title-with-pill {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-title {
  font-size: 16.5px;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
}

.pro-tag-badge {
  font-size: 10.5px;
  font-weight: 700;
  background: #eff6ff;
  color: #2563eb;
  border: 1px solid #bfdbfe;
  padding: 1px 6px;
  border-radius: 10px;
}

.header-subtitle {
  font-size: 12px;
  color: #64748b;
  margin: 1px 0 0 0;
}

.header-right-col {
  display: flex;
  align-items: center;
  gap: 10px;
}

.btn-toggle-task-board {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: #f1f5f9;
  color: #334155;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  padding: 6.5px 12px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-toggle-task-board:hover {
  background: #e2e8f0;
  color: #0f172a;
}

.btn-toggle-task-board.is-active {
  background: #eff6ff;
  color: #2563eb;
  border-color: #93c5fd;
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.12);
}

.task-badge-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #ef4444;
  color: #ffffff;
  font-size: 10.5px;
  font-weight: 700;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 9px;
  line-height: 1;
}

.btn-create-event {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  color: #ffffff;
  border: none;
  border-radius: 8px;
  padding: 7px 14px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 2px 5px rgba(37, 99, 235, 0.22);
  transition: all 0.2s ease;
}

.btn-create-event:hover {
  background: linear-gradient(135deg, #1d4ed8, #1e40af);
  transform: translateY(-1px);
  box-shadow: 0 4px 10px rgba(37, 99, 235, 0.3);
}

/* =========================================================================
   METRICS BAR (THỐNG KÊ NHANH COMPACT)
   ========================================================================= */
.calendar-metrics-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}

.metric-card {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #ffffff;
  padding: 8px 14px;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.02);
}

.metric-icon-circle {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.metric-icon-circle.blue-theme { background: #eff6ff; color: #2563eb; }
.metric-icon-circle.purple-theme { background: #f5f3ff; color: #7c3aed; }
.metric-icon-circle.green-theme { background: #ecfdf5; color: #059669; }
.metric-icon-circle.red-theme { background: #fef2f2; color: #dc2626; }

.metric-info {
  display: flex;
  flex-direction: column;
}

.metric-val {
  font-size: 17px;
  font-weight: 700;
  color: #1e293b;
  line-height: 1.15;
}

.metric-label {
  font-size: 11.5px;
  color: #64748b;
  font-weight: 500;
}

/* =========================================================================
   TOOLBAR & CONTROLS COMPACT
   ========================================================================= */
.calendar-toolbar-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #ffffff;
  padding: 8px 14px;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
  flex-wrap: wrap;
  gap: 10px;
}

.toolbar-nav-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.date-navigator-group {
  display: flex;
  align-items: center;
  gap: 2px;
  background: #f1f5f9;
  padding: 2px;
  border-radius: 7px;
}

.btn-nav-arrow {
  background: transparent;
  border: none;
  width: 26px;
  height: 26px;
  border-radius: 5px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #475569;
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-nav-arrow:hover {
  background: #ffffff;
  color: #2563eb;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.btn-today-pill {
  background: #ffffff;
  border: none;
  padding: 3px 10px;
  border-radius: 5px;
  font-size: 12px;
  font-weight: 600;
  color: #334155;
  cursor: pointer;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  transition: all 0.15s ease;
}

.btn-today-pill:hover {
  color: #2563eb;
}

/* Bộ chọn Tháng / Năm trực tiếp */
.period-quick-selector-group {
  display: flex;
  align-items: center;
}

.toolbar-month-year-picker {
  width: 155px !important;
}

:deep(.toolbar-month-year-picker .el-input__wrapper) {
  background: #ffffff !important;
  border: 1px solid #cbd5e1 !important;
  border-radius: 8px !important;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05) !important;
  height: 30px !important;
  padding: 0 8px !important;
}

:deep(.toolbar-month-year-picker .el-input__inner) {
  font-weight: 700 !important;
  color: #1e293b !important;
  font-size: 13px !important;
  cursor: pointer !important;
}

:deep(.toolbar-month-year-picker .el-input__prefix) {
  color: #2563eb !important;
}

/* Báo hiệu chọn khoảng ngày khi đang kéo chuột */
.drag-range-indicator-pill {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #eff6ff;
  border: 1px dashed #3b82f6;
  color: #1d4ed8;
  padding: 3px 10px;
  border-radius: 20px;
  font-size: 11.5px;
  animation: pulse-border 1.5s infinite;
}

.drag-pulse-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #2563eb;
  animation: ping 1s cubic-bezier(0, 0, 0.2, 1) infinite;
}

@keyframes pulse-border {
  0%, 100% { border-color: #3b82f6; }
  50% { border-color: #93c5fd; }
}

.toolbar-actions-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.search-event-input-wrapper {
  position: relative;
  width: 175px;
}

.search-event-input-wrapper .search-icon {
  position: absolute;
  left: 9px;
  top: 50%;
  transform: translateY(-50%);
  color: #94a3b8;
}

.search-event-input {
  width: 100%;
  height: 30px;
  padding: 4px 24px 4px 28px;
  border-radius: 7px;
  border: 1px solid #cbd5e1;
  font-size: 12.5px;
  outline: none;
  transition: all 0.2s ease;
}

.search-event-input:focus {
  border-color: #2563eb;
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.1);
}

.search-clear-btn {
  position: absolute;
  right: 7px;
  top: 50%;
  transform: translateY(-50%);
  background: transparent;
  border: none;
  color: #94a3b8;
  cursor: pointer;
  font-size: 11px;
}

.event-type-select {
  width: 165px;
}

.view-mode-tabs {
  display: flex;
  background: #f1f5f9;
  padding: 2px;
  border-radius: 7px;
}

.btn-view-tab {
  background: transparent;
  border: none;
  padding: 4px 9px;
  border-radius: 5px;
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-view-tab:hover {
  color: #1e293b;
}

.btn-view-tab.is-active {
  background: #ffffff;
  color: #2563eb;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

/* =========================================================================
   CALENDAR MAIN CONTENT CONTAINER
   ========================================================================= */
.calendar-content-card {
  background: #ffffff;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.03);
  overflow: hidden;
}

/* =========================================================================
   1. CHẾ ĐỘ XEM NĂM (YEAR VIEW STYLING)
   ========================================================================= */
.year-view-container {
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.year-header-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 12px;
  border-bottom: 1px solid #e2e8f0;
}

.year-main-title {
  font-size: 17px;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
}

.year-subtitle {
  font-size: 12px;
  color: #64748b;
  margin-top: 2px;
}

.stat-pill {
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  color: #1d4ed8;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
}

.year-grid-12months {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
}

.mini-month-card {
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background: #ffffff;
  padding: 8px 10px;
  transition: box-shadow 0.15s ease, border-color 0.15s ease;
}

.mini-month-card:hover {
  border-color: #bfdbfe;
  box-shadow: 0 3px 8px rgba(37, 99, 235, 0.08);
}

.mini-month-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 6px;
  margin-bottom: 6px;
  border-bottom: 1px solid #f1f5f9;
  cursor: pointer;
}

.mini-month-header:hover .mini-month-name {
  color: #2563eb;
  text-decoration: underline;
}

.mini-month-name {
  font-size: 13px;
  font-weight: 700;
  color: #1e293b;
}

.mini-events-badge {
  font-size: 10px;
  font-weight: 700;
  background: #eff6ff;
  color: #2563eb;
  padding: 1px 6px;
  border-radius: 8px;
}

.mini-days-row {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  text-align: center;
  font-size: 10px;
  font-weight: 600;
  color: #94a3b8;
  margin-bottom: 4px;
}

.mini-grid-cells {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 2px;
}

.mini-day-cell {
  height: 22px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  cursor: pointer;
  position: relative;
  font-size: 10.5px;
  color: #334155;
  transition: background 0.1s;
}

.mini-day-cell:not(.is-empty):hover {
  background: #eff6ff;
  color: #2563eb;
  font-weight: 700;
}

.mini-day-cell.is-empty {
  cursor: default;
}

.mini-day-cell.is-today {
  background: #2563eb;
  color: #ffffff !important;
  font-weight: 700;
  border-radius: 50%;
}

.mini-event-dot {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  position: absolute;
  bottom: 1px;
}

/* =========================================================================
   2. CHẾ ĐỘ XEM QUÝ (QUARTER VIEW STYLING)
   ========================================================================= */
.quarter-view-container {
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.quarter-header-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 12px;
  border-bottom: 1px solid #e2e8f0;
  flex-wrap: wrap;
  gap: 12px;
}

.quarter-main-title {
  font-size: 17px;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
}

.quarter-subtitle {
  font-size: 12px;
  color: #64748b;
  margin-top: 2px;
}

.quarter-nav-tabs {
  display: flex;
  gap: 6px;
  background: #f1f5f9;
  padding: 3px;
  border-radius: 8px;
}

.quarter-tab-pill {
  background: transparent;
  border: none;
  padding: 5px 12px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  cursor: pointer;
  transition: all 0.15s ease;
}

.quarter-tab-pill:hover {
  color: #1e293b;
}

.quarter-tab-pill.is-active {
  background: #ffffff;
  color: #2563eb;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.quarter-3months-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
}

.quarter-month-col {
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background: #ffffff;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.quarter-month-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
  cursor: pointer;
}

.quarter-month-header:hover h4 {
  color: #2563eb;
  text-decoration: underline;
}

.quarter-month-header h4 {
  font-size: 13.5px;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
}

.quarter-month-header .badge-count {
  font-size: 11px;
  font-weight: 600;
  color: #2563eb;
  background: #eff6ff;
  padding: 1px 6px;
  border-radius: 8px;
}

.mini-weekday-row {
  grid-template-columns: repeat(7, 1fr);
}

.mini-weekday-row .weekday-header-cell {
  padding: 4px;
  font-size: 10.5px;
}

.quarter-month-cells {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  grid-auto-rows: minmax(52px, 1fr);
}

.quarter-day-cell {
  border-right: 1px solid #f1f5f9;
  border-bottom: 1px solid #f1f5f9;
  padding: 2px 4px;
  display: flex;
  flex-direction: column;
  min-height: 52px;
  cursor: pointer;
  transition: background 0.1s ease;
}

.quarter-day-cell:nth-child(7n) {
  border-right: none;
}

.quarter-day-cell:hover {
  background: #f8fafc;
}

.quarter-day-cell.is-other-month {
  background: #fafbfc;
  opacity: 0.5;
}

.quarter-day-cell.is-today {
  background: #eff6ff80;
}

/* Agenda quý */
.quarter-events-agenda-card {
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background: #ffffff;
  padding: 14px 16px;
}

.agenda-section-title {
  font-size: 14px;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 12px 0;
}

.empty-quarter-events {
  color: #64748b;
  font-size: 13px;
  padding: 16px 0;
  text-align: center;
}

.quarter-events-table {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-height: 320px;
  overflow-y: auto;
}

.quarter-event-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  border-radius: 8px;
  background: #f8fafc;
  border-left: 3px solid #2563eb;
  cursor: pointer;
  transition: all 0.15s ease;
}

.quarter-event-row:hover {
  background: #eff6ff;
  transform: translateX(2px);
}

.row-date-badge {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 38px;
  background: #ffffff;
  border-radius: 6px;
  border: 1px solid #e2e8f0;
  padding: 2px 4px;
}

.row-date-badge .date-num {
  font-size: 13px;
  font-weight: 800;
  color: #1e293b;
  line-height: 1;
}

.row-date-badge .month-num {
  font-size: 8.5px;
  font-weight: 700;
  color: #2563eb;
  text-transform: uppercase;
}

.row-title-desc {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.row-event-title {
  font-size: 13px;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
}

.row-event-desc {
  font-size: 11.5px;
  color: #64748b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.row-meta {
  display: flex;
  align-items: center;
  gap: 10px;
}

/* =========================================================================
   3. MONTH VIEW
   ========================================================================= */
.month-view-container {
  display: flex;
  flex-direction: column;
}

.month-days-header-row {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
}

.weekday-header-cell {
  padding: 7px 10px;
  font-size: 11.5px;
  font-weight: 600;
  color: #64748b;
  text-align: center;
  text-transform: uppercase;
  letter-spacing: 0.3px;
}

.month-grid-cells {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  grid-auto-rows: minmax(76px, 1fr);
}

.month-day-cell {
  border-right: 1px solid #f1f5f9;
  border-bottom: 1px solid #f1f5f9;
  padding: 4px 6px;
  display: flex;
  flex-direction: column;
  background: #ffffff;
  transition: background 0.12s ease, border-color 0.12s ease;
  min-height: 76px;
  position: relative;
  cursor: pointer;
}

.month-day-cell:nth-child(7n) {
  border-right: none;
}

.month-day-cell:hover {
  background: #f8fafc;
}

.month-day-cell.is-other-month {
  background: #fafbfc;
  opacity: 0.6;
}

.month-day-cell.is-today {
  background: #eff6ff60;
}

.month-day-cell.is-in-drag-range {
  background: #dbeafe66 !important;
  border-color: #93c5fd !important;
}

.month-day-cell.is-drag-start {
  background: #bfdbfe99 !important;
  border-left: 2px solid #2563eb !important;
}

.month-day-cell.is-drag-end {
  background: #bfdbfe99 !important;
  border-right: 2px solid #2563eb !important;
}

.month-day-cell.is-drop-hover {
  background: #ecfdf5 !important;
  outline: 2px dashed #059669;
  outline-offset: -2px;
}

.cell-day-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 2px;
}

.day-number-badge {
  font-size: 12px;
  font-weight: 600;
  color: #334155;
  width: 22px;
  height: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
}

.day-number-badge.today-highlight {
  background: #2563eb;
  color: #ffffff;
  box-shadow: 0 1px 4px rgba(37, 99, 235, 0.3);
}

.btn-quick-add-event {
  background: transparent;
  border: none;
  font-size: 13px;
  color: #94a3b8;
  width: 18px;
  height: 18px;
  border-radius: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: all 0.15s ease;
}

.month-day-cell:hover .btn-quick-add-event {
  opacity: 1;
}

.btn-quick-add-event:hover {
  background: #e2e8f0;
  color: #2563eb;
}

.cell-events-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
  overflow: hidden;
}

.event-chip-item {
  display: flex;
  align-items: center;
  gap: 3px;
  padding: 1.5px 5px;
  border-radius: 4px;
  border-left: 3px solid #2563eb;
  cursor: grab;
  transition: transform 0.1s ease, box-shadow 0.1s ease;
  overflow: hidden;
  white-space: nowrap;
}

.event-chip-item:active {
  cursor: grabbing;
}

.event-chip-item.is-dragging {
  opacity: 0.4;
  transform: scale(0.95);
}

.event-chip-item:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);
}

.event-chip-time {
  font-size: 10px;
  font-weight: 700;
  color: #334155;
  flex-shrink: 0;
}

.event-chip-title {
  font-size: 11px;
  font-weight: 600;
  color: #1e293b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.event-more-pill {
  font-size: 10.5px;
  font-weight: 600;
  color: #2563eb;
  padding: 1px 4px;
  cursor: pointer;
  border-radius: 4px;
  transition: background 0.15s ease;
}

.event-more-pill:hover {
  background: #eff6ff;
  text-decoration: underline;
}

/* =========================================================================
   4. WEEK VIEW COMPACT
   ========================================================================= */
.week-view-container {
  display: flex;
  flex-direction: column;
}

.week-header-row {
  display: grid;
  grid-template-columns: 60px repeat(7, 1fr);
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
}

.time-col-header {
  padding: 7px;
  font-size: 11.5px;
  font-weight: 600;
  color: #94a3b8;
  text-align: center;
}

.week-day-header-col {
  padding: 7px 6px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  border-left: 1px solid #f1f5f9;
}

.week-day-header-col.is-today {
  background: #eff6ff;
}

.week-day-name {
  font-size: 11.5px;
  font-weight: 600;
  color: #64748b;
}

.week-day-num {
  font-size: 13px;
  font-weight: 700;
  color: #1e293b;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
}

.week-day-num.today-num {
  background: #2563eb;
  color: #ffffff;
}

.week-time-grid {
  display: flex;
  flex-direction: column;
}

.week-hour-row {
  display: grid;
  grid-template-columns: 60px repeat(7, 1fr);
  border-bottom: 1px solid #f1f5f9;
  min-height: 44px;
}

.time-label-cell {
  padding: 4px;
  font-size: 11px;
  font-weight: 600;
  color: #94a3b8;
  text-align: right;
  padding-right: 10px;
}

.week-hour-slot {
  border-left: 1px solid #f1f5f9;
  padding: 2px 4px;
  display: flex;
  flex-direction: column;
  gap: 2px;
  cursor: pointer;
}

.week-hour-slot:hover {
  background: #f8fafc;
}

.week-event-card {
  padding: 3px 5px;
  border-radius: 5px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.week-event-card:hover {
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.08);
}

.week-event-time {
  font-size: 9.5px;
  font-weight: 700;
  color: #475569;
}

.week-event-title {
  font-size: 11px;
  font-weight: 600;
  color: #1e293b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.week-event-location {
  font-size: 9.5px;
  color: #64748b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* =========================================================================
   5. DAY VIEW COMPACT
   ========================================================================= */
.day-view-container {
  display: flex;
  flex-direction: column;
}

.day-header-summary {
  padding: 10px 16px;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.day-summary-title {
  font-size: 15px;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
}

.day-events-count {
  font-size: 12.5px;
  color: #2563eb;
  font-weight: 600;
}

.day-timeline-scroll {
  display: flex;
  flex-direction: column;
}

.day-timeline-row {
  display: grid;
  grid-template-columns: 70px 1fr;
  border-bottom: 1px solid #f1f5f9;
  min-height: 52px;
}

.day-hour-marker {
  padding: 8px;
  font-size: 11.5px;
  font-weight: 600;
  color: #94a3b8;
  text-align: right;
  padding-right: 12px;
}

.day-slot-content {
  border-left: 1px solid #f1f5f9;
  padding: 4px 10px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  cursor: pointer;
}

.day-slot-content:hover {
  background: #fafbfc;
}

.day-event-banner {
  padding: 8px 12px;
  border-radius: 7px;
  display: flex;
  flex-direction: column;
  gap: 3px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  cursor: pointer;
}

.day-event-banner-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.day-event-type-tag {
  color: #ffffff;
  padding: 1px 6px;
  border-radius: 4px;
  font-size: 9.5px;
  font-weight: 700;
  text-transform: uppercase;
}

.day-event-banner-time {
  font-size: 11px;
  font-weight: 700;
  color: #475569;
}

.day-event-banner-title {
  font-size: 13.5px;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
}

.day-event-banner-desc {
  font-size: 12px;
  color: #475569;
  margin: 0;
}

.day-event-banner-footer {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 2px;
  font-size: 11.5px;
  color: #64748b;
}

.day-footer-link {
  color: #2563eb;
  font-weight: 600;
  text-decoration: none;
}

.day-footer-link:hover {
  text-decoration: underline;
}

/* =========================================================================
   6. AGENDA VIEW COMPACT
   ========================================================================= */
.agenda-view-container {
  padding: 14px 16px;
}

.agenda-empty-card {
  text-align: center;
  padding: 40px 16px;
}

.agenda-empty-card .empty-icon {
  font-size: 36px;
  margin-bottom: 6px;
}

.agenda-empty-card h4 {
  font-size: 16px;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 4px 0;
}

.agenda-empty-card p {
  color: #64748b;
  font-size: 13px;
  margin: 0 0 14px 0;
}

.agenda-groups-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.agenda-date-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.agenda-date-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.date-badge-box {
  width: 42px;
  height: 42px;
  border-radius: 9px;
  background: #eff6ff;
  color: #2563eb;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.date-badge-day {
  font-size: 16px;
  font-weight: 800;
  line-height: 1;
}

.date-badge-month {
  font-size: 8.5px;
  font-weight: 700;
  text-transform: uppercase;
}

.date-weekday-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.date-weekday-title h4 {
  font-size: 14px;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
}

.events-badge-pill {
  background: #e2e8f0;
  color: #475569;
  font-size: 10.5px;
  font-weight: 600;
  padding: 1px 7px;
  border-radius: 10px;
}

.agenda-events-cards {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding-left: 52px;
}

.agenda-card-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #ffffff;
  padding: 10px 14px;
  border-radius: 9px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
  cursor: pointer;
  transition: all 0.15s ease;
  gap: 12px;
}

.agenda-card-item:hover {
  transform: translateY(-1px);
  box-shadow: 0 3px 8px rgba(0, 0, 0, 0.05);
}

.agenda-card-left {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 3px;
  min-width: 85px;
}

.agenda-time-range {
  font-size: 11.5px;
  font-weight: 700;
  color: #1e293b;
}

.event-type-badge-mini {
  font-size: 10px;
  font-weight: 700;
  padding: 1px 5px;
  border-radius: 4px;
  border: 1px solid;
}

.agenda-card-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.agenda-item-title {
  font-size: 13.5px;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
}

.agenda-item-desc {
  font-size: 12px;
  color: #64748b;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
}

.agenda-item-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 2px;
}

.meta-tag {
  font-size: 11px;
  color: #64748b;
}

.btn-join-meet {
  padding: 5px 12px;
  border-radius: 7px;
  background: #eff6ff;
  color: #2563eb;
  border: 1px solid #bfdbfe;
  font-size: 12px;
  font-weight: 600;
  text-decoration: none;
  white-space: nowrap;
  transition: all 0.15s ease;
}

.btn-join-meet:hover {
  background: #2563eb;
  color: #ffffff;
}

/* =========================================================================
   MODAL DIALOGS
   ========================================================================= */
.modal-backdrop-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.55);
  backdrop-filter: blur(4px);
  z-index: 1050;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.custom-modal-card {
  background: #ffffff;
  border-radius: 14px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
  max-height: 90vh;
  overflow: hidden;
}

.calendar-modal-card {
  width: 100%;
  max-width: 620px;
}

.calendar-detail-card {
  width: 100%;
  max-width: 540px;
}

.modal-card-header {
  padding: 14px 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e2e8f0;
}

.header-icon-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.modal-header-icon {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  flex-shrink: 0;
}

.modal-title-group {
  display: flex;
  flex-direction: column;
}

.modal-card-title {
  font-size: 15px;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
}

.modal-card-desc {
  font-size: 11.5px;
  color: #64748b;
  margin-top: 1px;
}

.btn-close-modal {
  background: transparent;
  border: none;
  font-size: 15px;
  color: #94a3b8;
  cursor: pointer;
  padding: 4px;
  border-radius: 6px;
}

.btn-close-modal:hover {
  color: #ef4444;
  background: #fee2e2;
}

.modal-card-body {
  padding: 16px 18px;
}

.modal-form-scroll {
  overflow-y: auto;
  max-height: 65vh;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.detail-body-scroll {
  overflow-y: auto;
  max-height: 65vh;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.form-group-field {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.form-row-2col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.field-label {
  font-size: 12.5px;
  font-weight: 600;
  color: #334155;
}

.required-star {
  color: #ef4444;
}

.form-text-input {
  width: 100%;
  padding: 7px 11px;
  border-radius: 7px;
  border: 1px solid #cbd5e1;
  font-size: 13px;
  outline: none;
  transition: all 0.15s ease;
}

.form-text-input:focus {
  border-color: #2563eb;
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.1);
}

.form-textarea-input {
  width: 100%;
  padding: 7px 11px;
  border-radius: 7px;
  border: 1px solid #cbd5e1;
  font-size: 13px;
  outline: none;
  resize: vertical;
  font-family: inherit;
}

.form-textarea-input:focus {
  border-color: #2563eb;
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.1);
}

.custom-color-picker-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  padding: 4px 0;
}

.el-picker-container {
  display: flex;
  align-items: center;
}

.color-hex-input-wrapper {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: #f8fafc;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  padding: 4px 8px;
  transition: all 0.15s ease;
}

.color-hex-input-wrapper:focus-within {
  border-color: #2563eb;
  background: #ffffff;
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.1);
}

.color-dot-indicator {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  border: 1px solid rgba(0, 0, 0, 0.15);
  flex-shrink: 0;
}

.color-hex-text-input {
  border: none;
  background: transparent;
  font-size: 12.5px;
  font-weight: 600;
  font-family: monospace;
  color: #1e293b;
  width: 68px;
  outline: none;
  text-transform: uppercase;
}

.color-divider-line {
  width: 1px;
  height: 24px;
  background: #e2e8f0;
}

.color-preset-picker {
  display: flex;
  gap: 8px;
  align-items: center;
}

.color-dot-btn {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  border: 2px solid transparent;
  cursor: pointer;
  transition: all 0.15s ease;
}

.color-dot-btn:hover {
  transform: scale(1.15);
}

.color-dot-btn.is-selected {
  border-color: #1e293b;
  transform: scale(1.18);
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.2);
}

.modal-card-footer {
  padding: 12px 18px;
  border-top: 1px solid #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  background: #f8fafc;
}

.btn-modal-cancel {
  padding: 7px 14px;
  border-radius: 7px;
  background: #ffffff;
  border: 1px solid #cbd5e1;
  color: #475569;
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
}

.btn-modal-cancel:hover {
  background: #f1f5f9;
  color: #1e293b;
}

.btn-modal-primary {
  padding: 7px 16px;
  border-radius: 7px;
  background: #2563eb;
  border: none;
  color: #ffffff;
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-modal-primary:hover {
  background: #1d4ed8;
}

.btn-modal-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* DETAIL MODAL STYLES */
.detail-type-tag {
  color: #ffffff;
  padding: 2px 7px;
  border-radius: 4px;
  font-size: 10px;
  font-weight: 700;
  text-transform: uppercase;
}

.detail-priority-tag {
  font-size: 10px;
  font-weight: 700;
  padding: 2px 7px;
  border-radius: 4px;
  text-transform: uppercase;
}

.priority-critical { background: #fee2e2; color: #dc2626; border: 1px solid #fca5a5; }
.priority-urgent { background: #ffedd5; color: #ea580c; border: 1px solid #fdba74; }
.priority-high { background: #fef3c7; color: #d97706; border: 1px solid #fcd34d; }
.priority-medium { background: #eff6ff; color: #2563eb; border: 1px solid #bfdbfe; }
.priority-low { background: #f0fdf4; color: #16a34a; border: 1px solid #bbf7d0; }
.priority-optional { background: #f1f5f9; color: #64748b; border: 1px solid #cbd5e1; }

.detail-info-row {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  font-size: 13px;
}

.detail-row-icon {
  font-size: 15px;
  flex-shrink: 0;
  margin-top: 1px;
}

.detail-row-content {
  display: flex;
  flex-direction: column;
  gap: 2px;
  color: #334155;
}

.detail-time-range {
  font-size: 12px;
  color: #2563eb;
  font-weight: 600;
}

.detail-meet-link {
  color: #2563eb;
  font-weight: 600;
  text-decoration: none;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  word-break: break-all;
}

.detail-meet-link:hover {
  text-decoration: underline;
}

.attendees-tags-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  margin-top: 3px;
}

.attendee-pill {
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  color: #1d4ed8;
  padding: 2px 7px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 600;
}

.detail-desc-box {
  background: #f8fafc;
  padding: 10px 12px;
  border-radius: 7px;
  border: 1px solid #e2e8f0;
  font-size: 12.5px;
  color: #475569;
}

.detail-desc-box p {
  margin: 3px 0 0 0;
  line-height: 1.45;
}

.detail-modal-footer {
  justify-content: space-between;
}

.detail-footer-left {
  display: flex;
  align-items: center;
}

.detail-footer-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.btn-detail-delete {
  padding: 7px 12px;
  border-radius: 7px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  color: #dc2626;
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-detail-delete:hover {
  background: #fee2e2;
}

.btn-detail-edit {
  padding: 7px 14px;
  border-radius: 7px;
  background: #ffffff;
  border: 1px solid #cbd5e1;
  color: #334155;
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
}

.btn-detail-edit:hover {
  background: #f1f5f9;
}

.btn-detail-join {
  padding: 7px 14px;
  border-radius: 7px;
  background: #2563eb;
  color: #ffffff;
  font-size: 12.5px;
  font-weight: 600;
  text-decoration: none;
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-detail-join:hover {
  background: #1d4ed8;
}

/* =========================================================================
   WORKSPACE LAYOUT & TASK BOARD PANEL (BUỔI 3)
   ========================================================================= */
.calendar-workspace-layout {
  display: block;
  width: 100%;
}

.calendar-workspace-layout.with-task-board {
  display: grid;
  grid-template-columns: 1fr 340px;
  gap: 12px;
  align-items: start;
}

.task-board-panel {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 14px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: calc(100vh - 120px);
  position: sticky;
  top: 10px;
  overflow: hidden;
}

.task-panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f1f5f9;
}

.panel-header-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.panel-header-icon {
  font-size: 18px;
}

.panel-titles {
  display: flex;
  flex-direction: column;
}

.panel-main-title {
  font-size: 14.5px;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
}

.panel-sub-title {
  font-size: 11px;
  color: #64748b;
  margin-top: 1px;
}

.btn-panel-close {
  background: none;
  border: none;
  color: #94a3b8;
  font-size: 15px;
  cursor: pointer;
  padding: 4px;
  line-height: 1;
  border-radius: 4px;
}

.btn-panel-close:hover {
  color: #ef4444;
  background: #fee2e2;
}

/* Task KPI Bar */
.task-kpi-card {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 10px 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.kpi-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 11.5px;
}

.kpi-label {
  font-weight: 600;
  color: #475569;
}

.kpi-rate {
  font-weight: 700;
  color: #2563eb;
}

.kpi-progress-track {
  width: 100%;
  height: 6px;
  background: #e2e8f0;
  border-radius: 3px;
  overflow: hidden;
}

.kpi-progress-bar {
  height: 100%;
  background: linear-gradient(90deg, #3b82f6, #10b981);
  border-radius: 3px;
  transition: width 0.3s ease;
}

/* Filter pills */
.task-filter-pills {
  display: flex;
  gap: 5px;
  overflow-x: auto;
  padding-bottom: 2px;
}

.task-filter-pill {
  border: 1px solid #e2e8f0;
  background: #ffffff;
  color: #64748b;
  font-size: 11.5px;
  font-weight: 600;
  padding: 4px 8px;
  border-radius: 6px;
  white-space: nowrap;
  cursor: pointer;
  transition: all 0.15s ease;
}

.task-filter-pill:hover {
  background: #f8fafc;
  color: #1e293b;
}

.task-filter-pill.is-active {
  background: #eff6ff;
  color: #2563eb;
  border-color: #bfdbfe;
}

/* Quick input */
.quick-task-input-box {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
  padding: 4px 8px;
  transition: all 0.2s ease;
}

.quick-task-input-box:focus-within {
  border-color: #3b82f6;
  background: #ffffff;
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.1);
}

.quick-task-input {
  flex: 1;
  border: none;
  background: transparent;
  font-size: 12.5px;
  color: #1e293b;
  outline: none;
  padding: 4px 0;
}

.quick-task-input::placeholder {
  color: #94a3b8;
}

.btn-submit-quick-task {
  background: #2563eb;
  color: #ffffff;
  border: none;
  border-radius: 6px;
  padding: 4px 10px;
  font-size: 11.5px;
  font-weight: 600;
  cursor: pointer;
}

/* Task list scroll */
.task-list-scroll {
  display: flex;
  flex-direction: column;
  gap: 8px;
  overflow-y: auto;
  max-height: calc(100vh - 350px);
  padding-right: 2px;
}

.task-list-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 30px 10px;
  color: #94a3b8;
  font-size: 12px;
  text-align: center;
}

.empty-icon {
  font-size: 24px;
  margin-bottom: 6px;
}

/* Task card item */
.task-card-item {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-left-width: 4px;
  border-radius: 8px;
  padding: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  cursor: grab;
  transition: all 0.2s ease;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.02);
}

.task-card-item:hover {
  border-color: #cbd5e1;
  box-shadow: 0 3px 8px rgba(0, 0, 0, 0.06);
  transform: translateY(-1px);
}

.task-card-item.is-completed {
  background: #f8fafc;
  opacity: 0.75;
}

.task-card-item.is-dragging {
  opacity: 0.4;
  transform: scale(0.97);
}

.task-card-top-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.task-checkbox-btn {
  width: 18px;
  height: 18px;
  border-radius: 4px;
  border: 1.5px solid #cbd5e1;
  background: #ffffff;
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  flex-shrink: 0;
  margin-top: 2px;
  transition: all 0.15s ease;
  padding: 0;
}

.task-checkbox-btn:hover {
  border-color: #2563eb;
}

.task-checkbox-btn.is-checked {
  background: #10b981;
  border-color: #10b981;
}

.task-card-main-info {
  flex: 1;
  min-width: 0;
}

.task-card-title {
  font-size: 13px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 4px 0;
  line-height: 1.35;
  word-break: break-word;
}

.task-card-title.completed-strikethrough {
  text-decoration: line-through;
  color: #94a3b8;
}

.task-badges-row {
  display: flex;
  align-items: center;
  gap: 5px;
  flex-wrap: wrap;
}

.task-type-badge-mini {
  font-size: 10px;
  font-weight: 600;
  padding: 1.5px 6px;
  border-radius: 4px;
}

.task-priority-badge-mini {
  font-size: 10px;
  font-weight: 600;
  padding: 1.5px 6px;
  border-radius: 4px;
}

/* Progress bar inside card */
.task-card-progress-section {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.progress-labels {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 10.5px;
  color: #64748b;
}

.subtask-count-text {
  font-weight: 500;
}

.progress-percent-val {
  font-weight: 700;
}

.mini-progress-track {
  width: 100%;
  height: 4px;
  background: #f1f5f9;
  border-radius: 2px;
  overflow: hidden;
}

.mini-progress-fill {
  height: 100%;
  border-radius: 2px;
  transition: width 0.25s ease;
}

/* Card footer */
.task-card-footer-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 11px;
  color: #64748b;
  border-top: 1px solid #f8fafc;
  padding-top: 6px;
}

.task-assignee-box {
  display: flex;
  align-items: center;
  gap: 5px;
  max-width: 150px;
}

.assignee-avatar-mini {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #e0e7ff;
  color: #4338ca;
  font-size: 9.5px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.assignee-name {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.task-deadline-tag {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 10.5px;
  color: #64748b;
}

.task-deadline-tag.is-overdue {
  color: #ef4444;
  font-weight: 600;
}

/* FORM SUBTASKS & ASSIGNEE */
.field-label-with-val {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.field-val-badge {
  font-size: 11px;
  font-weight: 700;
  color: #2563eb;
  background: #eff6ff;
  padding: 1px 6px;
  border-radius: 6px;
}

.subtask-header-counter {
  font-size: 11.5px;
  font-weight: 600;
  color: #10b981;
  margin-left: 6px;
}

.subtask-input-row {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}

.subtask-text-input {
  flex: 1;
}

.btn-add-subtask {
  background: #eff6ff;
  color: #2563eb;
  border: 1px solid #bfdbfe;
  border-radius: 8px;
  padding: 0 14px;
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.15s ease;
}

.btn-add-subtask:hover {
  background: #2563eb;
  color: #ffffff;
}

.form-subtasks-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 8px 10px;
  max-height: 160px;
  overflow-y: auto;
}

.subtask-list-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 4px 6px;
  border-radius: 6px;
  background: #ffffff;
  border: 1px solid #f1f5f9;
}

.subtask-done-text {
  text-decoration: line-through;
  color: #94a3b8;
}

.btn-remove-subtask {
  background: none;
  border: none;
  color: #94a3b8;
  font-size: 13px;
  cursor: pointer;
  padding: 2px 4px;
  line-height: 1;
  border-radius: 4px;
}

.btn-remove-subtask:hover {
  color: #ef4444;
  background: #fee2e2;
}

/* DETAIL MODAL SUBTASKS & STATUS BUTTON */
.assignee-detail-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: #f1f5f9;
  padding: 3px 8px;
  border-radius: 6px;
  font-size: 12.5px;
  font-weight: 600;
  color: #334155;
  margin-top: 2px;
}

.detail-progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  margin-bottom: 4px;
}

.progress-val-text {
  font-weight: 700;
  color: #2563eb;
}

.detail-progress-track {
  width: 100%;
  height: 8px;
  background: #f1f5f9;
  border-radius: 4px;
  overflow: hidden;
}

.detail-progress-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.3s ease;
}

.detail-subtasks-box {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 10px 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 4px;
}

.detail-subtasks-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.subtask-ratio {
  font-size: 11.5px;
  font-weight: 600;
  color: #10b981;
}

.detail-subtasks-checklist {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.detail-subtask-item {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 6px 10px;
}

.subtask-checkbox-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12.5px;
  color: #1e293b;
  cursor: pointer;
  margin: 0;
}

.subtask-real-check {
  width: 16px;
  height: 16px;
  accent-color: #10b981;
  cursor: pointer;
}

.btn-detail-toggle-status {
  padding: 7px 12px;
  border-radius: 8px;
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  border: 1px solid #cbd5e1;
  background: #ffffff;
  color: #0f172a;
  transition: all 0.15s ease;
}

.btn-detail-toggle-status:hover {
  background: #f8fafc;
  border-color: #94a3b8;
}

.btn-detail-toggle-status.is-completed {
  background: #f0fdf4;
  color: #15803d;
  border-color: #bbf7d0;
}

/* =========================================================================
   RESPONSIVE DESIGN (TABLET & MOBILE)
   ========================================================================= */
@media (max-width: 1200px) {
  .year-grid-12months {
    grid-template-columns: repeat(3, 1fr);
  }
  .quarter-3months-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 992px) {
  .calendar-metrics-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .calendar-toolbar-card {
    flex-direction: column;
    align-items: stretch;
  }
  .toolbar-nav-left, .toolbar-actions-right {
    justify-content: space-between;
  }
  .year-grid-12months {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .calendar-page-container {
    padding: 8px;
    gap: 8px;
  }
  .calendar-header-card {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
  .calendar-metrics-grid {
    grid-template-columns: 1fr;
  }
  .toolbar-actions-right {
    flex-direction: column;
    align-items: stretch;
  }
  .search-event-input-wrapper {
    width: 100%;
  }
  .event-type-select {
    width: 100%;
  }
  .form-row-2col {
    grid-template-columns: 1fr;
  }
  .agenda-events-cards {
    padding-left: 0;
  }
  .month-grid-cells {
    grid-auto-rows: minmax(68px, 1fr);
  }
  .event-chip-time {
    display: none;
  }
  .year-grid-12months {
    grid-template-columns: 1fr;
  }
}
</style>
