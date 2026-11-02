/**
 * Tiện ích âm thanh thông báo và Desktop Notification cho Realtime Chat
 */

// Phát âm thanh chuông chime hai nốt êm ái bằng Web Audio API thuần (Không cần file mp3 ngoài)
export function playNotificationSound() {
  try {
    const AudioCtx = window.AudioContext || window.webkitAudioContext
    if (!AudioCtx) return

    const ctx = new AudioCtx()
    const now = ctx.currentTime

    // Tạo bộ dao động nốt 1 (587.33 Hz - Nốt D5)
    const osc1 = ctx.createOscillator()
    const gain1 = ctx.createGain()
    osc1.type = 'sine'
    osc1.frequency.setValueAtTime(587.33, now)

    gain1.gain.setValueAtTime(0, now)
    gain1.gain.linearRampToValueAtTime(0.2, now + 0.02)
    gain1.gain.exponentialRampToValueAtTime(0.001, now + 0.35)

    osc1.connect(gain1)
    gain1.connect(ctx.destination)

    osc1.start(now)
    osc1.stop(now + 0.35)

    // Tạo bộ dao động nốt 2 (880 Hz - Nốt A5, ngân vang hơn)
    const osc2 = ctx.createOscillator()
    const gain2 = ctx.createGain()
    osc2.type = 'sine'
    osc2.frequency.setValueAtTime(880, now + 0.08)

    gain2.gain.setValueAtTime(0, now + 0.08)
    gain2.gain.linearRampToValueAtTime(0.25, now + 0.1)
    gain2.gain.exponentialRampToValueAtTime(0.001, now + 0.5)

    osc2.connect(gain2)
    gain2.connect(ctx.destination)

    osc2.start(now + 0.08)
    osc2.stop(now + 0.5)
  } catch (err) {
    // Trình duyệt có thể hạn chế phát âm thanh nếu người dùng chưa tương tác lần đầu
    console.warn('Không thể phát âm thanh thông báo:', err)
  }
}

// Yêu cầu quyền hiển thị thông báo Desktop trên trình duyệt
export async function requestNotificationPermission() {
  if (!('Notification' in window)) return false
  if (Notification.permission === 'granted') return true

  if (Notification.permission !== 'denied') {
    const permission = await Notification.requestPermission()
    return permission === 'granted'
  }
  return false
}

// Hiển thị thông báo nổi trên màn hình (Desktop Notification)
export function showDesktopNotification(title, options = {}) {
  try {
    if (!('Notification' in window) || Notification.permission !== 'granted') return

    const notification = new Notification(title, {
      body: options.body || '',
      icon: options.icon || '/favicon.ico',
      badge: '/favicon.ico',
      tag: options.tag || 'realtime-chat',
      renotify: true,
      silent: true // Đã có âm thanh riêng từ Web Audio API
    })

    notification.onclick = () => {
      window.focus()
      notification.close()
    }

    // Tự động đóng thông báo sau 5 giây
    setTimeout(() => {
      notification.close()
    }, 5000)
  } catch (err) {
    console.warn('Lỗi hiển thị thông báo đẩy:', err)
  }
}

