import { api, type ApiResponse } from "./http";

export type RecommendationPolicy = {
  canRecommend: boolean;
  reason: string | null;
  dailyRemainingCount: number;
};

export type RecommendationResult = {
  recommended: boolean;
  recommendCount: number;
  myWeight: number;
};

export type VisitPolicy = {
  canVisitNow: boolean;
  radiusMeter: number;
  cooldownUntil: string | null;
};

export type VisitResult = {
  verified: boolean;
  distanceMeter: number;
  expGained: number;
  visitCount: number;
};

export type CommentItem = {
  commentId: number;
  placeId: number;
  placeName: string;
  userId: number;
  nickname: string;
  content: string;
  createdAt: string;
  updatedAt: string;
};

export type CommentCreateResult = {
  commentId: number;
};

export type CommentDeleteResult = {
  commentId: number;
  deleted: boolean;
  commentCount: number;
};

export const getRecommendationPolicy = async (placeId: number) => {
  const res = await api.get<ApiResponse<RecommendationPolicy>>(
    `/api/places/${placeId}/recommend-policy`,
  );
  return res.data.data;
};

export const recommendPlace = async (placeId: number) => {
  const res = await api.post<ApiResponse<RecommendationResult>>(
    `/api/places/${placeId}/recommend`,
  );
  return res.data.data;
};

export const cancelRecommendation = async (placeId: number) => {
  const res = await api.delete<ApiResponse<RecommendationResult>>(
    `/api/places/${placeId}/recommend`,
  );
  return res.data.data;
};

export const getVisitPolicy = async (placeId: number) => {
  const res = await api.get<ApiResponse<VisitPolicy>>(
    `/api/places/${placeId}/visit-policy`,
  );
  return res.data.data;
};

export const verifyVisit = async (
  placeId: number,
  latitude: number,
  longitude: number,
) => {
  const res = await api.post<ApiResponse<VisitResult>>(
    `/api/places/${placeId}/visits`,
    { latitude, longitude },
  );
  return res.data.data;
};

export const getPlaceComments = async (placeId: number) => {
  const res = await api.get<ApiResponse<CommentItem[]>>(
    `/api/places/${placeId}/comments`,
  );
  return res.data.data;
};

export const createComment = async (placeId: number, content: string) => {
  const res = await api.post<ApiResponse<CommentCreateResult>>(
    `/api/places/${placeId}/comments`,
    { content },
  );
  return res.data.data;
};

export const updateComment = async (commentId: number, content: string) => {
  const res = await api.patch<ApiResponse<CommentItem>>(
    `/api/comments/${commentId}`,
    { content },
  );
  return res.data.data;
};

export const deleteComment = async (commentId: number) => {
  const res = await api.delete<ApiResponse<CommentDeleteResult>>(
    `/api/comments/${commentId}`,
  );
  return res.data.data;
};
