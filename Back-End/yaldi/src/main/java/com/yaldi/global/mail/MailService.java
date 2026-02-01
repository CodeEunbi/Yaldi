package com.yaldi.global.mail;

import com.yaldi.global.exception.GeneralException;
import com.yaldi.global.response.status.ErrorStatus;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {
    private final JavaMailSender mailSender;

    // S3 로고 이미지 URL
    private static final String LOGO_URL = "https://yaldi-public-bucket.s3.ap-northeast-2.amazonaws.com/yaldi-logo.png";

    @Value("${frontend.url}")
    private String frontendUrl;

    @Async
    public void sendTeamInvitation(String toEmail, String teamName, String inviterNickname) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("[Yaldi] 새로운 팀에 초대되었습니다");
            helper.setText(
                    "<div style='max-width: 600px; margin: 0 auto; font-family: Arial, sans-serif; background: #f5f5f5;'>" +
                            "<div style='background: white; padding: 40px 20px;'>" +

                            "<div style='text-align: center; margin-bottom: 30px;'>" +
                            "<div style='display: inline-block; border: 1px solid #e0e0e0; border-radius: 8px; padding: 15px 30px;'>" +
                            "<span style='font-size: 14px; color: #666;'>✉️ 새로운 팀에 초대되었습니다</span>" +
                            "</div>" +
                            "</div>" +

                            "<div style='text-align: center; margin-bottom: 30px;'>" +
                            "<img src='" + LOGO_URL + "' alt='Yaldi' style='max-width: 200px; height: auto;' />" +
                            "</div>" +

                            "<div style='text-align: center; color: #333; margin-bottom: 30px;'>" +
                            "<p style='font-size: 16px; line-height: 1.6; margin: 10px 0;'>" +
                            "<strong>" + inviterNickname + "</strong>님이 <strong>" + teamName + "</strong>에 초대했습니다.<br>" +
                            "Yaldi에 접속하여 확인해보세요." +
                            "</p>" +
                            "</div>" +

                            "<div style='text-align: center; margin: 30px 0;'>" +
                            "<a href='" + frontendUrl + "/notification' style='display: inline-block; background: #4A90E2; color: white; padding: 15px 40px; text-decoration: none; border-radius: 8px; font-weight: bold; font-size: 16px;'>" +
                            "초대 확인하러 가기" +
                            "</a>" +
                            "</div>" +

                            "</div>" +
                            "</div>",
                    true
            );

            mailSender.send(message);
            log.info("팀 초대 이메일 발송 완료 :: 수신자={}, 팀명={}", toEmail, teamName);
        } catch (MessagingException e) {
            log.error("팀 초대 이메일 발송 실패 :: 수신자={}, 팀명={}, 에러={}",
                    toEmail, teamName, e.getMessage(), e);
        } catch (Exception e) {
            log.error("팀 초대 이메일 발송 중 예상치 못한 오류 :: 수신자={}, 팀명={}, 에러={}",
                    toEmail, teamName, e.getMessage(), e);
        }
    }
}
