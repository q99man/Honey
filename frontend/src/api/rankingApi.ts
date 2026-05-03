import { api, type ApiResponse } from "./http";

export type RankingRegionType = "dong" | "district" | "city";

export type CurrentSeason = {
  seasonCode: string;
  seasonName: string;
  seasonType: string;
  startAt: string;
  endAt: string;
  status: string;
};

export type PlaceRankingItem = {
  rank: number;
  placeId: number;
  name: string;
  starLevel: number;
  totalScore: number;
  audienceTags: string[];
};

export type PlaceRanking = {
  seasonCode: string;
  regionType: RankingRegionType;
  regionName: string;
  items: PlaceRankingItem[];
};

export type PlaceRankingHistoryItem = {
  seasonId: number;
  seasonCode: string;
  seasonName: string;
  seasonType: string;
  seasonStartAt: string;
  seasonEndAt: string;
  regionType: RankingRegionType;
  regionId: number;
  rank: number;
  starLevel: number;
  totalScore: number;
  finalizedAt: string;
};

export type PlaceRankingHistory = {
  placeId: number;
  name: string;
  items: PlaceRankingHistoryItem[];
};

export const getCurrentSeason = async () => {
  const res = await api.get<ApiResponse<CurrentSeason>>(
    "/api/rankings/seasons/current",
  );
  return res.data.data;
};

export const getPlaceRanking = async (
  regionType: RankingRegionType,
  regionId: number,
  seasonCode?: string,
) => {
  const res = await api.get<ApiResponse<PlaceRanking>>("/api/rankings/places", {
    params: { regionType, regionId, seasonCode },
  });
  return res.data.data;
};

export const getPlaceRankingHistory = async (placeId: number) => {
  const res = await api.get<ApiResponse<PlaceRankingHistory>>(
    `/api/places/${placeId}/ranking-history`,
  );
  return res.data.data;
};
