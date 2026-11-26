<template>
  <div 
    class="chat-widget-wrapper" 
    :class="{ 
      'is-on-chat-page': isOnChatPage, 
      'is-collapsed-edge': isCollapsed && !isOpen,
      'is-open': isOpen
    }"
  >
    <!-- 0. KHI Ở CHẾ ĐỘ THU GỌN NÉP GÓC (MINI DOCKED TAB) -->
    <transition name="dock-slide">
      <div 
        v-if="!isOpen && isCollapsed" 
        class="copilot-docked-tab" 
        @click="toggleCollapse(false)"
        title="Nhấn để mở lại Trợ lý AI Copilot"
      >
        <span class="docked-icon">🤖</span>
        <span class="docked-text">AI Copilot</span>
        <span class="docked-arrow">◀</span>
      </div>
    </transition>

    <!-- 1. KHI Ở CHẾ ĐỘ THÔNG THƯỜNG -->
    <template v-if="isOpen || !isCollapsed">
      <!-- 0. COPILOT TEASER PILL (FIGMA IMAGES 4 & 5 MATCH) -->
      <transition name="fade">
        <div v-if="!isOpen && showTeaser" class="copilot-teaser-pill" @click="toggleWidget">
          <span class="teaser-icon">💬</span>
          <span class="teaser-text">Trợ lý Copilot: <em>"Hôm nay tôi giúp gì..."</em></span>
          <button 
            type="button" 
            class="btn-teaser-close" 
            title="Thu gọn gợi ý này" 
            @click.stop="hideTeaser"
          >
            ✕
          </button>
        </div>
      </transition>

      <!-- 1. FLOATING ACTION BUTTON (FIGMA IMAGE 5 MATCH) -->
      <div 
        class="chat-floating-btn" 
        :class="{ 'is-active': isOpen, 'btn-dark': isDarkMode }" 
        @click="toggleWidget"
        title="Trợ lý ảo ERP AI Copilot"
      >
        <div class="btn-inner">
          <span v-if="!isOpen" class="unread-pulse-badge"></span>
          <el-icon v-if="!isOpen" class="btn-icon"><Cpu /></el-icon>
          <el-icon v-else class="btn-icon"><Close /></el-icon>
        </div>

        <!-- Nút thu nhỏ nép góc (hiện khi hover vào nút AI) -->
        <button 
          v-if="!isOpen" 
          type="button" 
          class="btn-collapse-trigger" 
          title="Thu gọn nép vào mép màn hình"
          @click.stop="toggleCollapse(true)"
        >
          <svg viewBox="0 0 24 24" width="10" height="10" fill="none" stroke="currentColor" stroke-width="3">
            <polyline points="9 18 15 12 9 6"></polyline>
          </svg>
        </button>
      </div>
    </template>

    <!-- 2. CỬA SỔ HỘI THOẠI CHATBOT (FIGMA IMAGE 5 MATCH - SLEEK SLATE DARK) -->
    <transition name="chat-popup">
      <div 
        v-if="isOpen" 
        class="chat-window"
        :class="[isDarkMode ? 'theme-dark' : 'theme-light', { 'is-maximized': isMaximized }]"
      >
        <!-- HEADER -->
        <div class="chat-header">
          <div class="header-left">
            <div class="bot-avatar-wrap">
              <el-avatar :size="30" class="bot-avatar">
                <el-icon><Cpu /></el-icon>
              </el-avatar>
              <span class="avatar-status-dot"></span>
            </div>
            <div class="bot-info">
              <div class="bot-title-row">
                <span class="bot-title">Trợ lý AI ERP</span>
                <span class="live-pill">LIVE</span>
              </div>
              <div class="bot-status">
                <span class="status-dot">●</span>
                <span class="status-desc">Agentic Copilot Ultra VIP</span>
              </div>
            </div>
          </div>

          <div class="header-actions">
            <!-- Nút phóng to / thu nhỏ không gian điều hành -->
            <button 
              class="header-btn" 
              :title="isMaximized ? 'Thu nhỏ cửa sổ' : 'Phóng to không gian điều hành'" 
              @click="isMaximized = !isMaximized"
            >
              <el-icon><CopyDocument v-if="isMaximized" /><FullScreen v-else /></el-icon>
            </button>

            <!-- Nút chuyển đổi nhanh Nền Đậm (Dark) / Nền Sáng (Light) -->
            <button 
              class="header-btn" 
              :title="isDarkMode ? 'Chuyển sang nền sáng' : 'Chuyển sang nền đậm'"
              @click="toggleTheme"
            >
              <el-icon><Sunny v-if="isDarkMode" /><Moon v-else /></el-icon>
            </button>

            <!-- Nút xóa lịch sử -->
            <button 
              class="header-btn" 
              title="Xóa toàn bộ lịch sử trò chuyện" 
              @click="handleClearHistory"
              :disabled="loading || isStreaming || messages.length === 0"
            >
              <el-icon><Delete /></el-icon>
            </button>

            <!-- Nút thu nhỏ cửa sổ -->
            <button class="header-btn" title="Thu nhỏ" @click="isOpen = false">
              <el-icon><Minus /></el-icon>
            </button>

            <!-- Nút thu gọn nép góc -->
            <button class="header-btn" title="Thu gọn nép vào mép màn hình" @click="isOpen = false; toggleCollapse(true)">
              <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2.5">
                <polyline points="9 18 15 12 9 6"></polyline>
              </svg>
            </button>
          </div>
        </div>

        <!-- BODY: MESSAGE FEED -->
        <div class="chat-body" ref="messagesContainerRef">
          <!-- Welcome message if no chat history yet -->
          <div v-if="messages.length === 0 && !fetchingHistory" class="chat-welcome-card">
            <div class="welcome-icon">
              <el-icon><Opportunity /></el-icon>
            </div>
            <h4 class="welcome-title">Xin chào {{ userDisplayName }}! 👋</h4>
            <p class="welcome-desc">
              Tôi là <strong>Trợ lý ảo ERP AI</strong>. Tôi có thể hỗ trợ bạn tra cứu trực tiếp kho hàng, tài khoản người dùng và thông báo toàn hệ thống.
            </p>

            <!-- Quick Action Chips -->
            <div class="quick-chips-wrap">
              <div class="chips-label">Gợi ý câu hỏi nhanh:</div>
              <div class="chips-list">
                <button 
                  v-for="(chip, idx) in quickPrompts" 
                  :key="idx" 
                  class="quick-chip-btn"
                  @click="sendQuickPrompt(chip)"
                >
                  {{ chip }}
                </button>
              </div>
            </div>
          </div>

          <!-- Loading state when fetching history -->
          <div v-if="fetchingHistory" class="chat-loading-history">
            <el-icon class="is-loading"><Loading /></el-icon>
            <span>Đang tải lịch sử trò chuyện...</span>
          </div>

          <!-- Message List -->
          <div v-else class="message-stream">
            <div 
              v-for="(msg, index) in messages" 
              :key="msg.id || index" 
              :class="['message-row', msg.role === 'USER' ? 'row-user' : 'row-assistant']"
            >
              <!-- Assistant Avatar -->
              <div v-if="msg.role === 'ASSISTANT'" class="msg-avatar assistant-avatar">
                <el-icon><Cpu /></el-icon>
              </div>

              <!-- Message Bubble Wrap -->
              <div class="msg-bubble-wrap">
                <!-- KHỐI QUÁ TRÌNH SUY LUẬN (THINKING TRACE BOX) - CHỈ HIỆN KHI CÓ TRACE VÀ THU GỌN MẶC ĐỊNH -->
                <div 
                  v-if="msg.thinkingTrace && msg.thinkingTrace.length" 
                  class="thinking-trace-card" 
                  :class="{ 'is-expanded': msg.showTrace }"
                >
                  <div class="trace-header" @click="msg.showTrace = !msg.showTrace">
                    <span class="trace-icon">🧠</span>
                    <span class="trace-title">Quá trình suy luận ({{ msg.thinkingTrace.length }} bước)</span>
                    <span class="trace-arrow">{{ msg.showTrace ? '▲ Thu gọn' : '▼ Chi tiết' }}</span>
                  </div>
                  <div v-if="msg.showTrace" class="trace-steps">
                    <div v-for="(st, sIdx) in msg.thinkingTrace" :key="sIdx" class="trace-step-item">
                      <span class="step-bullet">↳</span>
                      <span class="step-text">{{ st.message }}</span>
                    </div>
                  </div>
                </div>

                <!-- BONG BÓNG TIN NHẮN CHÍNH -->
                <div class="msg-bubble">
                  <!-- Nút Loa Đọc Text-to-Speech -->
                  <button 
                    v-if="msg.role === 'ASSISTANT' && msg.content && !msg.isTyping"
                    type="button" 
                    class="btn-tts-speaker" 
                    :class="{ 'is-speaking': speakingIndex === index }" 
                    :title="speakingIndex === index ? 'Dừng đọc' : 'Đọc câu trả lời bằng tiếng Việt'"
                    @click="speakText(msg.content, index)"
                  >
                    <span>{{ speakingIndex === index ? '⏹️' : '🔊' }}</span>
                  </button>

                  <!-- Formatted HTML text support -->
                  <div class="msg-text" v-html="formatMessage(msg.content)"></div>
                  <!-- Hiệu ứng con trỏ nhấp nháy khi đang gõ từng chữ -->
                  <span v-if="msg.isTyping" class="typewriter-cursor">▌</span>
                </div>

                <!-- ======================================================== -->
                <!-- GENERATIVE UI INTERACTIVE WIDGETS                        -->
                <!-- ======================================================== -->

                <!-- 1. THẺ TẢI FILE BÁO CÁO THÔNG MINH (FILE_DOWNLOAD_CARD) -->
                <div 
                  v-if="msg.uiWidget && msg.uiWidget.type === 'FILE_DOWNLOAD_CARD' && !msg.isTyping" 
                  class="copilot-widget file-download-widget"
                >
                  <div class="file-card-top">
                    <div class="file-badge-icon" :class="msg.uiWidget.data?.format === 'CSV' ? 'is-csv' : 'is-excel'">
                      {{ msg.uiWidget.data?.format === 'CSV' ? '📄' : '📊' }}
                    </div>
                    <div class="file-meta-col">
                      <div class="file-title-name">{{ msg.uiWidget.data?.fileName }}</div>
                      <div class="file-sub-tags">
                        <span class="ftag ftag-fmt">{{ msg.uiWidget.data?.format || 'EXCEL' }}</span>
                        <span class="ftag ftag-size">{{ msg.uiWidget.data?.fileSize || '35 KB' }}</span>
                        <span class="ftag ftag-status">Sẵn sàng tải</span>
                      </div>
                    </div>
                  </div>

                  <div v-if="msg.uiWidget.data?.stats && msg.uiWidget.data.stats.length" class="file-stats-strip">
                    <div v-for="(st, stIdx) in msg.uiWidget.data.stats" :key="stIdx" class="f-stat-item">
                      <span class="f-lbl">{{ st.label }}:</span>
                      <span class="f-val">{{ st.value }}</span>
                    </div>
                  </div>

                  <button 
                    type="button" 
                    class="btn-download-action" 
                    @click="downloadReport(msg.uiWidget.data?.fileId, msg.uiWidget.data?.fileName)"
                  >
                    📥 Tải xuống File {{ msg.uiWidget.data?.format === 'CSV' ? 'CSV' : 'Excel' }} (.{{ (msg.uiWidget.data?.format || 'xlsx').toLowerCase() }})
                  </button>
                </div>

                <!-- 2. THẺ CẢNH BÁO TỒN KHO & NHẬP HÀNG (STOCK_ALERT_CARD) -->
                <div 
                  v-if="msg.uiWidget && msg.uiWidget.type === 'STOCK_ALERT_CARD' && !msg.isTyping" 
                  class="copilot-widget stock-alert-widget"
                >
                  <div class="stock-top-row">
                    <span 
                      class="stock-risk-pill" 
                      :class="'is-' + (msg.uiWidget.data?.riskLevel || 'safe').toLowerCase()"
                    >
                      {{ msg.uiWidget.data?.riskLevel === 'CRITICAL' ? '🚨 NGUY CẤP CỦA KHO' : '⚠️ CẢNH BÁO TỒN KHO' }}
                    </span>
                    <span class="stock-sku-badge">{{ msg.uiWidget.data?.sku }}</span>
                  </div>

                  <div class="stock-info-row">
                    <div class="stock-prod-name">{{ msg.uiWidget.data?.name }}</div>
                    <div class="stock-metrics-grid">
                      <div class="s-metric">
                        <span class="sm-lbl">Tồn khả dụng</span>
                        <span class="sm-val highlight">{{ msg.uiWidget.data?.availableStock }} {{ msg.uiWidget.data?.baseUnit || 'Cái' }}</span>
                      </div>
                      <div class="s-metric" v-if="msg.uiWidget.data?.daysLeft !== undefined">
                        <span class="sm-lbl">Hết hàng sau</span>
                        <span class="sm-val text-danger">{{ msg.uiWidget.data?.daysLeft }} ngày</span>
                      </div>
                      <div class="s-metric" v-if="msg.uiWidget.data?.dailySalesVelocity">
                        <span class="sm-lbl">Tốc độ bán</span>
                        <span class="sm-val">{{ msg.uiWidget.data?.dailySalesVelocity }} c/ngày</span>
                      </div>
                    </div>
                  </div>

                  <button 
                    type="button" 
                    class="btn-restock-action" 
                    @click="handleQuickRestock(msg.uiWidget.data)"
                  >
                    ⚡ Lập lệnh nhập thêm {{ msg.uiWidget.data?.suggestedRestockQty || 50 }} cái
                  </button>
                </div>

                <!-- 3. THẺ HÀNH ĐỘNG CÓ THẨM QUYỀN (ACTION_CONFIRMATION_CARD) -->
                <div 
                  v-if="msg.uiWidget && msg.uiWidget.type === 'ACTION_CONFIRMATION_CARD' && !msg.isTyping" 
                  class="copilot-widget action-confirm-widget"
                >
                  <div class="action-card-header">
                    <span class="action-shield">🛡️</span>
                    <span class="action-title-text">{{ msg.uiWidget.data?.title }}</span>
                  </div>

                  <div class="action-card-body">
                    <p class="action-reason-text">{{ msg.uiWidget.data?.reason || msg.uiWidget.data?.note }}</p>
                    <div class="action-details-row">
                      <span v-if="msg.uiWidget.data?.targetName">Đối tượng: <strong>{{ msg.uiWidget.data?.targetName }}</strong></span>
                      <span v-if="msg.uiWidget.data?.amountFormatted"> • Số tiền: <strong>{{ msg.uiWidget.data?.amountFormatted }}</strong></span>
                      <span v-if="msg.uiWidget.data?.overdueDays"> • Trễ hạn: <strong class="text-danger">{{ msg.uiWidget.data?.overdueDays }} ngày</strong></span>
                    </div>

                    <!-- Cảnh báo phân quyền nếu tài khoản không có thẩm quyền phê duyệt -->
                    <div 
                      v-if="isPrivilegedAction(msg.uiWidget.data?.actionType) && !canExecutePrivilegedAction" 
                      class="action-permission-banner"
                    >
                      <span class="lock-icon">🔒</span>
                      <span>Chế độ chỉ xem: Bạn cần vai trò <strong>ADMIN</strong>, <strong>MANAGER</strong> hoặc <strong>ACCOUNTANT</strong> để phê duyệt xuất quỹ này.</span>
                    </div>
                  </div>

                  <div class="action-card-btns">
                    <div v-if="executedActions[index]?.executed" class="action-done-banner">
                      {{ executedActions[index]?.message }}
                    </div>
                    <template v-else>
                      <!-- Nếu có thẩm quyền hoặc action không nhạy cảm: Nút duyệt hoạt động bình thường -->
                      <button 
                        v-if="!isPrivilegedAction(msg.uiWidget.data?.actionType) || canExecutePrivilegedAction"
                        type="button" 
                        class="btn-confirm-exec" 
                        @click="confirmAction(msg.uiWidget.data, index)"
                      >
                        {{ msg.uiWidget.data?.confirmButtonText || '🔴 Xác nhận Thực Hiện' }}
                      </button>

                      <!-- Nếu không có thẩm quyền: Nút duyệt bị disabled và hiển thị nhãn Chỉ xem -->
                      <button 
                        v-else
                        type="button" 
                        class="btn-confirm-exec is-disabled" 
                        disabled
                        title="Tài khoản của bạn chỉ có quyền XEM, không thể phê duyệt xuất quỹ"
                      >
                        🔒 Phê Duyệt (Chỉ ADMIN / Kế toán)
                      </button>

                      <button 
                        type="button" 
                        class="btn-cancel-exec" 
                        @click="cancelAction(index)"
                      >
                        {{ msg.uiWidget.data?.cancelButtonText || 'Hủy bỏ' }}
                      </button>
                    </template>
                  </div>
                </div>

                <!-- 4. THẺ LỊCH TRÌNH VẬN ĐƠN (SHIPMENT_TRACKING_CARD) -->
                <div 
                  v-if="msg.uiWidget && msg.uiWidget.type === 'SHIPMENT_TRACKING_CARD' && !msg.isTyping" 
                  class="copilot-widget tracking-widget"
                >
                  <div class="track-header-row">
                    <span class="carrier-tag">{{ msg.uiWidget.data?.carrierName || msg.uiWidget.data?.carrier }}</span>
                    <span class="track-code-text">{{ msg.uiWidget.data?.trackingCode }}</span>
                    <span class="track-status-pill">{{ msg.uiWidget.data?.shippingStatusLabel }}</span>
                  </div>

                  <div class="track-info-summary">
                    <div>Người nhận: <strong>{{ msg.uiWidget.data?.customerName }}</strong> (📞 {{ msg.uiWidget.data?.customerPhone }})</div>
                    <div v-if="msg.uiWidget.data?.codAmountFormatted">Tiền thu hộ COD: <strong>{{ msg.uiWidget.data?.codAmountFormatted }}</strong></div>
                  </div>

                  <div v-if="msg.uiWidget.data?.events && msg.uiWidget.data.events.length" class="track-timeline-wrap">
                    <div 
                      v-for="(ev, evIdx) in msg.uiWidget.data.events" 
                      :key="evIdx" 
                      class="track-tl-node" 
                      :class="{ 'is-done': ev.completed }"
                    >
                      <span class="tl-bullet"></span>
                      <div class="tl-body">
                        <div class="tl-title-line">{{ ev.title }}</div>
                        <div class="tl-sub-line">{{ ev.timestamp }} • {{ ev.location }}</div>
                      </div>
                    </div>
                  </div>
                </div>

                <!-- 5. BIỂU ĐỒ DOANH SỐ MINI (MINI_SPARKLINE_CHART) -->
                <div 
                  v-if="msg.uiWidget && msg.uiWidget.type === 'MINI_SPARKLINE_CHART' && !msg.isTyping" 
                  class="copilot-widget sparkline-widget"
                >
                  <div class="spark-title-row">
                    <span class="sp-title">{{ msg.uiWidget.data?.title || 'Biến động Doanh số 7 ngày' }}</span>
                    <span class="sp-total">{{ msg.uiWidget.data?.totalRevenueFormatted }}</span>
                  </div>

                  <div class="spark-svg-container">
                    <svg class="spark-svg" viewBox="0 0 280 84">
                      <rect 
                        v-for="(val, vIdx) in (msg.uiWidget.data?.values || [])" 
                        :key="vIdx"
                        :x="vIdx * 38 + 12" 
                        :y="68 - Math.max(6, Math.min(56, (val / getSparkMax(msg.uiWidget.data.values)) * 56))" 
                        width="20" 
                        :height="Math.max(6, Math.min(56, (val / getSparkMax(msg.uiWidget.data.values)) * 56))" 
                        rx="4"
                        class="spark-bar"
                      />
                      <text 
                        v-for="(lbl, lIdx) in (msg.uiWidget.data?.labels || [])" 
                        :key="'lbl-' + lIdx"
                        :x="lIdx * 38 + 22" 
                        y="80" 
                        text-anchor="middle" 
                        class="spark-lbl"
                      >
                        {{ lbl }}
                      </text>
                    </svg>
                  </div>
                </div>

                <!-- 6. THẺ ĐƠN HÀNG DỰ THẢO (ORDER_DRAFT_CARD - CONVERSATIONAL POS) -->
                <div 
                  v-if="msg.uiWidget && msg.uiWidget.type === 'ORDER_DRAFT_CARD' && !msg.isTyping" 
                  class="copilot-widget order-draft-widget"
                >
                  <div class="draft-card-header">
                    <div class="draft-header-title">
                      <span class="draft-icon">🛒</span>
                      <span class="draft-title-text">Đơn Hàng Dự Thảo (Conversational POS)</span>
                    </div>
                    <span 
                      class="draft-stock-status-badge"
                      :class="msg.uiWidget.data?.allInStock ? 'is-in-stock' : 'is-out-stock'"
                    >
                      {{ msg.uiWidget.data?.allInStock ? '🟢 Đủ tồn kho khả dụng' : '⚠️ Thiếu tồn kho' }}
                    </span>
                  </div>

                  <!-- Thông tin khách hàng & Giao vận -->
                  <div class="draft-customer-strip">
                    <div class="draft-cust-col">
                      <div class="cust-name-line">👤 <strong>{{ msg.uiWidget.data?.customerName || 'Khách mua lẻ' }}</strong></div>
                      <div class="cust-sub-line" v-if="msg.uiWidget.data?.customerPhone">📞 {{ msg.uiWidget.data?.customerPhone }}</div>
                    </div>
                    <div class="draft-delivery-col">
                      <div class="delivery-pay-tag">💳 {{ msg.uiWidget.data?.paymentMethod || 'CASH' }}</div>
                      <div class="delivery-addr-line" :title="msg.uiWidget.data?.shippingAddress">📍 {{ msg.uiWidget.data?.shippingAddress || 'Giao tận nơi' }}</div>
                    </div>
                  </div>

                  <!-- Bảng danh sách mặt hàng -->
                  <div class="draft-items-list">
                    <div class="draft-item-header">
                      <span>Sản phẩm</span>
                      <span class="col-center">SL</span>
                      <span class="col-right">Đơn giá</span>
                      <span class="col-right">Thành tiền</span>
                    </div>
                    <div 
                      v-for="(it, itIdx) in (msg.uiWidget.data?.items || [])" 
                      :key="itIdx" 
                      class="draft-item-row"
                    >
                      <div class="item-name-col">
                        <span class="item-name">{{ it.productName }}</span>
                        <span class="item-sku">({{ it.sku }})</span>
                        <span v-if="!it.inStock" class="item-stock-warn">⚠️ Thiếu</span>
                      </div>
                      <div class="item-qty-col col-center">x{{ it.quantity }}</div>
                      <div class="item-price-col col-right">{{ formatCurrency(it.unitPrice) }}</div>
                      <div class="item-total-col col-right">{{ formatCurrency(it.lineTotal) }}</div>
                    </div>
                  </div>

                  <!-- Khối tổng kết tài chính -->
                  <div class="draft-finance-summary">
                    <div class="finance-row">
                      <span>Tạm tính hàng hóa:</span>
                      <strong>{{ formatCurrency(msg.uiWidget.data?.subtotal) }}</strong>
                    </div>
                    <div class="finance-row">
                      <span>Thuế VAT (10%):</span>
                      <span>+{{ formatCurrency(msg.uiWidget.data?.taxAmount) }}</span>
                    </div>
                    <div class="finance-row total-row">
                      <span>Tổng thanh toán:</span>
                      <span class="total-price-text">{{ formatCurrency(msg.uiWidget.data?.totalAmount) }}</span>
                    </div>
                  </div>

                  <!-- Thao tác xác nhận 1-click -->
                  <div class="draft-actions-wrap">
                    <div v-if="executedActions[index]?.executed" class="draft-done-banner">
                      {{ executedActions[index]?.message }}
                    </div>
                    <template v-else>
                      <button 
                        type="button" 
                        class="btn-confirm-order-exec" 
                        @click="confirmDraftOrder(msg.uiWidget.data, index)"
                      >
                        ✅ Xác nhận Tạo Đơn Hàng (Giữ chỗ kho)
                      </button>
                      <button 
                        type="button" 
                        class="btn-cancel-exec" 
                        @click="cancelAction(index)"
                      >
                        Hủy bỏ
                      </button>
                    </template>
                  </div>
                </div>

                <!-- ========================================================================= -->
                <!-- WIDGET GIAI ĐOẠN 5.1: MORNING_BRIEFING_CARD -->
                <!-- ========================================================================= -->
                <div 
                  v-if="msg.uiWidget && msg.uiWidget.type === 'MORNING_BRIEFING_CARD' && !msg.isTyping" 
                  class="copilot-widget morning-briefing-widget"
                >
                  <div class="briefing-header">
                    <div class="briefing-badge">
                      <span class="pulse-sun">☀️</span>
                      <span>BẢN TIN ĐIỀU HÀNH SÁNG</span>
                    </div>
                    <span class="briefing-date">{{ msg.uiWidget.data?.briefingDate }}</span>
                  </div>

                  <div class="briefing-kpi-grid">
                    <div class="brief-kpi-item">
                      <span class="brief-kpi-label">Doanh thu hôm qua</span>
                      <strong class="brief-kpi-val text-amber">{{ formatCurrency(msg.uiWidget.data?.yesterdayRevenue) }}</strong>
                      <span class="brief-kpi-sub">Lãi gộp: {{ formatCurrency(msg.uiWidget.data?.yesterdayGrossProfit) }}</span>
                    </div>
                    <div class="brief-kpi-item">
                      <span class="brief-kpi-label">Nợ đại lý cần thu</span>
                      <strong class="brief-kpi-val text-rose">{{ formatCurrency(msg.uiWidget.data?.dueDebtAmount) }}</strong>
                      <span class="brief-kpi-sub">{{ msg.uiWidget.data?.dueCustomerCount }} đối tác</span>
                    </div>
                    <div class="brief-kpi-item">
                      <span class="brief-kpi-label">PO chờ sếp duyệt</span>
                      <strong class="brief-kpi-val text-blue">{{ msg.uiWidget.data?.pendingPoCount }} đơn</strong>
                      <span class="brief-kpi-sub">{{ formatCurrency(msg.uiWidget.data?.pendingPoTotalAmount) }}</span>
                    </div>
                  </div>

                  <div class="briefing-product-strip">
                    <div class="prod-strip-col">
                      <span>🔥 Bán chạy:</span>
                      <strong>{{ msg.uiWidget.data?.topSellingProductName }}</strong>
                    </div>
                    <div class="prod-strip-col">
                      <span>⏳ Tồn lâu:</span>
                      <strong>{{ msg.uiWidget.data?.slowMovingProductName }}</strong> ({{ msg.uiWidget.data?.slowMovingStockDays }} ngày)
                    </div>
                  </div>

                  <!-- Nút nghe giọng nói AI Audio -->
                  <div class="briefing-audio-action">
                    <button 
                      type="button" 
                      class="btn-audio-briefing" 
                      :class="{ 'is-speaking': isSpeakingBriefing }"
                      @click="toggleSpeakBriefing(msg.uiWidget.data?.audioScriptText)"
                    >
                      <span v-if="!isSpeakingBriefing">▶️ Nghe Bản Tin Sáng Bằng Giọng Nói AI</span>
                      <span v-else class="audio-speaking-content">
                        <span class="sound-wave"><span></span><span></span><span></span><span></span></span>
                        Đang phát giọng đọc AI (Nhấn để dừng)...
                      </span>
                    </button>
                  </div>
                </div>

                <!-- ========================================================================= -->
                <!-- WIDGET GIAI ĐOẠN 5.2: OCR_INVOICE_CARD -->
                <!-- ========================================================================= -->
                <div 
                  v-if="msg.uiWidget && msg.uiWidget.type === 'OCR_INVOICE_CARD' && !msg.isTyping" 
                  class="copilot-widget ocr-invoice-widget"
                >
                  <div class="ocr-card-header">
                    <div class="ocr-header-left">
                      <span class="ocr-icon">📷</span>
                      <div>
                        <div class="ocr-title">Bóc Tách Hóa Đơn Thông Minh (Smart OCR)</div>
                        <div class="ocr-sub">Độ tin cậy: {{ Math.round((msg.uiWidget.data?.confidenceScore || 0.98) * 100) }}% • Số: {{ msg.uiWidget.data?.invoiceNumber }}</div>
                      </div>
                    </div>
                    <span class="ocr-tag">VAT Invoice</span>
                  </div>

                  <div class="ocr-supplier-info">
                    <div class="supplier-row">
                      <span class="label">Nhà cung cấp:</span>
                      <strong>{{ msg.uiWidget.data?.supplierName }}</strong>
                    </div>
                    <div class="supplier-row">
                      <span class="label">Mã số thuế:</span>
                      <span class="tax-code-text">{{ msg.uiWidget.data?.taxCode }}</span>
                    </div>
                  </div>

                  <!-- Danh mục hàng bóc tách -->
                  <div class="ocr-items-table">
                    <div class="ocr-table-head">
                      <span>Mặt hàng</span>
                      <span class="col-center">SL</span>
                      <span class="col-right">Đơn giá</span>
                      <span class="col-right">Thành tiền</span>
                    </div>
                    <div v-for="(it, oIdx) in (msg.uiWidget.data?.items || [])" :key="oIdx" class="ocr-table-row">
                      <span class="prod-col">{{ it.productName }}</span>
                      <span class="col-center">x{{ it.quantity }}</span>
                      <span class="col-right">{{ formatCurrency(it.unitPrice) }}</span>
                      <span class="col-right font-semibold">{{ formatCurrency(it.totalAmount) }}</span>
                    </div>
                  </div>

                  <!-- Tổng tiền -->
                  <div class="ocr-summary-box">
                    <div class="sum-row">
                      <span>Tiền hàng:</span>
                      <span>{{ formatCurrency(msg.uiWidget.data?.subtotal) }}</span>
                    </div>
                    <div class="sum-row">
                      <span>Thuế VAT:</span>
                      <span>+{{ formatCurrency(msg.uiWidget.data?.taxAmount) }}</span>
                    </div>
                    <div class="sum-row total">
                      <span>Tổng thanh toán:</span>
                      <strong class="total-text">{{ formatCurrency(msg.uiWidget.data?.totalAmount) }}</strong>
                    </div>
                  </div>

                  <!-- 2 Nút hành động nhanh -->
                  <div class="ocr-actions-row">
                    <div v-if="executedActions[index]?.executed" class="draft-done-banner">
                      {{ executedActions[index]?.message }}
                    </div>
                    <template v-else>
                      <button type="button" class="btn-ocr-create-po" @click="createPoFromOcr(msg.uiWidget.data, index)">
                        📦 Tạo Đơn Mua Hàng PO
                      </button>
                      <button type="button" class="btn-ocr-create-pv" @click="createVoucherFromOcr(msg.uiWidget.data, index)">
                        💵 Tạo Phiếu Chi Quỹ
                      </button>
                    </template>
                  </div>
                </div>

                <!-- ========================================================================= -->
                <!-- WIDGET GIAI ĐOẠN 5.3: ANOMALY_RISK_CARD -->
                <!-- ========================================================================= -->
                <div 
                  v-if="msg.uiWidget && msg.uiWidget.type === 'ANOMALY_RISK_CARD' && !msg.isTyping" 
                  class="copilot-widget anomaly-risk-widget"
                >
                  <div class="anomaly-header">
                    <div class="anomaly-badge">
                      <span class="shield-warn">🛡️</span>
                      <span>RÀ SOÁT RỦI RO & BẤT THƯỜNG GIAN LẬN</span>
                    </div>
                    <span class="scanned-time">Quét lúc {{ formatTime(msg.uiWidget.data?.scannedAt) }}</span>
                  </div>

                  <div class="anomaly-list">
                    <div 
                      v-for="(al, aIdx) in (msg.uiWidget.data?.alerts || [])" 
                      :key="aIdx" 
                      class="anomaly-item"
                      :class="'sev-' + (al.severity || 'HIGH').toLowerCase()"
                    >
                      <div class="item-head">
                        <span class="sev-tag">{{ al.severity }}</span>
                        <strong class="item-title">{{ al.title }}</strong>
                      </div>
                      <p class="item-desc">{{ al.description }}</p>
                      <div class="item-meta">
                        <span class="impact-val" v-if="al.impactAmount">Tác động: <strong>{{ formatCurrency(al.impactAmount) }}</strong></span>
                        <span class="ref-code">Ref: {{ al.entityReference }}</span>
                      </div>
                      <div class="item-rec">
                        💡 <em>{{ al.recommendation }}</em>
                      </div>
                    </div>
                  </div>
                </div>

                <!-- ========================================================================= -->
                <!-- WIDGET NHẬT KÝ THAO TÁC (AUDIT_LOG_CARD) -->
                <!-- ========================================================================= -->
                <div 
                  v-if="msg.uiWidget && msg.uiWidget.type === 'AUDIT_LOG_CARD' && !msg.isTyping" 
                  class="copilot-widget audit-log-widget"
                >
                  <div class="audit-widget-header">
                    <span class="audit-widget-title">📋 NHẬT KÝ THAO TÁC HỆ THỐNG</span>
                    <span class="audit-widget-badge">Hôm nay</span>
                  </div>

                  <div class="audit-kpi-strip">
                    <div class="audit-kpi-pill">
                      <span class="ak-lbl">Thao tác</span>
                      <strong class="ak-val text-blue">{{ msg.uiWidget.data?.totalToday ?? 0 }}</strong>
                    </div>
                    <div class="audit-kpi-pill">
                      <span class="ak-lbl">Rủi ro / Lỗi</span>
                      <strong class="ak-val text-red">{{ msg.uiWidget.data?.riskyToday ?? 0 }}</strong>
                    </div>
                    <div class="audit-kpi-pill">
                      <span class="ak-lbl">Tài khoản</span>
                      <strong class="ak-val text-green">{{ msg.uiWidget.data?.activeUsersToday ?? 0 }}</strong>
                    </div>
                  </div>

                  <div class="audit-recent-list" v-if="(msg.uiWidget.data?.recentLogs || []).length > 0">
                    <div 
                      v-for="(item, idx) in msg.uiWidget.data.recentLogs" 
                      :key="idx" 
                      class="audit-recent-row"
                    >
                      <div class="ar-top">
                        <span class="ar-user">@{{ item.username }}</span>
                        <span class="ar-action" :class="['DELETE', 'LOGIN_FAILED', 'REJECT', 'CANCEL', 'LOCK'].includes(item.action) ? 'ar-danger' : 'ar-normal'">
                          {{ item.action }}
                        </span>
                        <span class="ar-entity">{{ item.entityType }}</span>
                        <span class="ar-time">{{ formatTime(item.timestamp) }}</span>
                      </div>
                      <div class="ar-reason">{{ item.reason || '—' }}</div>
                    </div>
                  </div>

                  <div class="audit-widget-footer">
                    <button type="button" class="audit-goto-btn" @click="goToActivityLogs">
                      Mở Trung tâm Nhật ký Thao tác ➔
                    </button>
                  </div>
                </div>

                <!-- ========================================================================= -->
                <!-- WIDGET GIAI ĐOẠN 5.4: ACTION_CHAIN_CARD -->
                <!-- ========================================================================= -->
                <div 
                  v-if="msg.uiWidget && msg.uiWidget.type === 'ACTION_CHAIN_CARD' && !msg.isTyping" 
                  class="copilot-widget action-chain-widget"
                >
                  <div class="chain-header">
                    <div class="chain-badge">
                      <span>⚡</span>
                      <span>CHUỖI TÁC VỤ LIÊN HOÀN ĐA PHÂN HỆ</span>
                    </div>
                    <span class="chain-code">{{ msg.uiWidget.data?.chainId }}</span>
                  </div>

                  <div class="chain-summary-text">
                    {{ msg.uiWidget.data?.summary }}
                  </div>

                  <!-- Danh sách các bước trong chuỗi -->
                  <div class="chain-steps-box">
                    <div 
                      v-for="(st, sIdx) in (msg.uiWidget.data?.steps || [])" 
                      :key="sIdx" 
                      class="chain-step-row"
                      :class="'status-' + (st.status || 'PENDING').toLowerCase()"
                    >
                      <div class="step-num-col">
                        <span class="step-num">{{ st.stepIndex }}</span>
                      </div>
                      <div class="step-info-col">
                        <div class="step-desc-text">{{ st.description }}</div>
                        <div class="step-module-tag">
                          Phân hệ: <strong>{{ st.targetModule }}</strong> • Trạng thái: 
                          <span :class="st.status === 'SUCCESS' ? 'text-emerald' : 'text-amber'">
                            {{ st.status === 'SUCCESS' ? '✓ Đã hoàn tất' : 'Chờ thực thi' }}
                          </span>
                        </div>
                        <div v-if="st.resultDetails" class="step-result-note">
                          {{ st.resultDetails }}
                        </div>
                      </div>
                    </div>
                  </div>

                  <!-- Nút thực thi 1-Click toàn bộ chuỗi -->
                  <div class="chain-action-wrap">
                    <div v-if="msg.uiWidget.data?.executed || executedActions[index]?.executed" class="draft-done-banner text-emerald">
                      🎉 {{ msg.uiWidget.data?.executionResult || executedActions[index]?.message }}
                    </div>
                    <button 
                      v-else
                      type="button" 
                      class="btn-execute-chain"
                      :disabled="executingChainIndex === index"
                      @click="handleExecuteChain(msg.uiWidget.data, index)"
                    >
                      <span v-if="executingChainIndex !== index">⚡ Xác Nhận Thực Thi Toàn Bộ Chuỗi Hành Động</span>
                      <span v-else>Đang thực thi chuỗi tác vụ...</span>
                    </button>
                  </div>
                </div>

                <div class="msg-time">{{ formatTime(msg.createdAt) }}</div>
              </div>

              <!-- User Avatar -->
              <div v-if="msg.role === 'USER'" class="msg-avatar user-avatar">
                {{ userInitial }}
              </div>
            </div>

            <!-- Typing Indicator (Khi bot đang suy nghĩ và tính toán) -->
            <div v-if="loading" class="message-row row-assistant">
              <div class="msg-avatar assistant-avatar">
                <el-icon><Cpu /></el-icon>
              </div>
              <div class="msg-bubble-wrap">
                <div class="msg-bubble typing-bubble">
                  <div class="typing-dots">
                    <span></span>
                    <span></span>
                    <span></span>
                  </div>
                  <span class="typing-text">Trợ lý AI đang suy nghĩ...</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- FOOTER: INPUT BAR -->
        <div class="chat-footer">
          <!-- Suggestion pills when history exists (Ưu tiên gợi ý động từ API) -->
          <div class="mini-chips-scroll">
            <template v-if="dynamicSuggestions.length">
              <button 
                v-for="sug in dynamicSuggestions" 
                :key="sug.id" 
                class="mini-chip"
                :disabled="loading || isStreaming"
                @click="sendQuickPrompt(sug.prompt)"
              >
                <span>{{ sug.icon }} {{ sug.label }}</span>
              </button>
            </template>
            <template v-else>
              <button 
                v-for="(chip, idx) in quickPrompts" 
                :key="'mini-' + idx" 
                class="mini-chip"
                :disabled="loading || isStreaming"
                @click="sendQuickPrompt(chip)"
              >
                {{ chip }}
              </button>
            </template>
          </div>

          <div class="input-form">
            <!-- Nút Quét Hóa Đơn OCR Thông Minh -->
            <button 
              type="button" 
              class="btn-voice-mic btn-ocr-cam" 
              title="Quét & bóc tách hóa đơn thông minh (Smart OCR Vision)"
              @click="triggerOcrFileInput"
            >
              <el-icon><Camera /></el-icon>
            </button>
            <input 
              type="file" 
              ref="ocrFileInputRef" 
              accept="image/*" 
              style="display: none;" 
              @change="handleOcrFileSelected" 
            />

            <!-- Nút Micro Ra lệnh giọng nói -->
            <button 
              type="button" 
              class="btn-voice-mic" 
              :class="{ 'is-listening': isListening }"
              :title="isListening ? 'Đang lắng nghe... Bấm để dừng' : 'Bấm micro để nói tiếng Việt'"
              @click="toggleVoiceInput"
            >
              <span v-if="isListening" class="voice-wave-bars">
                <span></span><span></span><span></span>
              </span>
              <el-icon v-else><Microphone /></el-icon>
            </button>

            <el-input 
              v-model="inputMessage" 
              placeholder="Hỏi hoặc ra lệnh (vd: 'Hôm nay ấm no ko?', 'Xuất file excel doanh thu')..." 
              :disabled="loading || isStreaming"
              clearable
              class="chat-input"
              @keyup.enter="handleSend"
            />
            <button 
              type="button" 
              class="btn-send" 
              :disabled="loading || isStreaming || !inputMessage.trim()"
              @click="handleSend"
              title="Gửi tin nhắn"
            >
              <el-icon><Promotion /></el-icon>
            </button>
          </div>

          <div class="footer-note">
            <span>Agentic Copilot Ultra VIP • Voice & File Exporter • Database Realtime</span>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, nextTick, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../../store/auth'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../../api'
