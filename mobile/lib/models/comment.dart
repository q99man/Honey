class Comment {
  final int id;
  final int placeId;
  final int userId;
  final String userNickname;
  final String userTrustGrade;
  final String content;
  final bool isDeleted;
  final String createdAt;
  final String? updatedAt;

  Comment({
    required this.id,
    required this.placeId,
    required this.userId,
    required this.userNickname,
    required this.userTrustGrade,
    required this.content,
    required this.isDeleted,
    required this.createdAt,
    this.updatedAt,
  });

  factory Comment.fromJson(Map<String, dynamic> json) {
    return Comment(
      id: json['id'],
      placeId: json['placeId'] ?? 0,
      userId: json['userId'] ?? 0,
      userNickname: json['userNickname'] ?? 'Unknown',
      userTrustGrade: json['userTrustGrade'] ?? 'NORMAL',
      content: json['content'] ?? '',
      isDeleted: json['isDeleted'] ?? false,
      createdAt: json['createdAt'] ?? '',
      updatedAt: json['updatedAt'],
    );
  }
}
