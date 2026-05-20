import 'package:dio/dio.dart';
import '../../../core/api/api_client.dart';
import '../../../core/api/api_endpoints.dart';
import '../../../models/place.dart';

class PlaceService {
  final ApiClient _apiClient;

  PlaceService(this._apiClient);

  // Fetch nearby places
  Future<List<Place>> getNearbyPlaces(double lat, double lng, {double radius = 1000}) async {
    try {
      final response = await _apiClient.dio.get(
        ApiEndpoints.nearbyPlaces,
        queryParameters: {
          'lat': lat,
          'lng': lng,
          'radius': radius,
        },
      );

      if (response.statusCode == 200 && response.data != null) {
        final List dataList = response.data['data'] ?? [];
        return dataList.map((json) => Place.fromJson(json)).toList();
      }
      return [];
    } catch (e) {
      return [];
    }
  }

  // Search places
  Future<List<Place>> searchPlaces(String keyword) async {
    if (keyword.trim().isEmpty) return [];
    try {
      final response = await _apiClient.dio.get(
        ApiEndpoints.searchPlaces,
        queryParameters: {'keyword': keyword},
      );

      if (response.statusCode == 200 && response.data != null) {
        final List dataList = response.data['data'] ?? [];
        return dataList.map((json) => Place.fromJson(json)).toList();
      }
      return [];
    } catch (e) {
      return [];
    }
  }

  // Fetch places by region
  Future<List<Place>> getPlacesByRegion({int? cityId, int? districtId, int? dongId}) async {
    try {
      final Map<String, dynamic> params = {};
      if (cityId != null) params['cityId'] = cityId;
      if (districtId != null) params['districtId'] = districtId;
      if (dongId != null) params['dongId'] = dongId;

      final response = await _apiClient.dio.get(
        ApiEndpoints.places,
        queryParameters: params,
      );

      if (response.statusCode == 200 && response.data != null) {
        final List dataList = response.data['data'] ?? [];
        return dataList.map((json) => Place.fromJson(json)).toList();
      }
      return [];
    } catch (e) {
      return [];
    }
  }
  // Fetch place details
  Future<Map<String, dynamic>?> getPlace(int placeId) async {
    try {
      final response = await _apiClient.dio.get(ApiEndpoints.placeDetail(placeId));
      if (response.statusCode == 200 && response.data != null) {
        return response.data['data'];
      }
      return null;
    } catch (e) {
      return null;
    }
  }

  // Create a new place
  Future<Map<String, dynamic>> createPlace(Map<String, dynamic> placeData) async {
    try {
      final response = await _apiClient.dio.post(
        ApiEndpoints.places,
        data: placeData,
      );

      if (response.statusCode == 200 && response.data != null) {
        return {
          'success': true,
          'data': response.data['data'],
          'message': response.data['message'] ?? '등록 성공',
        };
      }
      return {
        'success': false,
        'message': '서버 응답 오류 (${response.statusCode})',
      };
    } on DioException catch (e) {
      final responseData = e.response?.data;
      String errMsg = '맛집 등록에 실패했습니다.';
      if (responseData != null && responseData is Map) {
        errMsg = responseData['message'] ?? errMsg;
      }
      return {
        'success': false,
        'message': errMsg,
      };
    } catch (e) {
      return {
        'success': false,
        'message': '네트워크 오류가 발생했습니다.',
      };
    }
  }

  // Get place registration policy
  Future<Map<String, dynamic>?> getRegistrationPolicy() async {
    try {
      final response = await _apiClient.dio.get(ApiEndpoints.registrationPolicy);
      if (response.statusCode == 200 && response.data != null) {
        return response.data['data'];
      }
      return null;
    } catch (e) {
      return null;
    }
  }

  // Recommend a place
  Future<bool> recommendPlace(int placeId) async {
    try {
      final response = await _apiClient.dio.post(ApiEndpoints.recommendPlace(placeId));
      return response.statusCode == 200;
    } catch (e) {
      return false;
    }
  }

  // Cancel recommendation of a place
  Future<bool> cancelRecommendPlace(int placeId) async {
    try {
      final response = await _apiClient.dio.delete(ApiEndpoints.recommendPlace(placeId));
      return response.statusCode == 200;
    } catch (e) {
      return false;
    }
  }

  // Get recommendation policy/status for a place
  Future<Map<String, dynamic>?> getRecommendationPolicy(int placeId) async {
    try {
      final response = await _apiClient.dio.get(ApiEndpoints.recommendPolicy(placeId));
      if (response.statusCode == 200 && response.data != null) {
        return response.data['data'];
      }
      return null;
    } catch (e) {
      return null;
    }
  }

  // Fetch comments of a place
  Future<List<dynamic>> getPlaceComments(int placeId) async {
    try {
      final response = await _apiClient.dio.get(ApiEndpoints.comments(placeId));
      if (response.statusCode == 200 && response.data != null) {
        return response.data['data'] ?? [];
      }
      return [];
    } catch (e) {
      return [];
    }
  }

  // Add comment to a place
  Future<Map<String, dynamic>> createComment(int placeId, String content) async {
    try {
      final response = await _apiClient.dio.post(
        ApiEndpoints.comments(placeId),
        data: {'content': content},
      );
      if (response.statusCode == 200 && response.data != null) {
        return {
          'success': true,
          'data': response.data['data'],
        };
      }
      return {'success': false, 'message': '댓글 작성 오류'};
    } on DioException catch (e) {
      final responseData = e.response?.data;
      String errMsg = '댓글 작성에 실패했습니다.';
      if (responseData != null && responseData is Map) {
        errMsg = responseData['message'] ?? errMsg;
      }
      return {'success': false, 'message': errMsg};
    } catch (e) {
      return {'success': false, 'message': '네트워크 오류'};
    }
  }

  // Delete a comment
  Future<bool> deleteComment(int commentId) async {
    try {
      final response = await _apiClient.dio.delete(ApiEndpoints.deleteComment(commentId));
      return response.statusCode == 200;
    } catch (e) {
      return false;
    }
  }

  // Fetch current user's primary region
  Future<Map<String, dynamic>?> getMyRegion() async {
    try {
      final response = await _apiClient.dio.get(ApiEndpoints.myRegion);
      if (response.statusCode == 200 && response.data != null) {
        return response.data['data'];
      }
      return null;
    } catch (e) {
      return null;
    }
  }
}
