import '../../../core/api/api_client.dart';
import '../../../core/api/api_endpoints.dart';
import '../../../core/api/token_manager.dart';
import '../../../models/user.dart';

class AuthService {
  final ApiClient _apiClient;

  AuthService(this._apiClient);

  // Local login
  Future<bool> login(String email, String password) async {
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
        return true;
      }
      return false;
    } catch (e) {
      return false;
    }
  }

  // Local signup
  Future<bool> signup(
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
      return response.statusCode == 200;
    } catch (e) {
      return false;
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
}
