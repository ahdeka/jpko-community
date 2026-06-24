package com.jpkocommunity.global.init;

import com.jpkocommunity.domain.category.entity.Category;
import com.jpkocommunity.domain.category.repository.CategoryRepository;
import com.jpkocommunity.domain.comment.entity.Comment;
import com.jpkocommunity.domain.comment.repository.CommentRepository;
import com.jpkocommunity.domain.like.entity.Like;
import com.jpkocommunity.domain.like.entity.LikeType;
import com.jpkocommunity.domain.like.repository.LikeRepository;
import com.jpkocommunity.domain.notice.entity.Notice;
import com.jpkocommunity.domain.notice.repository.NoticeRepository;
import com.jpkocommunity.domain.post.entity.Post;
import com.jpkocommunity.domain.post.repository.PostRepository;
import com.jpkocommunity.domain.user.entity.User;
import com.jpkocommunity.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
@Profile("dev")
@Component
@RequiredArgsConstructor
public class DevDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final LikeRepository likeRepository;
    private final NoticeRepository noticeRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager em; // createdAt 날짜 조작용

    // seed 고정 → 재시작해도 날짜 분산 패턴이 동일하게 재현됨
    private final Random random = new Random(42L);

    private static final String[] IPS = {
            "118.235.1.1", "121.130.2.2", "211.43.3.3", "175.195.4.4", "222.106.5.5",
            "59.3.6.6",    "203.229.7.7", "61.78.8.8",  "110.70.9.9",  "114.29.10.10"
    };

    @Override
    @Transactional
    public void run(String... args) {
        // ★ 중복 실행 방지: 재시작해도 데이터가 쌓이지 않음
        if (userRepository.count() > 0) {
            log.info("===== 개발용 데이터가 이미 존재합니다. 초기화 스킵 =====");
            return;
        }

        log.info("===== 개발용 초기 데이터 삽입 시작 =====");

        List<User>     users      = initUsers();
        List<Category> categories = initCategories();
        initNotices(users);
        List<Post>     posts      = initPosts(users, categories);
        initLikes(users, posts);
        initComments(users, posts);

        log.info("===== 개발용 초기 데이터 삽입 완료 =====");
        log.info("유저 {}명 / 게시글 {}개 / 댓글 {}개 / 좋아요 {}개",
                userRepository.count(), postRepository.count(),
                commentRepository.count(), likeRepository.count());
    }

    // =====================================================================
    // 유저 (10명)
    // =====================================================================

    private List<User> initUsers() {
        List<User> users = new ArrayList<>();

        users.add(userRepository.save(User.builder()
                .email("admin@test.com")
                .password(passwordEncoder.encode("test1234"))
                .nickname("관리자")
                .build()));

        String[][] userData = {
                {"user1@test.com", "도쿄직장인"},
                {"user2@test.com", "오사카워홀러"},
                {"user3@test.com", "후쿠오카생활자"},
                {"user4@test.com", "나고야이민자"},
                {"user5@test.com", "삿포로여행자"},
                {"user6@test.com", "교토문화인"},
                {"user7@test.com", "요코하마직장인"},
                {"user8@test.com", "고베유학생"},
                {"user9@test.com", "히로시마탐방자"},
        };

        for (String[] d : userData) {
            users.add(userRepository.save(User.builder()
                    .email(d[0])
                    .password(passwordEncoder.encode("test1234"))
                    .nickname(d[1])
                    .build()));
        }

        log.info("유저 {}명 생성", users.size());
        return users;
    }

    // =====================================================================
    // 카테고리 (6개)
    // =====================================================================

    private List<Category> initCategories() {
        List<Category> categories = categoryRepository.saveAll(List.of(
                Category.builder().name("취업").slug("employment").displayOrder(1).build(),
                Category.builder().name("워킹홀리데이").slug("working-holiday").displayOrder(2).build(),
                Category.builder().name("유학").slug("study-abroad").displayOrder(3).build(),
                Category.builder().name("일본생활").slug("life").displayOrder(4).build(),
                Category.builder().name("여행").slug("travel").displayOrder(5).build(),
                Category.builder().name("자유게시판").slug("free").displayOrder(6).build()
        ));
        // 인덱스: 0=취업, 1=워홀, 2=유학, 3=일본생활, 4=여행, 5=자유게시판
        log.info("카테고리 {}개 생성", categories.size());
        return categories;
    }

    // =====================================================================
    // 공지사항 (3개)
    // =====================================================================

    private void initNotices(List<User> users) {
        User admin = users.get(0);
        noticeRepository.saveAll(List.of(
                Notice.builder()
                        .user(admin)
                        .title("[공지] JPKO 커뮤니티 이용 규칙 안내")
                        .content("안녕하세요. JPKO 커뮤니티 운영진입니다.\n\n1. 타인 비방·혐오 게시글은 삭제됩니다.\n2. 광고·스팸은 즉시 삭제 및 이용 제한 조치합니다.\n3. 개인정보(연락처, 주소 등)는 게시물에 기재하지 마세요.\n\n즐거운 커뮤니티 활동 되세요!")
                        .pinned(true)
                        .build(),
                Notice.builder()
                        .user(admin)
                        .title("[공지] 게시판 카테고리 개편 안내")
                        .content("더 편리한 이용을 위해 게시판 카테고리를 개편하였습니다.\n\n■ 신설: 잡담(일상), 상담(비자·취업·생활)\n■ 변경: 기존 자유게시판 → 잡담으로 통합\n\n이용에 불편함이 없으시길 바랍니다.")
                        .pinned(true)
                        .build(),
                Notice.builder()
                        .user(admin)
                        .title("여름 이벤트: 일본 여행 후기 공모전")
                        .content("이번 여름을 맞아 일본 여행 후기 공모전을 개최합니다.\n\n■ 응모 기간: 7월 1일 ~ 7월 31일\n■ 응모 방법: 여행 카테고리에 후기 작성 후 댓글로 신청\n■ 대상(1명): 일본 왕복 항공권\n■ 우수상(3명): 여행 상품권 10만원\n\n많은 참여 부탁드립니다!")
                        .pinned(false)
                        .build()
        ));
        log.info("공지사항 3개 생성");
    }

    // =====================================================================
    // 게시글 (100개)
    // 잡담(0~39=40개), 취업(40~54=15개), 워홀(55~69=15개),
    // 유학(70~79=10개), 일본생활(80~89=10개), 여행(90~99=10개)
    // =====================================================================

    private List<Post> initPosts(List<User> users, List<Category> categories) {
        Category employment      = categories.get(0);
        Category workingHoliday  = categories.get(1);
        Category studyAbroad     = categories.get(2);
        Category life            = categories.get(3);
        Category travel          = categories.get(4);
        Category free            = categories.get(5);

        List<Post> posts = new ArrayList<>();
        posts.addAll(initChatPosts(users, free));                       // index 0~39
        posts.addAll(initEmploymentPosts(users, employment));           // index 40~54
        posts.addAll(initWorkingHolidayPosts(users, workingHoliday));  // index 55~69
        posts.addAll(initCulturePosts(users, studyAbroad));                 // index 70~79
        posts.addAll(initConsultingPosts(users, life));           // index 80~89
        posts.addAll(initTravelPosts(users, travel));                   // index 90~99

        log.info("게시글 {}개 생성", posts.size());
        return posts;
    }

    // -------------------- 잡담 (40개) --------------------

    private List<Post> initChatPosts(List<User> users, Category category) {
        List<Post> posts = new ArrayList<>();

        // 인기글 후보 10개 (좋아요 집중 대상 — initLikes에서 posts.get(0~9) 사용)
        String[][] named = {
                {"일본에서 첫 월급 받았어요!", "드디어 일본에서 첫 월급을 받았습니다. 세금이 생각보다 많이 나오네요. 주민세+소득세 합쳐 거의 20% 가까이 떼이는 것 같아요. 그래도 일본에서 일한다는 게 뿌듯합니다 ㅎㅎ"},
                {"도쿄 생활 6개월 후기", "도쿄에 온 지 딱 6개월이 됐어요. 처음엔 언어 장벽이 힘들었는데 이제 편의점 직원이랑 소소한 대화도 해요. 가장 힘든 건 혼자 있는 시간, 가장 좋은 건 대중교통 정시성이에요."},
                {"일본 편의점 음식 vs 한국 편의점 음식", "3년 일본 생활하다 보니 일본 편의점 샌드위치는 진심인 것 같아요. 근데 한국 편의점 컵라면이 너무 그리워요 ㅠㅠ 도쿄 한인마트에서 신라면 팔긴 하는데 두 배 가격이라..."},
                {"일본어 공부 어떻게 하고 계세요?", "일본 살면서 일본어 공부 방법 공유해요! 저는 넷플릭스 일본 드라마 보면서 공부 중인데 생활 회화엔 도움이 많이 돼요. 비즈니스 일본어는 또 다른 세계더라고요."},
                {"오사카 vs 도쿄 생활비 비교", "오사카 살다가 도쿄로 이직한 지 1년 됐어요. 도쿄 임대료가 30% 정도 비싼 것 같고 식비는 비슷해요. 연봉이 좀 더 높아서 전체적으로는 비슷한 느낌이에요."},
                {"혼자 오면 외롭지 않나요?", "일본에 오기 전에 제일 걱정했던 게 외로움인데, 생각보다 한인 커뮤니티가 잘 되어 있더라고요. 직장 동료들이랑 친해지기도 하고요. 혼자 오신 분들 어떻게 적응하셨나요?"},
                {"드디어 JLPT N2 합격!", "작년부터 준비한 JLPT N2 드디어 합격했습니다!! 4번 떨어지고 이번에 붙었어요. 청해 파트 약점이었는데, 팟캐스트 많이 들은 게 효과있었던 것 같아요."},
                {"일본 라이프 소소한 팁 모음", "1. 슈퍼에서 저녁 할인 타이밍 노리기 (마감 1~2시간 전)\n2. 드럭스토어 비타민C 한국보다 저렴\n3. PayPay 없으면 불편함이 이만저만\n4. 에이다이 교통 패스 월정액으로 교통비 절약"},
                {"일본 벚꽃 시즌 한국인이 몰리는 이유", "요즘 우에노 공원 가면 한국어가 진짜 많이 들려요. 벚꽃 보면서 치킨 먹는 한국식 소풍 문화가 일본에서도 인기라는 얘기를 들었는데 ㅎㅎ"},
                {"퇴근 후 뭐하세요?", "퇴근 후 혼자 시간 보내는 게 많아지는데, 저는 동네 주민센터 수채화 강습 다니고 있어요. 의외로 일본 어르신들이랑 친해지는 좋은 기회가 됐어요."},
        };

        // 인기글 후보: 최근 7일 이내 날짜로 집중 (주간 인기글 쿼리 테스트용)
        for (int i = 0; i < named.length; i++) {
            LocalDateTime createdAt = LocalDateTime.now().minusDays(random.nextInt(7)).minusHours(random.nextInt(24));
            posts.add(savePostWithDate(users.get((i % 9) + 1), category, named[i][0], named[i][1], i % 4 == 0, IPS[i % 10], createdAt));
        }

        // 패딩 30개 (2주 이내 날짜로 분산)
        String[][] padding = {
                {"오늘 점심 뭐 드셨어요?", "오늘 점심 규동 먹었는데 맛있었어요. 요시노야 vs 마츠야 vs 스키야 어디 좋아하세요?"},
                {"일본 생활 꿀팁 공유", "일본 생활에서 유용한 앱: 메루카리, PayPay, Suica 앱은 필수예요."},
                {"한국 음식 그리울 때", "한국 음식 그리울 때 신오쿠보에 가면 좀 해소돼요. 다들 어떻게 해결하세요?"},
                {"일본어 표현 헷갈리는 것", "\"수고하셨습니다\"를 일본어로 뭐라 하는지 아직도 헷갈려요. 오츠카레사마데시타 맞죠?"},
                {"퇴근길 소소한 행복", "퇴근길 편의점 에클레어가 하루의 낙이 됐어요 ㅎㅎ"},
                {"오늘 날씨 너무 좋네요", "오늘 도쿄 날씨 정말 좋았어요. 공원에서 점심 먹고 싶네요."},
                {"주말 뭐하세요?", "이번 주말에 아키하바라 가볼까 생각 중인데, 요즘 볼거리 있을까요?"},
                {"쓰레기 분리수거 적응기", "일본 생활 초반에 제일 힘든 게 쓰레기 분리수거였어요. 지역마다 규칙이 달라서요."},
                {"편의점 신상 후기", "패밀리마트 신상 딸기 타르트 먹어봤어요? 진짜 맛있어요!"},
                {"동네 맛집 소개", "우리 동네에 한국인이 하는 삼겹살집이 생겼어요. 그리움에 자주 가게 되네요."},
                {"한인 모임 후기", "지난 주말 한인회 모임 나갔다가 좋은 분들 많이 만났어요. 금방 친해졌어요."},
                {"일본 문화 적응기", "일본 생활 2년차인데 아직도 적응 안 되는 게 조용히 줄 서는 문화예요."},
                {"혼밥 추천 메뉴", "혼자 먹기 좋은 건 라멘 추천해요. 카운터석 있는 곳이 많아서 혼밥하기 편해요."},
                {"일본어 헷갈리는 표현", "이히키우케루와 우케이레루 헷갈리는 사람 저만 있나요? ㅠㅠ"},
                {"오늘의 소소한 일상", "오늘 상사한테 일본어로 칭찬받았어요. 더 뿌듯하게 느껴지는 것 같아요."},
                {"일본에서 명절 보내기", "추석에 일본에 있으면 그냥 평범한 출근일이라 처음엔 많이 허전했어요."},
                {"친구 사귀기 어렵나요?", "친구 사귀기 어렵다고들 하는데, 취미 동호회 들어가면 생각보다 쉽게 사귀어요."},
                {"일본 회식 문화 적응기", "일본 회식에서 노미호다이 문화 처음 경험했을 때 충격받았어요. 2시간에 음료 무제한이라니."},
                {"택배 받기 어려운 상황", "택배를 낮에 못 받아서 편의점 수령을 많이 이용하는데, 처음엔 어떻게 하는지 몰랐어요."},
                {"일본 스마트폰 개통 후기", "일본에서 스마트폰 개통하는 게 생각보다 까다롭더라고요. 재류카드 지참 필수예요."},
                {"자전거 통근 꿀팁", "자전거로 출퇴근하면 교통비도 절약되고 운동도 되서 일석이조예요."},
                {"닌텐도 스위치 일본판", "닌텐도 스위치 일본판 사면 한국에서도 쓸 수 있는 거 다들 아시죠?"},
                {"근처 한식당 소개", "신오쿠보 말고 도쿄에서 한식 맛있는 데 추천해주세요!"},
                {"일본 은행 계좌 개설 팁", "일본 은행 계좌 개설할 때 유신은행이 외국인에게 그나마 수월하더라고요."},
                {"일본어 회화 파트너 구해요", "일본어 회화 연습할 파트너 구하는 분 있나요? 초중급이에요."},
                {"인터넷 개통 경험 공유", "인터넷 개통할 때 일본어로 전화 응대하는 게 제일 힘들었어요."},
                {"코스트코 추천 상품", "코스트코 추천해요. 멤버십비가 있어도 대용량 상품 가성비가 좋아요."},
                {"일본 병원 이용 후기", "일본 병원 처음 갈 때 접수 절차가 복잡해서 당황했어요. 보험증 꼭 챙기세요."},
                {"겨울 난방비 절약 팁", "겨울에 코타츠 하나 장만하면 난방비 많이 절약해요. 진짜 추천!"},
                {"도쿄 1인 생활비 공유", "도쿄 1인 생활비 공유: 월세 70만원, 식비 30만원, 교통비 10만원 정도 나와요."},
        };

        for (int i = 0; i < padding.length; i++) {
            LocalDateTime createdAt = LocalDateTime.now().minusDays(random.nextInt(14)).minusHours(random.nextInt(24));
            posts.add(savePostWithDate(users.get((i % 9) + 1), category, padding[i][0], padding[i][1], false, IPS[i % 10], createdAt));
        }

        return posts;
    }

    // -------------------- 취업 (15개) --------------------

    private List<Post> initEmploymentPosts(List<User> users, Category category) {
        List<Post> posts = new ArrayList<>();
        String[][] data = {
                {"일본 IT 취업 준비 어떻게 하셨나요?", "백엔드 개발자로 일본 취업 준비 중입니다. 포트폴리오나 언어 준비 관련해서 조언 부탁드립니다."},
                {"일본 취업 비자 종류와 요건 정리", "엔지니어 비자, 특정기능 비자 등 종류가 여러 가지인데 어떤 비자로 들어오셨나요?"},
                {"리쿠나비/마이나비 사용 후기", "외국인 채용 공고가 꽤 많더라고요. 다들 어떤 취업 사이트 이용하시나요?"},
                {"일본 스타트업 면접 후기", "지난주에 도쿄 IT 스타트업 면접 봤어요. 일본어 면접인데 기술 질문은 영어로 해도 된다고 해서 수월했어요."},
                {"JLPT 없이 취업 가능한가요?", "IT 직군은 JLPT 없이도 취업 가능하다는 얘길 들었어요. 실제로 경험하신 분 있나요?"},
                {"일본 회사 연봉 협상 팁", "오퍼를 받았는데 연봉 협상을 어떻게 해야 할지 모르겠어요. 일본은 협상 문화가 많이 다르다고 들었는데요."},
                {"IT 외 직종 일본 취업 후기", "마케팅 직군으로 일본 취업했어요. 일본어 N1 있어야 하는 포지션이 많아서 준비 기간이 꽤 걸렸어요."},
                {"일본 취업 후 주거 구하는 방법", "취업은 됐는데 집 구하는 게 더 어렵네요. 외국인이라고 거절당한 적도 있어요. 보증인 없는 외국인이 계약하는 팁 아시는 분?"},
                {"일본 취업 1년 후기", "1년 됐어요. 가장 적응하기 어려웠던 건 보고 문화였어요. 모든 걸 문서화하고 단계별로 승인받는 게 처음엔 답답했어요."},
                {"엔지니어 비자 갱신 경험 공유", "3년 비자 만료가 다가와서 갱신 준비 중이에요. 회사 서류 발급 받는 것 외에 준비할 게 꽤 많더라고요."},
                {"일본 취업 시 영어 중요도", "일본어가 약한데 영어만으로 일본 취업 가능한 IT 회사가 있을까요? 외국계 기업 위주로 찾고 있어요."},
                {"일본 취업 준비 타임라인", "일본 취업 준비를 언제부터 시작하면 좋을까요? 한국에서 2년 경력 쌓고 이직 생각 중이에요."},
                {"일본 연봉 실수령 계산법", "세전 400만엔이면 실수령이 얼마나 될까요? 세금 구조가 복잡해서 계산이 어렵네요."},
                {"일본 IT 기업 복리후생 비교", "라쿠텐, 메루카리, 사이버에이전트 등 복리후생을 비교해본 분 있나요?"},
                {"취업 비자에서 영주권까지 로드맵", "일본 영주권을 목표로 취업 준비 중인데, 취업비자 → 영주권까지 보통 몇 년이나 걸리는지 궁금해요."},
        };

        for (int i = 0; i < data.length; i++) {
            LocalDateTime createdAt = LocalDateTime.now().minusDays(random.nextInt(14)).minusHours(random.nextInt(24));
            posts.add(savePostWithDate(users.get((i % 9) + 1), category, data[i][0], data[i][1], false, IPS[i % 10], createdAt));
        }
        return posts;
    }

    // -------------------- 워킹홀리데이 (15개) --------------------

    private List<Post> initWorkingHolidayPosts(List<User> users, Category category) {
        List<Post> posts = new ArrayList<>();
        String[][] data = {
                {"오사카 워홀 비자 준비 후기", "드디어 워킹홀리데이 비자 받았습니다! 영사관 예약부터 서류까지 준비 과정 공유해요."},
                {"워홀 비자 신청 소요 시간", "워홀 비자 신청하고 발급까지 얼마나 걸렸나요? 저는 3주 정도 걸렸어요."},
                {"워홀 편의점 알바 6개월 후기", "편의점 알바 6개월 후기 적어볼게요. 일본어 실력도 늘고 생활비도 버는 일석이조였어요."},
                {"워홀 예산 계획 어떻게 세우셨나요?", "6개월 워홀 예정인데 예산을 어떻게 잡아야 할지 막막해요. 초반 자리 잡는 비용도 꽤 든다고 하던데..."},
                {"쉐어하우스 vs 원룸 : 워홀 주거 선택", "워홀러들은 쉐어하우스 많이 사는 것 같던데, 장단점 알려주세요. 저는 원룸이 더 맞는 것 같아서 고민 중이에요."},
                {"워홀 중 취업 비자로 전환 가능한가요?", "워홀로 들어와 일하다가 회사에서 비자 지원을 해준다고 하는데, 워홀 중 취업 비자 전환이 가능한지 궁금해요."},
                {"일본 워홀 알바 구하는 법", "구인 정보는 어디서 찾으세요? 저는 Indeed에서 찾았는데, 다들 어떤 방법으로 구하시는지 공유해요."},
                {"도쿄 vs 오사카 워홀 비교", "둘 다 경험해 본 분 있나요? 도시마다 느낌이 많이 다를 것 같아서 비교 후기가 궁금해요."},
                {"워홀 1년 총정리", "1년 워홀 생활 마무리하고 귀국하면서 총정리 적어봤어요. 정말 많은 걸 배운 1년이었어요."},
                {"워홀 가기 전 일본어 수준은?", "워홀 가기 전에 일본어를 어느 정도 하고 가야 할까요? 초보인데 가도 될까요?"},
                {"워홀 중 세금 환급 받는 방법", "워홀 비자로 일하면 세금을 냈는데, 귀국할 때 환급이 된다고 들었어요. 절차가 어떻게 되나요?"},
                {"일본 워홀 준비물 체크리스트", "워홀 준비물 정리해봤어요. 재류카드 받는 것부터 은행 계좌 개설, 핸드폰 개통까지 순서대로 정리했습니다."},
                {"워홀 비자 기간 연장 가능한가요?", "워홀 비자 1년 다 쓰면 연장이 안 된다고 아는데, 혹시 방법이 있을까요?"},
                {"워홀 중 다른 도시 여행", "워홀하면서 주말에 다른 도시 여행 많이 다니셨나요? 신칸센이 비싸서 버스나 LCC 많이 이용했어요."},
                {"워홀 후 한국 복귀 vs 일본 잔류", "워홀 끝나고 취업 비자 전환이 안 되면 결국 귀국인데, 다들 어떻게 결정하셨나요?"},
        };

        for (int i = 0; i < data.length; i++) {
            LocalDateTime createdAt = LocalDateTime.now().minusDays(random.nextInt(14)).minusHours(random.nextInt(24));
            posts.add(savePostWithDate(users.get((i % 9) + 1), category, data[i][0], data[i][1], i % 3 == 0, IPS[i % 10], createdAt));
        }
        return posts;
    }

    // -------------------- 유학 (10개) --------------------

    private List<Post> initCulturePosts(List<User> users, Category category) {
        List<Post> posts = new ArrayList<>();
        String[][] data = {
                {"일본 오마츠리 꼭 가봐야 할 것", "스미다가와 하나비, 아오모리 네부타, 교토 기온 마츠리 3대 축제를 다 가봤어요. 각각의 매력이 달라요."},
                {"일본 음식 문화 적응 후기", "처음엔 일본 음식이 담백하다고 느꼈는데, 이젠 오히려 한국 음식이 짜게 느껴져요. 미각이 완전히 일본화된 것 같아요."},
                {"애니메이션 성지순례 후기", "좋아하는 애니 배경지 직접 가봤는데 감동이었어요. 다들 가보고 싶은 성지 있나요?"},
                {"일본 전통 공예 체험 추천", "교토에서 도자기 만들기 체험 했는데 정말 좋았어요. 화도, 다도 체험도 추천해요."},
                {"일본 온천 문화 완전 정복", "센토와 온천의 차이부터 타투 있는 분들을 위한 팁까지. 일본 목욕 문화 처음 접하신 분들을 위해 정리했어요."},
                {"일본 만화방(만가킷사) 문화", "24시간 운영하는 만화방에서 숙박까지 가능하다는 게 신기해요. 저렴한 숙소 필요할 때 이용하기도 했어요."},
                {"가부키 공연 처음 보다", "처음엔 어려울 것 같았는데 번역 자막 이어폰을 빌려주더라고요. 예상보다 훨씬 재미있었어요."},
                {"일본 게임 문화 vs 한국 게임 문화", "한국은 PC방 문화가 강한데, 일본은 게임센터 문화가 발달해 있어요. 아케이드 게임 재미에 빠졌어요."},
                {"일본 독서 문화: 서점이 살아있다", "한국은 서점이 많이 줄었는데 일본은 동네마다 서점이 있고 사람도 많이 와요. 츠타야 서점 문화도 독특하고요."},
                {"일본 집 문화: 다다미와 현관", "현관에서 신발 벗는 게 생활화되어 있어서 좋았어요. 다다미방에서 잠든 것도 처음이었고요."},
        };

        for (int i = 0; i < data.length; i++) {
            LocalDateTime createdAt = LocalDateTime.now().minusDays(random.nextInt(14)).minusHours(random.nextInt(24));
            posts.add(savePostWithDate(users.get((i % 9) + 1), category, data[i][0], data[i][1], false, IPS[i % 10], createdAt));
        }
        return posts;
    }

    // -------------------- 일본생활 (10개) --------------------

    private List<Post> initConsultingPosts(List<User> users, Category category) {
        List<Post> posts = new ArrayList<>();
        String[][] data = {
                {"일본에서 한국 운전면허 교환 방법", "한국 운전면허를 일본 면허로 교환하는 방법이 있다고 들었어요. 절차가 어떻게 되나요?"},
                {"재류카드 갱신 깜빡하면 어떻게 되나요?", "만료일이 다가오는데 이민국 예약이 꽉 차 있어요. 만료 후 유예 기간이 있는지 궁금해요."},
                {"일본 국민건강보험 신청 방법", "회사 보험이 없는 프리랜서나 워홀러는 건강보험을 어떻게 처리하나요?"},
                {"일본에서 한국 가족에게 송금하는 법", "부모님께 송금하려는데 수수료가 저렴한 방법이 있나요? 와이즈(Wise)랑 소니뱅크 중 고민 중이에요."},
                {"일본에서 세금 신고 해야 하나요?", "프리랜서로 일하면서 수입이 생겼는데 확정신고를 해야 하는지 궁금해요."},
                {"일본 주민세가 왜 이렇게 많이 나오나요?", "6월에 갑자기 주민세가 왕창 나왔는데, 왜 6월에 몰려서 나오는 건가요?"},
                {"일본 아파트 퇴실 시 주의사항", "이사를 앞두고 퇴실 청소나 원상복구 관련 분쟁 없이 보증금 돌려받는 법이 궁금해요."},
                {"일본 인터넷 계약 해지 절차", "귀국을 앞두고 인터넷 해지 절차가 복잡해요. 위약금도 있고 일본어로 전화해야 해서 막막해요."},
                {"일본 국민연금 탈퇴 일시금 신청", "귀국하면 낸 국민연금을 일부 돌려받을 수 있다고 들었는데, 신청 방법과 금액 계산법을 알려주세요."},
                {"일본에서 한국 전자레인지 쓸 수 있나요?", "한국 전자제품을 가져가려 하는데 일본은 전압이 100V라 변압기가 필요한지 궁금해요."},
        };

        for (int i = 0; i < data.length; i++) {
            LocalDateTime createdAt = LocalDateTime.now().minusDays(random.nextInt(14)).minusHours(random.nextInt(24));
            posts.add(savePostWithDate(users.get((i % 9) + 1), category, data[i][0], data[i][1], i % 2 == 0, IPS[i % 10], createdAt));
        }
        return posts;
    }

    // -------------------- 여행 (10개) --------------------

    private List<Post> initTravelPosts(List<User> users, Category category) {
        List<Post> posts = new ArrayList<>();
        String[][] data = {
                {"도쿄 3박4일 여행 코스 추천", "다음달 도쿄 여행 예정인데 처음이라 아무것도 모릅니다. 꼭 가봐야 할 곳 추천 부탁해요!"},
                {"오사카 먹방 여행 2박3일", "오사카는 먹기 위해 가는 도시라고 해도 과언이 아니죠. 타코야키, 오코노미야키, 구시카츠 먹방 코스 정리했어요."},
                {"교토 당일치기 추천 코스", "오사카에서 당일치기로 교토 가는 방법과 추천 코스 공유해요."},
                {"후쿠오카 여행 총정리", "한국에서 가장 가까운 일본 도시 후쿠오카. 하카타 라멘, 모츠나베, 야타이 문화까지 총정리했어요."},
                {"삿포로 겨울 여행 후기", "유키마츠리 맞춰서 삿포로 다녀왔어요. -15도의 추위 속에서도 축제 분위기는 최고였어요."},
                {"나고야 히든 여행지 소개", "이름은 들어봤는데 잘 안 가는 나고야. 나고야성, 오스 상점가, 나고야 모닝 문화까지 소개해요."},
                {"일본 JR패스 활용 여행법", "JR패스로 신칸센 타고 일본 일주 했어요. 루트 짜는 방법 공유할게요."},
                {"가마쿠라 당일치기 추천", "도쿄에서 1시간 거리 가마쿠라. 대불상, 하치만구, 골목 가게들 당일치기 코스 공유합니다."},
                {"일본 시골 마을 여행: 시라카와고", "관광지 말고 진짜 일본 시골을 느끼고 싶다면 기후현 시라카와고를 추천해요. 갓쇼즈쿠리 마을이 인상적이었어요."},
                {"일본 숙소 종류 비교 (호텔 vs 료칸 vs 게스트하우스)", "처음 일본 여행 가는 분들을 위해 숙소 종류별 장단점 정리했어요. 료칸은 꼭 한번 경험해보세요!"},
        };

        for (int i = 0; i < data.length; i++) {
            LocalDateTime createdAt = LocalDateTime.now().minusDays(random.nextInt(14)).minusHours(random.nextInt(24));
            posts.add(savePostWithDate(users.get((i % 9) + 1), category, data[i][0], data[i][1], i == 2, IPS[i % 10], createdAt));
        }
        return posts;
    }

    /**
     * 운영 코드에서는 절대 사용하지 않는 메서드
     */
    private Post savePostWithDate(User user, Category category, String title, String content,
                                  boolean anonymous, String ip, LocalDateTime createdAt) {
        Post post = postRepository.save(Post.builder()
                .user(user).category(category).title(title).content(content)
                .anonymous(anonymous).ipAddress(ip).build());

        em.createQuery("UPDATE Post p SET p.createdAt = :createdAt WHERE p.id = :id")
                .setParameter("createdAt", createdAt)
                .setParameter("id", post.getId())
                .executeUpdate();

        return post;
    }

    // =====================================================================
    // 좋아요/싫어요
    //
    // 인덱스 구조 (initPosts 순서와 일치):
    //   잡담 인기글 후보: posts.get(0~9)     ← 좋아요 집중
    //   취업 대표글:      posts.get(40)
    //   워홀 대표글:      posts.get(55)
    //   문화 대표글:      posts.get(70)
    //   상담 대표글:      posts.get(80)
    //   여행 대표글:      posts.get(90)
    // =====================================================================

    private void initLikes(List<User> users, List<Post> posts) {
        // 잡담 인기글 top 10 (주간 인기글 쿼리 테스트용 — 좋아요 수 차등 부여)
        addLikes(posts.get(0), users, new int[]{1,2,3,4,5,6,7,8}, LikeType.LIKE);
        addLikes(posts.get(1), users, new int[]{1,2,3,4,5,6,7},   LikeType.LIKE);
        addLikes(posts.get(2), users, new int[]{2,3,4,5,6,7,8,9}, LikeType.LIKE);
        addLikes(posts.get(3), users, new int[]{1,2,3,4,5,6},     LikeType.LIKE);
        addLikes(posts.get(4), users, new int[]{3,4,5,6,7,8,9},   LikeType.LIKE);
        addLikes(posts.get(5), users, new int[]{1,2,3,4,5},       LikeType.LIKE);
        addLikes(posts.get(6), users, new int[]{2,4,6,8,9},       LikeType.LIKE);
        addLikes(posts.get(7), users, new int[]{1,3,5,7,9},       LikeType.LIKE);
        addLikes(posts.get(8), users, new int[]{2,3,4,5},         LikeType.LIKE);
        addLikes(posts.get(9), users, new int[]{1,2,3},           LikeType.LIKE);

        // 각 카테고리 대표글에도 좋아요
        addLikes(posts.get(40), users, new int[]{1,2,3,4,5,6}, LikeType.LIKE);
        addLikes(posts.get(55), users, new int[]{1,2,3,4,5,6}, LikeType.LIKE);
        addLikes(posts.get(70), users, new int[]{1,2,3,4,5},   LikeType.LIKE);
        addLikes(posts.get(80), users, new int[]{1,2,3,4,5},   LikeType.LIKE);
        addLikes(posts.get(90), users, new int[]{1,2,3,4,5},   LikeType.LIKE);

        // 싫어요 (소수) — LIKE 목록과 겹치지 않는 유저로 지정
        addLike(posts.get(8),  users.get(9), LikeType.DISLIKE); // posts[8] LIKE: {2,3,4,5}
        addLike(posts.get(9),  users.get(6), LikeType.DISLIKE); // posts[9] LIKE: {1,2,3}
        addLike(posts.get(41), users.get(7), LikeType.DISLIKE);

        log.info("좋아요/싫어요 데이터 생성");
    }

    private void addLikes(Post post, List<User> users, int[] userIndices, LikeType type) {
        for (int idx : userIndices) {
            addLike(post, users.get(idx), type);
        }
    }

    private void addLike(Post post, User user, LikeType type) {
        likeRepository.save(Like.builder().post(post).user(user).type(type).build());
    }

    // =====================================================================
    // 댓글 — 맥락 있는 댓글은 주요 게시글에 수동으로, 나머지는 루프로 보충
    // =====================================================================

    private void initComments(List<User> users, List<Post> posts) {
        // 주요 게시글: 맥락에 맞는 자연스러운 댓글 수동 작성
        addCommentsToChat0(users, posts.get(0));
        addCommentsToChat1(users, posts.get(1));
        addCommentsToChat2(users, posts.get(2));
        addCommentsToChat3(users, posts.get(3));
        addCommentsToEmployment(users, posts.get(40));
        addCommentsToWorkingHoliday(users, posts.get(55));
        addCommentsToCulture(users, posts.get(70));
        addCommentsToConsulting(users, posts.get(80));
        addCommentsToTravel(users, posts.get(90));

        // 나머지 게시글: 댓글 1~3개 루프로 보충 (댓글 0인 게시글이 없도록)
        String[] genericComments = {
                "좋은 정보 감사합니다!", "저도 같은 고민 중이었는데 도움이 됐어요.",
                "공감해요. 저도 비슷한 경험 있어요.", "혹시 더 자세한 내용 알 수 있을까요?",
                "이런 글 너무 반가워요. 잘 읽었습니다.", "저장해 뒀어요. 나중에 참고할게요!",
                "오 이런 방법이 있었군요. 몰랐어요.", "처음 알았어요. 감사합니다.",
        };

        // 댓글이 이미 달린 게시글 인덱스 (수동 작성 대상)
        java.util.Set<Integer> manualIndices = java.util.Set.of(0, 1, 2, 3, 40, 55, 70, 80, 90);

        for (int i = 0; i < posts.size(); i++) {
            if (manualIndices.contains(i)) continue;
            Post post = posts.get(i);
            int count = 1 + (i % 3); // 1~3개
            for (int c = 0; c < count; c++) {
                User commenter = users.get(1 + ((i + c) % 9));
                save(post, commenter, null, genericComments[(i + c) % genericComments.length], false, IPS[(i + c) % 10]);
            }
        }

        log.info("댓글 데이터 생성 완료");
    }

    // -------------------- 주요 게시글 댓글 (맥락 맞춤) --------------------

    private void addCommentsToChat0(List<User> users, Post post) {
        Comment c1 = save(post, users.get(2), null, "저도 첫 월급 받았을 때 감동이었어요! 세금은 좀 적응이 필요하죠 ㅠㅠ", false, "121.130.2.2");
        save(post, users.get(3), c1, "주민세는 전년도 소득 기준이라 첫 해엔 없고 이듬해 6월에 폭탄이 날아와요.", false, "211.43.3.3");
        save(post, users.get(1), c1, "맞아요, 저도 첫 해 6월에 놀랐어요. 미리 저축해두는 게 좋아요!", false, "118.235.1.1");
        save(post, users.get(4), null, "일본 직장 첫 월급 실수령 얼마였어요? 참고용으로 궁금합니다.", true, "175.195.4.4");
        save(post, users.get(5), null, "부럽다... 저도 빨리 취직해야 하는데 ㅠㅠ", false, "222.106.5.5");
    }

    private void addCommentsToChat1(List<User> users, Post post) {
        Comment c1 = save(post, users.get(3), null, "6개월 만에 편의점 직원이랑 대화하다니 대단해요! 저는 1년 됐는데 아직도 어려워요.", false, "211.43.3.3");
        save(post, users.get(1), c1, "처음엔 저도 너무 어려웠어요. 매일 조금씩 하면 어느 순간 늘더라고요!", false, "118.235.1.1");
        Comment c2 = save(post, users.get(5), null, "대중교통 시간 정확한 거 진짜 최고... 한국 버스는 앱 믿기도 어렵잖아요 ㅋㅋ", false, "222.106.5.5");
        save(post, users.get(4), c2, "맞아요ㅋㅋ 일본 오고 나서 지각 한 번도 안 했어요.", false, "175.195.4.4");
    }

    private void addCommentsToChat2(List<User> users, Post post) {
        Comment c1 = save(post, users.get(6), null, "일본 편의점 샌드위치는 진짜 맛있죠! 특히 로손 크림치즈샌드는 진리예요.", false, "59.3.6.6");
        save(post, users.get(2), c1, "패밀리마트 타마고 산도도 진짜 맛있어요! 아 그리워라...", false, "121.130.2.2");
        save(post, users.get(7), null, "신라면은 코리아 마트 가면 파는데, 진라면이나 삼양라면은 찾기가 어렵더라고요.", false, "61.78.8.8");
        save(post, users.get(8), null, "일본 편의점 모두야키는 한국에 없어서 너무 그리워요 ㅠ", false, "110.70.9.9");
    }

    private void addCommentsToChat3(List<User> users, Post post) {
        Comment c1 = save(post, users.get(2), null, "저는 NHK 뉴스 매일 보면서 공부했어요. 느리게 읽어줘서 듣기 연습에 좋아요.", false, "121.130.2.2");
        save(post, users.get(4), c1, "NHK 야사시이 뉴스 웹사이트 추천해요. 히라가나로 되어 있어서 초보도 읽기 쉬워요.", false, "175.195.4.4");
        save(post, users.get(9), null, "비즈니스 일본어는 경어가 핵심이에요. 책으로 공부하고 실무에서 써보는 것 반복이 최고예요.", false, "114.29.10.10");
    }

    private void addCommentsToEmployment(List<User> users, Post post) {
        Comment c1 = save(post, users.get(2), null, "저도 작년에 준비했는데요, JLPT N2 이상은 거의 필수인 것 같더라고요.", false, "121.130.2.2");
        save(post, users.get(1), c1, "N2 정도면 충분할까요? N1까지 필요한지 고민 중이에요.", false, "118.235.1.1");
        save(post, users.get(2), c1, "회사마다 다른데 IT는 N2로도 많이 뽑더라고요.", false, "121.130.2.2");
        save(post, users.get(1), null, "포트폴리오는 깃허브에 영어로 정리하는 게 유리하다고 들었어요.", true, "118.235.1.1");
    }

    private void addCommentsToWorkingHoliday(List<User> users, Post post) {
        Comment c1 = save(post, users.get(1), null, "비자 준비 기간은 얼마나 걸리셨나요?", false, "118.235.1.1");
        save(post, users.get(2), c1, "영사관 예약 포함해서 한 달 정도요. 서류는 일주일이면 충분해요.", false, "121.130.2.2");
        save(post, users.get(3), null, "서류 목록 공유해주실 수 있나요? 준비하는 중이에요!", false, "211.43.3.3");
    }

    private void addCommentsToCulture(List<User> users, Post post) {
        save(post, users.get(4), null, "스미다가와 하나비는 자리 잡기 너무 힘들지 않나요? 몇 시간 전부터 자리 잡아야 하나요?", false, "175.195.4.4");
        save(post, users.get(5), null, "네부타 축제 때 참여 행렬에 직접 끼어서 춤출 수도 있다고 하던데, 진짜인가요?", false, "222.106.5.5");
    }

    private void addCommentsToConsulting(List<User> users, Post post) {
        save(post, users.get(6), null, "한국 면허 교환은 기술시험이 면제돼서 필기랑 시력 검사만 통과하면 돼요!", false, "59.3.6.6");
        save(post, users.get(7), null, "번역 공증도 필요한가요? 필요한 서류 목록 공유해 주세요~", false, "61.78.8.8");
    }

    private void addCommentsToTravel(List<User> users, Post post) {
        Comment c1 = save(post, users.get(3), null, "도쿄 처음이시면 신주쿠, 시부야, 아사쿠사는 꼭 가보세요!", false, "211.43.3.3");
        save(post, users.get(5), c1, "도쿄 스카이트리에서 야경 보는 것도 추천해요!", false, "222.106.5.5");
        save(post, users.get(7), null, "JR패스 쓰면 도쿄역에서 나리타까지 무료로 탈 수 있어요!", false, "61.78.8.8");
    }

    // -------------------- 공통 댓글 저장 --------------------

    private Comment save(Post post, User user, Comment parent,
                         String content, boolean anonymous, String ip) {
        return commentRepository.save(Comment.builder()
                .post(post).user(user).parent(parent)
                .content(content).anonymous(anonymous).ipAddress(ip).build());
    }
}