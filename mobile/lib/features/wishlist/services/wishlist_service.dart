import '../../../core/api/api_client.dart';
import '../../../core/api/api_endpoints.dart';
import '../../../models/place.dart';
import '../../../models/wishlist.dart';

class WishlistService {
  final ApiClient _apiClient;

  WishlistService(this._apiClient);

  // 저장한 맛집 목록 가져오기
  Future<List<SavedPlace>> getSavedPlaces() async {
    try {
      final response = await _apiClient.dio.get(ApiEndpoints.myRecommendations);
      if (response.statusCode == 200 && response.data != null) {
        final List dataList = response.data['data'] ?? [];
        return dataList.map((json) => SavedPlace.fromJson(json)).toList();
      }
      return [];
    } catch (e) {
      return [];
    }
  }

  // 각 SavedPlace의 정보를 바탕으로 실제 Place 상세 정보를 병렬로 가져와 List<Place> 목록을 반환
  Future<List<Place>> getSavedPlaceDetails(List<SavedPlace> savedPlaces) async {
    if (savedPlaces.isEmpty) return [];
    try {
      // Future.wait를 통해 병렬 비동기 요청 수행
      final List<Future<Place?>> futures = savedPlaces.map((sp) async {
        try {
          final response = await _apiClient.dio.get(ApiEndpoints.placeDetail(sp.placeId));
          if (response.statusCode == 200 && response.data != null) {
            final data = response.data['data'];
            if (data != null) {
              return Place.fromJson(data);
            }
          }
          return null;
        } catch (e) {
          return null;
        }
      }).toList();

      final List<Place?> results = await Future.wait(futures);
      return results.whereType<Place>().toList();
    } catch (e) {
      return [];
    }
  }

  // 맛집 저장 해제 (추천 취소)
  Future<bool> unsavePlace(int placeId) async {
    try {
      final response = await _apiClient.dio.delete(ApiEndpoints.recommendPlace(placeId));
      return response.statusCode == 200;
    } catch (e) {
      return false;
    }
  }
}
