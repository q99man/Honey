import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../../models/place.dart';
import '../../../models/wishlist.dart';
import '../../auth/providers/auth_provider.dart';
import '../../auth/views/login_screen.dart';
import '../../place/views/place_detail_screen.dart';
import '../services/wishlist_service.dart';

class WishlistScreen extends StatefulWidget {
  final VoidCallback? onExploreTap;

  const WishlistScreen({super.key, this.onExploreTap});

  @override
  State<WishlistScreen> createState() => _WishlistScreenState();
}

class _WishlistScreenState extends State<WishlistScreen> {
  bool _isLoading = false;
  List<SavedPlace> _savedPlaces = [];
  List<Place> _detailedPlaces = [];

  // Category code to Korean display mapping
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
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final authProvider = Provider.of<AuthProvider>(context, listen: false);
      if (authProvider.isAuthenticated) {
        _loadWishlist();
      }
    });
  }

  Future<void> _loadWishlist() async {
    setState(() {
      _isLoading = true;
    });

    try {
      final wishlistService = Provider.of<WishlistService>(context, listen: false);
      final saved = await wishlistService.getSavedPlaces();
      final details = await wishlistService.getSavedPlaceDetails(saved);

      if (mounted) {
        setState(() {
          _savedPlaces = saved;
          _detailedPlaces = details;
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

  Future<void> _unsavePlace(Place place) async {
    final index = _detailedPlaces.indexWhere((p) => p.id == place.id);
    if (index == -1) return;

    final removedPlace = _detailedPlaces[index];
    final correspondingSavedPlace = _savedPlaces.firstWhere(
      (sp) => sp.placeId == place.id,
      orElse: () => SavedPlace(recommendationId: 0, placeId: place.id, placeName: place.name, recommendedAt: DateTime.now()),
    );
    final savedIndex = _savedPlaces.indexOf(correspondingSavedPlace);

    // Optimistic UI Update
    setState(() {
      _detailedPlaces.removeAt(index);
      if (savedIndex != -1) {
        _savedPlaces.removeAt(savedIndex);
      }
    });

    final wishlistService = Provider.of<WishlistService>(context, listen: false);
    final success = await wishlistService.unsavePlace(place.id);

    if (success) {
      if (mounted) {
        _showSuccessSnackBar('${place.name} 저장이 취소되었습니다.');
      }
    } else {
      // Rollback on failure
      if (mounted) {
        setState(() {
          _detailedPlaces.insert(index, removedPlace);
          if (savedIndex != -1) {
            _savedPlaces.insert(savedIndex, correspondingSavedPlace);
          }
        });
        _showErrorSnackBar('저장 취소에 실패했습니다. 다시 시도해주세요.');
      }
    }
  }

  void _showSuccessSnackBar(String message) {
    ScaffoldMessenger.of(context).clearSnackBars();
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Row(
          children: [
            const Icon(Icons.check_circle_rounded, color: Colors.white, size: 20),
            const SizedBox(width: 10),
            Expanded(
              child: Text(
                message,
                style: const TextStyle(fontWeight: FontWeight.w600, color: Colors.white, fontSize: 13),
              ),
            ),
          ],
        ),
        backgroundColor: const Color(0xFFFF8F00),
        behavior: SnackBarBehavior.floating,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
        margin: const EdgeInsets.all(16),
        duration: const Duration(seconds: 2),
      ),
    );
  }

  void _showErrorSnackBar(String message) {
    ScaffoldMessenger.of(context).clearSnackBars();
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Row(
          children: [
            const Icon(Icons.error_outline_rounded, color: Colors.white, size: 20),
            const SizedBox(width: 10),
            Expanded(
              child: Text(
                message,
                style: const TextStyle(fontWeight: FontWeight.w600, color: Colors.white, fontSize: 13),
              ),
            ),
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
          '내 저장 목록',
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
          preferredSize: const Size.fromHeight(1.0),
          child: Container(
            color: const Color(0xFFF0EAE1),
            height: 1.0,
          ),
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh_rounded, color: Color(0xFF263238)),
            tooltip: '새로고침',
            onPressed: _isLoading ? null : _loadWishlist,
          ),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: _loadWishlist,
        color: const Color(0xFFFFB300),
        backgroundColor: Colors.white,
        child: _isLoading
            ? _buildSkeletonLoader()
            : _detailedPlaces.isEmpty
                ? _buildEmptyView()
                : _buildWishlistView(),
      ),
    );
  }

  // 1. Guest Screen for Non-logged-in Users
  Widget _buildGuestView() {
    return Scaffold(
      backgroundColor: const Color(0xFFFDFBF7),
      body: Center(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 32.0),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Container(
                width: 110,
                height: 110,
                decoration: BoxDecoration(
                  color: const Color(0xFFFFF8E1),
                  shape: BoxShape.circle,
                  boxShadow: [
                    BoxShadow(
                      color: const Color(0xFFFFB300).withOpacity(0.12),
                      blurRadius: 24,
                      offset: const Offset(0, 8),
                    ),
                  ],
                ),
                child: const Center(
                  child: Icon(
                    Icons.bookmark_add_rounded,
                    size: 52,
                    color: Color(0xFFFFB300),
                  ),
                ),
              ),
              const SizedBox(height: 28),
              const Text(
                '로그인이 필요합니다',
                style: TextStyle(
                  fontSize: 20,
                  fontWeight: FontWeight.bold,
                  color: Color(0xFF263238),
                  letterSpacing: -0.5,
                ),
              ),
              const SizedBox(height: 12),
              const Text(
                '자주 방문하고 싶거나 기억해 두고 싶은\n소중한 맛집을 저장하고 편리하게 찾아보세요!',
                textAlign: TextAlign.center,
                style: TextStyle(
                  fontSize: 14,
                  color: Color(0xFF78909C),
                  height: 1.5,
                ),
              ),
              const SizedBox(height: 36),
              GestureDetector(
                onTap: () {
                  Navigator.of(context).push(
                    MaterialPageRoute(builder: (context) => const LoginScreen()),
                  ).then((_) {
                    if (Provider.of<AuthProvider>(context, listen: false).isAuthenticated) {
                      _loadWishlist();
                    }
                  });
                },
                child: Container(
                  width: double.infinity,
                  height: 54,
                  decoration: BoxDecoration(
                    borderRadius: BorderRadius.circular(27),
                    gradient: const LinearGradient(
                      colors: [Color(0xFFFFC107), Color(0xFFFF8F00)],
                      begin: Alignment.topLeft,
                      end: Alignment.bottomRight,
                    ),
                    boxShadow: [
                      BoxShadow(
                        color: const Color(0xFFFF8F00).withOpacity(0.25),
                        blurRadius: 16,
                        offset: const Offset(0, 8),
                      ),
                    ],
                  ),
                  child: const Center(
                    child: Text(
                      '로그인하러 가기',
                      style: TextStyle(
                        color: Colors.white,
                        fontSize: 16,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  // 2. Custom Skeleton Loading Screen using pure Flutter Animation
  Widget _buildSkeletonLoader() {
    return ListView.builder(
      padding: const EdgeInsets.all(16),
      itemCount: 3,
      itemBuilder: (context, index) {
        return Container(
          margin: const EdgeInsets.only(bottom: 16),
          padding: const EdgeInsets.all(12),
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(16),
            boxShadow: [
              BoxShadow(
                color: Colors.black.withOpacity(0.01),
                blurRadius: 10,
                offset: const Offset(0, 4),
              ),
            ],
          ),
          child: Row(
            children: [
              const ShimmerLoading(
                width: 90,
                height: 90,
                borderRadius: BorderRadius.all(Radius.circular(12)),
              ),
              const SizedBox(width: 16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const ShimmerLoading(width: 60, height: 16),
                    const SizedBox(height: 8),
                    const ShimmerLoading(width: 140, height: 18),
                    const SizedBox(height: 8),
                    Row(
                      children: const [
                        ShimmerLoading(width: 14, height: 14),
                        SizedBox(width: 4),
                        ShimmerLoading(width: 80, height: 14),
                      ],
                    ),
                  ],
                ),
              ),
            ],
          ),
        );
      },
    );
  }

  // 3. Empty Wishlist Placeholder Screen
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
            Container(
              width: 100,
              height: 100,
              decoration: const BoxDecoration(
                color: Color(0xFFF5F0E6),
                shape: BoxShape.circle,
              ),
              child: const Center(
                child: Icon(
                  Icons.favorite_border_rounded,
                  size: 46,
                  color: Color(0xFFD7CCC8),
                ),
              ),
            ),
            const SizedBox(height: 24),
            const Text(
              '저장한 맛집이 없습니다',
              style: TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.bold,
                color: Color(0xFF263238),
                letterSpacing: -0.5,
              ),
            ),
            const SizedBox(height: 8),
            const Text(
              '지도나 랭킹 화면에서 꿀맛집을 탐색하고\n추천 버튼을 눌러 목록을 채워보세요.',
              textAlign: TextAlign.center,
              style: TextStyle(
                fontSize: 14,
                color: Color(0xFF90A4AE),
                height: 1.5,
              ),
            ),
            const SizedBox(height: 32),
            if (widget.onExploreTap != null)
              OutlinedButton(
                onPressed: widget.onExploreTap,
                style: OutlinedButton.styleFrom(
                  side: const BorderSide(color: Color(0xFFFFB300), width: 1.5),
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24)),
                  padding: const EdgeInsets.symmetric(horizontal: 28, vertical: 14),
                  backgroundColor: Colors.white,
                ),
                child: const Text(
                  '꿀맛집 찾으러 가기',
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

  // 4. Saved Places List View
  Widget _buildWishlistView() {
    return ListView.builder(
      padding: const EdgeInsets.all(16),
      itemCount: _detailedPlaces.length,
      itemBuilder: (context, index) {
        final place = _detailedPlaces[index];
        final categoryName = _categoryMap[place.categoryCode] ?? place.categoryCode;

        return Container(
          margin: const EdgeInsets.only(bottom: 16),
          child: Dismissible(
            key: Key('wishlist_dismiss_${place.id}'),
            direction: DismissDirection.endToStart,
            onDismissed: (direction) {
              _unsavePlace(place);
            },
            background: Container(
              alignment: Alignment.centerRight,
              padding: const EdgeInsets.only(right: 24),
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(18),
                gradient: const LinearGradient(
                  colors: [Color(0xFFFF5252), Color(0xFFFF8F00)],
                  begin: Alignment.centerLeft,
                  end: Alignment.centerRight,
                ),
              ),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.end,
                children: const [
                  Text(
                    '저장 취소',
                    style: TextStyle(
                      color: Colors.white,
                      fontWeight: FontWeight.bold,
                      fontSize: 15,
                    ),
                  ),
                  SizedBox(width: 8),
                  Icon(Icons.delete_outline_rounded, color: Colors.white, size: 26),
                ],
              ),
            ),
            child: Container(
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(18),
                border: Border.all(color: const Color(0xFFF3EFE9), width: 1.2),
                boxShadow: [
                  BoxShadow(
                    color: const Color(0xFF8D6E63).withOpacity(0.04),
                    blurRadius: 12,
                    offset: const Offset(0, 4),
                  ),
                ],
              ),
              child: Material(
                color: Colors.transparent,
                child: InkWell(
                  onTap: () {
                    Navigator.of(context).push(
                      MaterialPageRoute(
                        builder: (context) => PlaceDetailScreen(placeId: place.id),
                      ),
                    ).then((_) {
                      _loadWishlist();
                    });
                  },
                  borderRadius: BorderRadius.circular(18),
                  child: Padding(
                    padding: const EdgeInsets.all(12),
                    child: Row(
                      children: [
                        // Place Thumbnail
                        ClipRRect(
                          borderRadius: BorderRadius.circular(14),
                          child: place.imageUrls.isNotEmpty
                              ? Image.network(
                                  place.imageUrls[0],
                                  width: 88,
                                  height: 88,
                                  fit: BoxFit.cover,
                                  errorBuilder: (context, error, stackTrace) =>
                                      _buildImagePlaceholder(),
                                )
                              : _buildImagePlaceholder(),
                        ),
                        const SizedBox(width: 16),
                        // Place Details
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Row(
                                children: [
                                  Container(
                                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                                    decoration: BoxDecoration(
                                      color: const Color(0xFFFFF8E1),
                                      borderRadius: BorderRadius.circular(6),
                                    ),
                                    child: Text(
                                      categoryName,
                                      style: const TextStyle(
                                        fontSize: 10,
                                        fontWeight: FontWeight.w800,
                                        color: Color(0xFFFF8F00),
                                      ),
                                    ),
                                  ),
                                  const Spacer(),
                                  const Icon(
                                    Icons.star_rounded,
                                    color: Color(0xFFFFB300),
                                    size: 16,
                                  ),
                                  const SizedBox(width: 2),
                                  Text(
                                    place.stats != null
                                        ? place.stats!.trustWeightedScore.toStringAsFixed(1)
                                        : '0.0',
                                    style: const TextStyle(
                                      fontSize: 13,
                                      fontWeight: FontWeight.bold,
                                      color: Color(0xFF37474F),
                                    ),
                                  ),
                                ],
                              ),
                              const SizedBox(height: 6),
                              Text(
                                place.name,
                                style: const TextStyle(
                                  fontSize: 16,
                                  fontWeight: FontWeight.bold,
                                  color: Color(0xFF263238),
                                  letterSpacing: -0.3,
                                ),
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                              ),
                              const SizedBox(height: 4),
                              Row(
                                children: [
                                  const Icon(Icons.location_on_rounded, color: Color(0xFFB0BEC5), size: 14),
                                  const SizedBox(width: 2),
                                  Expanded(
                                    child: Text(
                                      place.regionDongName ?? place.addressRoad,
                                      style: const TextStyle(
                                        fontSize: 12,
                                        color: Color(0xFF78909C),
                                      ),
                                      maxLines: 1,
                                      overflow: TextOverflow.ellipsis,
                                    ),
                                  ),
                                ],
                              ),
                              const SizedBox(height: 6),
                              Text(
                                '대표메뉴: ${place.recommendedMenu}',
                                style: const TextStyle(
                                  fontSize: 12,
                                  color: Color(0xFF546E7A),
                                  fontWeight: FontWeight.w600,
                                ),
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                              ),
                            ],
                          ),
                        ),
                        const SizedBox(width: 8),
                        // Bookmark Unsave Button
                        IconButton(
                          icon: const Icon(
                            Icons.bookmark_rounded,
                            color: Color(0xFFFFB300),
                            size: 26,
                          ),
                          onPressed: () => _unsavePlace(place),
                          constraints: const BoxConstraints(),
                          padding: const EdgeInsets.all(4),
                          splashRadius: 22,
                        ),
                      ],
                    ),
                  ),
                ),
              ),
            ),
          ),
        );
      },
    );
  }

  Widget _buildImagePlaceholder() {
    return Container(
      width: 88,
      height: 88,
      color: const Color(0xFFFFF8E1),
      child: const Center(
        child: Icon(
          Icons.restaurant_rounded,
          color: Color(0xFFFFD54F),
          size: 32,
        ),
      ),
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
