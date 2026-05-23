import { api, type ApiResponse } from "./http";

export type AdminDashboard = {
  todayNewUsers: number;
  todayRecommendations: number;
  todayVisits: number;
  pendingReports: number;
  newPlaces: number;
};

export type AdminPolicyValueType = "INTEGER" | "DECIMAL" | "BOOLEAN" | "STRING";

export type AdminPolicy = {
  id: number;
  policyGroup: string;
  policyKey: string;
  fullKey: string;
  policyValue: string;
  valueType: AdminPolicyValueType;
  description: string | null;
  active: boolean;
  updatedBy: number | null;
  updatedAt: string;
};

export type AdminPolicyUpdateRequest = {
  value: string;
  memo: string | null;
};

export type AdminRegionPolicy = {
  regionChangeCooldownDays: number;
  registrationScope: string;
};

export type AdminRegionPolicyUpdateRequest = {
  regionChangeCooldownDays: number;
  registrationScope: string;
};

export type AdminUserRole = "USER" | "ADMIN" | "SUPER_ADMIN";
export type AdminUserStatus = "ACTIVE" | "SUSPENDED" | "DELETED";
export type AdminTrustGrade =
  | "SEED_BEE"
  | "VERIFIED_BEE"
  | "LOCAL_BEE"
  | "ACTIVE_BEE"
  | "TRUSTED_BEE"
  | "INFLUENCER_BEE";
export type AdminUserSanctionType =
  | "WARNING"
  | "TEMPORARY_RESTRICTION"
  | "PERMANENT_RESTRICTION";
export type AdminUserSanctionStatus = "ACTIVE" | "EXPIRED" | "CANCELED";

export type AdminUserListItem = {
  userId: number;
  nickname: string;
  email: string | null;
  phoneVerified: boolean;
  status: AdminUserStatus;
  role: AdminUserRole;
  languagePreference: string | null;
  createdAt: string;
  updatedAt: string;
  deletedAt: string | null;
};

export type AdminUserTrust = {
  trustScore: number;
  trustGrade: AdminTrustGrade;
  recommendWeight: number;
  sanctionCount: number;
  reportReceivedCount: number;
  reportConfirmedCount: number;
  abnormalActivityScore: number;
  phoneVerified: boolean;
  regionVerified: boolean;
  lastEvaluatedAt: string | null;
};

export type AdminUserLevel = {
  level: number;
  exp: number;
  totalExp: number;
  title: string | null;
  avatarStage: string | null;
  rankScore: number;
};

export type AdminUserDetail = AdminUserListItem & {
  phone: string | null;
  marketingAgreed: boolean;
  trust: AdminUserTrust | null;
  level: AdminUserLevel | null;
};

export type AdminUserSanctionRequest = {
  sanctionType: AdminUserSanctionType;
  reason: string | null;
  startAt: string | null;
  endAt: string | null;
  memo: string | null;
};

export type AdminUserSanction = {
  sanctionId: number;
  userId: number;
  nickname: string;
  sanctionType: AdminUserSanctionType;
  reason: string | null;
  startAt: string;
  endAt: string | null;
  status: AdminUserSanctionStatus;
  createdByUserId: number;
  createdByNickname: string;
  createdAt: string;
  updatedAt: string;
};

export type AdminUserTrustAdjustRequest = {
  trustScore: number;
  trustGrade: AdminTrustGrade;
  memo: string | null;
};

export type AdminUserRecommendWeightRequest = {
  recommendWeight: number;
  memo: string | null;
};

export type AdminUserTrustAdjustResponse = {
  userId: number;
  nickname: string;
  trustScore: number;
  trustGrade: AdminTrustGrade;
  recommendWeight: number;
  lastEvaluatedAt: string | null;
};

export type AdminPlaceApprovalStatus = "APPROVED" | "PENDING" | "REJECTED";
export type AdminPlaceExposureStatus = "VISIBLE" | "HIDDEN";
export type AdminPlaceFranchiseReviewStatus =
  | "PENDING"
  | "APPROVED"
  | "REJECTED";

