import 'package:flutter/material.dart';
import 'package:geolocator/geolocator.dart';
import 'package:provider/provider.dart';

import '../../../core/api/api_client.dart';
import '../../../core/config/app_config.dart';
import '../../../core/theme/app_theme.dart';
import '../../../core/widgets/app_empty_state.dart';
import '../../../models/place.dart';
import '../../auth/providers/auth_provider.dart';
import '../../auth/views/login_screen.dart';
import '../../place/services/place_service.dart';
import '../../place/views/place_detail_screen.dart';
import '../../place/views/place_register_screen.dart';
import '../widgets/home_place_card.dart';
import '../widgets/kakao_place_map.dart';

class HomeMapScreen extends StatefulWidget {
  const HomeMapScreen({super.key});

  @override
  State<HomeMapScreen> createState() => _HomeMapScreenState();
}

class _CategoryFilter {
  const _CategoryFilter(this.label, this.code);

  final String label;
  final String? code;
}

class _HomeMapScreenState extends State<HomeMapScreen> {
  static const _categories = [
    _CategoryFilter('전체', null),
    _CategoryFilter('한식', 'KOREAN'),
    _CategoryFilter('중식', 'CHINESE'),
    _CategoryFilter('일식', 'JAPANESE'),
    _CategoryFilter('양식', 'WESTERN'),
    _CategoryFilter('분식', 'SNACK'),
    _CategoryFilter('카페', 'CAFE'),
  ];

  static const _categoryLabels = {
    'KOREAN': '한식',
    'CHINESE': '중식',
    'JAPANESE': '일식',
    'WESTERN': '양식',
    'SNACK': '분식',
    'CAFE': '카페',
  };

  late final PlaceService _placeService;
  final _searchController = TextEditingController();

  Position? _currentPosition;
  Place? _selectedPlace;
  bool _isLoadingGps = false;
  bool _isLoadingPlaces = false;
  bool _isMapView = true;
  int _recenterRequestId = 0;
  String? _selectedCategoryCode;
  List<Place> _places = [];

