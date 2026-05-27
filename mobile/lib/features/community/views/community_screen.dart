import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../../core/theme/app_theme.dart';
import '../../../core/widgets/app_empty_state.dart';
import '../../../core/widgets/app_login_required_state.dart';
import '../../../models/community_post.dart';
import '../../auth/providers/auth_provider.dart';
import '../../auth/views/login_screen.dart';
import '../services/community_service.dart';
import 'community_create_screen.dart';
import 'community_detail_screen.dart';

class CommunityScreen extends StatefulWidget {
  final Function(int)? onTabSwitch;

  const CommunityScreen({super.key, this.onTabSwitch});

  @override
  State<CommunityScreen> createState() => _CommunityScreenState();
}

class _CommunityScreenState extends State<CommunityScreen> {
  bool _isLoading = false;
  bool _isAllFilter = true; // true: 전체 글, false: 내가 쓴 글
  List<CommunityPost> _posts = [];

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final authProvider = Provider.of<AuthProvider>(context, listen: false);
      if (authProvider.isAuthenticated) {
        _loadPosts();
      }
    });
  }

  Future<void> _loadPosts() async {
    setState(() {
      _isLoading = true;
    });

    try {
      final communityService =
          Provider.of<CommunityService>(context, listen: false);
      List<CommunityPost> fetchedPosts;
      if (_isAllFilter) {
        fetchedPosts = await communityService.getPosts();
      } else {
        fetchedPosts = await communityService.getMyPosts();
      }

      if (mounted) {
        setState(() {
          _posts = fetchedPosts;
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _isLoading = false;
        });
        _showErrorSnackBar('데이터를 불러오는 중 오류가 발생했습니다.');
      }
    }
  }

  void _showErrorSnackBar(String message) {
    ScaffoldMessenger.of(context).clearSnackBars();
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Row(
          children: [
            const Icon(Icons.error_outline_rounded,
                color: Colors.white, size: 20),
            const SizedBox(width: 10),
            Expanded(
                child: Text(message,
                    style: const TextStyle(
                        fontWeight: FontWeight.w600,
                        color: Colors.white,
                        fontSize: 13))),
          ],
        ),
        backgroundColor: Colors.redAccent,
        behavior: SnackBarBehavior.floating,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
        margin: const EdgeInsets.all(16),
        duration: const Duration(seconds: 2),
      ),
    );
  }

  String _formatDateTime(String dateTimeStr) {
    try {
      final dateTime = DateTime.parse(dateTimeStr);
      final now = DateTime.now();
      final difference = now.difference(dateTime);

      if (difference.inMinutes < 1) {
        return '방금 전';
      } else if (difference.inMinutes < 60) {
        return '${difference.inMinutes}분 전';
      } else if (difference.inHours < 24) {
        return '${difference.inHours}시간 전';
      } else if (difference.inDays < 7) {
        return '${difference.inDays}일 전';
      } else {
        return '${dateTime.year}.${dateTime.month.toString().padLeft(2, '0')}.${dateTime.day.toString().padLeft(2, '0')}';
      }
    } catch (e) {
      return dateTimeStr;
    }
  }

  void _handleWriteAction() {
    final authProvider = Provider.of<AuthProvider>(context, listen: false);

    // 휴대폰 인증 여부 확인
    final isVerified = authProvider.userProfile?.phoneVerified ?? false;

    if (!isVerified) {
      showDialog(
        context: context,
        builder: (context) => AlertDialog(
          shape:
              RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
          title: const Text('인증 필요',
              style: TextStyle(fontWeight: FontWeight.bold)),
          content: const Text(
              '게시글을 작성하려면 휴대폰 본인 인증이 필요합니다. 인증 화면(마이페이지)으로 이동하시겠습니까?'),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(context),
              child: const Text('취소', style: TextStyle(color: Colors.black54)),
            ),
            ElevatedButton(
              onPressed: () {
                Navigator.pop(context);
                if (widget.onTabSwitch != null) {
                  widget.onTabSwitch!(4); // 마이페이지 인덱스 4
                }
              },
              style: ElevatedButton.styleFrom(
                backgroundColor: const Color(0xFFFFB300),
                foregroundColor: Colors.white,
                shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(8)),
              ),
              child: const Text('이동하기'),
            ),
          ],
        ),
      );
      return;
    }

    // 작성 화면으로 이동
    Navigator.of(context)
        .push(
      MaterialPageRoute(builder: (context) => const CommunityCreateScreen()),
    )
        .then((reload) {
      if (reload == true) {
        _loadPosts();
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    final authProvider = Provider.of<AuthProvider>(context);

    if (!authProvider.isAuthenticated) {
      return _buildGuestView();
    }

    return Scaffold(
      backgroundColor: const Color(0xFFFDFBF7), // Creamy aesthetic background
      appBar: AppBar(
        title: const Text(
          '동네 광장',
          style: TextStyle(
            fontWeight: FontWeight.w900,
            fontSize: 22,
            color: Color(0xFF263238),
            letterSpacing: -0.5,
          ),
        ),
        centerTitle: false,
        backgroundColor: Colors.white,
        elevation: 0,
        bottom: PreferredSize(
          preferredSize: const Size.fromHeight(50.0),
          child: Container(
            color: Colors.white,
            padding:
                const EdgeInsets.symmetric(horizontal: 16.0, vertical: 8.0),
            child: Row(
              children: [
                _buildFilterChip('전체 수다', _isAllFilter),
                const SizedBox(width: 8),
                _buildFilterChip('내가 쓴 글', !_isAllFilter),
              ],
            ),
          ),
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh_rounded, color: Color(0xFF263238)),
            tooltip: '새로고침',
            onPressed: _isLoading ? null : _loadPosts,
          ),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: _loadPosts,
        color: const Color(0xFFFFB300),
        backgroundColor: Colors.white,
        child: _isLoading
            ? _buildSkeletonLoader()
            : _posts.isEmpty
                ? _buildEmptyView()
                : _buildPostsListView(),
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: _handleWriteAction,
        backgroundColor: Colors.transparent,
        elevation: 0,
        child: Container(
          width: 56,
          height: 56,
          decoration: BoxDecoration(
            shape: BoxShape.circle,
            gradient: const LinearGradient(
              colors: [Color(0xFFFFC107), Color(0xFFFF8F00)],
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
            ),
            boxShadow: [
              BoxShadow(
                color: const Color(0xFFFF8F00).withValues(alpha: 0.3),
                blurRadius: 12,
                offset: const Offset(0, 6),
              ),
            ],
          ),
          child: const Icon(Icons.edit_rounded, color: Colors.white),
        ),
      ),
    );
  }

  Widget _buildFilterChip(String label, bool isSelected) {
    return GestureDetector(
      onTap: () {
        if (isSelected) return;
        setState(() {
          _isAllFilter = label == '전체 수다';
        });
        _loadPosts();
      },
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(20),
          color: isSelected ? const Color(0xFFFFB300) : const Color(0xFFF5F0E6),
          boxShadow: isSelected
              ? [
                  BoxShadow(
                    color: const Color(0xFFFFB300).withValues(alpha: 0.2),
                    blurRadius: 6,
                    offset: const Offset(0, 3),
                  ),
                ]
              : null,
        ),
        child: Text(
          label,
          style: TextStyle(
            color: isSelected ? Colors.white : const Color(0xFF78909C),
            fontWeight: FontWeight.bold,
            fontSize: 13,
          ),
        ),
      ),
    );
  }

  Widget _buildGuestView() {
    return Scaffold(
      body: AppLoginRequiredState(
        icon: Icons.forum_rounded,
        title: '로그인이 필요합니다',
        description: '동네 주민들과 실시간으로 소통하고\n다양한 맛집 소식과 광장 이야기를 공유해보세요.',
        actionLabel: '로그인하러 가기',
        eyebrow: '우리 동네 광장',
        previewItems: const [
          AppLoginPreviewItem(
            icon: Icons.chat_bubble_rounded,
            title: '동네 맛집 이야기',
            description: '새로 찾은 장소와 방문 팁을 이웃과 나눕니다.',
          ),
          AppLoginPreviewItem(
            icon: Icons.verified_user_rounded,
            title: '인증 후 글쓰기',
            description: '전화 인증 후 안전하게 글을 작성합니다.',
          ),
        ],
        onActionPressed: () {
          Navigator.of(context)
              .push(
            MaterialPageRoute(builder: (context) => const LoginScreen()),
          )
              .then((_) {
            if (!mounted) return;
            if (Provider.of<AuthProvider>(context, listen: false)
                .isAuthenticated) {
              _loadPosts();
            }
          });
        },
      ),
    );
  }

  Widget _buildSkeletonLoader() {
    return ListView.builder(
      padding: const EdgeInsets.all(16),
      itemCount: 4,
      itemBuilder: (context, index) {
        return Container(
          margin: const EdgeInsets.only(bottom: 16),
          padding: const EdgeInsets.all(16),
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(16),
            boxShadow: [
              BoxShadow(
                color: Colors.black.withValues(alpha: 0.01),
                blurRadius: 10,
                offset: const Offset(0, 4),
              ),
            ],
          ),
          child: const Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              ShimmerLoading(width: 150, height: 20),
              SizedBox(height: 12),
              ShimmerLoading(width: double.infinity, height: 14),
              SizedBox(height: 6),
              ShimmerLoading(width: 200, height: 14),
              SizedBox(height: 16),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  ShimmerLoading(width: 80, height: 14),
                  ShimmerLoading(width: 60, height: 14),
                ],
              ),
            ],
          ),
        );
      },
    );
  }

  Widget _buildEmptyView() {
    return SingleChildScrollView(
      physics: const AlwaysScrollableScrollPhysics(),
      child: Container(
        height: MediaQuery.of(context).size.height - 220,
        alignment: Alignment.center,
        padding: const EdgeInsets.symmetric(horizontal: 32),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            AppEmptyState(
              icon: Icons.forum_outlined,
              title: _isAllFilter ? '등록된 게시글이 없습니다' : '작성한 게시글이 없습니다',
              description: _isAllFilter
                  ? '동네 광장의 첫 번째 글을 남기고\n다양한 이야기를 들려주세요.'
                  : '동네 광장에 새로운 이야기를 쓰고\n이웃들과 첫 소통을 시작해보세요.',
            ),
            const SizedBox(height: AppSpacing.sm),
            OutlinedButton(
              onPressed: _handleWriteAction,
              style: OutlinedButton.styleFrom(
                side: const BorderSide(color: Color(0xFFFFB300), width: 1.5),
                shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(24)),
                padding:
                    const EdgeInsets.symmetric(horizontal: 28, vertical: 14),
                backgroundColor: Colors.white,
              ),
              child: const Text(
                '첫 이야기 작성하기',
                style: TextStyle(
                  color: Color(0xFFFF8F00),
                  fontWeight: FontWeight.bold,
                  fontSize: 14,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildPostsListView() {
    return ListView.builder(
      padding: const EdgeInsets.all(16),
      itemCount: _posts.length,
      itemBuilder: (context, index) {
        final post = _posts[index];

        return Container(
          margin: const EdgeInsets.only(bottom: 16),
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(18),
            border: Border.all(color: const Color(0xFFF3EFE9), width: 1.2),
            boxShadow: [
              BoxShadow(
                color: const Color(0xFF8D6E63).withValues(alpha: 0.03),
                blurRadius: 10,
                offset: const Offset(0, 4),
              ),
            ],
          ),
          child: Material(
            color: Colors.transparent,
            child: InkWell(
              onTap: () {
                Navigator.of(context)
                    .push(
                  MaterialPageRoute(
                    builder: (context) =>
                        CommunityDetailScreen(postId: post.postId),
                  ),
                )
                    .then((reload) {
                  if (reload == true) {
                    _loadPosts();
                  }
                });
              },
              borderRadius: BorderRadius.circular(18),
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Expanded(
                          child: Text(
                            post.title,
                            style: const TextStyle(
                              fontSize: 17,
                              fontWeight: FontWeight.bold,
                              color: Color(0xFF263238),
                              letterSpacing: -0.3,
                            ),
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                          ),
                        ),
                        if (post.mine) ...[
                          const SizedBox(width: 8),
                          Container(
                            padding: const EdgeInsets.symmetric(
                                horizontal: 8, vertical: 3),
                            decoration: BoxDecoration(
                              gradient: const LinearGradient(
                                colors: [Color(0xFFFFB300), Color(0xFFFF8F00)],
                                begin: Alignment.topLeft,
                                end: Alignment.bottomRight,
                              ),
                              borderRadius: BorderRadius.circular(8),
                            ),
                            child: const Text(
                              '내 글',
                              style: TextStyle(
                                fontSize: 10,
                                fontWeight: FontWeight.w800,
                                color: Colors.white,
                              ),
                            ),
                          ),
                        ],
                      ],
                    ),
                    const SizedBox(height: 8),
                    Text(
                      post.content,
                      style: const TextStyle(
                        fontSize: 14,
                        color: Color(0xFF546E7A),
                        height: 1.45,
                      ),
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                    ),
                    const SizedBox(height: 16),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Row(
                          children: [
                            const Icon(Icons.account_circle_rounded,
                                color: Color(0xFFFFB300), size: 18),
                            const SizedBox(width: 4),
                            Text(
                              post.authorNickname,
                              style: const TextStyle(
                                fontSize: 12,
                                color: Color(0xFF78909C),
                                fontWeight: FontWeight.w700,
                              ),
                            ),
                          ],
                        ),
                        Text(
                          _formatDateTime(post.createdAt),
                          style: const TextStyle(
                            fontSize: 12,
                            color: Color(0xFFB0BEC5),
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ),
          ),
        );
      },
    );
  }
}

// Custom Pure Flutter Shimmer/Pulse Loading Animation Widget
class ShimmerLoading extends StatefulWidget {
  final double width;
  final double height;
  final BorderRadius? borderRadius;

  const ShimmerLoading({
    super.key,
    required this.width,
    required this.height,
    this.borderRadius,
  });

  @override
  State<ShimmerLoading> createState() => _ShimmerLoadingState();
}

class _ShimmerLoadingState extends State<ShimmerLoading>
    with SingleTickerProviderStateMixin {
  late AnimationController _controller;
  late Animation<double> _animation;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1400),
    )..repeat(reverse: true);
    _animation = Tween<double>(begin: 0.35, end: 0.85).animate(
      CurvedAnimation(parent: _controller, curve: Curves.easeInOut),
    );
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: _animation,
      builder: (context, child) {
        return Opacity(
          opacity: _animation.value,
          child: Container(
            width: widget.width,
            height: widget.height,
            decoration: BoxDecoration(
              color: const Color(0xFFECEFF1),
              borderRadius: widget.borderRadius ?? BorderRadius.circular(8),
            ),
          ),
        );
      },
    );
  }
}