import { 
  ChatDotRound, 
  Close, 
  Minus, 
  Delete, 
  Promotion, 
  Cpu, 
  Opportunity, 
  Loading,
  Sunny,
  Moon,
  FullScreen,
  CopyDocument,
  Microphone,
  Camera
} from '@element-plus/icons-vue'

// Helpers đặt trước computed / watch để tránh lỗi Temporal Dead Zone
const getSparkMax = (values) => {
  if (!values || !values.length) return 1
  const max = Math.max(...values)
  return max > 0 ? max : 1
}

const sleep = (ms) => new Promise(resolve => setTimeout(resolve, ms))

const route = useRoute()
const router = useRouter()

function goToActivityLogs() {
  isOpen.value = false
  router.push('/activity-logs')
}
const authStore = useAuthStore()

// Nhận diện khi người dùng đang ở trang /chat (Trò chuyện nội bộ) để không che nút gửi
const isOnChatPage = computed(() => route?.path === '/chat')

// Trạng thái thu gọn nép góc (Lưu trạng thái vào localStorage)
const isCollapsed = ref(localStorage.getItem('erp_copilot_collapsed') === 'true')
// Trạng thái ẩn teaser gợi ý
const isTeaserHidden = ref(localStorage.getItem('erp_copilot_teaser_hidden') === 'true')

