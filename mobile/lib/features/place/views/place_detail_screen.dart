import 'package:flutter/material.dart';
import 'package:geolocator/geolocator.dart';
import 'package:provider/provider.dart';
import '../../../core/api/api_client.dart';
import '../../auth/providers/auth_provider.dart';
import '../../auth/views/login_screen.dart';
import '../../visit/services/visit_service.dart';
import '../services/place_service.dart';

class PlaceDetailScreen extends StatefulWidget {
  final int placeId;

  const PlaceDetailScreen({super.key, required this.placeId});

  @override
  State<PlaceDetailScreen> createState() => _PlaceDetailScreenState();
}

class _HomeDetailActivityIndicator extends StatelessWidget {
  const _HomeDetailActivityIndicator();

  @override
  Widget build(BuildContext context) {
    return const Center(
      child: CircularProgressIndicator(
        valueColor: AlwaysStoppedAnimation<Color>(Color(0xFFFFB300)),
      ),
    );
  }
}

class _PlaceDetailScreenState extends State<PlaceDetailScreen> {
  late PlaceService _placeService;
  late VisitService _visitService;

  Map<String, dynamic>? _placeDetails;
  Map<String, dynamic>? _recommendationPolicy;
  List<dynamic> _comments = [];

  bool _isLoadingDetails = true;
  bool _isLoadingComments = true;
  bool _isActionInProgress = false;

  final _commentController = TextEditingController();

  final Map<String, String> _categoryMap = {
    'KOREAN': '한식',
    'CHINESE': '중식',
    'JAPANESE': '일식',
    'WESTERN': '양식',
    'SNACK': '분식',
    'CAFE': '카페',
  };

  @override
  void initState() {
    super.initState();
    final apiClient = Provider.of<ApiClient>(context, listen: false);
    _placeService = PlaceService(apiClient);
    _visitService = VisitService(apiClient);

    _loadAllData();
  }

  @override
  void dispose() {
    _commentController.dispose();
    super.dispose();
  }

  Future<void> _loadAllData() async {
    setState(() {
      _isLoadingDetails = true;
      _isLoadingComments = true;
    });

    final authProvider = Provider.of<AuthProvider>(context, listen: false);

    // 1. Place details
    final details = await _placeService.getPlace(widget.placeId);
    if (!mounted) return;

    // 2. Recommendation policy (only if authenticated)
    Map<String, dynamic>? policy;
    if (authProvider.isAuthenticated) {
      policy = await _placeService.getRecommendationPolicy(widget.placeId);
      if (!mounted) return;
    }

    // 3. Comments
    final commentsList = await _placeService.getPlaceComments(widget.placeId);
    if (!mounted) return;

    setState(() {
      _placeDetails = details;
      _recommendationPolicy = policy;
      _comments = commentsList;
      _isLoadingDetails = false;
      _isLoadingComments = false;
    });
  }

  Future<void> _refreshComments() async {
    setState(() => _isLoadingComments = true);
    final commentsList = await _placeService.getPlaceComments(widget.placeId);
    if (!mounted) return;
    setState(() {
      _comments = commentsList;
      _isLoadingComments = false;
    });
  }

  bool get _isRecommended {
    if (_recommendationPolicy == null) return false;
    return _recommendationPolicy!['reason'] == 'ALREADY_RECOMMENDED';
  }