export type AdminPlaceListItem = {
  placeId: number;
  name: string;
  categoryCode: string;
  createdByUserId: number;
  createdByNickname: string;
  cityId: number;
  districtId: number;
  dongId: number;
  cityName: string;
  districtName: string;
  dongName: string;
  franchise: boolean;
  franchiseReviewStatus: AdminPlaceFranchiseReviewStatus;
  approvalStatus: AdminPlaceApprovalStatus;
  exposureStatus: AdminPlaceExposureStatus;
  rankingExcluded: boolean;
  starLevel: number;
  flowerGrade: string;
  recommendCount: number;
  visitCount: number;
  commentCount: number;
  createdAt: string;
  updatedAt: string;
};

export type AdminPlaceDetail = AdminPlaceListItem & {
  addressRoad: string | null;
  addressJibun: string | null;
  latitude: number;
  longitude: number;
  priceRangeCode: string | null;
  recommendedMenu: string | null;
  shortRecommendation: string;
  featureText: string | null;
  uniqueUserCount: number;
  scoreTotal: number;
  manualAdjustmentScore: number;
  recentScore: number;
  diversityScore: number;
  trustWeightedScore: number;
  lastActivityAt: string | null;
  imageUrls: string[];
  aiTags?: string[];
};

export type AdminPlaceModerationResponse = {
  placeId: number;
  name: string;
  approvalStatus: AdminPlaceApprovalStatus;
  exposureStatus: AdminPlaceExposureStatus;
  franchiseReviewStatus: AdminPlaceFranchiseReviewStatus;
};

export type AdminPlaceExposureStatusRequest = {
  exposureStatus: AdminPlaceExposureStatus;
  memo: string | null;
};

export type AdminPlaceApprovalStatusRequest = {
  approvalStatus: AdminPlaceApprovalStatus;
  memo: string | null;
};

export type AdminPlaceFranchiseStatusRequest = {
  franchiseReviewStatus: AdminPlaceFranchiseReviewStatus;
  memo: string | null;
};

export type AdminPlaceScoreAdjustmentRequest = {
  scoreDelta: number;
  memo: string | null;
};

export type AdminPlaceScoreAdjustmentResponse = {
  placeId: number;
  name: string;
  scoreTotal: number;
  manualAdjustmentScore: number;
};

export type AdminRankingPlaceExclusionRequest = {
  excluded: boolean;
  memo: string | null;
};

export type AdminRankingPlaceExclusionResponse = {
  placeId: number;
  name: string;
  rankingExcluded: boolean;
  starLevel: number;
};

export type AdminRecommendationStatus =
  | "ACTIVE"
  | "CANCELED"
  | "INVALIDATED";

export type AdminRecommendation = {
  recommendationId: number;
  userId: number;
  nickname: string;
  placeId: number;
  placeName: string;
  categoryCode: string;
  status: AdminRecommendationStatus;
  recommendWeight: number;
  createdAt: string;
  updatedAt: string;
};

export type AdminRecommendationInvalidationResponse = {
  recommendationId: number;
  userId: number;
  nickname: string;
  placeId: number;
  placeName: string;
  status: AdminRecommendationStatus;
  recommendCount: number;
};

export type AdminVisit = {
  visitId: number;
  userId: number;
  nickname: string;
  placeId: number;
  placeName: string;
  categoryCode: string;
  latitude: number;
  longitude: number;
  distanceMeter: number | null;
  imageUrl: string | null;
  valid: boolean;
  validReason: string | null;
  createdAt: string;
  updatedAt: string;
};

export type AdminVisitInvalidationResponse = {
  visitId: number;
  userId: number;
  nickname: string;
  placeId: number;
  placeName: string;
  valid: boolean;
  validReason: string | null;
  visitCount: number;
};

