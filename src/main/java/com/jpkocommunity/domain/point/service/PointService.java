package com.jpkocommunity.domain.point.service;

import com.jpkocommunity.domain.point.dto.response.PointResponse;
import com.jpkocommunity.domain.point.entity.Point;
import com.jpkocommunity.domain.point.repository.PointRepository;
import com.jpkocommunity.domain.user.entity.User;
import com.jpkocommunity.domain.user.service.UserService;
import com.jpkocommunity.global.exception.CustomException;
import com.jpkocommunity.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PointService {

    private final PointRepository pointRepository;
    private final UserService userService;
    private final TransactionService transactionService;

    public PointResponse getPointByUserId(Long userId) {
        Point res = get(userId);
        return new PointResponse(res.getPoint(), res.getUser());
    }










    // ========== private 메서드 ==========

    private Point get(Long userId) {
        User user = userService.findById(userId);
        Optional<Point> existing = pointRepository.findByUser(user);

        if (existing.isEmpty()) {
            return createFirst(userId);
        }

        return existing.orElseThrow(() -> new CustomException(ErrorCode.LOADPOINT_ERROR));
    }

    private Point update(Long userId, Long updatedPoint) {
        Point target = get(userId);

        if (target.getUser().getId().equals(userId)) {
            target.setPoint(updatedPoint);
            pointRepository.save(target);
            return get(userId);
        } else  {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
    }

    private Point createFirst(Long userId) {
        try {
            User user = userService.findById(userId);

            // TODO: 관리자 페이지에서 초기 부여 포인트 관리하는 기능 추가 후 변경 필요
            Long initialPoint = 0L;
            pointRepository.save(Point.builder()
                    .user(user)
                    .point(initialPoint)
                    .real_point(initialPoint)
                    .build());



            return get(userId);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.INITIALPOINT_ERROR);
        }
    }

    private PointResponse buildResponse(Long userId) {
        if (userId == null) {
            return null;
        }

        long point = get(userId).getPoint();
        User user = userService.findById(userId);

        return new PointResponse(point, user);
    }
}