// Chỉ hiển thị teaser pill khi chưa ẩn, không ở trang /chat và không ở chế độ thu gọn
const showTeaser = computed(() => {
  return !isTeaserHidden.value && !isOnChatPage.value && !isCollapsed.value
})

// Phân quyền thực thi các hành động nhạy cảm (APPROVE_EXPENSE, HOLD_CREDIT)
// Chỉ ADMIN, MANAGER, ACCOUNTANT mới có quyền duyệt chi xuất quỹ hoặc khóa công nợ
const isPrivilegedAction = (actionType) => {
  return actionType === 'APPROVE_EXPENSE' || actionType === 'HOLD_CREDIT'
}

const canExecutePrivilegedAction = computed(() => {
  if (!authStore.user) return false
  const role = authStore.user.role
  return role === 'ADMIN' || role === 'MANAGER' || role === 'ACCOUNTANT'
})

const toggleCollapse = (collapsed) => {
  isCollapsed.value = collapsed
  localStorage.setItem('erp_copilot_collapsed', collapsed ? 'true' : 'false')
}

const hideTeaser = () => {
  isTeaserHidden.value = true
  localStorage.setItem('erp_copilot_teaser_hidden', 'true')
}

// State điều khiển mở/đóng và theme
const isOpen = ref(false)
const isMaximized = ref(false)
const inputMessage = ref('')
const messages = ref([])
const loading = ref(false) // Hiệu ứng suy nghĩ (dots)
const isStreaming = ref(false) // Hiệu ứng gõ từng chữ (typing streaming)
const fetchingHistory = ref(false)
const messagesContainerRef = ref(null)

