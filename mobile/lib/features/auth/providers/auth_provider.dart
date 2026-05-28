import 'package:flutter/material.dart';
import 'package:kakao_flutter_sdk_user/kakao_flutter_sdk_user.dart' as kakao;

import '../../../core/api/token_manager.dart';
import '../../../core/config/app_config.dart';
import '../../../models/user.dart';
import '../services/auth_service.dart';

class AuthProvider extends ChangeNotifier {
  AuthProvider(this._authService, {bool autoInitialize = true}) {
    if (autoInitialize) {
      initialize();
    }
  }

  final AuthService _authService;

  bool _isAuthenticated = false;
  bool _isInitializing = false;
  bool _isLoading = false;
  String? _errorMessage;

  UserProfile? _userProfile;
  UserStatus? _userStatus;
  UserActivitySummary? _userSummary;

  bool get isAuthenticated => _isAuthenticated;
  bool get isInitializing => _isInitializing;
  bool get isLoading => _isLoading;
  String? get errorMessage => _errorMessage;
  UserProfile? get userProfile => _userProfile;
  UserStatus? get userStatus => _userStatus;
  UserActivitySummary? get userSummary => _userSummary;

  Future<void> initialize() async {
    _setInitializing(true);
    final hasToken = await TokenManager.isLoggedIn();
    if (hasToken) {
      final success = await _fetchUserData();
      if (success) {
        _isAuthenticated = true;
      } else {
        await _authService.logout();
        _isAuthenticated = false;
      }
    } else {
      _isAuthenticated = false;
    }
    _setInitializing(false);
    notifyListeners();
  }

  Future<bool> login(String email, String password) async {
    _setLoading(true);
    _clearError();
    final result = await _authService.login(email, password);
    if (result.success) {
      final fetched = await _fetchUserData();
      if (fetched) {
        _isAuthenticated = true;
        _setLoading(false);
        notifyListeners();
        return true;
      }

      await TokenManager.clearTokens();
      _setError('로그인은 되었지만 내 정보를 불러오지 못했습니다. 다시 시도해주세요.');
      _setLoading(false);
      return false;
    }

    _setError(result.errorMessage ?? '로그인에 실패했습니다. 이메일과 비밀번호를 확인해주세요.');
    _setLoading(false);
    return false;
  }

  Future<bool> signup(
    String email,
    String password,
    String nickname,
    String phone,
  ) async {
    _setLoading(true);
    _clearError();
    final result = await _authService.signup(email, password, nickname, phone);
    _setLoading(false);
    if (result.success) {
      return true;
    }

    _setError(
      result.errorMessage ?? '회원가입에 실패했습니다. 이미 사용 중인 이메일이거나 입력값이 올바르지 않습니다.',
    );
    return false;
  }

  Future<bool> loginWithKakao() async {
    _setLoading(true);
    _clearError();
    try {
      String? accessToken;

      try {
        final isInstalled = await kakao.isKakaoTalkInstalled();
        final token = isInstalled
            ? await kakao.UserApi.instance.loginWithKakaoTalk()
            : await kakao.UserApi.instance.loginWithKakaoAccount();
        accessToken = token.accessToken;
      } catch (e) {
        debugPrint('Kakao native SDK failed or is not configured: $e');
        if (!AppConfig.allowMockKakaoLogin) {
          _setError('카카오 로그인 설정을 확인해주세요.');
          _setLoading(false);
          return false;
        }
        debugPrint('Falling back to mock Kakao login for development.');
        accessToken = 'mock_kakao_access_token_for_development';
      }

      final success = await _authService.oauthLogin('kakao', accessToken);
      if (success) {
        final fetched = await _fetchUserData();
        if (fetched) {
          _isAuthenticated = true;
          _setLoading(false);
          notifyListeners();
          return true;
        }
      }
      _setError('카카오 인증 처리 중 백엔드 검증에 실패했습니다.');
      _setLoading(false);
      return false;
    } catch (e) {
      _setError('카카오 로그인 중 오류가 발생했습니다: $e');
      _setLoading(false);
      return false;
    }
  }

  Future<void> logout() async {
    _setLoading(true);
    await _authService.logout();
    _isAuthenticated = false;
    _userProfile = null;
    _userStatus = null;
    _userSummary = null;
    _setLoading(false);
    notifyListeners();
  }

  Future<bool> sendPhoneVerification(String phone) async {
    _setLoading(true);
    _clearError();
    final success = await _authService.sendPhoneVerificationCode(phone);
    _setLoading(false);
    if (!success) {
      _setError('인증번호 전송에 실패했습니다.');
    }
    return success;
  }

  Future<bool> verifyPhoneCode(String phone, String code) async {
    _setLoading(true);
    _clearError();
    final success = await _authService.verifyPhoneCode(phone, code);
    if (success) {
      await _fetchUserData();
      _setLoading(false);
      notifyListeners();
      return true;
    }
    _setError('인증번호가 일치하지 않거나 만료되었습니다.');
    _setLoading(false);
    return false;
  }

  Future<bool> verifyRegion({
    required double latitude,
    required double longitude,
  }) async {
    _setLoading(true);
    _clearError();
    final success = await _authService.verifyRegion(
      latitude: latitude,
      longitude: longitude,
    );
    if (success) {
      await _fetchUserData();
      _setLoading(false);
      notifyListeners();
      return true;
    }
    _setError('현재 위치로 동네 인증에 실패했습니다.');
    _setLoading(false);
    return false;
  }

  Future<void> refreshUserProfile() async {
    if (_isAuthenticated) {
      await _fetchUserData();
      notifyListeners();
    }
  }

  Future<bool> updateMyProfile({
    required String nickname,
    required String languagePreference,
    int? birthYear,
    String? gender,
    String? nationalityCode,
    String? profileImageUrl,
  }) async {
    _setLoading(true);
    _clearError();
    final profile = await _authService.updateMyProfile(
      nickname: nickname,
      languagePreference: languagePreference,
      birthYear: birthYear,
      gender: gender,
      nationalityCode: nationalityCode,
      profileImageUrl: profileImageUrl,
    );
    if (profile == null) {
      _setError('프로필 수정에 실패했습니다.');
      _setLoading(false);
      return false;
    }
    _userProfile = profile;
    _setLoading(false);
    notifyListeners();
    return true;
  }

  Future<bool> _fetchUserData() async {
    final profile = await _authService.getMyProfile();
    if (profile == null) return false;

    _userProfile = profile;
    _userStatus = await _authService.getMyStatus();
    _userSummary = await _authService.getMyActivitySummary();
    return true;
  }

  void _setInitializing(bool value) {
    _isInitializing = value;
    notifyListeners();
  }

  void _setLoading(bool value) {
    _isLoading = value;
    notifyListeners();
  }

  void _setError(String message) {
    _errorMessage = message;
  }

  void _clearError() {
    _errorMessage = null;
  }
}
