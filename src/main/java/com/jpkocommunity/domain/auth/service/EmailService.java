package com.jpkocommunity.domain.auth.service;

import com.jpkocommunity.global.config.properties.MailProperties;
import com.jpkocommunity.global.exception.CustomException;
import com.jpkocommunity.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailService {


    private final SesClient sesClient;
    private final MailProperties mailProperties;

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

    private static final Pattern IP_ALLOWED = Pattern.compile("^[0-9a-fA-F.:]{1,45}$");
    private String sanitizeIp(String ipAddress) {
        return (ipAddress != null && IP_ALLOWED.matcher(ipAddress).matches()) ? ipAddress : "알 수 없음";
    }

    public void sendAccountRecoveryEmail(String toEmail, String loginId, String token, String ipAddress) {
        String link = mailProperties.baseUrl() + "/reset-password?token=" + token;
        String ip = sanitizeIp(ipAddress);
        String htmlBody = """
                <!DOCTYPE html>
                <html>
                <body style="font-family: Arial, sans-serif; line-height:1.6; color:#333;">
                    <h2>JPKO Community 계정 찾기</h2>
                    <p>누군가 이 메일 주소로 아이디/비밀번호 찾기를 시도했습니다.</p>
                    <p>연결된 계정 아이디 : <strong>%s</strong></p>
                    <p>이어서 비밀번호를 변경하시려면 아래 버튼을 눌러주세요.</p>
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
                    <p style="color:#888; font-size:12px;">요청한 IP 주소 : %s</p>
                    <p>감사합니다.<br>JPKO Community</p>
                </body>
                </html>
                """.formatted(loginId, link, link, link, ip);
        String textBody = """
                JPKO Community
                
                누군가 이 메일 주소로 아이디/비밀번호 찾기를 시도했습니다.
                
                연결된 계정 아이디 : %s
                
                이어서 비밀번호를 변경하시려면 아래 링크로 이동해주세요:
                %s
                
                • 재설정 링크는 30분 동안 유효합니다.
                
                요청한 IP 주소 : %s
                
                감사합니다.
                JPKO Community
                """.formatted(loginId, link, ip);
        send(toEmail, "[JPKO Community] 계정 찾기 안내", htmlBody, textBody);
    }
    public void sendAccountNotFoundEmail(String toEmail, String ipAddress) {
        String ip = sanitizeIp(ipAddress);
        String htmlBody = """
                <!DOCTYPE html>
                <html>
                <body style="font-family: Arial, sans-serif; line-height:1.6; color:#333;">
                    <h2>JPKO Community 계정 찾기</h2>
                    <p>누군가 이 메일 주소로 아이디/비밀번호 찾기를 시도했습니다.</p>
                    <p>그러나 이 메일 주소와 연결된 계정이 없거나, 탈퇴한 계정이라 찾을 수 없습니다.</p>
                    <p>요청이 취소되었습니다.</p>
                    <hr>
                    <p>• 본인이 요청하셨다면, 가입에 사용하신 다른 메일 주소를 확인해주세요.</p>
                    <p>• 본인이 요청하지 않았다면 이 메일은 무시하셔도 됩니다.</p>
                    <p style="color:#888; font-size:12px;">요청한 IP 주소 : %s</p>
                    <p>감사합니다.<br>JPKO Community</p>
                </body>
                </html>
                """.formatted(ip);
        String textBody = """
                JPKO Community
                
                누군가 이 메일 주소로 아이디/비밀번호 찾기를 시도했습니다.
                
                그러나 이 메일 주소와 연결된 계정이 없거나, 탈퇴한 계정이라 찾을 수 없습니다.
                요청이 취소되었습니다.
                
                • 본인이 요청하셨다면, 가입에 사용하신 다른 메일 주소를 확인해주세요.
                • 본인이 요청하지 않았다면 이 메일은 무시하셔도 됩니다.
                
                요청한 IP 주소 : %s
                
                감사합니다.
                JPKO Community
                """.formatted(ip);
        send(toEmail, "[JPKO Community] 계정 찾기 안내", htmlBody, textBody);
    }

    public void sendPreSignupEmail(String toEmail, String token) {
        String link = mailProperties.baseUrl() + "/signup/" + token;

        String htmlBody = """
                <!DOCTYPE html>
                <html>
                <body style="font-family: Arial, sans-serif; line-height:1.6; color:#333;">
                    <h2>JPKO Community 계정 만들기</h2>
                    <p>안녕하세요.</p>
                    <p>
                        누군가 이 메일 주소로 계정 생성을 시도했습니다.
                        본인이 맞다면 아래 버튼을 눌러 회원가입을 계속 진행해주세요.
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
                            회원가입 계속하기
                        </a>
                    </p>
                    <p>위 버튼이 동작하지 않는 경우 아래 주소를 브라우저에 직접 붙여넣어 주세요.</p>
                    <p><a href="%s">%s</a></p>
                    <hr>
                    <p>• 링크는 <strong>24시간 동안</strong> 유효합니다.</p>
                    <p>• 본인이 요청하지 않았다면 이 메일은 무시하셔도 됩니다.</p>
                    <p>감사합니다.<br>JPKO Community</p>
                </body>
                </html>
                """.formatted(link, link, link);

        String textBody = """
                JPKO Community
                
                누군가 이 메일 주소로 계정 생성을 시도했습니다.
                
                본인이 맞다면 아래 링크를 클릭해주세요:
                %s
                
                • 링크는 24시간 동안 유효합니다.
                • 요청하지 않았다면 이 메일을 무시해주세요.
                
                감사합니다.
                JPKO Community
                """.formatted(link);

        send(toEmail, "[JPKO Community] 계정 만들기", htmlBody, textBody);
    }

    public void sendChangeEmail(String toEmail, String token) {
        String link = mailProperties.baseUrl() + "/verify-email-change?token=" + token;

        String htmlBody = """
                <!DOCTYPE html>
                <html>
                <body style="font-family: Arial, sans-serif; line-height:1.6; color:#333;">
                    <h2>JPKO Community 이메일 인증</h2>
                    <p>안녕하세요.</p>
                    <p>
                        이 메일 주소를 JPKO Community 계정의 이메일로 등록하기 위한 인증 요청입니다.
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
                    <p>• 인증을 완료하면 이 주소가 계정의 이메일로 등록됩니다.</p>
                    <p>• 본인이 요청하지 않았다면 이 메일은 무시하셔도 됩니다.</p>
                    <p>감사합니다.<br>JPKO Community</p>
                </body>
                </html>
                """.formatted(link, link, link);

        String textBody = """
                JPKO Community
                
                이 메일 주소를 계정 이메일로 등록하기 위한 인증 요청입니다.
                
                아래 링크를 클릭해주세요:
                %s
                
                • 링크는 24시간 동안 유효합니다.
                • 인증을 완료하면 이 주소가 계정의 이메일로 등록됩니다.
                • 요청하지 않았다면 이 메일을 무시해주세요.
                
                감사합니다.
                JPKO Community
                """.formatted(link);

        send(toEmail, "[JPKO Community] 이메일 인증을 완료해주세요", htmlBody, textBody);

    }

}