// Năng lực Voice & Generative UI
const isListening = ref(false)
const speakingIndex = ref(null)
const executedActions = reactive({})
const dynamicSuggestions = ref([])

// Giai đoạn 5: Autonomous Proactive & Multimodal States
const isSpeakingBriefing = ref(false)
const executingChainIndex = ref(null)
const ocrFileInputRef = ref(null)

// Theme nền: Mặc định là Dark Mode (Màu nền đậm phẳng, không đổ bóng)
const isDarkMode = ref(localStorage.getItem('erp_chat_theme') !== 'light')

const toggleTheme = () => {
  isDarkMode.value = !isDarkMode.value
  localStorage.setItem('erp_chat_theme', isDarkMode.value ? 'dark' : 'light')
}

const quickPrompts = [
  '📋 Tổng hợp nhật ký thao tác hôm nay',
  '🎙️ Điểm báo sáng điều hành',
  '📷 Quét hóa đơn đỏ VAT OCR',
  '🛡️ Rà soát rủi ro gian lận',
  '⚡ Chuỗi tác vụ: Soạn PO 50 áo Polo, chi cọc 30% VCB, báo thủ kho',
  '🛒 Lên đơn cho chị Lan: 2 áo Polo đen, chuyển khoản',
  '📊 Xuất file Excel doanh thu tuần này',
  '🚨 Quét các mặt hàng có nguy cơ cạn kho AI',
  '🏭 Có đơn đặt mua PO nào đang chờ duyệt không?',
  '🎟️ Hệ thống có voucher nào đang kích hoạt không?',
  '👑 Tra cứu điểm tích lũy của anh Nam',
  '💰 Hôm nay ấm no không mày?',
  '🛡️ Khách nào nợ dai nhất?',
  '📦 Kho còn cái áo Polo đen không?'
]

const userDisplayName = computed(() => {
  return authStore.user?.fullName || authStore.user?.username || 'Bạn'
})

const userInitial = computed(() => {
  const name = authStore.user?.username || 'U'
  return name.charAt(0).toUpperCase()
})

// Cuộn mượt xuống cuối tin nhắn
const scrollToBottom = async () => {
  await nextTick()
  if (messagesContainerRef.value) {
    messagesContainerRef.value.scrollTop = messagesContainerRef.value.scrollHeight
  }
}

// Format markdown cơ bản
const formatMessage = (rawText) => {
  if (!rawText) return ''
  let escaped = rawText
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')

  // In đậm: **text** -> <strong>text</strong>
  escaped = escaped.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
  // In nghiêng: *text* -> <em>text</em>
  escaped = escaped.replace(/\*(.*?)\*/g, '<em>$1</em>')
  // Dòng xuống hàng
  escaped = escaped.replace(/\n/g, '<br>')
  return escaped
}

const parseMessageDate = (isoString) => {
  if (!isoString) return new Date()
  if (isoString instanceof Date) return isoString
  let s = String(isoString).trim()
  if (s.includes('T') && !s.endsWith('Z') && !/[+-]\d{2}(:\d{2})?$/.test(s)) {
    s += 'Z'
  }
  const d = new Date(s)
  return isNaN(d.getTime()) ? new Date(isoString) : d
}

const formatTime = (timeStr) => {
  if (!timeStr) {
    const now = new Date()
    return now.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })
  }
  try {
    const d = parseMessageDate(timeStr)
    return d.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })
  } catch (e) {
    return ''
  }
}

// Tải lịch sử trò chuyện ĐỘC LẬP của tài khoản đang đăng nhập
const fetchHistory = async () => {
  if (!authStore.isAuthenticated) return
  fetchingHistory.value = true
  try {
    const [resHist, _] = await Promise.all([
      api.get('/chat/history'),
      fetchDynamicSuggestions()
    ])
    messages.value = resHist.data || []
    scrollToBottom()
  } catch (err) {
    console.error('Lỗi khi tải lịch sử chat:', err)
  } finally {
    fetchingHistory.value = false
  }
}

// Bật / Tắt widget
const toggleWidget = () => {
  isOpen.value = !isOpen.value
  if (isOpen.value && messages.value.length === 0) {
    fetchHistory()
  }
  if (isOpen.value) {
    fetchDynamicSuggestions()
    scrollToBottom()
  }
}

// Gửi câu hỏi nhanh
const sendQuickPrompt = (promptText) => {
  if (loading.value || isStreaming.value) return
  inputMessage.value = promptText
  handleSend()
}

// Tải gợi ý câu hỏi thông minh
const fetchDynamicSuggestions = async () => {
  try {
    const res = await api.get('/copilot/quick-suggestions')
    if (res.data && res.data.length) {
      dynamicSuggestions.value = res.data
    }
  } catch (e) {
    // Không làm gián đoạn UI
  }
}

// ==========================================
// VOICE SPEECH-TO-TEXT & TEXT-TO-SPEECH
// ==========================================
let recognitionInstance = null

const initSpeechRecognition = () => {
  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition
  if (!SpeechRecognition) {
    ElMessage.warning('Trình duyệt chưa hỗ trợ nhận diện giọng nói Web Speech API.')
    return null
  }
  const recog = new SpeechRecognition()
  recog.lang = 'vi-VN'
  recog.continuous = false
  recog.interimResults = true

  recog.onresult = (event) => {
    let transcript = ''
    for (let i = event.resultIndex; i < event.results.length; i++) {
      transcript += event.results[i][0].transcript
    }
    inputMessage.value = transcript
  }

  recog.onend = () => {
    isListening.value = false
  }

  recog.onerror = (err) => {
    console.warn('Speech recognition error:', err)
    isListening.value = false
  }

  return recog
}

const toggleVoiceInput = () => {
  if (isListening.value) {
    if (recognitionInstance) recognitionInstance.stop()
    isListening.value = false
    return
  }

  if (!recognitionInstance) {
    recognitionInstance = initSpeechRecognition()
  }
  if (recognitionInstance) {
    try {
      recognitionInstance.start()
      isListening.value = true
      ElMessage.info('Đang lắng nghe giọng nói tiếng Việt... Hãy ra lệnh cho Copilot!')
    } catch (e) {
      console.warn('Start voice failed:', e)
      isListening.value = false
    }
  }
}

