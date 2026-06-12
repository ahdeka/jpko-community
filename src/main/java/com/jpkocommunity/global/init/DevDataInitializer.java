package com.jpkocommunity.global.init;

import com.jpkocommunity.domain.category.entity.Category;
import com.jpkocommunity.domain.category.repository.CategoryRepository;
import com.jpkocommunity.domain.comment.entity.Comment;
import com.jpkocommunity.domain.comment.repository.CommentRepository;
import com.jpkocommunity.domain.post.entity.Post;
import com.jpkocommunity.domain.post.repository.PostRepository;
import com.jpkocommunity.domain.user.entity.User;
import com.jpkocommunity.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Profile("dev")
@Component
@RequiredArgsConstructor
public class DevDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("===== 개발용 초기 데이터 삽입 시작 =====");

        List<User> users           = initUsers();
        List<Category> categories  = initCategories();
        List<Post> posts           = initPosts(users, categories);
        initComments(users, posts);

        log.info("===== 개발용 초기 데이터 삽입 완료 =====");
    }

    // ========== 유저 ==========

    private List<User> initUsers() {
        User admin = userRepository.save(User.builder()
                .email("admin@test.com")
                .password(passwordEncoder.encode("test1234"))
                .nickname("관리자")
                .build());

        User user1 = userRepository.save(User.builder()
                .email("user1@test.com")
                .password(passwordEncoder.encode("test1234"))
                .nickname("도쿄직장인")
                .build());

        User user2 = userRepository.save(User.builder()
                .email("user2@test.com")
                .password(passwordEncoder.encode("test1234"))
                .nickname("오사카워홀러")
                .build());

        log.info("유저 {}명 생성", 3);
        return List.of(admin, user1, user2);
    }

    // ========== 카테고리 ==========

    private List<Category> initCategories() {
        List<Category> categories = categoryRepository.saveAll(List.of(
                Category.builder().name("취업").slug("employment").displayOrder(2).build(),
                Category.builder().name("워킹홀리데이").slug("working-holiday").displayOrder(3).build(),
                Category.builder().name("문화").slug("culture").displayOrder(4).build(),
                Category.builder().name("상담").slug("consulting").displayOrder(5).build(),
                Category.builder().name("여행").slug("travel").displayOrder(6).build(),
                Category.builder().name("잡담").slug("chat").displayOrder(1).build()
        ));

        log.info("카테고리 {}개 생성", categories.size());
        return categories;
    }

    // ========== 게시글 ==========

    private List<Post> initPosts(List<User> users, List<Category> categories) {
        User user1    = users.get(1);
        User user2    = users.get(2);
        Category employment     = categories.get(0); // 취업
        Category workingHoliday = categories.get(1); // 워킹홀리데이
        Category travel         = categories.get(4); // 여행

        Post post1 = createPost(user1, employment,
                "일본 IT 취업 준비 어떻게 하셨나요?",
                "저는 현재 백엔드 개발자로 일본 취업을 준비 중입니다. 포트폴리오나 언어 준비 관련해서 조언 부탁드립니다.",
                false, "118.235.1.1");

        Post post2 = createPost(user2, workingHoliday,
                "오사카 워홀 비자 준비 후기",
                "드디어 워킹홀리데이 비자 받았습니다! 준비 과정 공유해요. 영사관 예약부터 서류까지 정리했어요.",
                false, "121.130.2.2");

        Post post3 = createPost(user1, travel,
                "도쿄 3박4일 여행 코스 추천해줘요",
                "다음달 도쿄 여행 예정인데 처음이라 아무것도 모릅니다. 꼭 가봐야 할 곳 추천 부탁해요!",
                true, "118.235.1.1");

        log.info("게시글 {}개 생성", 3);
        return List.of(post1, post2, post3);
    }

    private Post createPost(User user, Category category, String title, String content,
                            boolean anonymous, String ip) {
        Post post = Post.builder()
                .user(user)
                .category(category)
                .title(title)
                .content(content)
                .anonymous(anonymous)
                .ipAddress(ip)
                .build();

        return postRepository.save(post);
    }

    // ========== 댓글 ==========

    private void initComments(List<User> users, List<Post> posts) {
        User user1 = users.get(1);
        User user2 = users.get(2);
        Post post1 = posts.get(0); // 취업 게시글
        Post post2 = posts.get(1); // 워홀 게시글

        // 취업 게시글 댓글
        Comment comment1 = commentRepository.save(Comment.builder()
                .post(post1)
                .user(user2)
                .content("저도 작년에 준비했는데요, JLPT N2 이상은 거의 필수인 것 같더라고요.")
                .anonymous(false)
                .ipAddress("121.130.2.2")
                .build());

        // 대댓글
        commentRepository.save(Comment.builder()
                .post(post1)
                .user(user1)
                .parent(comment1)
                .content("N2 정도면 충분할까요? N1까지 필요한지 고민 중이에요.")
                .anonymous(false)
                .ipAddress("118.235.1.1")
                .build());

        commentRepository.save(Comment.builder()
                .post(post1)
                .user(user2)
                .parent(comment1)
                .content("회사마다 다른데 IT는 N2로도 많이 뽑더라고요.")
                .anonymous(false)
                .ipAddress("121.130.2.2")
                .build());

        // 익명 댓글
        commentRepository.save(Comment.builder()
                .post(post1)
                .user(user1)
                .content("포트폴리오는 깃허브에 영어로 정리하는 게 유리하다고 들었어요.")
                .anonymous(true)
                .ipAddress("118.235.1.1")
                .build());

        // 워홀 게시글 댓글
        Comment comment2 = commentRepository.save(Comment.builder()
                .post(post2)
                .user(user1)
                .content("비자 준비 기간은 얼마나 걸리셨나요?")
                .anonymous(false)
                .ipAddress("118.235.1.1")
                .build());

        commentRepository.save(Comment.builder()
                .post(post2)
                .user(user2)
                .parent(comment2)
                .content("영사관 예약 포함해서 한 달 정도요. 서류는 일주일이면 충분해요.")
                .anonymous(false)
                .ipAddress("121.130.2.2")
                .build());

        log.info("댓글 {}개 생성", 6);
    }
}