export type AdminCommentStatus = "VISIBLE" | "BLINDED" | "DELETED";

export type AdminComment = {
  commentId: number;
  userId: number;
  nickname: string;
  placeId: number;
  placeName: string;
  categoryCode: string;
  content: string;
  status: AdminCommentStatus;
  reportCount: number;
  createdAt: string;
  updatedAt: string;
  deletedAt: string | null;
};

export type AdminCommentModerationResponse = {
  commentId: number;
  userId: number;
  nickname: string;
  placeId: number;
  placeName: string;
  content: string;
  status: AdminCommentStatus;
  commentCount: number;
};

export type AdminModerationMemoRequest = {
  memo: string | null;
};

export type AdminActionLog = {
  logId: number;
  adminUserId: number;
  adminNickname: string;
  actionType: string;
  targetType: string;
  targetId: number;
  beforeValue: string | null;
  afterValue: string | null;
  memo: string | null;
  createdAt: string;
};

export type AdminUserActionLog = {
  logId: number;
  userId: number;
  nickname: string;
  actionType: string;
  targetType: string | null;
  targetId: number | null;
  ipAddress: string | null;
  userAgent: string | null;
  metadataJson: string | null;
  createdAt: string;
};

export const getAdminDashboard = async () => {
  const res = await api.get<ApiResponse<AdminDashboard>>(
    "/api/admin/dashboard",
  );
  return res.data.data;
};

export const getAdminUsers = async () => {
  const res = await api.get<ApiResponse<AdminUserListItem[]>>(
    "/api/admin/users",
  );
  return res.data.data;
};

export const getAdminUser = async (userId: number) => {
  const res = await api.get<ApiResponse<AdminUserDetail>>(
    `/api/admin/users/${userId}`,
  );
  return res.data.data;
};

export const createAdminUserSanction = async (
  userId: number,
  request: AdminUserSanctionRequest,
) => {
  const res = await api.post<ApiResponse<AdminUserSanction>>(
    `/api/admin/users/${userId}/sanctions`,
    request,
  );
  return res.data.data;
};

export const adjustAdminUserTrust = async (
  userId: number,
  request: AdminUserTrustAdjustRequest,
) => {
  const res = await api.patch<ApiResponse<AdminUserTrustAdjustResponse>>(
    `/api/admin/users/${userId}/trust`,
    request,
  );
  return res.data.data;
};

export const adjustAdminUserRecommendWeight = async (
  userId: number,
  request: AdminUserRecommendWeightRequest,
) => {
  const res = await api.patch<ApiResponse<AdminUserTrustAdjustResponse>>(
    `/api/admin/users/${userId}/recommend-weight`,
    request,
  );
  return res.data.data;
};

export const getAdminPlaces = async () => {
  const res = await api.get<ApiResponse<AdminPlaceListItem[]>>(
    "/api/admin/places",
  );
  return res.data.data;
};

export const getAdminPlace = async (placeId: number) => {
  const res = await api.get<ApiResponse<AdminPlaceDetail>>(
    `/api/admin/places/${placeId}`,
  );
  return res.data.data;
};

export const changeAdminPlaceExposure = async (
  placeId: number,
  request: AdminPlaceExposureStatusRequest,
) => {
  const res = await api.patch<ApiResponse<AdminPlaceModerationResponse>>(
    `/api/admin/places/${placeId}/exposure`,
    request,
  );
  return res.data.data;
};

export const changeAdminPlaceApproval = async (
  placeId: number,
  request: AdminPlaceApprovalStatusRequest,
) => {
  const res = await api.patch<ApiResponse<AdminPlaceModerationResponse>>(
    `/api/admin/places/${placeId}/approval`,
    request,
  );
  return res.data.data;
};