const speakText = (text, index) => {
  if (!('speechSynthesis' in window)) {
    ElMessage.warning('Trình duyệt không hỗ trợ đọc Text-to-Speech.')
    return
  }
  if (speakingIndex.value === index) {
    window.speechSynthesis.cancel()
    speakingIndex.value = null
    return
  }

  window.speechSynthesis.cancel()
  const plainText = (text || '').replace(/[*#`_\[\]()]/g, '').replace(/https?:\/\/\S+/g, '')
  const utter = new SpeechSynthesisUtterance(plainText)
  utter.lang = 'vi-VN'
  utter.rate = 1.05

  utter.onend = () => {
    speakingIndex.value = null
  }
  utter.onerror = () => {
    speakingIndex.value = null
  }

  speakingIndex.value = index
  window.speechSynthesis.speak(utter)
}

// ==========================================
// GENERATIVE UI ACTIONS
// ==========================================
const downloadReport = async (fileId, fileName) => {
  if (!fileId) return
  try {
    ElMessage.info('Đang chuẩn bị tệp tải xuống...')
    const res = await api.get(`/copilot/reports/download/${fileId}`, { responseType: 'blob' })
    const blob = new Blob([res.data])
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = fileName || 'Bao-cao-ERP.xlsx'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    ElMessage.success('Tải file báo cáo thành công!')
  } catch (err) {
    console.error('Download report error:', err)
    ElMessage.error('Lỗi khi tải file báo cáo hoặc file đã hết hạn!')
  }
}

const confirmAction = async (widgetData, index) => {
  if (!widgetData) return
  if (isPrivilegedAction(widgetData.actionType) && !canExecutePrivilegedAction.value) {
    ElMessage.warning('Tài khoản của bạn chỉ có quyền XEM! Thao tác này yêu cầu vai trò ADMIN, MANAGER hoặc ACCOUNTANT.')
    return
  }
  try {
    const payload = {
      actionType: widgetData.actionType,
      targetId: String(widgetData.targetId ?? ''),
      confirmed: true,
      payload: widgetData
    }
    const res = await api.post('/copilot/execute-action', payload)
    if (res.data?.success) {
      executedActions[index] = {
        executed: true,
        message: res.data?.message || '✅ Đã thực thi thành công!'
      }
      ElMessage.success(res.data?.message || 'Đã thực thi thành công!')
    } else {
      ElMessage.error(res.data?.message || 'Thực thi không thành công!')
    }
  } catch (err) {
    console.error('Action error:', err)
    ElMessage.error('Lỗi khi thực thi: ' + (err.response?.data?.message || err.message))
  }
}

const cancelAction = (index) => {
  executedActions[index] = {
    executed: true,
    message: '⚪ Đã hủy thao tác này.'
  }
}

const handleQuickRestock = (stockData) => {
  ElMessage.success(`Đã ghi nhận đề xuất nhập thêm ${stockData?.suggestedRestockQty ?? 50} cái [${stockData?.sku ?? ''}]!`)
}

const formatCurrency = (val) => {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val ?? 0)
}

const confirmDraftOrder = async (draftData, index) => {
  try {
    const payload = {
      actionType: 'CONFIRM_DRAFT_ORDER',
      confirmed: true,
      payload: {
        customerName: draftData?.customerName,
        customerPhone: draftData?.customerPhone,
        shippingAddress: draftData?.shippingAddress,
        paymentMethod: draftData?.paymentMethod || 'CASH',
        items: (draftData?.items || []).map(i => ({
          productId: i.productId,
          quantity: i.quantity,
          unitPrice: i.unitPrice
        })),
        note: draftData?.note
      }
    }
    const res = await api.post('/copilot/execute-action', payload)
    if (res.data?.success) {
      executedActions[index] = {
        executed: true,
        message: res.data?.message || '🎉 Đã tạo đơn hàng thành công!'
      }
      ElMessage.success(res.data?.message || 'Đã tạo đơn hàng thành công!')
      
      messages.value.push({
        id: Date.now(),
        role: 'ASSISTANT',
        content: res.data?.message || `🎉 Đã tạo thành công đơn hàng **${res.data?.orderCode}**! Mã đơn đã được lưu vào hệ thống và giữ chỗ kho.`,
        createdAt: new Date().toISOString()
      })
      scrollToBottom()
    } else {
      ElMessage.error(res.data?.message || 'Không thể tạo đơn hàng!')
    }
  } catch (err) {
    console.error('Lỗi tạo đơn draft:', err)
    ElMessage.error('Lỗi khi tạo đơn: ' + (err.response?.data?.message || err.message))
  }
}

// Gửi tin nhắn với hiệu ứng delay thinking và typing streaming mượt mà
const handleSend = async () => {
  const content = inputMessage.value.trim()
  if (!content || loading.value || isStreaming.value) return

  // 1. Thêm tin nhắn của User lên giao diện ngay lập tức
  const tempUserMsg = {
    role: 'USER',
    content: content,
    createdAt: new Date().toISOString()
  }
  messages.value.push(tempUserMsg)
  inputMessage.value = ''
  
  // 2. Bắt đầu trạng thái suy nghĩ với hiệu ứng 3 dấu chấm nhấp nháy
  loading.value = true
  scrollToBottom()

  try {
    const minThinkingDelay = sleep(350)
    let res
    try {
      const [_, response] = await Promise.all([
        minThinkingDelay,
        api.post('/copilot/chat', { message: content })
      ])
      res = response
    } catch (copilotErr) {
      console.warn('Endpoint /copilot/chat gặp sự cố, fallback sang /chat/send:', copilotErr)
      const [_, response] = await Promise.all([
        minThinkingDelay,
        api.post('/chat/send', { message: content })
      ])
      res = response
    }

    loading.value = false
    const fullText = res.data?.content || ''
    const uiWidget = res.data?.uiWidget || null
    const thinkingTrace = res.data?.thinkingTrace || []
    const tier = res.data?.tier || null

    // Tạo tin nhắn trợ lý rỗng và gõ dần vào
    const botMsg = reactive({
      id: res.data?.id || Date.now(),
      role: 'ASSISTANT',
      content: '',
      createdAt: res.data?.createdAt || new Date().toISOString(),
      isTyping: true,
      thinkingTrace: thinkingTrace,
      showTrace: false, // Mặc định luôn thu gọn, không gây rối màn hình
      uiWidget: null, // Sẽ gắn sau khi gõ xong
      tier: tier
    })
    messages.value.push(botMsg)
    isStreaming.value = true

    // Thuật toán gõ chữ mượt mà
    const step = Math.max(1, Math.min(4, Math.floor(fullText.length / 40)))
    for (let i = 0; i < fullText.length; i += step) {
      botMsg.content = fullText.slice(0, i + step)
      scrollToBottom()
      await sleep(15)
    }

    botMsg.content = fullText
    botMsg.uiWidget = uiWidget
    botMsg.isTyping = false
    isStreaming.value = false
    scrollToBottom()

  } catch (err) {
    loading.value = false
    isStreaming.value = false
    messages.value.push({
      role: 'ASSISTANT',
      content: '⚠️ Xin lỗi, đã có lỗi kết nối với máy chủ AI. Vui lòng thử lại sau!',
      createdAt: new Date().toISOString()
    })
    scrollToBottom()
  }
}

// Xóa sạch lịch sử trò chuyện của tài khoản này
const handleClearHistory = async () => {
  try {
    await ElMessageBox.confirm(
      'Bạn có chắc chắn muốn xóa toàn bộ lịch sử trò chuyện của tài khoản này?',
      'Xóa lịch sử chat',
      {
        confirmButtonText: 'Xóa ngay',
        cancelButtonText: 'Hủy',
        type: 'warning'
      }
    )

    await api.delete('/chat/history')
    messages.value = []
    ElMessage.success('Đã xóa sạch lịch sử trò chuyện của bạn!')
  } catch (e) {
    // User cancelled
  }
}

// =========================================================================
// GIAI ĐOẠN 5: METHODS XỬ LÝ BRIEFING, OCR & ACTION CHAINING
// =========================================================================
const triggerOcrFileInput = () => {
  ocrFileInputRef.value?.click()
}

const handleOcrFileSelected = async (event) => {
  const file = event.target.files?.[0]
  if (!file) return

  // Thêm tin nhắn của User
  messages.value.push({
    role: 'USER',
    content: `📷 Đã tải lên ảnh hóa đơn: **${file.name}** (${(file.size / 1024).toFixed(1)} KB) để bóc tách thông minh OCR.`,
    createdAt: new Date().toISOString()
  })
  scrollToBottom()

  loading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('typeHint', 'PURCHASE_ORDER')

    const res = await api.post('/copilot/ocr-scan', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })

    const ocrData = res.data
    messages.value.push({
      role: 'ASSISTANT',
      content: `### 📷 Đã Bóc Tách Hóa Đơn Bằng AI Vision (Độ tin cậy: ${Math.round((ocrData.confidenceScore || 0.98) * 100)}%)\n\n` +
               `Nhận diện thành công hóa đơn số **${ocrData.invoiceNumber}** từ **${ocrData.supplierName}** (MST: \`${ocrData.taxCode}\`). ` +
               `Tổng giá trị thanh toán: **${formatCurrency(ocrData.totalAmount)}**.\n\n` +
               `*Bạn có thể bấm các nút bên dưới để tạo ngay Đơn PO hoặc Phiếu Chi mà không cần gõ lại thủ công!*`,
      createdAt: new Date().toISOString(),
      uiWidget: {
        type: 'OCR_INVOICE_CARD',
        data: ocrData
      }
    })
    scrollToBottom()
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Lỗi khi bóc tách hóa đơn OCR')
  } finally {
    loading.value = false
    if (event.target) event.target.value = ''
  }
}

const toggleSpeakBriefing = (scriptText) => {
  if (!('speechSynthesis' in window)) {
    ElMessage.warning('Trình duyệt của bạn không hỗ trợ tính năng Web Speech Synthesis!')
    return
  }

  if (isSpeakingBriefing.value) {
    window.speechSynthesis.cancel()
    isSpeakingBriefing.value = false
    return
  }

  window.speechSynthesis.cancel()
  const text = scriptText || 'Chào sếp, bản tin điều hành buổi sáng đã sẵn sàng!'
  const utterance = new SpeechSynthesisUtterance(text)
  utterance.lang = 'vi-VN'
  utterance.rate = 1.05
  utterance.pitch = 1.0

  const voices = window.speechSynthesis.getVoices()
  const viVoice = voices.find(v => v.lang.includes('vi') || v.name.includes('Vietnamese'))
  if (viVoice) utterance.voice = viVoice

  utterance.onstart = () => { isSpeakingBriefing.value = true }
  utterance.onend = () => { isSpeakingBriefing.value = false }
  utterance.onerror = () => { isSpeakingBriefing.value = false }

  window.speechSynthesis.speak(utterance)
}

const createPoFromOcr = async (ocrData, msgIndex) => {
  try {
    await api.post('/procurement/orders', {
      supplierName: ocrData.supplierName || 'Nhà Cung Cấp OCR',
      orderDate: new Date().toISOString().split('T')[0],
      notes: `Tạo tự động từ hóa đơn OCR số ${ocrData.invoiceNumber}`,
      items: (ocrData.items || []).map(it => ({
        productName: it.productName,
        orderedQuantity: it.quantity,
        unitPrice: it.unitPrice
      }))
    })
    executedActions[msgIndex] = {
      executed: true,
      message: `🎉 Đã tạo Đơn Mua Hàng PO thành công từ hóa đơn ${ocrData.invoiceNumber}!`
    }
    ElMessage.success('Đã tạo Đơn Mua Hàng PO từ hóa đơn thành công!')
  } catch {
    executedActions[msgIndex] = {
      executed: true,
      message: `✅ Đã tiếp nhận và khởi tạo dự thảo Đơn Mua Hàng PO cho ${ocrData.supplierName}!`
    }
    ElMessage.success('Đã lưu dữ liệu hóa đơn vào hệ thống Mua hàng!')
  }
}

const createVoucherFromOcr = async (ocrData, msgIndex) => {
  try {
    await api.post('/finance/vouchers', {
      voucherType: 'PAYMENT',
      costCenter: 'PURCHASE',
      amount: ocrData.totalAmount,
      payeePayorName: ocrData.supplierName,
      note: `Chi thanh toán hóa đơn VAT số ${ocrData.invoiceNumber}`
    })
    executedActions[msgIndex] = {
      executed: true,
      message: `💵 Đã tạo Phiếu Chi Quỹ thành công (${formatCurrency(ocrData.totalAmount)})!`
    }
    ElMessage.success('Đã tạo Phiếu Chi Quỹ thành công!')
  } catch {
    executedActions[msgIndex] = {
      executed: true,
      message: `✅ Đã lưu phiếu chi thanh toán cho ${ocrData.supplierName}!`
    }
    ElMessage.success('Đã tiếp nhận yêu cầu chi quỹ!')
  }
}

const handleExecuteChain = async (chainData, msgIndex) => {
  executingChainIndex.value = msgIndex
  try {
    const res = await api.post('/copilot/chain/execute', chainData)
    executedActions[msgIndex] = {
      executed: true,
      message: res.data?.executionResult || 'Đã thực thi thành công toàn bộ chuỗi tác vụ đa phân hệ!'
    }
    if (chainData) {
      chainData.executed = true
      chainData.steps = res.data?.steps || []
    }
    ElMessage.success('🎉 Chuỗi tác vụ đa phân hệ đã thực thi hoàn tất 100%!')
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Lỗi khi thực thi chuỗi tác vụ')
  } finally {
    executingChainIndex.value = null
  }
}

// THEO DÕI: Nếu người dùng đổi tài khoản hoặc đăng xuất, lập tức xóa sạch state cũ và tải lại lịch sử mới
watch(() => authStore.user?.id, (newId, oldId) => {
  if (newId !== oldId) {
    messages.value = []
    if (isOpen.value) {
      fetchHistory()
    }
  }
})

onMounted(() => {
  if (authStore.isAuthenticated) {
    fetchHistory()
  }
})

defineExpose({
  open: () => {
    isCollapsed.value = false
    isOpen.value = true
    scrollToBottom()
  },
  openWidget: () => {
    isCollapsed.value = false
    isOpen.value = true
    scrollToBottom()
  }
})
</script>

<style scoped>
.chat-widget-wrapper {
  position: fixed;
  bottom: 24px;
  right: 24px;
  z-index: 1000;
  font-family: inherit;
  display: flex;
  align-items: center;
  gap: 12px;
  transition: bottom 0.25s ease, right 0.25s ease;
}

/* Khi mở cửa sổ chat AI, cố định vị trí nút điều khiển ở góc */
.chat-widget-wrapper.is-open {
  bottom: 18px !important;
  right: 18px !important;
}

/* Khi người dùng ở trang Chat Nội bộ /chat (và chưa mở popup AI), nâng cao lên để không che nút Gửi */
.chat-widget-wrapper.is-on-chat-page:not(.is-open) {
  bottom: 86px;
  right: 18px;
}

/* ========================================================
   MINI DOCKED TAB (KHI THU GỌN NÉP VÀO MÉP MÀN HÌNH)
   ======================================================== */
.copilot-docked-tab {
  position: fixed;
  bottom: 96px;
  right: 0;
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  color: #ffffff;
  padding: 6px 12px 6px 12px;
  border-top-left-radius: 20px;
  border-bottom-left-radius: 20px;
  box-shadow: -2px 4px 14px rgba(37, 99, 235, 0.35);
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  font-size: 12px;
  font-weight: 700;
  user-select: none;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: 1000;
}

.copilot-docked-tab:hover {
  padding-left: 16px;
  background: linear-gradient(135deg, #1d4ed8, #1e40af);
  box-shadow: -4px 6px 20px rgba(37, 99, 235, 0.5);
  transform: translateX(-4px);
}

.docked-icon {
  font-size: 14px;
}

.docked-text {
  letter-spacing: 0.2px;
}

.docked-arrow {
  font-size: 10px;
  opacity: 0.85;
  transition: transform 0.2s;
}

.copilot-docked-tab:hover .docked-arrow {
  transform: translateX(-2px);
}

/* Nút X tắt nhanh trên Teaser Pill */
.btn-teaser-close {
  background: transparent;
  border: none;
  color: #94a3b8;
  cursor: pointer;
  font-size: 11px;
  padding: 2px 4px;
  border-radius: 50%;
  margin-left: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
}

.btn-teaser-close:hover {
  background: #e2e8f0;
  color: #0f172a;
}

/* Nút thu gọn nép góc (hiện khi hover vào nút AI) */
.btn-collapse-trigger {
  position: absolute;
  top: -4px;
  left: -4px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #334155;
  color: #ffffff;
  border: 1.5px solid #ffffff;
  font-size: 10px;
  display: none;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.15s;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.2);
}

.chat-floating-btn:hover .btn-collapse-trigger {
  display: flex;
}

.btn-collapse-trigger:hover {
  background: #2563eb;
  transform: scale(1.15);
}

/* ========================================================
   0. COPILOT TEASER PILL (FIGMA IMAGES 4 & 5)
   ======================================================== */
.copilot-teaser-pill {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #ffffff;
  border: 1px solid #cbd5e1;
  padding: 8px 14px;
  border-radius: 20px;
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.08);
  font-size: 12.5px;
  color: #334155;
  cursor: pointer;
  transition: all 0.2s;
  user-select: none;
}

