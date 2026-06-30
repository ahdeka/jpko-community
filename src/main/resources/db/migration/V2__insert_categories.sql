-- ============================================================
-- V2: 카테고리 시드 데이터 (고정 레퍼런스 데이터)
-- 사이트 운영에 필수. 거의 변경되지 않는 정적 데이터.
-- ============================================================
INSERT INTO categories (name, slug, display_order, created_at, updated_at)
VALUES ('취업', 'employment', 1, NOW(), NOW()),
       ('워킹홀리데이', 'working-holiday', 2, NOW(), NOW()),
       ('유학', 'study-abroad', 3, NOW(), NOW()),
       ('일본생활', 'life', 4, NOW(), NOW()),
       ('여행', 'travel', 5, NOW(), NOW()),
       ('자유게시판', 'free', 6, NOW(), NOW());