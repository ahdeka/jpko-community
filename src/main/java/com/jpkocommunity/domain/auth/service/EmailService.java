package com.jpkocommunity.domain.auth.service;

import com.jpkocommunity.global.config.MailProperties;
import com.jpkocommunity.global.exception.CustomException;
import com.jpkocommunity.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailService {


    private final SesClient sesClient;
    private final MailProperties mailProperties;

    public void sendVerificationEmail(String toEmail, String token) {
        String link = mailProperties.baseUrl() + "/verify-email?token=" + token;

        String htmlBody = """
            <!DOCTYPE html>
            <html>
            <body style="font-family: Arial, sans-serif; line-height:1.6; color:#333;">
                <h2>JPKO Community 이메일 인증</h2>
                <p>안녕하세요.</p>
                <p>
                    JPKO Community 회원가입을 위해 이메일 인증이 필요합니다.
                    아래 버튼을 눌러 인증을 완료해주세요.
                </p>
                <p style="margin:30px 0;">
                    <a href="%s"
                       style="
                            background:#2563eb;
                            color:#ffffff;
                            padding:12px 24px;
                            text-decoration:none;
                            border-radius:6px;
                            display:inline-block;
                       ">
                        이메일 인증하기
                    </a>
                </p>
                <p>위 버튼이 동작하지 않는 경우 아래 주소를 브라우저에 직접 붙여넣어 주세요.</p>
                <p><a href="%s">%s</a></p>
                <hr>
                <p>• 인증 링크는 <strong>24시간 동안</strong> 유효합니다.</p>
                <p>• 본인이 요청하지 않았다면 이 메일은 무시하셔도 됩니다.</p>
                <p>감사합니다.<br>JPKO Community</p>
            </body>
            </html>
            """.formatted(link, link, link);

        String textBody = """
            JPKO Community

            회원가입을 위해 이메일 인증을 진행해주세요.

            아래 링크를 클릭해주세요:
            %s

            • 링크는 24시간 동안 유효합니다.
            • 요청하지 않았다면 이 메일을 무시해주세요.

            감사합니다.
            JPKO Community
            """.formatted(link);

        send(toEmail, "[JPKO Community] 이메일 인증을 완료해주세요", htmlBody, textBody);
    }

    public void sendPasswordResetEmail(String toEmail, String token) {
        String link = mailProperties.baseUrl() + "/reset-password?token=" + token;

        String htmlBody = """
            <!DOCTYPE html>
            <html>
            <body style="font-family: Arial, sans-serif; line-height:1.6; color:#333;">
                <h2>JPKO Community 비밀번호 재설정</h2>
                <p>안녕하세요.</p>
                <p>
                    비밀번호 재설정을 요청하셨습니다.
                    아래 버튼을 눌러 새 비밀번호를 설정해주세요.
                </p>
                <p style="margin:30px 0;">
                    <a href="%s"
                       style="
                            background:#2563eb;
                            color:#ffffff;
                            padding:12px 24px;
                            text-decoration:none;
                            border-radius:6px;
                            display:inline-block;
                       ">
                        비밀번호 재설정하기
                    </a>
                </p>
                <p>위 버튼이 동작하지 않는 경우 아래 주소를 브라우저에 직접 붙여넣어 주세요.</p>
                <p><a href="%s">%s</a></p>
                <hr>
                <p>• 재설정 링크는 <strong>30분 동안</strong> 유효합니다.</p>
                <p>• 본인이 요청하지 않았다면 이 메일을 무시하고, 계정 보안을 위해 비밀번호를 변경하는 것을 권장합니다.</p>
                <p>감사합니다.<br>JPKO Community</p>
            </body>
            </html>
            """.formatted(link, link, link);

        String textBody = """
            JPKO Community

            비밀번호 재설정을 요청하셨습니다.

            아래 링크를 클릭해 새 비밀번호를 설정해주세요:
            %s

            • 재설정 링크는 30분 동안 유효합니다.
            • 본인이 요청하지 않았다면 이 메일을 무시하고, 계정 보안을 위해 비밀번호를 변경하는 것을 권장합니다.

            감사합니다.
            JPKO Community
            """.formatted(link);

        send(toEmail, "[JPKO Community] 비밀번호 재설정 안내", htmlBody, textBody);
    }

    private void send(String toEmail, String subject, String htmlBody, String textBody) {
        SendEmailRequest request = SendEmailRequest.builder()
                .source(mailProperties.fromName() + " <" + mailProperties.from() + ">")
                .destination(Destination.builder().toAddresses(toEmail).build())
                .message(Message.builder()
                        .subject(Content.builder().data(subject).charset("UTF-8").build())
                        .body(Body.builder()
                                .html(Content.builder().data(htmlBody).charset("UTF-8").build())
                                .text(Content.builder().data(textBody).charset("UTF-8").build())
                                .build())
                        .build())
                .build();

        try {
            sesClient.sendEmail(request);
        } catch (SesException e) {
            log.error("SES 발송 실패 - to: {}, error: {}", toEmail, e.getMessage());
            throw new CustomException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }
}
