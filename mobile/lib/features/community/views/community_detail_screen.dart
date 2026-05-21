import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../../core/api/api_client.dart';
import '../../../models/community_post.dart';
import '../services/community_service.dart';
import 'community_edit_screen.dart';

class CommunityDetailScreen extends StatefulWidget {
  final int postId;

  const CommunityDetailScreen({super.key, required this.postId});

  @override
  State<CommunityDetailScreen> createState() => _CommunityDetailScreenState();
}

class _CommunityDetailScreenState extends State<CommunityDetailScreen> {
  bool _isLoading = true;
  CommunityPost? _post;
  late CommunityService _communityService;
  bool _anyChangesMade = false;

  @override
  void initState() {
    super.initState();
    _communityService = CommunityService(Provider.of<ApiClient>(context, listen: false));
    _loadPost();
  }

  Future<void> _loadPost() async {
    setState(() {
      _isLoading = true;
    });

    try {
      final fetched = await _communityService.getPost(widget.postId);
      if (mounted) {
        setState(() {
          _post = fetched;
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _isLoading = false;
        });
        _showErrorSnackBar('상세 정보를 불러오는 데 실패했습니다.');
      }
    }
  }

  void _showErrorSnackBar(String message) {
    ScaffoldMessenger.of(context).clearSnackBars();
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Row(
          children: [
            const Icon(Icons.error_outline_rounded, color: Colors.white, size: 20),
            const SizedBox(width: 10),
            Expanded(child: Text(message, style: const TextStyle(fontWeight: FontWeight.w600, color: Colors.white, fontSize: 13))),
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

  void _showSuccessSnackBar(String message) {
    ScaffoldMessenger.of(context).clearSnackBars();
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Row(
          children: [
            const Icon(Icons.check_circle_rounded, color: Colors.white, size: 20),
            const SizedBox(width: 10),
            Expanded(child: Text(message, style: const TextStyle(fontWeight: FontWeight.w600, color: Colors.white, fontSize: 13))),
          ],
        ),
        backgroundColor: Colors.green,
        behavior: SnackBarBehavior.floating,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
        margin: const EdgeInsets.all(16),
        duration: const Duration(seconds: 2),
      ),
    );
  }

  Future<void> _deletePost() async {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        title: const Text('이야기 삭제', style: TextStyle(fontWeight: FontWeight.bold)),
        content: const Text('작성하신 이야기를 정말 삭제하시겠습니까?\n삭제 후에는 복구할 수 없습니다.'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('취소', style: TextStyle(color: Colors.black54)),
          ),
          ElevatedButton(
            onPressed: () async {
              final navigator = Navigator.of(context);
              navigator.pop(); // Close dialog
              setState(() {
                _isLoading = true;
              });

              final success = await _communityService.deletePost(widget.postId);
              
              if (success) {
                if (mounted) {
                  _showSuccessSnackBar('이야기가 성공적으로 삭제되었습니다.');
                  navigator.pop(true); // Return reload=true to refresh list
                }
              } else {
                if (mounted) {
                  setState(() {
                    _isLoading = false;
                  });
                  _showErrorSnackBar('이야기 삭제에 실패했습니다.');
                }
              }
            },
            style: ElevatedButton.styleFrom(
              backgroundColor: Colors.redAccent,
              foregroundColor: Colors.white,
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
            ),
            child: const Text('삭제하기'),
          ),
        ],
      ),
    );
  }

  void _navigateToEdit() {
    if (_post == null) return;
    Navigator.of(context).push(
      MaterialPageRoute(
        builder: (context) => CommunityEditScreen(
          postId: _post!.postId,
          initialTitle: _post!.title,
          initialContent: _post!.content,
        ),
      ),
    ).then((reload) {
      if (reload == true) {
        _anyChangesMade = true;
        _loadPost();
      }
    });
  }

  String _formatDateTime(String dateTimeStr) {
    try {
      final dateTime = DateTime.parse(dateTimeStr);
      return '${dateTime.year}.${dateTime.month.toString().padLeft(2, '0')}.${dateTime.day.toString().padLeft(2, '0')} ${dateTime.hour.toString().padLeft(2, '0')}:${dateTime.minute.toString().padLeft(2, '0')}';
    } catch (e) {
      return dateTimeStr;
    }
  }

  @override
  Widget build(BuildContext context) {
    return PopScope(
      canPop: false,
      onPopInvokedWithResult: (didPop, result) {
        if (didPop) return;
        Navigator.pop(context, _anyChangesMade);
      },
      child: Scaffold(
        backgroundColor: const Color(0xFFFDFBF7),
        appBar: AppBar(
          backgroundColor: Colors.white,
          elevation: 0.5,
          iconTheme: const IconThemeData(color: Color(0xFF263238)),
          actions: [
            if (_post != null && _post!.mine) ...[
              IconButton(
                icon: const Icon(Icons.edit_outlined, color: Color(0xFF263238)),
                tooltip: '수정',
                onPressed: _isLoading ? null : _navigateToEdit,
              ),
              IconButton(
                icon: const Icon(Icons.delete_outline_rounded, color: Colors.redAccent),
                tooltip: '삭제',
                onPressed: _isLoading ? null : _deletePost,
              ),
            ]
          ],
        ),
        body: _isLoading
            ? const Center(
                child: CircularProgressIndicator(
                  valueColor: AlwaysStoppedAnimation<Color>(Color(0xFFFFB300)),
                ),
              )
            : _post == null
                ? _buildErrorView()
                : _buildDetailContent(),
      ),
    );
  }

  Widget _buildErrorView() {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32.0),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(Icons.error_outline_rounded, size: 64, color: Colors.redAccent),
            const SizedBox(height: 16),
            const Text(
              '게시글을 찾을 수 없습니다',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: Color(0xFF263238)),
            ),
            const SizedBox(height: 8),
            const Text(
              '삭제되었거나 접근할 수 없는 글입니다.',
              style: TextStyle(color: Color(0xFF78909C)),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 24),
            ElevatedButton(
              onPressed: () => Navigator.pop(context, _anyChangesMade),
              style: ElevatedButton.styleFrom(
                backgroundColor: const Color(0xFFFFB300),
                foregroundColor: Colors.white,
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
              ),
              child: const Text('목록으로 돌아가기'),
            )
          ],
        ),
      ),
    );
  }

  Widget _buildDetailContent() {
    final post = _post!;

    return SingleChildScrollView(
      padding: const EdgeInsets.all(20.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Author & Date Profile Card
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(16),
              border: Border.all(color: const Color(0xFFF3EFE9)),
            ),
            child: Row(
              children: [
                Container(
                  width: 44,
                  height: 44,
                  decoration: const BoxDecoration(
                    color: Color(0xFFFFF8E1),
                    shape: BoxShape.circle,
                  ),
                  child: const Center(
                    child: Icon(Icons.account_circle_rounded, color: Color(0xFFFFB300), size: 30),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          Text(
                            post.authorNickname,
                            style: const TextStyle(
                              fontSize: 14,
                              fontWeight: FontWeight.bold,
                              color: Color(0xFF263238),
                            ),
                          ),
                          const SizedBox(width: 6),
                          if (post.mine)
                            Container(
                              padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                              decoration: BoxDecoration(
                                color: const Color(0xFFFFF8E1),
                                borderRadius: BorderRadius.circular(4),
                              ),
                              child: const Text(
                                '내 글',
                                style: TextStyle(
                                  fontSize: 9,
                                  fontWeight: FontWeight.w800,
                                  color: Color(0xFFFF8F00),
                                ),
                              ),
                            ),
                        ],
                      ),
                      const SizedBox(height: 4),
                      Text(
                        _formatDateTime(post.createdAt),
                        style: const TextStyle(
                          fontSize: 11,
                          color: Color(0xFF90A4AE),
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 24),
          
          // Post Title
          Text(
            post.title,
            style: const TextStyle(
              fontSize: 22,
              fontWeight: FontWeight.bold,
              color: Color(0xFF263238),
              height: 1.3,
            ),
          ),
          const SizedBox(height: 16),
          const Divider(color: Color(0xFFF0EAE1)),
          const SizedBox(height: 16),

          // Post Content
          Text(
            post.content,
            style: const TextStyle(
              fontSize: 15,
              color: Color(0xFF37474F),
              height: 1.6,
            ),
          ),
          const SizedBox(height: 48),
        ],
      ),
    );
  }
}
