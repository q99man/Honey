import {
  api,
  clearAuthTokens,
  getStoredRefreshToken,
  storeAuthTokens,
  type ApiResponse,
} from "./http";

export type SignupResult = {
  userId: number;
  nickname: string;
};

export type TokenResult = {
  accessToken: string;
  refreshToken: string;
};

export type PhoneVerificationStatus = {
  phoneVerified: boolean;
};

export const signup = async (
  email: string,
  password: string,
  nickname: string,
) => {
  const res = await api.post<ApiResponse<SignupResult>>("/api/auth/signup", {
    email,
    password,
    nickname,
  });
  return res.data.data;
};

export const login = async (email: string, password: string) => {
  const res = await api.post<ApiResponse<TokenResult>>("/api/auth/login", {
    email,
    password,
  });
  storeAuthTokens(res.data.data.accessToken, res.data.data.refreshToken);
  return res.data.data;
};

export const logout = async () => {
  const refreshToken = getStoredRefreshToken();
  try {
    if (refreshToken) {
      await api.post<ApiResponse<null>>("/api/auth/logout", { refreshToken });
    }
  } finally {
    clearAuthTokens();
  }
};

export const getPhoneVerificationStatus = async () => {
  const res = await api.get<ApiResponse<PhoneVerificationStatus>>(
    "/api/auth/phone/status",
  );
  return res.data.data;
};

export const sendPhoneVerificationCode = async (phone: string) => {
  const res = await api.post<ApiResponse<{ sent: boolean }>>(
    "/api/auth/phone/send-code",
    { phone },
  );
  return res.data.data;
};

export const verifyPhoneCode = async (phone: string, code: string) => {
  const res = await api.post<ApiResponse<PhoneVerificationStatus>>(
    "/api/auth/phone/verify-code",
    { phone, code },
  );
  return res.data.data;
};
