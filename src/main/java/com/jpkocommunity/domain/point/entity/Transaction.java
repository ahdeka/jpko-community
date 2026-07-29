package com.jpkocommunity.domain.point.entity;

import com.jpkocommunity.domain.user.entity.User;
import com.jpkocommunity.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
//@Getter
@Builder
@AllArgsConstructor
@Data
// 포인트 거래 기록용 엔티티
public class Transaction extends BaseEntity {
    // 송금인
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private User sender;

    // 수취인
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id")
    private User recipient;

    // 거래액
    private Long amount;

    // 거래 결과
    @Enumerated(EnumType.STRING)
    private TransactionType type;

    // 송금인 거래 전 잔액
    private Long prev_sender;
    // 송금인 거래 후 잔액
    private Long post_sender;

    // 수취인 거래 전 잔액
    private Long prev_recipient;
    // 수취인 거래 후 잔액
    private Long post_recipient;

    // 비고
    private String msg;
}
