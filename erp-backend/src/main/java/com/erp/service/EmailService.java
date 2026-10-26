package com.erp.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:dolemta090899@gmail.com}")
    private String senderEmail = "dolemta090899@gmail.com";

    public void sendOtpEmail(String toEmail, String username, String otpCode) throws MessagingException {
        log.info("Bắt đầu gửi email OTP tới: {} cho user: {}", toEmail, username);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(senderEmail);
        helper.setTo(toEmail);
        helper.setSubject("[ERP Cloud] Mã xác thực OTP khôi phục mật khẩu: " + otpCode);

        String htmlContent = buildOtpHtmlTemplate(username, otpCode);
        helper.setText(htmlContent, true);

        mailSender.send(message);
        log.info("Đã gửi thành công email OTP tới: {}", toEmail);
    }

    private String buildOtpHtmlTemplate(String username, String otpCode) {
        return "<!DOCTYPE html>"
                + "<html>"
                + "<head><meta charset='UTF-8'></head>"
                + "<body style='font-family: Arial, sans-serif; background-color: #f8fafc; margin: 0; padding: 24px; color: #1e293b;'>"
                + "  <div style='max-width: 520px; margin: 0 auto; background-color: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 16px rgba(0,0,0,0.06); border: 1px solid #e2e8f0;'>"
                + "    <div style='background: linear-gradient(135deg, #0284c7 0%, #0369a1 100%); padding: 24px; text-align: center; color: #ffffff;'>"
                + "      <h1 style='margin: 0; font-size: 22px; font-weight: bold;'>ERP Cloud System</h1>"
                + "      <p style='margin: 6px 0 0 0; font-size: 13px; opacity: 0.9;'>Xác thực khôi phục mật khẩu tài khoản</p>"
                + "    </div>"
                + "    <div style='padding: 28px 24px;'>"
                + "      <p style='font-size: 15px; margin-top: 0;'>Xin chào <strong>" + username + "</strong>,</p>"
                + "      <p style='font-size: 14px; line-height: 1.6; color: #475569;'>"
                + "        Bạn vừa gửi yêu cầu đặt lại mật khẩu cho tài khoản trên hệ thống ERP Cloud. Vui lòng sử dụng mã xác thực OTP dưới đây để hoàn tất:"
                + "      </p>"
                + "      <div style='background: #f1f5f9; border: 2px dashed #0284c7; border-radius: 12px; padding: 18px; text-align: center; margin: 24px 0;'>"
                + "        <span style='display: block; font-size: 12px; color: #64748b; text-transform: uppercase; letter-spacing: 1px; margin-bottom: 6px;'>Mã xác thực OTP của bạn</span>"
                + "        <span style='font-size: 32px; font-weight: 800; letter-spacing: 8px; color: #0284c7; font-family: monospace;'>" + otpCode + "</span>"
                + "      </div>"
                + "      <p style='font-size: 13px; color: #64748b; line-height: 1.5; margin: 0;'>"
                + "        ⏰ <strong>Thời hạn hiệu lực:</strong> Mã này có hiệu lực trong vòng <strong>15 phút</strong>.<br>"
                + "        🛡️ <strong>Lưu ý bảo mật:</strong> Tuyệt đối không chia sẻ mã này cho bất kỳ ai."
                + "      </p>"
                + "      <hr style='border: none; border-top: 1px solid #e2e8f0; margin: 24px 0;'>"
                + "      <p style='font-size: 12px; color: #94a3b8; line-height: 1.4; margin: 0; text-align: center;'>"
                + "        Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email hoặc liên hệ Quản trị viên hệ thống."
                + "      </p>"
                + "    </div>"
                + "  </div>"
                + "</body>"
                + "</html>";
    }
}
