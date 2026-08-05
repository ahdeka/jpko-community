-- ============================================================
-- V32: 정지 기한 프리셋에서 이름(label)·표시순서(display_order) 제거.
--   ROOT 요청: 이름은 필요 없고 기한(분)만 입력하면 되고, 표시 순서도 수동 지정이 아니라
--   항상 minutes 오름차순(짧은 것 → 긴 것)으로 자동 정렬하면 된다. 기존 5개 프리셋의
--   minutes 값은 그대로 유지된다 — 사라지는 건 label/display_order 컬럼뿐이다.
-- ============================================================

ALTER TABLE suspend_duration_presets
    DROP COLUMN label,
    DROP COLUMN display_order;
