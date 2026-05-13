import { api, type ApiResponse } from "./http";

export type CommunityPost = {
  postId: number;
  authorUserId: number;
  authorNickname: string;
  title: string;
  content: string;
  createdAt: string;
  updatedAt: string;
  mine: boolean;
};

export type CommunityPostRequest = {
  title: string;
  content: string;
};

export const getCommunityPosts = async () => {
  const res =
    await api.get<ApiResponse<CommunityPost[]>>("/api/community/posts");
  return res.data.data;
};

export const getCommunityPost = async (postId: number) => {
  const res = await api.get<ApiResponse<CommunityPost>>(
    `/api/community/posts/${postId}`,
  );
  return res.data.data;
};

export const createCommunityPost = async (
  request: CommunityPostRequest,
) => {
  const res = await api.post<ApiResponse<{ postId: number }>>(
    "/api/community/posts",
    request,
  );
  return res.data.data;
};

export const updateCommunityPost = async (
  postId: number,
  request: CommunityPostRequest,
) => {
  const res = await api.patch<ApiResponse<CommunityPost>>(
    `/api/community/posts/${postId}`,
    request,
  );
  return res.data.data;
};

export const deleteCommunityPost = async (postId: number) => {
  const res = await api.delete<
    ApiResponse<{ postId: number; deleted: boolean }>
  >(`/api/community/posts/${postId}`);
  return res.data.data;
};
