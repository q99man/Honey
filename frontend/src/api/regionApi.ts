import { api, type ApiResponse } from "./http";

export type MyRegion = {
  cityId: number;
  districtId: number;
  dongId: number;
  cityName: string;
  districtName: string;
  dongName: string;
  verified: boolean;
};

export type RegionChangePolicy = {
  changeAllowed: boolean;
  cooldownDays: number;
  nextAvailableAt: string | null;
};

export const getMyRegion = async () => {
  const response = await api.get<ApiResponse<MyRegion>>("/api/regions/me");
  return response.data.data;
};

export const verifyRegion = async (latitude: number, longitude: number) => {
  const response = await api.post<ApiResponse<MyRegion>>(
    "/api/regions/verify",
    {
      latitude,
      longitude,
    },
  );
  return response.data.data;
};

export const getRegionChangePolicy = async () => {
  const response = await api.get<ApiResponse<RegionChangePolicy>>(
    "/api/regions/me/change-policy",
  );
  return response.data.data;
};