export const changeAdminPlaceFranchiseStatus = async (
  placeId: number,
  request: AdminPlaceFranchiseStatusRequest,
) => {
  const res = await api.patch<ApiResponse<AdminPlaceModerationResponse>>(
    `/api/admin/places/${placeId}/franchise-status`,
    request,
  );
  return res.data.data;
};

export const adjustAdminPlaceScore = async (
  placeId: number,
  request: AdminPlaceScoreAdjustmentRequest,
) => {
  const res = await api.patch<ApiResponse<AdminPlaceScoreAdjustmentResponse>>(
    `/api/admin/places/${placeId}/score-adjustment`,
    request,
  );
  return res.data.data;
};

export const changeAdminPlaceRankingExclusion = async (
  placeId: number,
  request: AdminRankingPlaceExclusionRequest,
) => {
  const res = await api.patch<ApiResponse<AdminRankingPlaceExclusionResponse>>(
    `/api/admin/rankings/places/${placeId}/exclude`,
    request,
  );
  return res.data.data;
};

export const getAdminRecommendations = async () => {
  const res = await api.get<ApiResponse<AdminRecommendation[]>>(
    "/api/admin/recommendations",
  );
  return res.data.data;
};

export const invalidateAdminRecommendation = async (
  recommendationId: number,
  request: AdminModerationMemoRequest,
) => {
  const res = await api.patch<
    ApiResponse<AdminRecommendationInvalidationResponse>
  >(`/api/admin/recommendations/${recommendationId}/invalidate`, request);
  return res.data.data;
};

export const getAdminVisits = async () => {
  const res = await api.get<ApiResponse<AdminVisit[]>>("/api/admin/visits");
  return res.data.data;
};

export const invalidateAdminVisit = async (
  visitId: number,
  request: AdminModerationMemoRequest,
) => {
  const res = await api.patch<ApiResponse<AdminVisitInvalidationResponse>>(
    `/api/admin/visits/${visitId}/invalidate`,
    request,
  );
  return res.data.data;
};

export const getAdminComments = async () => {
  const res = await api.get<ApiResponse<AdminComment[]>>(
    "/api/admin/comments",
  );
  return res.data.data;
};

export const blindAdminComment = async (
  commentId: number,
  request: AdminModerationMemoRequest,
) => {
  const res = await api.patch<ApiResponse<AdminCommentModerationResponse>>(
    `/api/admin/comments/${commentId}/blind`,
    request,
  );
  return res.data.data;
};

export const deleteAdminComment = async (
  commentId: number,
  request: AdminModerationMemoRequest,
) => {
  const res = await api.delete<ApiResponse<AdminCommentModerationResponse>>(
    `/api/admin/comments/${commentId}`,
    { data: request },
  );
  return res.data.data;
};

export const getAdminActionLogs = async () => {
  const res = await api.get<ApiResponse<AdminActionLog[]>>(
    "/api/admin/action-logs",
  );
  return res.data.data;
};

export const getAdminUserActionLogs = async () => {
  const res = await api.get<ApiResponse<AdminUserActionLog[]>>(
    "/api/admin/user-action-logs",
  );
  return res.data.data;
};

export const getAdminPolicies = async () => {
  const res =
    await api.get<ApiResponse<AdminPolicy[]>>("/api/admin/policies");
  return res.data.data;
};

export const updateAdminPolicy = async (
  fullKey: string,
  request: AdminPolicyUpdateRequest,
) => {
  const res = await api.patch<ApiResponse<AdminPolicy>>(
    `/api/admin/policies/${fullKey}`,
    request,
  );
  return res.data.data;
};

export const getAdminRegionPolicy = async () => {
  const res = await api.get<ApiResponse<AdminRegionPolicy>>(
    "/api/admin/policies/region",
  );
  return res.data.data;
};

export const updateAdminRegionPolicy = async (
  request: AdminRegionPolicyUpdateRequest,
) => {
  const res = await api.patch<ApiResponse<AdminRegionPolicy>>(
    "/api/admin/policies/region",
    request,
  );
  return res.data.data;
};
