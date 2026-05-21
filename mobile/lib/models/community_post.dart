class CommunityPost {
  final int postId;
  final int authorUserId;
  final String authorNickname;
  final String title;
  final String content;
  final String createdAt;
  final String updatedAt;
  final bool mine;

  CommunityPost({
    required this.postId,
    required this.authorUserId,
    required this.authorNickname,
    required this.title,
    required this.content,
    required this.createdAt,
    required this.updatedAt,
    required this.mine,
  });

  factory CommunityPost.fromJson(Map<String, dynamic> json) {
    return CommunityPost(
      postId: json['postId'] ?? 0,
      authorUserId: json['authorUserId'] ?? 0,
      authorNickname: json['authorNickname'] ?? '',
      title: json['title'] ?? '',
      content: json['content'] ?? '',
      createdAt: json['createdAt'] ?? '',
      updatedAt: json['updatedAt'] ?? '',
      mine: json['mine'] ?? false,
    );
  }
}
