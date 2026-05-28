import 'package:dio/dio.dart';

import '../../../core/api/api_client.dart';
import '../../../core/api/api_endpoints.dart';
import '../../../core/api/token_manager.dart';
import '../../../models/user.dart';

class AuthResult {
  const AuthResult._({
    required this.success,
    this.errorMessage,
  });

  final bool success;
  final String? errorMessage;

  factory AuthResult.success() => const AuthResult._(success: true);

  factory AuthResult.failure(String errorMessage) => AuthResult._(
        success: false,
        errorMessage: errorMessage,
      );
}

class AuthService {
  final ApiClient _apiClient;

  AuthService(this._apiClient);

  // Local login
  Future<AuthResult> login(String email, String password) async {
    try {
      final response = await _apiClient.dio.post(
        ApiEndpoints.login,
        data: {
          'email': email,
          'password': password,
        },
      );

      if (response.statusCode == 200 && response.data != null) {
        final data = response.data['data'];
        await TokenManager.saveTokens(
          accessToken: data['accessToken'],
          refreshToken: data['refreshToken'],
        );
        return AuthResult.success();
      }
      return AuthResult.failure('로그인 응답을 확인할 수 없습니다. 다시 시도해주세요.');
    } catch (error) {
      return AuthResult.failure(
        _messageFromError(
          error,
          fallback: '로그인에 실패했습니다. 이메일과 비밀번호를 확인해주세요.',
        ),
      );
    }
  }

  // Local signup
  Future<AuthResult> signup(
    String email,
    String password,
    String nickname,
    String phone,
  ) async {
    try {
      final response = await _apiClient.dio.post(
        ApiEndpoints.signup,
        data: {
          'email': email,
          'password': password,
          'nickname': nickname,
        },
      );
      if (response.statusCode == 200) {
        return AuthResult.success();
      }
      return AuthResult.failure('회원가입 응답을 확인할 수 없습니다. 다시 시도해주세요.');
    } catch (error) {
      return AuthResult.failure(
        _messageFromError(
          error,
          fallback: '회원가입에 실패했습니다. 이미 사용 중인 이메일이거나 입력값이 올바르지 않습니다.',
        ),
      );
    }
  }

  // OAuth Login (Mobile client SDK logs in first, gets token, then calls this)
  Future<bool> oauthLogin(String provider, String providerAccessToken) async {
    try {
      final response = await _apiClient.dio.post(
        '${ApiEndpoints.oauthLogin}/$provider',
        data: {
          'accessToken': providerAccessToken,
        },
      );

      if (response.statusCode == 200 && response.data != null) {
        final data = response.data['data'];
        await TokenManager.saveTokens(
          accessToken: data['accessToken'],
          refreshToken: data['refreshToken'],
        );
        return true;
      }
      return false;
    } catch (e) {
      return false;
    }
  }

  // Logout
  Future<void> logout() async {
    try {
      final refreshToken = await TokenManager.getRefreshToken();
      if (refreshToken != null) {
        await _apiClient.dio.post(
          ApiEndpoints.logout,
          data: {'refreshToken': refreshToken},
        );
      }
    } finally {
      await TokenManager.clearTokens();
    }
  }

  // Send Phone Verification Code
  Future<bool> sendPhoneVerificationCode(String phone) async {
    try {
      final response = await _apiClient.dio.post(
        ApiEndpoints.sendPhoneCode,
        data: {'phone': phone},
      );
      return response.statusCode == 200;
    } catch (e) {
      return false;
    }
  }

  // Verify Phone Code
  Future<bool> verifyPhoneCode(String phone, String code) async {
    try {
      final response = await _apiClient.dio.post(
        ApiEndpoints.verifyPhoneCode,
        data: {
          'phone': phone,
          'code': code,
        },
      );
      if (response.statusCode == 200 && response.data != null) {
        final data = response.data['data'];
        return data['phoneVerified'] == true || data['verified'] == true;
      }
      return false;
    } catch (e) {
      return false;
    }
  }

  // Get Phone Verification Status
  Future<bool> getPhoneVerificationStatus() async {
    try {
      final response = await _apiClient.dio.get(ApiEndpoints.phoneStatus);
      if (response.statusCode == 200 && response.data != null) {
        final data = response.data['data'];
        return data['phoneVerified'] == true || data['verified'] == true;
      }
      return false;
    } catch (e) {
      return false;
    }
  }

  // Verify Region With Current GPS Coordinate
  Future<bool> verifyRegion({
    required double latitude,
    required double longitude,
  }) async {
    try {
      final response = await _apiClient.dio.post(
        ApiEndpoints.verifyRegion,
        data: {
          'latitude': latitude,
          'longitude': longitude,
        },
      );
      if (response.statusCode == 200 && response.data != null) {
        return response.data['data']['verified'] == true;
      }
      return false;
    } catch (e) {
      return false;
    }
  }

  // Get My Profile
  Future<UserProfile?> getMyProfile() async {
    try {
      final response = await _apiClient.dio.get(ApiEndpoints.userProfile);
      if (response.statusCode == 200 && response.data != null) {
        return UserProfile.fromJson(response.data['data']);
      }
      return null;
    } catch (e) {
      return null;
    }
  }

  Future<UserProfile?> updateMyProfile({
    required String nickname,
    required String languagePreference,
    int? birthYear,
    String? gender,
    String? nationalityCode,
    String? profileImageUrl,
  }) async {
    try {
      final response = await _apiClient.dio.patch(
        ApiEndpoints.userProfile,
        data: {
          'nickname': nickname,
          'languagePreference': languagePreference,
          'birthYear': birthYear,
          'gender': gender,
          'nationalityCode': nationalityCode,
          'profileImageUrl': profileImageUrl,
        },
      );
      if (response.statusCode == 200 && response.data != null) {
        return UserProfile.fromJson(response.data['data']);
      }
      return null;
    } catch (_) {
      return null;
    }
  }

  // Get My Status
  Future<UserStatus?> getMyStatus() async {
    try {
      final response = await _apiClient.dio.get(ApiEndpoints.userStatus);
      if (response.statusCode == 200 && response.data != null) {
        return UserStatus.fromJson(response.data['data']);
      }
      return null;
    } catch (e) {
      return null;
    }
  }

  // Get My Activity Summary
  Future<UserActivitySummary?> getMyActivitySummary() async {
    try {
      final response = await _apiClient.dio.get(ApiEndpoints.userSummary);
      if (response.statusCode == 200 && response.data != null) {
        return UserActivitySummary.fromJson(response.data['data']);
      }
      return null;
    } catch (e) {
      return null;
    }
  }

  String _messageFromError(Object error, {required String fallback}) {
    if (error is! DioException) {
      return fallback;
    }

    final data = error.response?.data;
    if (data is Map) {
      final message = data['message'];
      if (message is String && message.trim().isNotEmpty) {
        return message.trim();
      }
    }

    switch (error.type) {
      case DioExceptionType.connectionTimeout:
      case DioExceptionType.sendTimeout:
      case DioExceptionType.receiveTimeout:
      case DioExceptionType.connectionError:
      case DioExceptionType.unknown:
        return '서버에 연결하지 못했습니다. 네트워크와 개발 서버 연결을 확인해주세요.';
      case DioExceptionType.badCertificate:
      case DioExceptionType.badResponse:
      case DioExceptionType.cancel:
        return fallback;
    }
  }
}
