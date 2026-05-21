import 'package:dio/dio.dart';
import '../../../../core/api/api_client.dart';
import '../../../../core/api/api_endpoints.dart';
import '../../../../models/ranking.dart';
import '../../../../models/region.dart';

class RankingService {
  final ApiClient _apiClient;

  RankingService(this._apiClient);

  // 현재 활성화된 시즌 정보 가져오기
  Future<Season?> getCurrentSeason() async {
    try {
      final response = await _apiClient.dio.get(ApiEndpoints.currentSeason);
      if (response.statusCode == 200 && response.data != null) {
        final data = response.data['data'];
        if (data != null) {
          return Season.fromJson(data);
        }
      }
      return null;
    } catch (e) {
      return null;
    }
  }

  // 특정 시즌 및 지역의 맛집 랭킹 조회하기
  Future<PlaceRankingResponse?> getPlaceRankings({
    required String regionType,
    required int regionId,
    String? seasonCode,
  }) async {
    try {
      final Map<String, dynamic> queryParams = {
        'regionType': regionType,
        'regionId': regionId,
      };
      if (seasonCode != null && seasonCode.isNotEmpty) {
        queryParams['seasonCode'] = seasonCode;
      }

      final response = await _apiClient.dio.get(
        ApiEndpoints.rankingPlaces,
        queryParameters: queryParams,
      );

      if (response.statusCode == 200 && response.data != null) {
        final data = response.data['data'];
        if (data != null) {
          return PlaceRankingResponse.fromJson(data);
        }
      }
      return null;
    } catch (e) {
      return null;
    }
  }

  // 행정구역: 시도 목록 가져오기 (공개 API)
  Future<List<RegionCity>> getCities() async {
    try {
      final response = await _apiClient.dio.get('/api/regions/cities');
      if (response.statusCode == 200 && response.data != null) {
        final List dataList = response.data['data'] ?? [];
        return dataList.map((json) => RegionCity.fromJson(json)).toList();
      }
      return [];
    } catch (e) {
      return [];
    }
  }

  // 행정구역: 특정 시도의 시군구 목록 가져오기 (공개 API)
  Future<List<RegionDistrict>> getDistricts(int cityId) async {
    try {
      final response = await _apiClient.dio.get('/api/regions/cities/$cityId/districts');
      if (response.statusCode == 200 && response.data != null) {
        final List dataList = response.data['data'] ?? [];
        return dataList.map((json) => RegionDistrict.fromJson(json)).toList();
      }
      return [];
    } catch (e) {
      return [];
    }
  }

  // 행정구역: 특정 시군구의 읍면동 목록 가져오기 (공개 API)
  Future<List<RegionDong>> getDongs(int districtId) async {
    try {
      final response = await _apiClient.dio.get('/api/regions/districts/$districtId/dongs');
      if (response.statusCode == 200 && response.data != null) {
        final List dataList = response.data['data'] ?? [];
        return dataList.map((json) => RegionDong.fromJson(json)).toList();
      }
      return [];
    } catch (e) {
      return [];
    }
  }
}
