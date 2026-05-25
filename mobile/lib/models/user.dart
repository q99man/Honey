class UserProfile {
  UserProfile({
    required this.id,
    required this.nickname,
    required this.phoneVerified,
    this.languagePreference,
    this.birthYear,
    this.gender,
    this.nationalityCode,
  });

  final int id;
  final String nickname;
  final bool phoneVerified;
  final String? languagePreference;
  final int? birthYear;
  final String? gender;
  final String? nationalityCode;

  String get displayName => nickname;

  factory UserProfile.fromJson(Map<String, dynamic> json) {
    return UserProfile(
      id: json['id'] ?? 0,
      nickname: json['nickname'] ?? '',
      phoneVerified: json['phoneVerified'] ?? false,
      languagePreference: json['languagePreference'],
      birthYear: json['birthYear'],
      gender: json['gender'],
      nationalityCode: json['nationalityCode'],
    );
  }
}

class UserStatus {
  UserStatus({
    required this.level,
    required this.exp,
    required this.totalExp,
    required this.nextLevelExp,
    required this.trustGrade,
    required this.recommendWeight,
    required this.phoneVerified,
    required this.regionVerified,
  });

  final int level;
  final int exp;
  final int totalExp;
  final int nextLevelExp;
  final String trustGrade;
  final double recommendWeight;
  final bool phoneVerified;
  final bool regionVerified;

  factory UserStatus.fromJson(Map<String, dynamic> json) {
    return UserStatus(
      level: json['level'] ?? 1,
      exp: json['exp'] ?? 0,
      totalExp: json['totalExp'] ?? 0,
      nextLevelExp: json['nextLevelExp'] ?? 0,
      trustGrade: json['trustGrade'] ?? 'NORMAL',
      recommendWeight: (json['recommendWeight'] ?? 1.0).toDouble(),
      phoneVerified: json['phoneVerified'] ?? false,
      regionVerified: json['regionVerified'] ?? false,
    );
  }
}

class UserActivitySummary {
  UserActivitySummary({
    required this.recommendationCount,
    required this.visitCount,
    required this.commentCount,
    required this.placeCount,
  });

  final int recommendationCount;
  final int visitCount;
  final int commentCount;
  final int placeCount;

  factory UserActivitySummary.fromJson(Map<String, dynamic> json) {
    return UserActivitySummary(
      recommendationCount:
          json['recommendationCount'] ?? json['recommendedCount'] ?? 0,
      visitCount: json['visitCount'] ?? 0,
      commentCount: json['commentCount'] ?? 0,
      placeCount: json['placeCount'] ?? json['registeredPlaceCount'] ?? 0,
    );
  }
}