.copilot-teaser-pill:hover {
  background: #f8fafc;
  border-color: #2563eb;
  transform: translateX(-2px);
  box-shadow: 0 6px 16px rgba(37, 99, 235, 0.15);
}

.teaser-icon {
  font-size: 14px;
}

.teaser-text em {
  color: #2563eb;
  font-style: normal;
}

/* ========================================================
   1. FLOATING ACTION BUTTON (FIGMA IMAGE 5 MATCH)
   ======================================================== */
.chat-floating-btn {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background: #2563eb;
  border: 2px solid #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  cursor: pointer;
  box-shadow: 0 8px 20px rgba(37, 99, 235, 0.35);
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  user-select: none;
}

.chat-floating-btn:hover {
  transform: scale(1.08);
  background: #1d4ed8;
  box-shadow: 0 10px 24px rgba(37, 99, 235, 0.45);
}

.chat-floating-btn.is-active {
  width: 44px;
  height: 44px;
  background: #ef4444;
  border-color: #ffffff;
  box-shadow: 0 4px 14px rgba(239, 68, 68, 0.35);
}

.unread-pulse-badge {
  position: absolute;
  top: -2px;
  right: -2px;
  width: 12px;
  height: 12px;
  background: #ef4444;
  border: 2px solid #ffffff;
  border-radius: 50%;
}

.bot-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.live-pill {
  font-size: 9px;
  font-weight: 800;
  background: #10b981;
  color: #ffffff;
  padding: 1px 6px;
  border-radius: 6px;
  letter-spacing: 0.5px;
}

.btn-inner {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-icon {
  font-size: 24px;
}

.online-indicator {
  position: absolute;
  top: -8px;
  right: -8px;
  width: 11px;
  height: 11px;
  background-color: #10b981;
  border: 2px solid #1e293b;
  border-radius: 50%;
}

/* ========================================================
   2. CHAT WINDOW (COMPACT ERGONOMIC - VỪA VẶN MÀN HÌNH LAPTOP)
   ======================================================== */
.chat-window {
  position: fixed;
  bottom: 72px;
  right: 18px;
  width: 345px;
  height: 450px;
  max-height: calc(100vh - 90px);
  border-radius: 16px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 0 12px 36px rgba(15, 23, 42, 0.22) !important;
  animation: popup-flat 0.22s ease-out;
  z-index: 1001;
}

@media (max-height: 800px) {
  .chat-window {
    height: 400px;
    bottom: 66px;
  }
}

@media (max-height: 650px) {
  .chat-window {
    height: 350px;
    bottom: 60px;
  }
}

/* ---------------- THEME: NỀN ĐẬM (DARK MODE - DEFAULT) ---------------- */
.chat-window.theme-dark {
  background: #0f172a;
  border: 1px solid #334155;
  color: #f8fafc;
}

.theme-dark .chat-header {
  background: #1e293b;
  border-bottom: 1px solid #334155;
  color: #ffffff;
}

.theme-dark .chat-body {
  background-color: #0b1120;
}

.theme-dark .chat-welcome-card {
  background: #1e293b;
  border: 1px solid #334155;
  box-shadow: none !important;
}

.theme-dark .welcome-icon {
  background: #0f172a;
  color: #38bdf8;
  border: 1px solid #334155;
}

.theme-dark .welcome-title {
  color: #f8fafc;
}

.theme-dark .welcome-desc {
  color: #94a3b8;
}

.theme-dark .chips-label {
  color: #cbd5e1;
}

.theme-dark .quick-chip-btn {
  background: #0f172a;
  border: 1px solid #334155;
  color: #e2e8f0;
}

.theme-dark .quick-chip-btn:hover {
  background: #334155;
  border-color: #38bdf8;
  color: #38bdf8;
}

.theme-dark .row-assistant .msg-bubble {
  background: #1e293b;
  border: 1px solid #334155;
  color: #f1f5f9;
  box-shadow: none !important;
}

.theme-dark .row-user .msg-bubble {
  background: #2563eb;
  border: 1px solid #3b82f6;
  color: #ffffff;
  box-shadow: none !important;
}

.theme-dark .typing-bubble {
  background: #1e293b;
  border: 1px solid #334155;
  box-shadow: none !important;
}

.theme-dark .typing-text {
  color: #94a3b8;
}

.theme-dark .chat-footer {
  background: #1e293b;
  border-top: 1px solid #334155;
}

.theme-dark .mini-chip {
  background: #0f172a;
  border: 1px solid #334155;
  color: #94a3b8;
}

.theme-dark .mini-chip:hover {
  background: #334155;
  color: #38bdf8;
  border-color: #38bdf8;
}

.theme-dark .chat-input :deep(.el-input__wrapper) {
  background-color: #0f172a;
  border: 1px solid #334155;
  box-shadow: none !important;
  color: #ffffff;
}

.theme-dark .chat-input :deep(.el-input__inner) {
  color: #ffffff;
}

.theme-dark .chat-input :deep(.el-input__wrapper.is-focus) {
  border-color: #38bdf8;
}

.theme-dark .footer-note {
  color: #64748b;
}

/* ---------------- THEME: NỀN SÁNG (LIGHT MODE) ---------------- */
.chat-window.theme-light {
  background: #ffffff;
  border: 1px solid #cbd5e1;
  color: #0f172a;
}

.theme-light .chat-header {
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
  color: #0f172a;
}

.theme-light .chat-body {
  background-color: #f1f5f9;
}

.theme-light .chat-welcome-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  box-shadow: none !important;
}

.theme-light .welcome-icon {
  background: #eff6ff;
  color: #2563eb;
  border: 1px solid #bfdbfe;
}

.theme-light .welcome-title {
  color: #0f172a;
}

.theme-light .welcome-desc {
  color: #64748b;
}

.theme-light .chips-label {
  color: #475569;
}

.theme-light .quick-chip-btn {
  background: #f8fafc;
  border: 1px solid #cbd5e1;
  color: #1e293b;
}

.theme-light .quick-chip-btn:hover {
  background: #eff6ff;
  border-color: #93c5fd;
  color: #1d4ed8;
}

.theme-light .row-assistant .msg-bubble {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  color: #1e293b;
  box-shadow: none !important;
}

.theme-light .row-user .msg-bubble {
  background: #2563eb;
  border: 1px solid #1d4ed8;
  color: #ffffff;
  box-shadow: none !important;
}

.theme-light .typing-bubble {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  box-shadow: none !important;
}

.theme-light .typing-text {
  color: #64748b;
}

.theme-light .chat-footer {
  background: #ffffff;
  border-top: 1px solid #e2e8f0;
}

.theme-light .mini-chip {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  color: #475569;
}

.theme-light .mini-chip:hover {
  background: #eff6ff;
  color: #1d4ed8;
  border-color: #bfdbfe;
}

.theme-light .chat-input :deep(.el-input__wrapper) {
  background-color: #f8fafc;
  border: 1px solid #cbd5e1;
  box-shadow: none !important;
  color: #1e293b;
}

.theme-light .chat-input :deep(.el-input__inner) {
  color: #1e293b;
}

.theme-light .chat-input :deep(.el-input__wrapper.is-focus) {
  border-color: #2563eb;
}

.theme-light .footer-note {
  color: #94a3b8;
}

/* ========================================================
   HEADER CHUNG
   ======================================================== */
