import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

test("Project creation modal and view UI copy are user-friendly and avoid technical terms", () => {
  const modalFilePath = path.resolve(__dirname, "../components/project/ProjectCreateModal.tsx");
  const modalContent = fs.readFileSync(modalFilePath, "utf-8");

  // Ensure button copy does not use technical/internal terms like "Tạo Dự Án Thật"
  assert.equal(
    modalContent.includes("Tạo Dự Án Thật"),
    false,
    "ProjectCreateModal should not contain technical label 'Tạo Dự Án Thật'"
  );
  assert.equal(
    modalContent.includes("Tạo dự án"),
    true,
    "ProjectCreateModal should contain friendly label 'Tạo dự án'"
  );
  assert.equal(
    modalContent.includes("Tạo dự án từ mẫu"),
    true,
    "ProjectCreateModal should contain friendly label 'Tạo dự án từ mẫu'"
  );

  const viewFilePath = path.resolve(__dirname, "../components/project/ProjectView.tsx");
  const viewContent = fs.readFileSync(viewFilePath, "utf-8");

  // Ensure toast copy does not expose database implementation details
  assert.equal(
    viewContent.includes("Tạo thành công trong Database!"),
    false,
    "ProjectView should not contain 'Tạo thành công trong Database!'"
  );
  assert.equal(
    viewContent.includes("showToast('Đã tạo dự án thành công.', 'success')"),
    true,
    "ProjectView should show friendly success toast 'Đã tạo dự án thành công.'"
  );

  // Ensure refresh toast does not mention Database
  assert.equal(
    viewContent.includes("Đã làm mới dữ liệu từ Database"),
    false,
    "ProjectView should not contain 'Đã làm mới dữ liệu từ Database'"
  );
  assert.equal(
    viewContent.includes("showToast('Đã làm mới dữ liệu.', 'info')"),
    true,
    "ProjectView should show friendly refresh toast 'Đã làm mới dữ liệu.'"
  );
});
