import axios from "axios";
import type { Place } from "../types/place";

type ApiResponse<T> = {
  success: boolean;
  data: T;
  errorCode?: string;
  message: string;
};

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
  imageUrls: string[];
};

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080",
});

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
  };
};

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
