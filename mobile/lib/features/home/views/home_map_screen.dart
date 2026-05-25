import 'package:flutter/material.dart';
import 'package:geolocator/geolocator.dart';
import 'package:provider/provider.dart';

import '../../../core/api/api_client.dart';
import '../../../core/config/app_config.dart';
import '../../../models/place.dart';
import '../../auth/providers/auth_provider.dart';
import '../../auth/views/login_screen.dart';
import '../../place/services/place_service.dart';
import '../../place/views/place_detail_screen.dart';
import '../../place/views/place_register_screen.dart';
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
    _determinePosition();
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
      final fallbackPosition = Position(
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
      backgroundColor: const Color(0xFFFAFAFA),
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
                    valueColor: AlwaysStoppedAnimation(Color(0xFFFFB300)),
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
          backgroundColor: const Color(0xFFFF8F00),
          foregroundColor: Colors.white,
          tooltip: '맛집 등록',
          child: const Icon(Icons.add_location_alt_outlined),
        ),
        const SizedBox(height: 10),
        FloatingActionButton.small(
          heroTag: 'gps_btn',
          onPressed: _determinePosition,
          backgroundColor: Colors.white,
          foregroundColor: const Color(0xFFFFB300),
          tooltip: '내 위치',
          child: _isLoadingGps
              ? const SizedBox(
                  height: 16,
                  width: 16,
                  child: CircularProgressIndicator(
                    strokeWidth: 2,
                    valueColor: AlwaysStoppedAnimation(Color(0xFFFFB300)),
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
          backgroundColor: const Color(0xFFFFB300),
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
        color: Colors.white,
        borderRadius: BorderRadius.circular(30),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.06),
            blurRadius: 10,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: TextField(
        controller: _searchController,
        textInputAction: TextInputAction.search,
        onSubmitted: (_) => _search(),
        decoration: InputDecoration(
          hintText: '맛집 이름이나 메뉴를 검색해 보세요',
          hintStyle: const TextStyle(fontSize: 14, color: Colors.black38),
          prefixIcon: const Icon(Icons.search, color: Color(0xFFFFB300)),
          suffixIcon: _searchController.text.isNotEmpty
              ? IconButton(
                  icon: const Icon(Icons.clear, size: 20),
                  color: Colors.black38,
                  onPressed: () {
                    _searchController.clear();
                    _search();
                  },
                )
              : null,
          border: InputBorder.none,
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
                  color: isSelected ? Colors.white : Colors.black87,
                  fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
                ),
              ),
              selected: isSelected,
              onSelected: (_) {
                setState(() {
                  _selectedCategoryCode = category.code;
                  _syncSelectedPlace();
                });
              },
              selectedColor: const Color(0xFFFFB300),
              backgroundColor: Colors.white,
              elevation: 1,
              shadowColor: Colors.black12,
              padding: const EdgeInsets.symmetric(horizontal: 12),
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
    return Container(
      color: const Color(0xFFF9F7F2),
      padding: const EdgeInsets.symmetric(horizontal: 32),
      child: const Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.map_outlined, size: 56, color: Color(0xFFFFB300)),
            SizedBox(height: 16),
            Text(
              '카카오맵 설정이 필요합니다.',
              style: TextStyle(
                fontSize: 17,
                fontWeight: FontWeight.bold,
                color: Colors.black87,
              ),
              textAlign: TextAlign.center,
            ),
            SizedBox(height: 8),
            Text(
              'KAKAO_NATIVE_APP_KEY를 빌드 설정에 추가하면 실제 지도가 표시됩니다.',
              style: TextStyle(fontSize: 13, color: Colors.black54),
              textAlign: TextAlign.center,
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildListView(List<Place> places) {
    if (_isLoadingPlaces) {
      return const Center(
        child: CircularProgressIndicator(
          valueColor: AlwaysStoppedAnimation(Color(0xFFFFB300)),
        ),
      );
    }

    if (places.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(Icons.search_off, size: 60, color: Colors.black26),
            const SizedBox(height: 16),
            const Text(
              '검색 결과나 가까운 맛집이 없습니다.',
              style: TextStyle(color: Colors.black54),
            ),
            const SizedBox(height: 8),
            TextButton(
              onPressed: () {
                _searchController.clear();
                _determinePosition();
              },
              child: const Text(
                '내 위치 기준으로 다시 찾기',
                style: TextStyle(
                  color: Color(0xFFFFB300),
                  fontWeight: FontWeight.bold,
                ),
              ),
            ),
          ],
        ),
      );
    }

    return ListView.builder(
      padding: const EdgeInsets.only(top: 130, left: 16, right: 16, bottom: 20),
      itemCount: places.length,
      itemBuilder: (context, index) => _buildPlaceListItem(places[index]),
    );
  }

  Widget _buildPlaceListItem(Place place) {
    return Card(
      elevation: 0,
      color: Colors.white,
      margin: const EdgeInsets.only(bottom: 12),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
        side: const BorderSide(color: Colors.black12),
      ),
      child: ListTile(
        contentPadding: const EdgeInsets.all(12),
        leading: _buildPlaceIcon(size: 60, iconSize: 28),
        title: Row(
          children: [
            Expanded(
              child: Text(
                place.name,
                style:
                    const TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
              ),
            ),
            const SizedBox(width: 6),
            _buildCategoryBadge(place.categoryCode),
          ],
        ),
        subtitle: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const SizedBox(height: 4),
            Text(
              '추천 메뉴: ${place.recommendedMenu}',
              style: const TextStyle(fontSize: 13, color: Colors.black87),
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
            ),
            const SizedBox(height: 2),
            Text(
              place.addressRoad.isNotEmpty
                  ? place.addressRoad
                  : place.addressJibun,
              style: const TextStyle(fontSize: 12, color: Colors.black45),
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
            ),
          ],
        ),
        trailing: const Icon(Icons.chevron_right),
        onTap: () => _openPlaceDetail(place),
      ),
    );
  }

  Widget _buildSelectedPlaceCard(Place place) {
    return Card(
      elevation: 3,
      shadowColor: Colors.black26,
      margin: EdgeInsets.zero,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      color: Colors.white,
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Row(
          children: [
            _buildPlaceIcon(size: 72, iconSize: 32),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisSize: MainAxisSize.min,
                children: [
                  Row(
                    children: [
                      Expanded(
                        child: Text(
                          place.name,
                          style: const TextStyle(
                            fontSize: 15,
                            fontWeight: FontWeight.bold,
                          ),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                      _buildCategoryBadge(place.categoryCode),
                    ],
                  ),
                  const SizedBox(height: 4),
                  Text(
                    '추천 메뉴: ${place.recommendedMenu}',
                    style: const TextStyle(fontSize: 12, color: Colors.black87),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                  const SizedBox(height: 4),
                  Text(
                    place.shortRecommendation,
                    style: const TextStyle(fontSize: 11, color: Colors.black54),
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                  ),
                ],
              ),
            ),
            const SizedBox(width: 8),
            Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                IconButton(
                  tooltip: '닫기',
                  onPressed: () => setState(() => _selectedPlace = null),
                  icon: const Icon(Icons.close, size: 20),
                ),
                TextButton(
                  onPressed: () => _openPlaceDetail(place),
                  child: const Text('상세'),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildPlaceIcon({required double size, required double iconSize}) {
    return Container(
      width: size,
      height: size,
      decoration: BoxDecoration(
        color: const Color(0xFFFFB300).withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(12),
      ),
      child: Icon(Icons.restaurant,
          color: const Color(0xFFFFB300), size: iconSize),
    );
  }

  Widget _buildCategoryBadge(String categoryCode) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
      decoration: BoxDecoration(
        color: const Color(0xFFFFB300).withValues(alpha: 0.15),
        borderRadius: BorderRadius.circular(4),
      ),
      child: Text(
        _categoryLabels[categoryCode] ?? categoryCode,
        style: const TextStyle(
          fontSize: 10,
          color: Color(0xFFFF8F00),
          fontWeight: FontWeight.bold,
        ),
      ),
    );
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
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        title: const Text(
          '로그인이 필요합니다',
          style: TextStyle(fontWeight: FontWeight.bold),
        ),
        content: const Text(
          '맛집 등록은 로그인 후 사용할 수 있습니다.\n로그인 화면으로 이동할까요?',
        ),
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
              );
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
}
