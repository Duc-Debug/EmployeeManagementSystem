import { describe, test } from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// Regex and validation logic matching password-policy.ts
const PASSWORD_POLICY_REGEX = /^(?=.*[A-Za-z])(?=.*\d).{8,}$/;
const PASSWORD_POLICY_MESSAGE = "Mật khẩu phải có tối thiểu 8 ký tự, bao gồm cả chữ cái và chữ số.";

function isValidPassword(password) {
    return typeof password === "string" && PASSWORD_POLICY_REGEX.test(password);
}

describe("Password Policy & Unified Validator Tests", () => {
    test("Valid passwords with min 8 chars and both letters & numbers pass validation", () => {
        const validList = [
            "Pass1234",
            "StrongPassword123!",
            "Admin123",
            "1234567a",
            "a1b2c3d4",
            "P@ssw0rd2026"
        ];
        for (const pwd of validList) {
            assert.equal(isValidPassword(pwd), true, `Password '${pwd}' should be valid`);
        }
    });

    test("Invalid passwords fail validation", () => {
        const invalidList = [
            "",
            null,
            undefined,
            "123456",          // 6 digits
            "12345678",        // 8 digits no letter
            "abcdefgh",        // 8 letters no digit
            "pass1",           // 5 chars
            "Short1"           // 6 chars
        ];
        for (const pwd of invalidList) {
            assert.equal(isValidPassword(pwd), false, `Password '${pwd}' should be invalid`);
        }
    });

    test("Frontend components use unified password policy validation", () => {
        const changePwdPage = fs.readFileSync(path.resolve(__dirname, "../pages/ChangePasswordPage.tsx"), "utf-8");
        assert.ok(changePwdPage.includes("isValidPassword"), "ChangePasswordPage must import/use isValidPassword");
        assert.ok(!changePwdPage.includes("newPassword.length < 6"), "ChangePasswordPage should not use outdated min 6 check");

        const forceChangePwdPage = fs.readFileSync(path.resolve(__dirname, "../components/auth/ForceChangePasswordPage.tsx"), "utf-8");
        assert.ok(forceChangePwdPage.includes("isValidPassword"), "ForceChangePasswordPage must import/use isValidPassword");

        const resetPwdPage = fs.readFileSync(path.resolve(__dirname, "../components/auth/ResetPasswordPage.tsx"), "utf-8");
        assert.ok(resetPwdPage.includes("isValidPassword"), "ResetPasswordPage must import/use isValidPassword");

        const changePwdModal = fs.readFileSync(path.resolve(__dirname, "../components/profile/ChangePasswordModal.tsx"), "utf-8");
        assert.ok(changePwdModal.includes("isValidPassword"), "ChangePasswordModal must import/use isValidPassword");
        assert.ok(!changePwdModal.includes("newPassword.length < 6"), "ChangePasswordModal should not use outdated min 6 check");
    });
});
