// This is a test file for Honeytong mobile models.

import 'package:flutter_test/flutter_test.dart';
import 'package:honeytong_mobile/models/community_post.dart';

void main() {
  group('CommunityPost Model Tests', () {
    test('Should parse CommunityPost from valid JSON', () {
      final json = {
        'postId': 1,
        'authorUserId': 10,
        'authorNickname': '꿀벌123',
        'title': '오늘 날씨가 너무 좋네요',
        'content': '동네 광장에 꿀 따러 가기 좋은 날입니다. 🐝',
        'createdAt': '2026-05-21T15:30:00Z',
        'updatedAt': '2026-05-21T15:30:00Z',
        'mine': true,
      };

      final post = CommunityPost.fromJson(json);

      expect(post.postId, 1);
      expect(post.authorUserId, 10);
      expect(post.authorNickname, '꿀벌123');
      expect(post.title, '오늘 날씨가 너무 좋네요');
      expect(post.content, '동네 광장에 꿀 따러 가기 좋은 날입니다. 🐝');
      expect(post.createdAt, '2026-05-21T15:30:00Z');
      expect(post.updatedAt, '2026-05-21T15:30:00Z');
      expect(post.mine, true);
    });

    test(
        'Should parse CommunityPost from JSON with missing fields (null safety)',
        () {
      final json = <String, dynamic>{};

      final post = CommunityPost.fromJson(json);

      expect(post.postId, 0);
      expect(post.authorUserId, 0);
      expect(post.authorNickname, '');
      expect(post.title, '');
      expect(post.content, '');
      expect(post.createdAt, '');
      expect(post.updatedAt, '');
      expect(post.mine, false);
    });
  });
}
