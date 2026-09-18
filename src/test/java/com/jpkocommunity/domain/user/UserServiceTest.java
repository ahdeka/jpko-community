package com.jpkocommunity.domain.user;

import com.jpkocommunity.domain.user.dto.request.UpdateNicknameRequest;
import com.jpkocommunity.domain.user.entity.User;
import com.jpkocommunity.domain.user.repository.UserRepository;
import com.jpkocommunity.domain.user.service.UserService;
import com.jpkocommunity.global.exception.CustomException;
import com.jpkocommunity.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void 금칙어_닉네임으론_변경_불가능() {
        User user = User.builder().nickname("기존닉네임").build();
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        assertThatThrownBy(() -> userService.updateNickname(1L, new UpdateNicknameRequest("관리자")))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BLACKLIST_NICKNAME);
    }
}