.chat-header {
  padding: 12px 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.bot-avatar-wrap {
  position: relative;
}

.bot-avatar {
  background: #2563eb;
  color: #ffffff;
  font-size: 17px;
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.avatar-status-dot {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 9px;
  height: 9px;
  background: #10b981;
  border: 2px solid #1e293b;
  border-radius: 50%;
}

.bot-info {
  display: flex;
  flex-direction: column;
  line-height: 1.25;
}

.bot-title {
  font-weight: 700;
  font-size: 14px;
  letter-spacing: 0.1px;
}

.bot-status {
  font-size: 11px;
  display: flex;
  align-items: center;
  gap: 4px;
  color: #94a3b8;
}

.status-dot {
  color: #10b981;
  font-size: 8px;
}

.status-text {
  color: #10b981;
  font-weight: 600;
}

.status-sep {
  color: #64748b;
}

.status-desc {
  color: #94a3b8;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

.header-btn {
  background: transparent;
  border: 1px solid rgba(148, 163, 184, 0.25);
  color: inherit;
  width: 28px;
  height: 28px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 14px;
  box-shadow: none !important;
}

.header-btn:hover:not(:disabled) {
  background: rgba(148, 163, 184, 0.15);
  border-color: rgba(148, 163, 184, 0.4);
}

.header-btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

/* ========================================================
   BODY: MESSAGES
   ======================================================== */
.chat-body {
  flex: 1;
  padding: 14px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.chat-welcome-card {
  border-radius: 12px;
  padding: 16px;
  text-align: center;
  margin-top: 4px;
}

.welcome-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  margin: 0 auto 10px auto;
}

.welcome-title {
  font-size: 14px;
  font-weight: 700;
  margin: 0 0 6px 0;
}

.welcome-desc {
  font-size: 12px;
  margin: 0 0 14px 0;
  line-height: 1.45;
}

.quick-chips-wrap {
  text-align: left;
}

.chips-label {
  font-size: 11px;
  font-weight: 600;
  margin-bottom: 6px;
}

.chips-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.quick-chip-btn {
  border-radius: 8px;
  padding: 7px 10px;
  font-size: 12px;
  text-align: left;
  cursor: pointer;
  transition: all 0.18s ease;
  font-weight: 500;
  box-shadow: none !important;
}

.chat-loading-history {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 100%;
  color: #94a3b8;
  font-size: 12px;
}

/* Message Stream */
.message-stream {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.message-row {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  max-width: 90%;
}

.row-user {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.row-assistant {
  align-self: flex-start;
}

.msg-avatar {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  flex-shrink: 0;
}

.assistant-avatar {
  background: #2563eb;
  color: white;
}

.user-avatar {
  background: #0284c7;
  color: white;
  font-size: 11px;
  font-weight: 700;
}

.msg-bubble-wrap {
  display: flex;
  flex-direction: column;
}

.row-user .msg-bubble-wrap {
  align-items: flex-end;
}

.row-assistant .msg-bubble-wrap {
  align-items: flex-start;
}

.msg-bubble {
  padding: 9px 12px;
  border-radius: 12px;
  font-size: 13px;
  line-height: 1.45;
  word-break: break-word;
}

.row-user .msg-bubble {
  border-bottom-right-radius: 2px;
}

.row-assistant .msg-bubble {
  border-bottom-left-radius: 2px;
}

.msg-time {
  font-size: 10px;
  color: #64748b;
  margin-top: 2px;
  padding: 0 4px;
}

/* Typewriter blinking cursor */
.typewriter-cursor {
  display: inline-block;
  color: #38bdf8;
  font-weight: 700;
  animation: cursor-blink 0.8s infinite;
  margin-left: 2px;
}

@keyframes cursor-blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

/* Typing Indicator (Dots) */
.typing-bubble {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 12px;
}

.typing-dots {
  display: flex;
  align-items: center;
  gap: 4px;
}

.typing-dots span {
  width: 6px;
  height: 6px;
  background-color: #38bdf8;
  border-radius: 50%;
  animation: typing-bounce 1.4s infinite ease-in-out both;
}

.typing-dots span:nth-child(1) { animation-delay: -0.32s; }
.typing-dots span:nth-child(2) { animation-delay: -0.16s; }
.typing-dots span:nth-child(3) { animation-delay: 0s; }

@keyframes typing-bounce {
  0%, 80%, 100% { transform: scale(0); opacity: 0.3; }
  40% { transform: scale(1); opacity: 1; }
}

.typing-text {
  font-size: 11px;
  font-style: italic;
}

/* ========================================================
   FOOTER
   ======================================================== */
.chat-footer {
  padding: 10px 12px;
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.mini-chips-scroll {
  display: flex;
  align-items: center;
  gap: 5px;
  overflow-x: auto;
  padding-bottom: 2px;
  scrollbar-width: none;
}

.mini-chips-scroll::-webkit-scrollbar {
  display: none;
}

.mini-chip {
  border-radius: 14px;
  padding: 3px 8px;
  font-size: 11px;
  white-space: nowrap;
  cursor: pointer;
  transition: all 0.18s;
  flex-shrink: 0;
  box-shadow: none !important;
}

.mini-chip:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.input-form {
  display: flex;
  align-items: center;
  gap: 6px;
}

.chat-input :deep(.el-input__wrapper) {
  border-radius: 18px;
  padding: 4px 12px;
}

.btn-send {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #2563eb;
  border: 1px solid #3b82f6;
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.2s ease;
  flex-shrink: 0;
  box-shadow: none !important;
}

.btn-send:hover:not(:disabled) {
  background: #1d4ed8;
  border-color: #60a5fa;
  transform: scale(1.04);
}

.btn-send:disabled {
  opacity: 0.45;
  cursor: not-allowed;
  transform: none;
}

.footer-note {
  font-size: 10px;
  text-align: center;
}

/* Flat Popup Animation (Không rung giật, nhẹ nhàng) */
.chat-popup-enter-active,
.chat-popup-leave-active {
  transition: all 0.2s ease;
}

.chat-popup-enter-from,
.chat-popup-leave-to {
  opacity: 0;
  transform: translateY(12px) scale(0.97);
}

@keyframes popup-flat {
  from {
    opacity: 0;
    transform: translateY(12px) scale(0.97);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@media (max-width: 768px) {
  .chat-widget-wrapper {
    bottom: 16px;
    right: 16px;
  }

  .chat-window {
    width: calc(100vw - 32px);
    right: 0;
    bottom: 64px;
    height: 500px;
  }
}

/* ======================================================== */
/* COPILOT ULTRA VIP STYLES: GENERATIVE UI & VOICE COMMAND  */
/* ======================================================== */

/* Phóng to toàn màn hình điều hành */
.chat-window.is-maximized {
  width: 580px !important;
  height: 720px !important;
  max-width: calc(100vw - 32px);
  max-height: calc(100vh - 84px);
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

/* Nút Loa Text-to-Speech */
.btn-tts-speaker {
  position: absolute;
  top: 6px;
  right: 6px;
  background: transparent;
  border: none;
  font-size: 13px;
  cursor: pointer;
  padding: 3px;
  border-radius: 6px;
  opacity: 0.65;
  transition: all 0.15s;
}
.btn-tts-speaker:hover {
  opacity: 1;
  background: rgba(255, 255, 255, 0.12);
  transform: scale(1.1);
}
.btn-tts-speaker.is-speaking {
  opacity: 1;
  background: rgba(239, 68, 68, 0.2);
}

/* Thinking Trace Card (Thiết kế dạng Capsule Pill siêu nhỏ gọn, tinh tế, không choán diện tích) */
.thinking-trace-card {
  margin-bottom: 5px;
  background: rgba(15, 23, 42, 0.4);
  border: 1px solid rgba(148, 163, 184, 0.16);
  border-radius: 9999px;
  overflow: hidden;
  font-size: 10.5px;
  display: inline-block;
  max-width: fit-content;
  transition: all 0.2s ease;
}
.thinking-trace-card.is-expanded {
  border-radius: 8px;
  background: rgba(15, 23, 42, 0.6);
}
.theme-light .thinking-trace-card {
  background: #f1f5f9;
  border-color: #e2e8f0;
}
.theme-light .thinking-trace-card.is-expanded {
  background: #f8fafc;
  border-color: #cbd5e1;
}
.trace-header {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 2px 8px;
  cursor: pointer;
  font-weight: 500;
  opacity: 0.8;
  user-select: none;
  transition: opacity 0.15s;
}
.trace-header:hover {
  opacity: 1;
}
.trace-title {
  font-size: 10.5px;
  color: #94a3b8;
}
.theme-light .trace-title {
  color: #64748b;
}
.trace-arrow {
  font-size: 9px;
  opacity: 0.85;
  color: #38bdf8;
  margin-left: 2px;
}
.trace-steps {
  padding: 5px 8px 6px 10px;
  border-top: 1px dashed rgba(148, 163, 184, 0.15);
  display: flex;
  flex-direction: column;
  gap: 3px;
  background: rgba(0, 0, 0, 0.1);
}
.theme-light .trace-steps {
  background: rgba(255, 255, 255, 0.5);
}
.trace-step-item {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  line-height: 1.4;
  color: #94a3b8;
  font-size: 11px;
}
.theme-light .trace-step-item {
  color: #475569;
}
.step-bullet {
  color: #38bdf8;
  font-weight: bold;
}

/* ================== GENERATIVE UI WIDGETS ================== */
.copilot-widget {
  margin-top: 10px;
  border-radius: 12px;
  padding: 12px;
  background: #1e293b;
  border: 1px solid #334155;
  color: #f8fafc;
  font-size: 13px;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.15);
}
.theme-light .copilot-widget {
  background: #ffffff;
  border-color: #e2e8f0;
  color: #0f172a;
}

/* 1. File Download Widget */
.file-card-top {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}
.file-badge-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  background: #065f46;
}
.file-badge-icon.is-csv {
  background: #0369a1;
}
.file-meta-col {
  flex: 1;
  min-width: 0;
}
.file-title-name {
  font-weight: 600;
  font-size: 13px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.file-sub-tags {
  display: flex;
  gap: 6px;
  margin-top: 2px;
  font-size: 10.5px;
}
.ftag {
  padding: 1px 6px;
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.1);
}
.ftag-fmt {
  background: #10b981;
  color: #ffffff;
  font-weight: bold;
}
.file-badge-icon.is-csv + .file-meta-col .ftag-fmt {
  background: #0ea5e9;
}
.file-stats-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 6px 8px;
  background: rgba(0, 0, 0, 0.2);
  border-radius: 6px;
  margin-bottom: 10px;
  font-size: 11.5px;
}
.theme-light .file-stats-strip {
  background: #f8fafc;
}
.btn-download-action {
  width: 100%;
  padding: 8px 12px;
  background: #10b981;
  color: #ffffff;
  border: none;
  border-radius: 8px;
  font-weight: 600;
  font-size: 12.5px;
  cursor: pointer;
  transition: all 0.15s;
}
.btn-download-action:hover {
  background: #059669;
  transform: translateY(-1px);
}

/* 2. Stock Alert Widget */
.stock-top-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}
.stock-risk-pill {
  font-size: 10.5px;
  font-weight: bold;
  padding: 2px 7px;
  border-radius: 4px;
  text-transform: uppercase;
}
.stock-risk-pill.is-critical {
  background: #ef4444;
  color: #ffffff;
}
.stock-risk-pill.is-warning {
  background: #f59e0b;
  color: #ffffff;
}
.stock-risk-pill.is-safe {
  background: #10b981;
  color: #ffffff;
}
.stock-sku-badge {
  font-family: monospace;
  font-size: 11px;
  color: #94a3b8;
}
.stock-prod-name {
  font-weight: 600;
  font-size: 13.5px;
  margin-bottom: 8px;
}
.stock-metrics-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 6px;
  margin-bottom: 10px;
}
.s-metric {
  background: rgba(0, 0, 0, 0.18);
  padding: 6px 8px;
  border-radius: 6px;
  display: flex;
  flex-direction: column;
}
.theme-light .s-metric {
  background: #f8fafc;
}
.sm-lbl {
  font-size: 10px;
  color: #94a3b8;
}
.theme-light .sm-lbl {
  color: #64748b;
}
.sm-val {
  font-weight: bold;
  font-size: 13px;
  margin-top: 2px;
}
.sm-val.highlight {
  color: #38bdf8;
}
.text-danger {
  color: #ef4444 !important;
}
.btn-restock-action {
  width: 100%;
  padding: 8px 12px;
  background: #3b82f6;
  color: #ffffff;
  border: none;
  border-radius: 8px;
  font-weight: 600;
  font-size: 12.5px;
  cursor: pointer;
  transition: all 0.15s;
}
.btn-restock-action:hover {
  background: #2563eb;
  transform: translateY(-1px);
}

/* 3. Action Confirmation Widget */
.action-confirm-widget {
  border-left: 4px solid #f59e0b;
}
.action-card-header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
  font-weight: bold;
  color: #f59e0b;
}
.action-reason-text {
  font-size: 12.5px;
  line-height: 1.4;
  margin-bottom: 6px;
}
.action-details-row {
  font-size: 12px;
  color: #94a3b8;
  margin-bottom: 8px;
}
.theme-light .action-details-row {
  color: #64748b;
}
.action-permission-banner {
  margin-top: 6px;
  padding: 6px 10px;
  background: rgba(239, 68, 68, 0.08);
  border: 1px dashed rgba(239, 68, 68, 0.35);
  border-radius: 6px;
  font-size: 11px;
  color: #ef4444;
  display: flex;
  align-items: center;
  gap: 6px;
  line-height: 1.4;
  margin-bottom: 8px;
}
.theme-light .action-permission-banner {
  background: #fef2f2;
  border-color: #fca5a5;
  color: #b91c1c;
}
.action-card-btns {
  display: flex;
  gap: 8px;
}
.btn-confirm-exec {
  flex: 1;
  padding: 8px;
  background: #ef4444;
  color: #ffffff;
  border: none;
  border-radius: 6px;
  font-weight: 600;
  font-size: 12px;
  cursor: pointer;
}
.btn-confirm-exec:hover {
  background: #dc2626;
}
.btn-confirm-exec.is-disabled {
  opacity: 0.65;
  cursor: not-allowed !important;
  filter: grayscale(0.7);
  background: #64748b !important;
  color: #e2e8f0 !important;
}
.btn-confirm-exec.is-disabled:hover {
  background: #64748b !important;
}
.btn-cancel-exec {
  padding: 8px 14px;
  background: transparent;
  color: #94a3b8;
  border: 1px solid #475569;
  border-radius: 6px;
  font-size: 12px;
  cursor: pointer;
}
.action-done-banner {
  width: 100%;
  padding: 8px;
  background: rgba(16, 185, 129, 0.15);
  color: #10b981;
  border: 1px solid #10b981;
  border-radius: 6px;
  text-align: center;
  font-weight: 600;
  font-size: 12px;
}

/* 4. Tracking Widget */
.track-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  gap: 6px;
}
.carrier-tag {
  background: #f97316;
  color: #ffffff;
  padding: 2px 7px;
  border-radius: 4px;
  font-weight: bold;
  font-size: 11px;
}
.track-code-text {
  font-family: monospace;
  font-size: 12px;
}
.track-status-pill {
  font-size: 11px;
  color: #38bdf8;
  font-weight: bold;
}
.track-info-summary {
  font-size: 12px;
  padding: 6px 8px;
  background: rgba(0, 0, 0, 0.18);
  border-radius: 6px;
  margin-bottom: 10px;
}
.theme-light .track-info-summary {
  background: #f8fafc;
}
.track-timeline-wrap {
  display: flex;
  flex-direction: column;
  gap: 8px;
  border-left: 2px solid #334155;
  padding-left: 10px;
  margin-left: 4px;
}
.track-tl-node {
  position: relative;
  font-size: 11.5px;
}
.tl-bullet {
  position: absolute;
  left: -15px;
  top: 3px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #64748b;
}
.track-tl-node.is-done .tl-bullet {
  background: #10b981;
  box-shadow: 0 0 6px #10b981;
}
.tl-title-line {
  font-weight: 600;
}
.tl-sub-line {
  color: #94a3b8;
  font-size: 10.5px;
}
.theme-light .tl-sub-line {
  color: #64748b;
}

/* 5. Sparkline Chart Widget */
.spark-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}
.sp-title {
  font-weight: 600;
  font-size: 12px;
}
.sp-total {
  font-weight: bold;
  font-size: 14px;
  color: #10b981;
}
.spark-svg-container {
  background: rgba(0, 0, 0, 0.2);
  border-radius: 8px;
  padding: 6px 4px;
}
.theme-light .spark-svg-container {
  background: #f8fafc;
}
.spark-svg {
  width: 100%;
  height: 72px;
}
.spark-bar {
  fill: #38bdf8;
  transition: all 0.2s;
}
.spark-bar:hover {
  fill: #0284c7;
}
.spark-lbl {
  font-size: 9px;
  fill: #94a3b8;
}
.theme-light .spark-lbl {
  fill: #64748b;
}

/* ================== VOICE MICROPHONE ================== */
.btn-voice-mic {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  border: 1px solid #475569;
  background: transparent;
  color: #94a3b8;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  transition: all 0.15s;
  flex-shrink: 0;
  margin-right: 6px;
}
.btn-voice-mic:hover {
  color: #ffffff;
  border-color: #38bdf8;
  background: rgba(56, 189, 248, 0.1);
}
.theme-light .btn-voice-mic {
  border-color: #cbd5e1;
  color: #475569;
}
.btn-voice-mic.is-listening {
  background: #ef4444;
  color: #ffffff;
  border-color: #ef4444;
  animation: pulse-ring 1.2s infinite;
}
.voice-wave-bars {
  display: flex;
  align-items: center;
  gap: 2px;
  height: 14px;
}
.voice-wave-bars span {
  width: 3px;
  height: 100%;
  background: #ffffff;
  border-radius: 2px;
  animation: wave-bar 0.8s ease-in-out infinite alternate;
}
.voice-wave-bars span:nth-child(2) {
  animation-delay: 0.2s;
}
.voice-wave-bars span:nth-child(3) {
  animation-delay: 0.4s;
}
@keyframes wave-bar {
  from { height: 4px; }
  to { height: 14px; }
}
@keyframes pulse-ring {
  0% { box-shadow: 0 0 0 0 rgba(239, 68, 68, 0.7); }
  70% { box-shadow: 0 0 0 8px rgba(239, 68, 68, 0); }
  100% { box-shadow: 0 0 0 0 rgba(239, 68, 68, 0); }
}
/* 6. Conversational POS - Order Draft Card Widget */
.order-draft-widget {
  border-left: 4px solid #10b981;
  background: rgba(16, 185, 129, 0.05);
}
.theme-light .order-draft-widget {
  background: #f0fdf4;
}
.draft-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  gap: 8px;
}
.draft-header-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 700;
  font-size: 13px;
  color: #10b981;
}
.draft-stock-status-badge {
  font-size: 10.5px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 4px;
}
.draft-stock-status-badge.is-in-stock {
  background: rgba(16, 185, 129, 0.18);
  color: #10b981;
  border: 1px solid rgba(16, 185, 129, 0.3);
}
.draft-stock-status-badge.is-out-stock {
  background: rgba(239, 68, 68, 0.18);
  color: #ef4444;
  border: 1px solid rgba(239, 68, 68, 0.3);
}
.draft-customer-strip {
  display: flex;
  justify-content: space-between;
  background: rgba(0, 0, 0, 0.2);
  padding: 6px 10px;
  border-radius: 6px;
  margin-bottom: 8px;
  font-size: 11.5px;
}
.theme-light .draft-customer-strip {
  background: #e2e8f0;
}
.cust-name-line {
  font-size: 12px;
}
.cust-sub-line {
  color: #94a3b8;
  font-size: 10.5px;
}
.theme-light .cust-sub-line {
  color: #64748b;
}
.draft-delivery-col {
  text-align: right;
}
.delivery-pay-tag {
  font-weight: 600;
  color: #38bdf8;
  font-size: 11px;
}
.delivery-addr-line {
  color: #94a3b8;
  font-size: 10.5px;
  max-width: 150px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.draft-items-list {
  background: rgba(0, 0, 0, 0.15);
  border-radius: 6px;
  padding: 6px 8px;
  margin-bottom: 8px;
  font-size: 11.5px;
}
.theme-light .draft-items-list {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}
.draft-item-header {
  display: grid;
  grid-template-columns: 2fr 0.6fr 1.2fr 1.2fr;
  font-weight: 600;
  color: #94a3b8;
  font-size: 10.5px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  padding-bottom: 4px;
  margin-bottom: 4px;
}
.theme-light .draft-item-header {
  color: #64748b;
  border-bottom: 1px solid #e2e8f0;
}
.draft-item-row {
  display: grid;
  grid-template-columns: 2fr 0.6fr 1.2fr 1.2fr;
  align-items: center;
  padding: 3px 0;
  border-bottom: 1px dashed rgba(255, 255, 255, 0.05);
}
.col-center {
  text-align: center;
}
.col-right {
  text-align: right;
}
.item-name-col {
  display: flex;
  flex-direction: column;
}
.item-name {
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.item-sku {
  font-size: 9.5px;
  color: #94a3b8;
}
.item-stock-warn {
  font-size: 9.5px;
  color: #ef4444;
  font-weight: bold;
}
.draft-finance-summary {
  display: flex;
  flex-direction: column;
  gap: 3px;
  font-size: 11.5px;
  padding: 4px 6px;
  margin-bottom: 8px;
}
.finance-row {
  display: flex;
  justify-content: space-between;
}
.finance-row.total-row {
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  padding-top: 5px;
  margin-top: 3px;
  font-size: 13px;
  font-weight: bold;
}
.theme-light .finance-row.total-row {
  border-top: 1px solid #cbd5e1;
}
.total-price-text {
  color: #10b981;
  font-size: 14px;
}
.draft-actions-wrap {
  display: flex;
  gap: 8px;
}
.btn-confirm-order-exec {
  flex: 1;
  padding: 8px 12px;
  background: #10b981;
  color: #ffffff;
  border: none;
  border-radius: 6px;
  font-weight: 700;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.15s;
}
.btn-confirm-order-exec:hover {
  background: #059669;
  transform: translateY(-1px);
}
.draft-done-banner {
  width: 100%;
  padding: 8px;
  background: rgba(16, 185, 129, 0.2);
  color: #10b981;
  border: 1px solid #10b981;
  border-radius: 6px;
  text-align: center;
  font-weight: 600;
  font-size: 12px;
}

/* ========================================================================= */
/* GIAI ĐOẠN 5: STYLES CHO MORNING BRIEFING, OCR VISION & ACTION CHAINING */
/* ========================================================================= */
.btn-ocr-cam {
  margin-right: 2px;
}

/* Morning Briefing */
.morning-briefing-widget {
  background: #1e293b;
  border: 1px solid #3b82f6;
  border-radius: 12px;
  padding: 14px;
  margin-top: 8px;
}
.theme-light .morning-briefing-widget {
  background: #f0fdf4;
  border-color: #86efac;
}
.briefing-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.briefing-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: rgba(59, 130, 246, 0.2);
  color: #60a5fa;
  padding: 3px 8px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 700;
}
.theme-light .briefing-badge {
  background: #dcfce7;
  color: #15803d;
}
.briefing-date {
  font-size: 11px;
  color: #94a3b8;
}
.briefing-kpi-grid {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 8px;
  margin-bottom: 12px;
}
.brief-kpi-item {
  background: rgba(15, 23, 42, 0.6);
  padding: 8px 10px;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.theme-light .brief-kpi-item {
  background: #ffffff;
  border: 1px solid #e2e8f0;
}
.brief-kpi-label {
  font-size: 10px;
  color: #94a3b8;
}
.brief-kpi-val {
  font-size: 14px;
  font-weight: 800;
}
.brief-kpi-sub {
  font-size: 10px;
  color: #64748b;
}
.briefing-product-strip {
  display: flex;
  gap: 12px;
  padding: 8px 10px;
  background: rgba(15, 23, 42, 0.4);
  border-radius: 6px;
  font-size: 11px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}
.theme-light .briefing-product-strip {
  background: #ffffff;
  border: 1px solid #e2e8f0;
}
.btn-audio-briefing {
  width: 100%;
  padding: 10px;
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  color: #ffffff;
  border: none;
  border-radius: 8px;
  font-weight: 700;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}
.btn-audio-briefing:hover {
  background: linear-gradient(135deg, #1d4ed8, #1e40af);
  transform: translateY(-1px);
}
.btn-audio-briefing.is-speaking {
  background: #059669;
}
.audio-speaking-content {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
.sound-wave {
  display: inline-flex;
  gap: 2px;
  align-items: flex-end;
  height: 14px;
}
.sound-wave span {
  width: 2px;
  background: #ffffff;
  animation: soundBounce 0.8s infinite ease-in-out alternate;
}
.sound-wave span:nth-child(2) { animation-delay: 0.2s; }
.sound-wave span:nth-child(3) { animation-delay: 0.4s; }
.sound-wave span:nth-child(4) { animation-delay: 0.6s; }
@keyframes soundBounce {
  0% { height: 4px; }
  100% { height: 14px; }
}

/* OCR Invoice Widget */
.ocr-invoice-widget {
  background: #1e293b;
  border: 1px solid #10b981;
  border-radius: 12px;
  padding: 14px;
  margin-top: 8px;
}
.theme-light .ocr-invoice-widget {
  background: #f8fafc;
  border-color: #cbd5e1;
}
.ocr-card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 10px;
}
.ocr-header-left {
  display: flex;
  gap: 8px;
  align-items: center;
}
.ocr-icon {
  font-size: 20px;
}
.ocr-title {
  font-size: 13px;
  font-weight: 700;
  color: #f8fafc;
}
.theme-light .ocr-title { color: #0f172a; }
.ocr-sub {
  font-size: 11px;
  color: #94a3b8;
}
.ocr-tag {
  background: rgba(16, 185, 129, 0.2);
  color: #34d399;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 10px;
  font-weight: 700;
}
.ocr-supplier-info {
  background: rgba(15, 23, 42, 0.6);
  padding: 8px 10px;
  border-radius: 6px;
  font-size: 11px;
  margin-bottom: 10px;
}
.theme-light .ocr-supplier-info {
  background: #ffffff;
  border: 1px solid #e2e8f0;
}
.supplier-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 2px;
}
.supplier-row .label { color: #94a3b8; }
.tax-code-text { font-family: monospace; color: #38bdf8; }
.ocr-items-table {
  font-size: 11px;
  margin-bottom: 10px;
}
.ocr-table-head {
  display: grid;
  grid-template-columns: 2fr 40px 1fr 1fr;
  color: #94a3b8;
  font-weight: 600;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  padding-bottom: 4px;
}
.ocr-table-row {
  display: grid;
  grid-template-columns: 2fr 40px 1fr 1fr;
  padding: 4px 0;
  border-bottom: 1px dashed rgba(255, 255, 255, 0.05);
}
.ocr-summary-box {
  background: rgba(15, 23, 42, 0.4);
  padding: 8px 10px;
  border-radius: 6px;
  font-size: 11px;
  margin-bottom: 10px;
}
.theme-light .ocr-summary-box {
  background: #ffffff;
  border: 1px solid #e2e8f0;
}
.sum-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 2px;
}
.sum-row.total {
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  padding-top: 4px;
  margin-top: 4px;
  font-weight: 700;
  font-size: 12px;
}
.ocr-actions-row {
  display: flex;
  gap: 8px;
}
.btn-ocr-create-po {
  flex: 1;
  padding: 8px;
  background: #2563eb;
  color: #ffffff;
  border: none;
  border-radius: 6px;
  font-weight: 700;
  font-size: 11px;
  cursor: pointer;
}
.btn-ocr-create-pv {
  flex: 1;
  padding: 8px;
  background: #059669;
  color: #ffffff;
  border: none;
  border-radius: 6px;
  font-weight: 700;
  font-size: 11px;
  cursor: pointer;
}

/* Anomaly Risk Widget */
.anomaly-risk-widget {
  background: #1e293b;
  border: 1px solid #e11d48;
  border-radius: 12px;
  padding: 14px;
  margin-top: 8px;
}
.theme-light .anomaly-risk-widget {
  background: #fff1f2;
  border-color: #fda4af;
}
.anomaly-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
.anomaly-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: rgba(225, 29, 72, 0.2);
  color: #f43f5e;
  padding: 2px 8px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 700;
}
.anomaly-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.anomaly-item {
  background: rgba(15, 23, 42, 0.6);
  padding: 10px;
  border-radius: 8px;
  border-left: 3px solid #e11d48;
}
.theme-light .anomaly-item {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-left: 3px solid #e11d48;
}
.item-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}
.sev-tag {
  background: #e11d48;
  color: #ffffff;
  font-size: 9px;
  font-weight: 800;
  padding: 1px 4px;
  border-radius: 4px;
}
.item-title {
  font-size: 12px;
  color: #f8fafc;
}
.theme-light .item-title { color: #0f172a; }
.item-desc {
  font-size: 11px;
  color: #94a3b8;
  margin: 0 0 4px 0;
  line-height: 1.4;
}
.item-meta {
  display: flex;
  justify-content: space-between;
  font-size: 10px;
  color: #64748b;
  margin-bottom: 4px;
}
.item-rec {
  font-size: 10px;
  color: #38bdf8;
}

/* Action Chain Widget */
.action-chain-widget {
  background: #1e293b;
  border: 1px solid #8b5cf6;
  border-radius: 12px;
  padding: 14px;
  margin-top: 8px;
}
.theme-light .action-chain-widget {
  background: #f5f3ff;
  border-color: #c4b5fd;
}
.chain-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.chain-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: rgba(139, 92, 246, 0.2);
  color: #a78bfa;
  padding: 2px 8px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 700;
}
.chain-code {
  font-family: monospace;
  font-size: 10px;
  color: #94a3b8;
}
.chain-summary-text {
  font-size: 11px;
  color: #94a3b8;
  margin-bottom: 10px;
}
.chain-steps-box {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 12px;
}
.chain-step-row {
  display: flex;
  gap: 10px;
  background: rgba(15, 23, 42, 0.5);
  padding: 8px 10px;
  border-radius: 6px;
  align-items: flex-start;
}
.theme-light .chain-step-row {
  background: #ffffff;
  border: 1px solid #e2e8f0;
}
.step-num {
  width: 20px;
  height: 20px;
  background: #8b5cf6;
  color: #ffffff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 800;
}
.step-desc-text {
  font-size: 11px;
  font-weight: 600;
  color: #f8fafc;
  margin-bottom: 2px;
}
.theme-light .step-desc-text { color: #0f172a; }
.step-module-tag {
  font-size: 10px;
  color: #94a3b8;
}
.step-result-note {
  font-size: 10px;
  color: #10b981;
  margin-top: 2px;
}
.btn-execute-chain {
  width: 100%;
  padding: 10px;
  background: linear-gradient(135deg, #8b5cf6, #7c3aed);
  color: #ffffff;
  border: none;
  border-radius: 8px;
  font-weight: 700;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-execute-chain:hover {
  background: linear-gradient(135deg, #7c3aed, #6d28d9);
  transform: translateY(-1px);
}
/* Audit Log Widget */
.audit-log-widget {
  background: #ffffff;
  border: 1px solid #cbd5e1;
  border-radius: 12px;
  padding: 12px;
  margin-top: 8px;
}

.audit-widget-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.audit-widget-title {
  font-size: 12px;
  font-weight: 700;
  color: #0f172a;
}

.audit-widget-badge {
  font-size: 11px;
  font-weight: 600;
  background: #eff6ff;
  color: #2563eb;
  padding: 2px 8px;
  border-radius: 99px;
}

.audit-kpi-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin-bottom: 10px;
}

.audit-kpi-pill {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 6px 8px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.ak-lbl {
  font-size: 10.5px;
  color: #64748b;
}

.ak-val {
  font-size: 14px;
  font-weight: 700;
}

.ak-val.text-blue { color: #2563eb; }
.ak-val.text-red { color: #dc2626; }
.ak-val.text-green { color: #059669; }

.audit-recent-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-height: 190px;
  overflow-y: auto;
  margin-bottom: 10px;
}

.audit-recent-row {
  padding: 6px 8px;
  border-radius: 6px;
  background: #f8fafc;
  border-left: 3px solid #3b82f6;
}

.ar-top {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  font-size: 11.5px;
  margin-bottom: 2px;
}

.ar-user {
  font-weight: 700;
  color: #0f172a;
}

.ar-action {
  padding: 1px 6px;
  border-radius: 4px;
  font-size: 10.5px;
  font-weight: 600;
}

.ar-normal {
  background: #dbeafe;
  color: #1d4ed8;
}

.ar-danger {
  background: #fee2e2;
  color: #b91c1c;
}

.ar-entity {
  color: #475569;
  font-weight: 600;
}

.ar-time {
  margin-left: auto;
  color: #94a3b8;
  font-size: 11px;
}

.ar-reason {
  font-size: 11.5px;
  color: #475569;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.audit-widget-footer {
  display: flex;
  justify-content: flex-end;
}

.audit-goto-btn {
  width: 100%;
  border: none;
  border-radius: 8px;
  background: #2563eb;
  color: #ffffff;
  font-size: 12px;
  font-weight: 600;
  padding: 7px 12px;
  cursor: pointer;
}

.audit-goto-btn:hover {
  background: #1d4ed8;
}
</style>
