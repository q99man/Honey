import '../../../core/api/api_client.dart';
import '../../../core/api/api_endpoints.dart';
import '../../../models/community_post.dart';

class CommunityService {
  final ApiClient _apiClient;

  CommunityService(this._apiClient);

  // 커뮤니티 게시글 목록 조회
  Future<List<CommunityPost>> getPosts() async {
    try {
      final response = await _apiClient.dio.get(ApiEndpoints.communityPosts);
      if (response.statusCode == 200 && response.data != null) {
        final List dataList = response.data['data'] ?? [];
        return dataList.map((json) => CommunityPost.fromJson(json)).toList();
      }
      return [];
    } catch (e) {
      return [];
    }
  }

  // 단일 게시글 상세 조회
  Future<CommunityPost?> getPost(int id) async {
    try {
      final response = await _apiClient.dio.get(ApiEndpoints.communityPostDetail(id));
      if (response.statusCode == 200 && response.data != null) {
        final data = response.data['data'];
        if (data != null) {
          return CommunityPost.fromJson(data);
        }
      }
      return null;
    } catch (e) {
      return null;
    }
  }

  // 신규 게시글 등록
  Future<int?> createPost(String title, String content) async {
    try {
      final response = await _apiClient.dio.post(
        ApiEndpoints.communityPosts,
        data: {
          'title': title,
          'content': content,
        },
      );
      if (response.statusCode == 200 && response.data != null) {
        final data = response.data['data'];
        if (data != null && data['postId'] != null) {
          return data['postId'] as int;
        }
      }
      return null;
    } catch (e) {
      return null;
    }
  }

  // 게시글 수정
  Future<CommunityPost?> updatePost(int id, String title, String content) async {
    try {
      final response = await _apiClient.dio.patch(
        ApiEndpoints.updateCommunityPost(id),
        data: {
          'title': title,
          'content': content,
        },
      );
      if (response.statusCode == 200 && response.data != null) {
        final data = response.data['data'];
        if (data != null) {
          return CommunityPost.fromJson(data);
        }
      }
      return null;
    } catch (e) {
      return null;
    }
  }

  // 게시글 삭제
  Future<bool> deletePost(int id) async {
    try {
      final response = await _apiClient.dio.delete(ApiEndpoints.deleteCommunityPost(id));
      if (response.statusCode == 200 && response.data != null) {
        final data = response.data['data'];
        return data != null && data['deleted'] == true;
      }
      return false;
    } catch (e) {
      return false;
    }
  }

  // 내가 쓴 게시글 목록 조회
  Future<List<CommunityPost>> getMyPosts() async {
    try {
      final response = await _apiClient.dio.get(ApiEndpoints.myCommunityPosts);
      if (response.statusCode == 200 && response.data != null) {
        final List dataList = response.data['data'] ?? [];
        return dataList.map((json) => CommunityPost.fromJson(json)).toList();
      }
      return [];
    } catch (e) {
      return [];
    }
  }
}