  @override
  void initState() {
    super.initState();
    _placeService =
        PlaceService(Provider.of<ApiClient>(context, listen: false));
    _currentPosition = _fallbackPosition();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) {
        _determinePosition();
      }
    });
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  Future<void> _determinePosition() async {
    setState(() => _isLoadingGps = true);

    try {
      final serviceEnabled = await Geolocator.isLocationServiceEnabled();
      if (!serviceEnabled) {
        throw '위치 서비스가 꺼져 있습니다.';
      }

      var permission = await Geolocator.checkPermission();
      if (permission == LocationPermission.denied) {
        permission = await Geolocator.requestPermission();
        if (permission == LocationPermission.denied) {
          throw '위치 권한이 거부되었습니다.';
        }
      }

      if (permission == LocationPermission.deniedForever) {
        throw '위치 권한이 영구적으로 거부되었습니다.';
      }

      final position = await Geolocator.getCurrentPosition(
        desiredAccuracy: LocationAccuracy.high,
        timeLimit: const Duration(seconds: 8),
      );

      if (!mounted) return;
      setState(() {
        _currentPosition = position;
        _selectedPlace = null;
        _recenterRequestId++;
        _isLoadingGps = false;
      });

      await _loadNearbyPlaces(position);
    } catch (error) {
      debugPrint('GPS Error: $error');
      final fallbackPosition = _fallbackPosition();

      if (!mounted) return;
      setState(() {
        _currentPosition = fallbackPosition;
        _selectedPlace = null;
        _recenterRequestId++;
        _isLoadingGps = false;
      });

      await _loadNearbyPlaces(fallbackPosition);
    }
  }

  Position _fallbackPosition() {
    return Position(
      latitude: 37.556456,
      longitude: 126.924456,
      timestamp: DateTime.now(),
      accuracy: 0.0,
      altitude: 0.0,
      altitudeAccuracy: 0.0,
      heading: 0.0,
      headingAccuracy: 0.0,
      speed: 0.0,
      speedAccuracy: 0.0,
    );
  }

  Future<void> _loadNearbyPlaces(Position position) async {
    setState(() => _isLoadingPlaces = true);

    final result = await _placeService.getNearbyPlaces(
      position.latitude,
      position.longitude,
    );

    if (!mounted) return;
    setState(() {
      _isLoadingPlaces = false;
      _places = result;
      _syncSelectedPlace();
    });
  }

  Future<void> _search() async {
    final keyword = _searchController.text.trim();
    if (keyword.isEmpty) {
      if (_currentPosition != null) {
        await _loadNearbyPlaces(_currentPosition!);
      }
      return;
    }

    setState(() => _isLoadingPlaces = true);
    final result = await _placeService.searchPlaces(keyword);

    if (!mounted) return;
    setState(() {
      _isLoadingPlaces = false;
      _places = result;
      _selectedPlace = null;
    });
  }

  List<Place> _getFilteredPlaces() {
    final selectedCategoryCode = _selectedCategoryCode;
    if (selectedCategoryCode == null) {
      return _places;
    }

    return _places
        .where((place) => place.categoryCode == selectedCategoryCode)
        .toList();
  }

  void _syncSelectedPlace() {
    final selectedPlace = _selectedPlace;
    if (selectedPlace == null) {
      return;
    }

    final places = _getFilteredPlaces();
    final stillVisible = places.any((place) => place.id == selectedPlace.id);
    if (!stillVisible) {
      _selectedPlace = null;
    }
  }

  @override
  Widget build(BuildContext context) {
    final filteredPlaces = _getFilteredPlaces();
    final selectedPlace = _selectedPlace;

    return Scaffold(
      backgroundColor: AppColors.background,
      body: Stack(
        children: [
          Positioned.fill(
            child: _isMapView
                ? _buildMapView(filteredPlaces)
                : _buildListView(filteredPlaces),
          ),
          Positioned(
            top: 16,
            left: 16,
            right: 16,
            child: _buildSearchBar(),
          ),
          Positioned(
            top: 76,
            left: 0,
            right: 0,
            child: _buildCategoryChips(),
          ),
          Positioned(
            right: 16,
            bottom: _isMapView && selectedPlace != null ? 132 : 20,
            child: _buildFloatingActions(),
          ),
          if (_isMapView && selectedPlace != null)
            Positioned(
              left: 16,
              right: 16,
              bottom: 16,
              child: _buildSelectedPlaceCard(selectedPlace),
            ),
          if (_isMapView && _isLoadingPlaces)
            const Positioned(
              left: 0,
              right: 0,
              bottom: 28,
              child: Center(
                child: SizedBox(
                  width: 28,
                  height: 28,
                  child: CircularProgressIndicator(
                    strokeWidth: 3,
                    valueColor: AlwaysStoppedAnimation(AppColors.honey),
                  ),
                ),
              ),
            ),
        ],
      ),
    );
  }

  Widget _buildFloatingActions() {
    return Column(
      children: [
        FloatingActionButton(
          heroTag: 'register_btn',
          onPressed: _openRegister,
          backgroundColor: AppColors.nectar,
          foregroundColor: Colors.white,
          tooltip: '맛집 등록',
          child: const Icon(Icons.add_location_alt_outlined),
        ),
        const SizedBox(height: 10),
        FloatingActionButton.small(
          heroTag: 'gps_btn',
          onPressed: _determinePosition,
          backgroundColor: AppColors.surface,
          foregroundColor: AppColors.honey,
          tooltip: '내 위치',
          child: _isLoadingGps
              ? const SizedBox(
                  height: 16,
                  width: 16,
                  child: CircularProgressIndicator(
                    strokeWidth: 2,
                    valueColor: AlwaysStoppedAnimation(AppColors.honey),
                  ),
                )
              : const Icon(Icons.my_location),
        ),
        const SizedBox(height: 10),
        FloatingActionButton(
          heroTag: 'view_toggle_btn',
          onPressed: () {
            setState(() {
              _isMapView = !_isMapView;
              _selectedPlace = null;
            });
          },
          backgroundColor: AppColors.honey,
          foregroundColor: Colors.white,
          tooltip: _isMapView ? '리스트 보기' : '지도 보기',
          child: Icon(_isMapView ? Icons.list : Icons.map),
        ),
      ],
    );
  }

  Widget _buildSearchBar() {
    return Container(
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(AppRadius.pill),
        boxShadow: [
          BoxShadow(
            color: AppColors.ink.withValues(alpha: 0.08),
            blurRadius: 16,
            offset: const Offset(0, 6),
          ),
        ],
      ),
      child: TextField(
        controller: _searchController,
        textInputAction: TextInputAction.search,
        onSubmitted: (_) => _search(),
        decoration: InputDecoration(
          hintText: '맛집 이름이나 메뉴를 검색해 보세요',
          hintStyle: const TextStyle(fontSize: 14, color: AppColors.muted),
          prefixIcon: const Icon(Icons.search, color: AppColors.honey),
          suffixIcon: _searchController.text.isNotEmpty
              ? IconButton(
                  icon: const Icon(Icons.clear, size: 20),
                  color: AppColors.muted,
                  onPressed: () {
                    _searchController.clear();
                    _search();
                  },
                )
              : null,
          border: InputBorder.none,
          enabledBorder: InputBorder.none,
          focusedBorder: InputBorder.none,
          contentPadding:
              const EdgeInsets.symmetric(vertical: 14, horizontal: 20),
        ),
      ),
    );
  }

  Widget _buildCategoryChips() {
    return SizedBox(
      height: 40,
      child: ListView.builder(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.symmetric(horizontal: 16),
        itemCount: _categories.length,
        itemBuilder: (context, index) {
          final category = _categories[index];
          final isSelected = _selectedCategoryCode == category.code;

          return Padding(
            padding: const EdgeInsets.only(right: 8),
            child: ChoiceChip(
              label: Text(
                category.label,
                style: TextStyle(
                  color: isSelected ? Colors.white : AppColors.ink,
                  fontWeight: isSelected ? FontWeight.w800 : FontWeight.w700,
                ),
              ),
              selected: isSelected,
              onSelected: (_) {
                setState(() {
                  _selectedCategoryCode = category.code;
                  _syncSelectedPlace();
                });
              },
              selectedColor: AppColors.honey,
              backgroundColor: AppColors.surface,
              elevation: 1,
              shadowColor: AppColors.ink.withValues(alpha: 0.08),
              side: const BorderSide(color: AppColors.outline),
              padding: const EdgeInsets.symmetric(horizontal: AppSpacing.sm),
            ),
          );
        },
      ),
    );
  }

  Widget _buildMapView(List<Place> places) {
    if (!AppConfig.isKakaoNativeConfigured) {
      return _buildMapConfigurationState();
    }

    return KakaoPlaceMap(
      places: places,
      currentPosition: _currentPosition,
      recenterRequestId: _recenterRequestId,
      onPlaceSelected: (place) {
        setState(() => _selectedPlace = place);
      },
    );
  }

  Widget _buildMapConfigurationState() {
    return const ColoredBox(
      color: AppColors.background,
      child: AppEmptyState(
        icon: Icons.map_outlined,
        title: '카카오맵 설정이 필요합니다',
        description: 'KAKAO_NATIVE_APP_KEY를 빌드 설정에 추가하면 실제 지도가 표시됩니다.',
      ),
    );
  }

  Widget _buildListView(List<Place> places) {
    if (_isLoadingPlaces) {
      return const Center(
        child: CircularProgressIndicator(
          valueColor: AlwaysStoppedAnimation(AppColors.honey),
        ),
      );
    }

    if (places.isEmpty) {
      return AppEmptyState(
        icon: Icons.search_off_rounded,
        title: '검색 결과가 없습니다',
        description: '검색어를 바꾸거나 내 위치 기준으로 가까운 맛집을 다시 찾아보세요.',
        actionLabel: '내 위치 기준으로 다시 찾기',
        onActionPressed: () {
          _searchController.clear();
          _determinePosition();
        },
      );
    }

    return ListView.builder(
      padding: const EdgeInsets.only(top: 130, left: 16, right: 16, bottom: 20),
      itemCount: places.length,
      itemBuilder: (context, index) => _buildPlaceListItem(places[index]),
    );
  }

  Widget _buildPlaceListItem(Place place) {
    return Padding(
      padding: const EdgeInsets.only(bottom: AppSpacing.sm),
      child: HomePlaceCard(
        place: place,
        categoryLabel: _categoryLabel(place.categoryCode),
        onTap: () => _openPlaceDetail(place),
      ),
    );
  }

  Widget _buildSelectedPlaceCard(Place place) {
    return DecoratedBox(
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(AppRadius.lg),
        boxShadow: [
          BoxShadow(
            color: AppColors.ink.withValues(alpha: 0.14),
            blurRadius: 20,
            offset: const Offset(0, 8),
          ),
        ],
      ),
      child: HomePlaceCard(
        place: place,
        categoryLabel: _categoryLabel(place.categoryCode),
        compact: true,
        onTap: () => _openPlaceDetail(place),
        onClose: () => setState(() => _selectedPlace = null),
      ),
    );
  }

  String _categoryLabel(String categoryCode) {
    return _categoryLabels[categoryCode] ?? categoryCode;
  }

  Future<void> _openRegister() async {
    final authProvider = Provider.of<AuthProvider>(context, listen: false);
    if (!authProvider.isAuthenticated) {
      _showLoginPrompt();
      return;
    }

    final result = await Navigator.of(context).push(
      MaterialPageRoute(builder: (context) => const PlaceRegisterScreen()),
    );
    if (result == true && _currentPosition != null) {
      await _loadNearbyPlaces(_currentPosition!);
    }
  }

  Future<void> _openPlaceDetail(Place place) async {
    final result = await Navigator.of(context).push(
      MaterialPageRoute(
        builder: (context) => PlaceDetailScreen(placeId: place.id),
      ),
    );
    if (result == true && _currentPosition != null) {
      await _loadNearbyPlaces(_currentPosition!);
    }
  }

  void _showLoginPrompt() {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(AppRadius.lg),
        ),
        title: const Text(
          '로그인이 필요합니다',
          style: TextStyle(fontWeight: FontWeight.w800),
        ),
        content: const Text(
          '맛집 등록은 로그인 후 사용할 수 있습니다.\n로그인 화면으로 이동할까요?',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('취소'),
          ),
          FilledButton(
            onPressed: () {
              Navigator.pop(context);
              Navigator.of(context).push(
                MaterialPageRoute(builder: (context) => const LoginScreen()),
              );
            },
            child: const Text('로그인하기'),
          ),
        ],
      ),
    );
  }
}
