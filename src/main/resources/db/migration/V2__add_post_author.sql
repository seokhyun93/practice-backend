-- 1) 일단 NULL을 허용하는 컬럼으로 추가 (기존 글이 있어도 성공)
ALTER TABLE post ADD COLUMN author VARCHAR(30) NULL AFTER content;

-- 2) 기존 글에 값 채우기
UPDATE post SET author = '익명' WHERE author IS NULL;

-- 3) 이제 모든 글에 값이 있으니 NOT NULL로 변경
ALTER TABLE post MODIFY author VARCHAR(30) NOT NULL;
