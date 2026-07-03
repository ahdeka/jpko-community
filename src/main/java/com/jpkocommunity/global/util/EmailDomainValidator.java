package com.jpkocommunity.global.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import java.util.Hashtable;


/**
 * 이메일 도메인 검증 유틸리티 클래스
 * - 이메일 도메인의 MX 레코드 존재 여부를 확인한다.
 */
@Slf4j
@Component
public class EmailDomainValidator {

    public boolean hasMxRecord(String email) {
        String domain = extractDomain(email);
        if (domain == null) {
            return false;
        }

        Hashtable<String, String> env = new Hashtable<>();
        env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
        // DNS 조회가 응답 없이 오래 걸리는 걸 방지 (인증 요청 API가 여기서 막히면 안 됨)
        env.put("com.sun.jndi.dns.timeout.initial", "1500");
        env.put("com.sun.jndi.dns.timeout.retries", "1");

        try {
            InitialDirContext context = new InitialDirContext(env);
            Attributes attributes = context.getAttributes(domain, new String[]{"MX"});
            Attribute mxAttribute = attributes.get("MX");
            return mxAttribute != null && mxAttribute.size() > 0;
        } catch (NamingException e) {
            log.debug("MX 레코드 조회 실패 - domain: {}, error: {}", domain, e.getMessage());
            return false;
        }
    }

    private String extractDomain(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex == -1 || atIndex == email.length() - 1) {
            return null;
        }
        return email.substring(atIndex + 1);
    }
}
