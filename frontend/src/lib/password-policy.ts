export const PASSWORD_POLICY_REGEX = /^(?=.*[A-Za-z])(?=.*\d).{8,}$/;
export const PASSWORD_POLICY_MESSAGE = "Mật khẩu phải có tối thiểu 8 ký tự, bao gồm cả chữ cái và chữ số.";

export function isValidPassword(password: string): boolean {
  return typeof password === "string" && PASSWORD_POLICY_REGEX.test(password);
}
