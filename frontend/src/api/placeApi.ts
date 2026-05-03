import type { Place } from "../types/place";
import { api, type ApiResponse } from "./http";

type PlaceListItemResponse = {
  id: number;
  name: string;
  categoryCode: string;
  cityId: number;
  districtId: number;
  dongId: number;
  regionName: string;
  address: string | null;
  shortRecommendation: string;
  starLevel: number;
  flowerGrade: string;
  recommendCount: number;
  visitCount: number;
  commentCount: number;
  distanceMeter: number | null;
  representativeImageUrl: string | null;
};

type PlaceDetailResponse = PlaceListItemResponse & {
  cityName: string;
  districtName: string;
  dongName: string;
  addressRoad: string | null;
  addressJibun: string | null;
  latitude: number;
  longitude: number;
  priceRangeCode: string | null;
  recommendedMenu: string | null;
  featureText: string | null;
  franchise: boolean;
  imageUrls: string[];
};

export type PlaceCreateRequest = {
  name: string;
  categoryCode: string;
  dongId: number;
  addressRoad?: string | null;
  addressJibun?: string | null;
  latitude: number;
  longitude: number;
  priceRangeCode?: string | null;
  recommendedMenu?: string | null;
  shortRecommendation: string;
  featureText?: string | null;
  franchise: boolean;
  imageUrls: string[];
};

export type PlaceCreateResponse = {
  placeId: number;
  approvalStatus: string;
};

export type PlaceEditDetail = {
  id: number;
  name: string;
  categoryCode: string;
  dongId: number;
  addressRoad: string | null;
  addressJibun: string | null;
  latitude: number;
  longitude: number;
  priceRangeCode: string | null;
  recommendedMenu: string | null;
  shortRecommendation: string;
  featureText: string | null;
  franchise: boolean;
  imageUrls: string[];
};

export type PlaceUpdateRequest = {
  name?: string | null;
  categoryCode?: string | null;
  dongId?: number | null;
  addressRoad?: string | null;
  addressJibun?: string | null;
  latitude?: number | null;
  longitude?: number | null;
  priceRangeCode?: string | null;
  recommendedMenu?: string | null;
  shortRecommendation?: string | null;
  featureText?: string | null;
  franchise?: boolean | null;
  imageUrls?: string[] | null;
};

export type PlaceUpdateResponse = Omit<PlaceEditDetail, "id"> & {
  placeId: number;
};

export type PlaceDeleteResponse = {
  placeId: number;
  deleted: boolean;
};

export type PlaceRegistrationPolicy = {
  canRegister: boolean;
  registrationScope: string;
  registrationLimit: number;
  currentUsage: number;
};

export const getPlaces = async () => {
  const res = await api.get<ApiResponse<PlaceListItemResponse[]>>("/api/places");
  return res.data.data.map(toPlace);
};

export const searchPlaces = async (keyword: string) => {
  const res = await api.get<ApiResponse<PlaceListItemResponse[]>>(
    "/api/places/search",
    { params: { keyword } },
  );
  return res.data.data.map(toPlace);
};

export const getNearbyPlaces = async (
  lat: number,
  lng: number,
  radius = 1000,
) => {
  const res = await api.get<ApiResponse<PlaceListItemResponse[]>>(
    "/api/places/nearby",
    { params: { lat, lng, radius } },
  );
  return res.data.data.map(toPlace);
};

export const getPlace = async (id: number) => {
  const res = await api.get<ApiResponse<PlaceDetailResponse>>(
    `/api/places/${id}`,
  );
  return toPlace(res.data.data);
};

export const getPlaceEditDetail = async (id: number) => {
  const res = await api.get<ApiResponse<PlaceDetailResponse>>(
    `/api/places/${id}`,
  );
  return toPlaceEditDetail(res.data.data);
};

export const createPlace = async (request: PlaceCreateRequest) => {
  const res = await api.post<ApiResponse<PlaceCreateResponse>>(
    "/api/places",
    request,
  );
  return res.data.data;
};

export const updatePlaceDetails = async (
  placeId: number,
  request: PlaceUpdateRequest,
) => {
  const res = await api.patch<ApiResponse<PlaceUpdateResponse>>(
    `/api/places/${placeId}`,
    request,
  );
  return res.data.data;
};

export const deletePlace = async (placeId: number) => {
  const res = await api.delete<ApiResponse<PlaceDeleteResponse>>(
    `/api/places/${placeId}`,
  );
  return res.data.data;
};

export const getPlaceRegistrationPolicy = async () => {
  const res = await api.get<ApiResponse<PlaceRegistrationPolicy>>(
    "/api/places/registration-policy",
  );
  return res.data.data;
};

export const getMyRegisteredPlaces = async () => {
  const res = await api.get<ApiResponse<PlaceListItemResponse[]>>(
    "/api/users/me/places",
  );
  return res.data.data.map(toPlace);
};

const toPlace = (
  item: PlaceListItemResponse | PlaceDetailResponse,
): Place => {
  const detail = item as Partial<PlaceDetailResponse>;
  const address = detail.addressRoad ?? item.address ?? detail.addressJibun ?? "";
  const description = detail.featureText ?? item.shortRecommendation;
  const imageUrl =
    detail.imageUrls?.[0] ?? item.representativeImageUrl ?? undefined;

  return {
    id: item.id,
    cityId: item.cityId,
    districtId: item.districtId,
    dongId: item.dongId,
    regionName: detail.dongName ?? item.regionName,
    title: item.name,
    desc: description,
    distance: formatDistance(item.distanceMeter),
    rating: item.starLevel,
    price: toDisplayPrice(detail.priceRangeCode),
    reviewCount: item.commentCount,
    category: item.categoryCode,
    address,
    isWished: false,
    imageUrl,
    recommendCount: item.recommendCount,
    visitCount: item.visitCount,
    commentCount: item.commentCount,
  };
};

const toPlaceEditDetail = (item: PlaceDetailResponse): PlaceEditDetail => ({
  id: item.id,
  name: item.name,
  categoryCode: item.categoryCode,
  dongId: item.dongId,
  addressRoad: item.addressRoad,
  addressJibun: item.addressJibun,
  latitude: item.latitude,
  longitude: item.longitude,
  priceRangeCode: item.priceRangeCode,
  recommendedMenu: item.recommendedMenu,
  shortRecommendation: item.shortRecommendation,
  featureText: item.featureText,
  franchise: item.franchise,
  imageUrls: item.imageUrls,
});

const formatDistance = (distanceMeter: number | null) => {
  if (distanceMeter == null) {
    return "\uac70\ub9ac \uc815\ubcf4 \uc5c6\uc74c";
  }
  if (distanceMeter < 1000) {
    return `${distanceMeter}m`;
  }
  return `${(distanceMeter / 1000).toFixed(1)}km`;
};

const toDisplayPrice = (priceRangeCode?: string | null) => {
  if (!priceRangeCode) {
    return "\uac00\uaca9 \uc815\ubcf4 \uc5c6\uc74c";
  }
  return `${priceRangeCode.replace("_", "\uc6d0 ~ ")}\uc6d0`;
};
