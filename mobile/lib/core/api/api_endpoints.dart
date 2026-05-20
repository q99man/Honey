class ApiEndpoints {
  // Base URL (Change to your backend server host)
  // For Android Emulator, 10.0.2.2 points to localhost of the host machine
  static const String baseUrl = 'http://10.0.2.2:8080';

  // Auth Endpoints
  static const String signup = '/api/auth/signup';
  static const String login = '/api/auth/login';
  static const String logout = '/api/auth/logout';
  static const String refresh = '/api/auth/refresh';
  static const String oauthLogin = '/api/auth/oauth'; // e.g. /api/auth/oauth/kakao
  
  // Phone Verification
  static const String sendPhoneCode = '/api/auth/phone/send-code';
  static const String verifyPhoneCode = '/api/auth/phone/verify-code';
  static const String phoneStatus = '/api/auth/phone/status';

  // Region
  static const String myRegion = '/api/regions/me';
  static const String verifyRegion = '/api/regions/verify';
  static const String regionPolicy = '/api/regions/me/change-policy';

  // Place
  static const String places = '/api/places';
  static const String nearbyPlaces = '/api/places/nearby';
  static const String searchPlaces = '/api/places/search';
  static const String registrationPolicy = '/api/places/registration-policy';
  static String placeDetail(int id) => '/api/places/$id';
  static String placeRankingHistory(int id) => '/api/places/$id/ranking-history';

  // Recommendation
  static String recommendPlace(int id) => '/api/places/$id/recommend';
  static String recommendPolicy(int id) => '/api/places/$id/recommend-policy';

  // Visit
  static String verifyVisit(int id) => '/api/places/$id/visits';
  static String visitPolicy(int id) => '/api/places/$id/visit-policy';
  static String visitSummary(int id) => '/api/places/$id/visits/summary';

  // Comment
  static String comments(int placeId) => '/api/places/$placeId/comments';
  static String updateComment(int commentId) => '/api/comments/$commentId';
  static String deleteComment(int commentId) => '/api/comments/$commentId';

  // Rankings
  static const String currentSeason = '/api/rankings/seasons/current';
  static const String rankingPlaces = '/api/rankings/places';

  // User Status & Activity
  static const String userStatus = '/api/users/me/status';
  static const String userSummary = '/api/users/me/activity-summary';
  static const String userProfile = '/api/users/me';
  static const String userPlaces = '/api/users/me/places';
  static const String userReports = '/api/users/me/reports';
}
