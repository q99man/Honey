import { api, type ApiResponse } from "./http";

export type MyProfile = {
  id: number;
  nickname: string;
  phoneVerified: boolean;
  languagePreference: string;
};

export type MyStatus = {
  level: number;
  exp: number;
  nextLevelExp: number;
  trustGrade: string;
  recommendWeight: number;
  phoneVerified: boolean;
  regionVerified: boolean;
};

export type MyActivitySummary = {
  recommendedCount: number;
  visitCount: number;
  commentCount: number;
  registeredPlaceCount: number;
};

export const getMyProfile = async () => {
  const res = await api.get<ApiResponse<MyProfile>>("/api/users/me");
  return res.data.data;
};

export const getMyStatus = async () => {
  const res = await api.get<ApiResponse<MyStatus>>("/api/users/me/status");
  return res.data.data;
};

export const getMyActivitySummary = async () => {
  const res = await api.get<ApiResponse<MyActivitySummary>>(
    "/api/users/me/activity-summary",
  );
  return res.data.data;
};