  void _handleRecommendation() async {
    final authProvider = Provider.of<AuthProvider>(context, listen: false);
    if (!authProvider.isAuthenticated) {
      _showLoginPrompt();
      return;
    }

    setState(() => _isActionInProgress = true);

    bool success;
    if (_isRecommended) {
      // Cancel
      success = await _placeService.cancelRecommendPlace(widget.placeId);
      if (!mounted) return;
      if (success) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('맛집 추천을 취소했습니다.')),
        );
      }
    } else {
      // Check policy
      if (_recommendationPolicy != null && _recommendationPolicy!['canRecommend'] == false) {
        final reason = _recommendationPolicy!['reason'];
        if (reason == 'DAILY_LIMIT_EXCEEDED') {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(
              content: Text('일일 추천 한도를 초과했습니다.'),
              backgroundColor: Colors.redAccent,
            ),
          );
          setState(() => _isActionInProgress = false);
          return;
        }
      }

      success = await _placeService.recommendPlace(widget.placeId);
      if (!mounted) return;
      if (success) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('이 맛집을 추천했습니다! 👍')),
        );
      }
    }

    if (success) {
      // Reload details and recommendation status
      final details = await _placeService.getPlace(widget.placeId);
      final policy = await _placeService.getRecommendationPolicy(widget.placeId);
      if (!mounted) return;
      setState(() {
        _placeDetails = details;
        _recommendationPolicy = policy;
      });
    }

    setState(() => _isActionInProgress = false);
  }

  void _handleVisitVerification() async {
    final authProvider = Provider.of<AuthProvider>(context, listen: false);
    if (!authProvider.isAuthenticated) {
      _showLoginPrompt();
      return;
    }

    // First check user verification
    if (authProvider.userProfile?.phoneVerified != true) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('방문 인증을 위해서는 휴대폰 번호 인증이 필요합니다.'),
          backgroundColor: Colors.orangeAccent,
        ),
      );
      return;
    }

    setState(() => _isActionInProgress = true);

    // Get current location
    try {
      Position position = await Geolocator.getCurrentPosition(
        desiredAccuracy: LocationAccuracy.high,
      );

      final result = await _visitService.verifyVisit(
        widget.placeId,
        position.latitude,
        position.longitude,
      );
      if (!mounted) return;

      setState(() => _isActionInProgress = false);

      if (result['success'] == true) {
        _showVisitSuccessDialog(result);
        
        // Reload details to update stats
        final details = await _placeService.getPlace(widget.placeId);
        if (!mounted) return;
        setState(() {
          _placeDetails = details;
        });
      } else {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(result['message']),
            backgroundColor: Colors.redAccent,
          ),
        );
      }
    } catch (e) {
      if (!mounted) return;
      setState(() => _isActionInProgress = false);
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('GPS 위치정보 획득에 실패했습니다: $e'),
          backgroundColor: Colors.redAccent,
        ),
      );
    }
  }

  void _submitComment() async {
    final text = _commentController.text.trim();
    if (text.isEmpty) return;

    final authProvider = Provider.of<AuthProvider>(context, listen: false);
    if (!authProvider.isAuthenticated) {
      _showLoginPrompt();
      return;
    }

    FocusScope.of(context).unfocus();
    setState(() => _isLoadingComments = true);

    final result = await _placeService.createComment(widget.placeId, text);
    if (!mounted) return;

    if (result['success'] == true) {
      _commentController.clear();
      _refreshComments();
      // Reload place stats to show updated comment count
      final details = await _placeService.getPlace(widget.placeId);
      if (!mounted) return;
      setState(() {
        _placeDetails = details;
      });
    } else {
      setState(() => _isLoadingComments = false);
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(result['message'] ?? '댓글 등록 실패'),
          backgroundColor: Colors.redAccent,
        ),
      );
    }
  }

  void _deleteComment(int commentId) async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('댓글 삭제', style: TextStyle(fontWeight: FontWeight.bold)),
        content: const Text('이 댓글을 정말 삭제하시겠습니까?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('취소', style: TextStyle(color: Colors.black54)),
          ),
          ElevatedButton(
            onPressed: () => Navigator.pop(context, true),
            style: ElevatedButton.styleFrom(backgroundColor: Colors.redAccent, foregroundColor: Colors.white),
            child: const Text('삭제'),
          ),
        ],
      ),
    );

    if (!mounted) return;
    if (confirm != true) return;

    setState(() => _isLoadingComments = true);
    final success = await _placeService.deleteComment(commentId);
    if (!mounted) return;

    if (success) {
      _refreshComments();
      // Reload place details
      final details = await _placeService.getPlace(widget.placeId);
      if (!mounted) return;
      setState(() {
        _placeDetails = details;
      });
    } else {
      setState(() => _isLoadingComments = false);
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('댓글 삭제에 실패했습니다.'),
          backgroundColor: Colors.redAccent,
        ),
      );
    }
  }

  void _showVisitSuccessDialog(Map<String, dynamic> data) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        title: const Row(
          children: [
            Icon(Icons.stars, color: Color(0xFFFFB300), size: 28),
            SizedBox(width: 8),
            Text('방문 인증 성공!', style: TextStyle(fontWeight: FontWeight.bold)),
          ],
        ),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('현재 위치가 매장과 가까운 것으로 인증되었습니다.'),
            const SizedBox(height: 12),
            Text('📍 오차 거리: ${data['distanceMeter']}m'),
            Text('✨ 획득 경험치: +${data['expGained']} EXP'),
            Text('🔥 누적 방문 횟수: ${data['visitCount']}회'),
          ],
        ),
        actions: [
          ElevatedButton(
            onPressed: () {
              Navigator.pop(context);
              // Refresh user profile/status so XP updates in MyPage
              Provider.of<AuthProvider>(context, listen: false).refreshUserProfile();
            },
            style: ElevatedButton.styleFrom(
              backgroundColor: const Color(0xFFFFB300),
              foregroundColor: Colors.white,
            ),
            child: const Text('확인'),
          ),
        ],
      ),
    );
  }

  void _showLoginPrompt() {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        title: const Text('로그인 필요', style: TextStyle(fontWeight: FontWeight.bold)),
        content: const Text('이 기능은 로그인 후 사용하실 수 있습니다.\n로그인 화면으로 이동할까요?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('취소', style: TextStyle(color: Colors.black54)),
          ),
          ElevatedButton(
            onPressed: () {
              Navigator.pop(context);
              Navigator.of(context).push(
                MaterialPageRoute(builder: (context) => const LoginScreen()),
              ).then((_) {
                _loadAllData();
              });
            },
            style: ElevatedButton.styleFrom(
              backgroundColor: const Color(0xFFFFB300),
              foregroundColor: Colors.white,
            ),
            child: const Text('로그인하기'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    if (_isLoadingDetails) {
      return const Scaffold(
        body: _HomeDetailActivityIndicator(),
      );
    }

    if (_placeDetails == null) {
      return Scaffold(
        appBar: AppBar(title: const Text('오류')),
        body: const Center(child: Text('장소 정보를 불러올 수 없습니다.')),
      );
    }

    final String catKo = _categoryMap[_placeDetails!['categoryCode']] ?? _placeDetails!['categoryCode'];
    final bool hasImages = _placeDetails!['imageUrls'] != null && (_placeDetails!['imageUrls'] as List).isNotEmpty;

    return Scaffold(
      backgroundColor: const Color(0xFFFAFAFA),
      body: PopScope(
        canPop: false,
        onPopInvokedWithResult: (didPop, result) {
          if (didPop) return;
          Navigator.of(context).pop(true); // Return true so home map reloads
        },
        child: Stack(
          children: [
            CustomScrollView(
              slivers: [
                // 1. Sliver App Bar with Image
                SliverAppBar(
                  expandedHeight: 220,
                  pinned: true,
                  backgroundColor: Colors.white,
                  foregroundColor: Colors.black87,
                  elevation: 0.5,
                  leading: IconButton(
                    icon: const Icon(Icons.arrow_back),
                    onPressed: () => Navigator.of(context).pop(true),
                  ),
                  flexibleSpace: FlexibleSpaceBar(
                    background: hasImages
                        ? Image.network(
                            (_placeDetails!['imageUrls'] as List).first,
                            fit: BoxFit.cover,
                          )
                        : Container(
                            decoration: const BoxDecoration(
                              gradient: LinearGradient(
                                colors: [Color(0xFFFFE082), Color(0xFFFFB300)],
                                begin: Alignment.topLeft,
                                end: Alignment.bottomRight,
                              ),
                            ),
                            child: const Center(
                              child: Icon(Icons.restaurant, size: 70, color: Colors.white),
                            ),
                          ),
                  ),
                ),

                // 2. Info Content
                SliverToBoxAdapter(
                  child: Padding(
                    padding: const EdgeInsets.all(16.0),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        // Title & Category Row
                        Row(
                          children: [
                            Flexible(
                              child: Text(
                                _placeDetails!['name'],
                                style: const TextStyle(fontSize: 22, fontWeight: FontWeight.bold),
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                              ),
                            ),
                            const SizedBox(width: 10),
                            Container(
                              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                              decoration: BoxDecoration(
                                color: const Color(0xFFFFB300).withValues(alpha: 0.15),
                                borderRadius: BorderRadius.circular(6),
                              ),
                              child: Text(
                                catKo,
                                style: const TextStyle(
                                  fontSize: 11,
                                  color: Color(0xFFFF8F00),
                                  fontWeight: FontWeight.bold,
                                ),
                              ),
                            ),
                          ],
                        ),
                        if (_placeDetails!['aiTags'] != null && (_placeDetails!['aiTags'] as List).isNotEmpty) ...[
                          const SizedBox(height: 8),
                          Wrap(
                            spacing: 6.0,
                            runSpacing: 6.0,
                            children: (_placeDetails!['aiTags'] as List).map<Widget>((tag) {
                              return Container(
                                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                                decoration: BoxDecoration(
                                  color: const Color(0xFFF3E5F5),
                                  borderRadius: BorderRadius.circular(20),
                                  border: Border.all(color: const Color(0xFFE1BEE7), width: 0.8),
                                ),
                                child: Row(
                                  mainAxisSize: MainAxisSize.min,
                                  children: [
                                    const Icon(Icons.auto_awesome, size: 12, color: Color(0xFF8E24AA)),
                                    const SizedBox(width: 4),
                                    Text(
                                      '#$tag',
                                      style: const TextStyle(
                                        fontSize: 11,
                                        color: Color(0xFF8E24AA),
                                        fontWeight: FontWeight.bold,
                                      ),
                                    ),
                                  ],
                                ),
                              );
                            }).toList(),
                          ),
                        ],
                        const SizedBox(height: 8),

                        // Stats Summary Row
                        Row(
                          children: [
                            const Icon(Icons.star, color: Color(0xFFFFB300), size: 18),
                            const SizedBox(width: 4),
                            Text(
                              '레벨 ${_placeDetails!['starLevel'] ?? 0} 꽃',
                              style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13),
                            ),
                            const SizedBox(width: 14),
                            const Icon(Icons.thumb_up, color: Colors.black38, size: 16),
                            const SizedBox(width: 4),
                            Text(
                              '추천 ${_placeDetails!['recommendCount'] ?? 0}명',
                              style: const TextStyle(fontSize: 13, color: Colors.black54),
                            ),
                            const SizedBox(width: 14),
                            const Icon(Icons.check_circle_outline, color: Colors.black38, size: 16),
                            const SizedBox(width: 4),
                            Text(
                              '방문 ${_placeDetails!['visitCount'] ?? 0}회',
                              style: const TextStyle(fontSize: 13, color: Colors.black54),
                            ),
                          ],
                        ),
                        const Divider(height: 32),

                        // Highlight box for recommended menu & reason
                        Container(
                          width: double.infinity,
                          padding: const EdgeInsets.all(16),
                          decoration: BoxDecoration(
                            color: Colors.white,
                            borderRadius: BorderRadius.circular(16),
                            border: Border.all(color: Colors.black12),
                          ),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              const Row(
                                children: [
                                  Icon(Icons.restaurant_menu, color: Color(0xFFFFB300), size: 20),
                                  SizedBox(width: 8),
                                  Text(
                                    '추천 메뉴',
                                    style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14),
                                  ),
                                ],
                              ),
                              const SizedBox(height: 6),
                              Text(
                                _placeDetails!['recommendedMenu'],
                                style: const TextStyle(fontSize: 15, fontWeight: FontWeight.bold),
                              ),
                              const SizedBox(height: 12),
                              const Row(
                                children: [
                                  Icon(Icons.chat_bubble_outline_rounded, color: Color(0xFFFFB300), size: 20),
                                  SizedBox(width: 8),
                                  Text(
                                    '등록인 한 줄 추천',
                                    style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14),
                                  ),
                                ],
                              ),
                              const SizedBox(height: 6),
                              Text(
                                _placeDetails!['shortRecommendation'],
                                style: const TextStyle(fontSize: 13, color: Colors.black87, height: 1.4),
                              ),
                            ],
                          ),
                        ),
                        const SizedBox(height: 20),

                        // Map / Address Section
                        _buildSectionHeader('위치 정보'),
                        const SizedBox(height: 8),
                        Container(
                          width: double.infinity,
                          padding: const EdgeInsets.all(16),
                          decoration: BoxDecoration(
                            color: Colors.white,
                            borderRadius: BorderRadius.circular(16),
                            border: Border.all(color: Colors.black12),
                          ),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Row(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  const Icon(Icons.location_on, color: Colors.black54, size: 20),
                                  const SizedBox(width: 8),
                                  Expanded(
                                    child: Column(
                                      crossAxisAlignment: CrossAxisAlignment.start,
                                      children: [
                                        Text(
                                          _placeDetails!['addressRoad'],
                                          style: const TextStyle(fontSize: 13, fontWeight: FontWeight.bold),
                                        ),
                                        const SizedBox(height: 2),
                                        Text(
                                          '[지번] ${_placeDetails!['addressJibun']}',
                                          style: const TextStyle(fontSize: 11, color: Colors.black45),
                                        ),
                                      ],
                                    ),
                                  ),
                                ],
                              ),
                            ],
                          ),
                        ),
                        const SizedBox(height: 24),

                        // Comments Section
                        Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            _buildSectionHeader('실시간 피드백 및 댓글 (${_comments.length})'),
                            IconButton(
                              icon: const Icon(Icons.refresh, size: 20, color: Colors.black54),
                              onPressed: _refreshComments,
                            ),
                          ],
                        ),
                        const SizedBox(height: 8),
                        _buildCommentInput(),
                        const SizedBox(height: 12),
                        _buildCommentsList(),
                        const SizedBox(height: 100), // padding for bottom action buttons
                      ],
                    ),
                  ),
                ),
              ],
            ),

            // 3. Floating Bottom Actions Row
            Positioned(
              left: 16,
              right: 16,
              bottom: 24,
              child: _buildBottomActionsRow(),
            ),

            if (_isActionInProgress)
              Container(
                color: Colors.black26,
                child: const Center(child: CircularProgressIndicator(valueColor: AlwaysStoppedAnimation(Color(0xFFFFB300)))),
              ),
          ],
        ),
      ),
    );
  }

  Widget _buildSectionHeader(String title) {
    return Text(
      title,
      style: const TextStyle(
        fontSize: 15,
        fontWeight: FontWeight.bold,
        color: Colors.black87,
      ),
    );
  }

  Widget _buildCommentInput() {
    return Container(
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: Colors.black12),
      ),
      child: Row(
        children: [
          Expanded(
            child: TextField(
              controller: _commentController,
              decoration: const InputDecoration(
                hintText: '맛집에 대한 생생한 피드백을 남겨주세요...',
                hintStyle: TextStyle(fontSize: 12, color: Colors.black38),
                contentPadding: EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                border: InputBorder.none,
              ),
            ),
          ),
          IconButton(
            icon: const Icon(Icons.send, color: Color(0xFFFFB300)),
            onPressed: _submitComment,
          ),
        ],
      ),
    );
  }

  Widget _buildCommentsList() {
    if (_isLoadingComments) {
      return const Center(
        child: Padding(
          padding: EdgeInsets.symmetric(vertical: 20.0),
          child: CircularProgressIndicator(strokeWidth: 2, valueColor: AlwaysStoppedAnimation(Color(0xFFFFB300))),
        ),
      );
    }

    if (_comments.isEmpty) {
      return Container(
        width: double.infinity,
        padding: const EdgeInsets.symmetric(vertical: 40),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(16),
          border: Border.all(color: Colors.black12),
        ),
        child: const Column(
          children: [
            Icon(Icons.chat_bubble_outline_rounded, size: 36, color: Colors.black26),
            SizedBox(height: 8),
            Text('첫 피드백(댓글)을 남겨보세요!', style: TextStyle(color: Colors.black38, fontSize: 12)),
          ],
        ),
      );
    }

    final authProvider = Provider.of<AuthProvider>(context, listen: false);
    final currentUserId = authProvider.userProfile?.id;

    return ListView.builder(
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      itemCount: _comments.length,
      itemBuilder: (context, index) {
        final comment = _comments[index];
        final isMine = currentUserId != null && comment['userId'] == currentUserId;

        // Extract creation time
        String timeStr = '';
        if (comment['createdAt'] != null) {
          try {
            final parsed = DateTime.parse(comment['createdAt']);
            timeStr = '${parsed.month}/${parsed.day} ${parsed.hour.toString().padLeft(2, '0')}:${parsed.minute.toString().padLeft(2, '0')}';
          } catch (_) {
            timeStr = comment['createdAt'].toString().substring(0, 10);
          }
        }

        return Card(
          elevation: 0,
          color: Colors.white,
          margin: const EdgeInsets.only(bottom: 8),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(12),
            side: const BorderSide(color: Colors.black12),
          ),
          child: ListTile(
            contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
            leading: CircleAvatar(
              backgroundColor: const Color(0xFFFFB300).withValues(alpha: 0.15),
              radius: 18,
              child: Text(
                comment['nickname'] != null && comment['nickname'].toString().isNotEmpty
                    ? comment['nickname'].toString().substring(0, 1)
                    : 'U',
                style: const TextStyle(fontWeight: FontWeight.bold, color: Color(0xFFFF8F00), fontSize: 13),
              ),
            ),
            title: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  comment['nickname'] ?? '익명',
                  style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13),
                ),
                Text(
                  timeStr,
                  style: const TextStyle(fontSize: 10, color: Colors.black38),
                ),
              ],
            ),
            subtitle: Padding(
              padding: const EdgeInsets.only(top: 4.0),
              child: Text(
                comment['content'] ?? '',
                style: const TextStyle(color: Colors.black87, fontSize: 12, height: 1.3),
              ),
            ),
            trailing: isMine
                ? IconButton(
                    icon: const Icon(Icons.delete_outline, size: 18, color: Colors.redAccent),
                    onPressed: () => _deleteComment(comment['commentId']),
                  )
                : null,
          ),
        );
      },
    );
  }

  Widget _buildBottomActionsRow() {
    return Row(
      children: [
        // 1. Recommendation Button
        Expanded(
          flex: 1,
          child: SizedBox(
            height: 52,
            child: ElevatedButton.icon(
              onPressed: _handleRecommendation,
              icon: Icon(
                _isRecommended ? Icons.thumb_up : Icons.thumb_up_outlined,
                color: _isRecommended ? Colors.white : const Color(0xFFFF8F00),
                size: 20,
              ),
              label: Text(
                _isRecommended ? '추천 중' : '추천하기',
                style: TextStyle(
                  fontSize: 14,
                  fontWeight: FontWeight.bold,
                  color: _isRecommended ? Colors.white : const Color(0xFFFF8F00),
                ),
              ),
              style: ElevatedButton.styleFrom(
                backgroundColor: _isRecommended ? const Color(0xFFFF8F00) : Colors.white,
                elevation: 0,
                side: const BorderSide(color: Color(0xFFFF8F00), width: 1.5),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(14),
                ),
              ),
            ),
          ),
        ),
        const SizedBox(width: 12),

        // 2. Visit verification Button
        Expanded(
          flex: 2,
          child: SizedBox(
            height: 52,
            child: ElevatedButton.icon(
              onPressed: _handleVisitVerification,
              icon: const Icon(Icons.gps_fixed, color: Colors.white, size: 20),
              label: const Text(
                '방문 인증하기 (GPS)',
                style: TextStyle(fontSize: 14, fontWeight: FontWeight.bold, color: Colors.white),
              ),
              style: ElevatedButton.styleFrom(
                backgroundColor: const Color(0xFFFFB300),
                elevation: 1,
                shadowColor: Colors.black26,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(14),
                ),
              ),
            ),
          ),
        ),
      ],
    );
  }
}
