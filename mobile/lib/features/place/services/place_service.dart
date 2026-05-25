import 'package:dio/dio.dart';

import '../../../core/api/api_client.dart';
import '../../../core/api/api_endpoints.dart';
import '../../../models/place.dart';

class PlaceService {
  PlaceService(this._apiClient);

  final ApiClient _apiClient;

  Future<List<Place>> getNearbyPlaces(
    double lat,
    double lng, {
    int radius = 1000,
  }) async {
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
    } catch (_) {
      return [];
    }
  }

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
    } catch (_) {
      return [];
    }
  }

  Future<List<Place>> getPlacesByRegion({
    int? cityId,
    int? districtId,
    int? dongId,
  }) async {
    try {
      final params = <String, dynamic>{};
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
    } catch (_) {
      return [];
    }
  }

  Future<Map<String, dynamic>?> getPlace(int placeId) async {
    try {
      final response = await _apiClient.dio.get(
        ApiEndpoints.placeDetail(placeId),
      );
      if (response.statusCode == 200 && response.data != null) {
        return response.data['data'];
      }
      return null;
    } catch (_) {
      return null;
    }
  }

  Future<Map<String, dynamic>> createPlace(
    Map<String, dynamic> placeData,
  ) async {
    try {
      final response = await _apiClient.dio.post(
        ApiEndpoints.places,
        data: placeData,
      );

      if (response.statusCode == 200 && response.data != null) {
        return {
          'success': true,
          'data': response.data['data'],
          'message': response.data['message'] ?? '등록이 완료되었습니다.',
        };
      }
      return {
        'success': false,
        'message': '서버 응답 오류 (${response.statusCode})',
      };
    } on DioException catch (e) {
      final responseData = e.response?.data;
      var errMsg = '맛집 등록에 실패했습니다.';
      if (responseData is Map && responseData['message'] is String) {
        errMsg = responseData['message'];
      }
      return {
        'success': false,
        'message': errMsg,
      };
    } catch (_) {
      return {
        'success': false,
        'message': '네트워크 오류가 발생했습니다.',
      };
    }
  }

  Future<Map<String, dynamic>> updatePlace(
    int placeId,
    Map<String, dynamic> placeData,
  ) async {
    try {
      final response = await _apiClient.dio.patch(
        ApiEndpoints.placeDetail(placeId),
        data: placeData,
      );

      if (response.statusCode == 200 && response.data != null) {
        return {
          'success': true,
          'data': response.data['data'],
          'message': response.data['message'] ?? '맛집 정보가 수정되었습니다.',
        };
      }
      return {
        'success': false,
        'message': '서버 응답 오류 (${response.statusCode})',
      };
    } on DioException catch (e) {
      final responseData = e.response?.data;
      var errMsg = '맛집 수정에 실패했습니다.';
      if (responseData is Map && responseData['message'] is String) {
        errMsg = responseData['message'];
      }
      return {
        'success': false,
        'message': errMsg,
      };
    } catch (_) {
      return {
        'success': false,
        'message': '네트워크 오류가 발생했습니다.',
      };
    }
  }

  Future<Map<String, dynamic>> deletePlace(int placeId) async {
    try {
      final response = await _apiClient.dio.delete(
        ApiEndpoints.placeDetail(placeId),
      );
      if (response.statusCode == 200 && response.data != null) {
        return {
          'success': true,
          'data': response.data['data'],
          'message': response.data['message'] ?? '맛집이 삭제되었습니다.',
        };
      }
      return {
        'success': false,
        'message': '서버 응답 오류 (${response.statusCode})',
      };
    } on DioException catch (e) {
      final responseData = e.response?.data;
      var errMsg = '맛집 삭제에 실패했습니다.';
      if (responseData is Map && responseData['message'] is String) {
        errMsg = responseData['message'];
      }
      return {
        'success': false,
        'message': errMsg,
      };
    } catch (_) {
      return {
        'success': false,
        'message': '네트워크 오류가 발생했습니다.',
      };
    }
  }

  Future<List<Place>> getMyRegisteredPlaces() async {
    try {
      final response = await _apiClient.dio.get(ApiEndpoints.userPlaces);
      if (response.statusCode == 200 && response.data != null) {
        final List dataList = response.data['data'] ?? [];
        return dataList.map((json) => Place.fromJson(json)).toList();
      }
      return [];
    } catch (_) {
      return [];
    }
  }

  Future<Map<String, dynamic>?> getRegistrationPolicy() async {
    try {
      final response = await _apiClient.dio.get(
        ApiEndpoints.registrationPolicy,
      );
      if (response.statusCode == 200 && response.data != null) {
        return response.data['data'];
      }
      return null;
    } catch (_) {
      return null;
    }
  }

  Future<bool> recommendPlace(int placeId) async {
    try {
      final response = await _apiClient.dio.post(
        ApiEndpoints.recommendPlace(placeId),
      );
      return response.statusCode == 200;
    } catch (_) {
      return false;
    }
  }

  Future<bool> cancelRecommendPlace(int placeId) async {
    try {
      final response = await _apiClient.dio.delete(
        ApiEndpoints.recommendPlace(placeId),
      );
      return response.statusCode == 200;
    } catch (_) {
      return false;
    }
  }

  Future<Map<String, dynamic>?> getRecommendationPolicy(int placeId) async {
    try {
      final response = await _apiClient.dio.get(
        ApiEndpoints.recommendPolicy(placeId),
      );
      if (response.statusCode == 200 && response.data != null) {
        return response.data['data'];
      }
      return null;
    } catch (_) {
      return null;
    }
  }

  Future<List<dynamic>> getPlaceComments(int placeId) async {
    try {
      final response = await _apiClient.dio.get(ApiEndpoints.comments(placeId));
      if (response.statusCode == 200 && response.data != null) {
        return response.data['data'] ?? [];
      }
      return [];
    } catch (_) {
      return [];
    }
  }

  Future<Map<String, dynamic>> createComment(
    int placeId,
    String content,
  ) async {
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
      return {'success': false, 'message': '평가 작성 오류'};
    } on DioException catch (e) {
      final responseData = e.response?.data;
      var errMsg = '평가 작성에 실패했습니다.';
      if (responseData is Map && responseData['message'] is String) {
        errMsg = responseData['message'];
      }
      return {'success': false, 'message': errMsg};
    } catch (_) {
      return {'success': false, 'message': '네트워크 오류'};
    }
  }

  Future<bool> deleteComment(int commentId) async {
    try {
      final response = await _apiClient.dio.delete(
        ApiEndpoints.deleteComment(commentId),
      );
      return response.statusCode == 200;
    } catch (_) {
      return false;
    }
  }

  Future<Map<String, dynamic>?> getMyRegion() async {
    try {
      final response = await _apiClient.dio.get(ApiEndpoints.myRegion);
      if (response.statusCode == 200 && response.data != null) {
        return response.data['data'];
      }
      return null;
    } catch (_) {
      return null;
    }
  }
}
