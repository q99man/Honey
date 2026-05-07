import axios from "axios";

export type ApiResponse<T> = {
  success: boolean;
  data: T;
  errorCode?: string;
  message: string;
};

const ACCESS_TOKEN_KEYS = [
  "honeytong-access-token",
  "accessToken",
  "access_token",
];
const REFRESH_TOKEN_KEY = "honeytong-refresh-token";
const PRIMARY_ACCESS_TOKEN_KEY = ACCESS_TOKEN_KEYS[0];

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080",
});

api.interceptors.request.use((config) => {
  const token = getStoredAccessToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export const getStoredAccessToken = () => {
  for (const key of ACCESS_TOKEN_KEYS) {
    const token = localStorage.getItem(key);
    if (token) {
      return token;
    }
  }
  return null;
};

export const hasStoredAccessToken = () => getStoredAccessToken() !== null;

export const getStoredRefreshToken = () => {
  return localStorage.getItem(REFRESH_TOKEN_KEY);
};

export const storeAuthTokens = (accessToken: string, refreshToken: string) => {
  localStorage.setItem(PRIMARY_ACCESS_TOKEN_KEY, accessToken);
  localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
};

export const clearAuthTokens = () => {
  for (const key of ACCESS_TOKEN_KEYS) {
    localStorage.removeItem(key);
  }
  localStorage.removeItem(REFRESH_TOKEN_KEY);
};

export const getApiErrorMessage = (error: unknown, fallback: string) => {
  if (axios.isAxiosError<ApiResponse<unknown>>(error)) {
    const data = error.response?.data;
    if (data?.message) {
      return data.message;
    }
    if (data?.errorCode) {
      return toKoreanErrorMessage(data.errorCode);
    }
  }
  return fallback;
};

const toKoreanErrorMessage = (errorCode: string) => {
  switch (errorCode) {
    case "UNAUTHORIZED":
      return "로그인이 필요합니다.";
    case "FORBIDDEN":
    case "ADMIN_ONLY":
      return "이 작업을 수행할 권한이 없습니다.";
    case "PHONE_VERIFICATION_REQUIRED":
      return "전화 인증 후 이용할 수 있습니다.";
    case "REGION_VERIFICATION_REQUIRED":
      return "동네 인증 후 이용할 수 있습니다.";
    case "INVALID_REGION_CHANGE":
      return "동네 변경 요청이 올바르지 않습니다.";
    case "PLACE_REGISTRATION_LIMIT_EXCEEDED":
      return "맛집 등록 가능 횟수를 모두 사용했습니다.";
    case "POLICY_VIOLATION":
      return "운영 정책 조건을 만족하지 않습니다.";
    case "INVALID_REQUEST":
      return "입력한 값을 다시 확인해주세요.";
    case "NOT_PLACE_OWNER":
      return "이 맛집을 수정할 권한이 없습니다.";
    case "USER_SANCTION_ACTIVE":
      return "활성 제재로 인해 이 작업을 수행할 수 없습니다.";
    case "DAILY_RECOMMEND_LIMIT_EXCEEDED":
      return "오늘 추천 가능 횟수를 모두 사용했습니다.";
    case "RECOMMEND_ALREADY_EXISTS":
      return "이미 추천한 맛집입니다.";
    case "COMMENT_ALREADY_EXISTS":
      return "이미 댓글을 작성한 맛집입니다.";
    case "VISIT_COOLDOWN_ACTIVE":
      return "아직 다시 방문 인증할 수 없습니다.";
    case "OUT_OF_VISIT_RADIUS":
      return "방문 인증 허용 반경을 벗어났습니다.";
    case "RESOURCE_NOT_FOUND":
      return "요청한 정보를 찾을 수 없습니다.";
    default:
      return "요청을 처리하지 못했습니다.";
  }
};
