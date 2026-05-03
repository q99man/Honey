import { api, type ApiResponse } from "./http";

export type ReportTargetType = "PLACE" | "COMMENT" | "USER";
export type ReportStatus = "PENDING" | "APPROVED" | "REJECTED";
export type AdminReportFollowUpActionType =
  | "HIDE_PLACE"
  | "DELETE_PLACE"
  | "BLIND_COMMENT"
  | "DELETE_COMMENT"
  | "SANCTION_USER";
export type UserSanctionType =
  | "WARNING"
  | "TEMPORARY_RESTRICTION"
  | "PERMANENT_RESTRICTION";

export type ReportCreateRequest = {
  targetType: ReportTargetType;
  targetId: number;
  reasonCode: string;
  reasonText?: string | null;
};

export type ReportCreateResponse = {
  reportId: number;
};

export type MyReport = {
  reportId: number;
  targetType: ReportTargetType;
  targetId: number;
  reasonCode: string;
  reasonText: string | null;
  status: ReportStatus;
  createdAt: string;
  reviewedAt: string | null;
  reviewNote: string | null;
};

export type AdminReport = {
  reportId: number;
  reporterUserId: number;
  reporterNickname: string;
  targetType: ReportTargetType;
  targetId: number;
  reasonCode: string;
  reasonText: string | null;
  status: ReportStatus;
  reviewedByUserId: number | null;
  reviewedByNickname: string | null;
  reviewedAt: string | null;
  reviewNote: string | null;
  createdAt: string;
  updatedAt: string;
};

export type AdminReportProcessRequest = {
  status: Exclude<ReportStatus, "PENDING">;
  reviewNote?: string | null;
};

export type AdminReportFollowUpActionRequest = {
  actionType: AdminReportFollowUpActionType;
  sanctionType?: UserSanctionType | null;
  reason?: string | null;
  startAt?: string | null;
  endAt?: string | null;
  memo?: string | null;
};

export type AdminReportFollowUpActionResponse = {
  reportId: number;
  actionType: AdminReportFollowUpActionType;
  reportTargetType: ReportTargetType;
  reportTargetId: number;
  resultTargetType: string;
  resultTargetId: number;
  applied: boolean;
};

export const createReport = async (request: ReportCreateRequest) => {
  const res = await api.post<ApiResponse<ReportCreateResponse>>(
    "/api/reports",
    request,
  );
  return res.data.data;
};

export const getMyReports = async () => {
  const res = await api.get<ApiResponse<MyReport[]>>("/api/users/me/reports");
  return res.data.data;
};

export const getAdminReports = async (status?: ReportStatus | "ALL") => {
  const res = await api.get<ApiResponse<AdminReport[]>>("/api/admin/reports", {
    params: status && status !== "ALL" ? { status } : undefined,
  });
  return res.data.data;
};

export const getAdminReport = async (reportId: number) => {
  const res = await api.get<ApiResponse<AdminReport>>(
    `/api/admin/reports/${reportId}`,
  );
  return res.data.data;
};

export const processAdminReport = async (
  reportId: number,
  request: AdminReportProcessRequest,
) => {
  const res = await api.patch<ApiResponse<AdminReport>>(
    `/api/admin/reports/${reportId}`,
    request,
  );
  return res.data.data;
};

export const applyAdminReportFollowUpAction = async (
  reportId: number,
  request: AdminReportFollowUpActionRequest,
) => {
  const res = await api.post<ApiResponse<AdminReportFollowUpActionResponse>>(
    `/api/admin/reports/${reportId}/actions`,
    request,
  );
  return res.data.data;
};
