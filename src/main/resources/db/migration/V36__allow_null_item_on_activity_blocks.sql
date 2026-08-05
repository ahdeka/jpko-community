-- ============================================================
-- V36: activity_blocks.item_id를 nullable로 변경
--   ADMIN이 관리자 페이지에서 유저를 정지시키면 상점 구매가 아니라 이 차단으로 대체되므로
--   연결할 상품(item)이 없다. item_id가 null이면 "관리자 정지" 유래 차단으로 취급한다.
-- ============================================================

ALTER TABLE activity_blocks ALTER COLUMN item_id DROP NOT NULL;
