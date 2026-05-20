class UserProfile {
  final int id;
  final String username;
  final String nickname;
  final String phone;
  final bool phoneVerified;
  final String? role;

  UserProfile({
    required this.id,
    required this.username,
    required this.nickname,
    required this.phone,
    required this.phoneVerified,
    this.role,
  });

  factory UserProfile.fromJson(Map<String, dynamic> json) {
    return UserProfile(
      id: json['id'],
      username: json['username'],
      nickname: json['nickname'],
      phone: json['phone'] ?? '',
      phoneVerified: json['phoneVerified'] ?? false,
      role: json['role'],
    );
  }
}

class UserStatus {
  final int level;
  final int exp;
  final int totalExp;
  final int nextLevelExp;
  final String trustGrade;
  final double recommendWeight;

  UserStatus({
    required this.level,
    required this.exp,
    required this.totalExp,
    required this.nextLevelExp,
    required this.trustGrade,
    required this.recommendWeight,
  });

  factory UserStatus.fromJson(Map<String, dynamic> json) {
    return UserStatus(
      level: json['level'] ?? 1,
      exp: json['exp'] ?? 0,
      totalExp: json['totalExp'] ?? 0,
      nextLevelExp: json['nextLevelExp'] ?? 0,
      trustGrade: json['trustGrade'] ?? 'NORMAL',
      recommendWeight: (json['recommendWeight'] ?? 1.0).toDouble(),
    );
  }
}

class UserActivitySummary {
  final int recommendationCount;
  final int visitCount;
  final int commentCount;
  final int placeCount;

  UserActivitySummary({
    required this.recommendationCount,
    required this.visitCount,
    required this.commentCount,
    required this.placeCount,
  });

  factory UserActivitySummary.fromJson(Map<String, dynamic> json) {
    return UserActivitySummary(
      recommendationCount: json['recommendationCount'] ?? 0,
      visitCount: json['visitCount'] ?? 0,
      commentCount: json['commentCount'] ?? 0,
      placeCount: json['placeCount'] ?? 0,
    );
  }
}
