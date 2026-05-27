import 'package:dio/dio.dart';
import '../../../core/api/api_client.dart';
import '../../../core/api/api_endpoints.dart';

class VisitService {
  final ApiClient _apiClient;

  VisitService(this._apiClient);

  // Send request to verify visit
  Future<Map<String, dynamic>> verifyVisit(
      int placeId, double lat, double lng) async {
    try {
      final response = await _apiClient.dio.post(
        ApiEndpoints.verifyVisit(placeId),
        data: {
          'latitude': lat,
          'longitude': lng,
          'imageUrl': null,
        },
      );

      if (response.statusCode == 200 && response.data != null) {
        final data = response.data['data'] ?? {};
        final message = response.data['message'] ?? '방문 인증 완료';
        return {
          'success': true,
          'verified': data['verified'] ?? false,
          'distanceMeter': data['distanceMeter'] ?? 0,
          'expGained': data['expGained'] ?? 0,
          'visitCount': data['visitCount'] ?? 0,
          'message': message,
        };
      }

      return {
        'success': false,
        'message': '서버 응답 오류 (상태 코드: ${response.statusCode})',
      };
    } on DioException catch (e) {
      final responseData = e.response?.data;
      String errMsg = '방문 인증에 실패했습니다.';
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
        'message': '인터넷 연결을 확인하고 다시 시도해주세요.',
      };
    }
  }

  // Fetch visit policy for a place
  Future<Map<String, dynamic>?> getVisitPolicy(int placeId) async {
    try {
      final response =
          await _apiClient.dio.get(ApiEndpoints.visitPolicy(placeId));
      if (response.statusCode == 200 && response.data != null) {
        return response.data['data'];
      }
      return null;
    } catch (e) {
      return null;
    }
  }

  // Fetch visit summary for a place
  Future<Map<String, dynamic>?> getVisitSummary(int placeId) async {
    try {
      final response =
          await _apiClient.dio.get(ApiEndpoints.visitSummary(placeId));
      if (response.statusCode == 200 && response.data != null) {
        return response.data['data'];
      }
      return null;
    } catch (e) {
      return null;
    }
  }
}
