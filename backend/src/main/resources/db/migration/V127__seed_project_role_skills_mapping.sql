-- ============================================================
-- FLYWAY MIGRATION V127: SEED PROJECT ROLE TO SKILL MAPPINGS
-- Epic: NCL-10 (Báo cáo & Phân tích)
-- Story: NCL-10-CN-005 (Báo cáo nhu cầu tuyển dụng theo kỹ năng)
-- ============================================================

-- 1. Bổ sung các Kỹ năng chuẩn còn thiếu nếu chưa có
INSERT INTO skills (code, name, category, description)
SELECT 'TESTING', 'Kiểm thử (Testing / QA)', 'Testing', 'Kiểm thử phần mềm, kiểm thử tự động và đảm bảo chất lượng'
WHERE NOT EXISTS (SELECT 1 FROM skills WHERE code IN ('TESTING', 'TEST'));

INSERT INTO skills (code, name, category, description)
SELECT 'BUSINESS_ANALYSIS', 'Phân tích nghiệp vụ (BA)', 'Analysis', 'Phân tích yêu cầu nghiệp vụ, đặc tả quy trình và thiết kế luồng người dùng'
WHERE NOT EXISTS (SELECT 1 FROM skills WHERE code IN ('BUSINESS_ANALYSIS', 'BA'));

INSERT INTO skills (code, name, category, description)
SELECT 'UI_UX_DESIGN', 'Thiết kế UI/UX', 'Design', 'Thiết kế trải nghiệm người dùng, giao diện Figma và xây dựng prototype'
WHERE NOT EXISTS (SELECT 1 FROM skills WHERE code IN ('UI_UX_DESIGN', 'UIUX'));

-- 2. Ánh xạ Vai trò DEV (Lập trình) -> Các kỹ năng lập trình (JAVA, SPRING_BOOT, REACT, TYPESCRIPT)
INSERT INTO project_role_skills (role_id, skill_id, required_level, status)
SELECT pr.id, s.id, 1, 'ACTIVE'
FROM project_roles pr
CROSS JOIN skills s
WHERE pr.code = 'DEV'
  AND s.code IN ('JAVA', 'SPRING_BOOT', 'REACT', 'TYPESCRIPT')
  AND NOT EXISTS (
      SELECT 1 FROM project_role_skills prs 
      WHERE prs.role_id = pr.id AND prs.skill_id = s.id
  );

-- 3. Ánh xạ Vai trò TEST (Kiểm thử) -> Kỹ năng TESTING
INSERT INTO project_role_skills (role_id, skill_id, required_level, status)
SELECT pr.id, s.id, 1, 'ACTIVE'
FROM project_roles pr
CROSS JOIN skills s
WHERE pr.code = 'TEST'
  AND s.code IN ('TESTING', 'TEST')
  AND NOT EXISTS (
      SELECT 1 FROM project_role_skills prs 
      WHERE prs.role_id = pr.id AND prs.skill_id = s.id
  );

-- 4. Ánh xạ Vai trò BA (Phân tích nghiệp vụ) -> Kỹ năng BUSINESS_ANALYSIS
INSERT INTO project_role_skills (role_id, skill_id, required_level, status)
SELECT pr.id, s.id, 1, 'ACTIVE'
FROM project_roles pr
CROSS JOIN skills s
WHERE pr.code = 'BA'
  AND s.code IN ('BUSINESS_ANALYSIS', 'BA')
  AND NOT EXISTS (
      SELECT 1 FROM project_role_skills prs 
      WHERE prs.role_id = pr.id AND prs.skill_id = s.id
  );

-- 5. Ánh xạ Vai trò UIUX (Thiết kế UI/UX) -> Kỹ năng UI_UX_DESIGN
INSERT INTO project_role_skills (role_id, skill_id, required_level, status)
SELECT pr.id, s.id, 1, 'ACTIVE'
FROM project_roles pr
CROSS JOIN skills s
WHERE pr.code = 'UIUX'
  AND s.code IN ('UI_UX_DESIGN', 'UIUX')
  AND NOT EXISTS (
      SELECT 1 FROM project_role_skills prs 
      WHERE prs.role_id = pr.id AND prs.skill_id = s.id
  );

-- 6. Ánh xạ Vai trò DEVOPS -> Kỹ năng DOCKER
INSERT INTO project_role_skills (role_id, skill_id, required_level, status)
SELECT pr.id, s.id, 1, 'ACTIVE'
FROM project_roles pr
CROSS JOIN skills s
WHERE pr.code = 'DEVOPS'
  AND s.code IN ('DOCKER')
  AND NOT EXISTS (
      SELECT 1 FROM project_role_skills prs 
      WHERE prs.role_id = pr.id AND prs.skill_id = s.id
  );
