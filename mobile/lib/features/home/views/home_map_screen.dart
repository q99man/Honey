import 'package:flutter/material.dart';
import 'package:geolocator/geolocator.dart';
import 'package:provider/provider.dart';
import '../../../core/api/api_client.dart';
import '../../../models/place.dart';
import '../../place/services/place_service.dart';
import '../../auth/providers/auth_provider.dart';
import '../../place/views/place_register_screen.dart';
import '../../place/views/place_detail_screen.dart';
import '../../auth/views/login_screen.dart';

class HomeMapScreen extends StatefulWidget {
  const HomeMapScreen({super.key});

  @override
  State<HomeMapScreen> createState() => _HomeMapScreenState();
}

class _HomeMapScreenState extends State<HomeMapScreen> with SingleTickerProviderStateMixin {
  late PlaceService _placeService;
  final _searchController = TextEditingController();
  
  Position? _currentPosition;
  bool _isLoadingGps = false;
  bool _isLoadingPlaces = false;
  List<Place> _places = [];
  String _selectedCategory = '전체';
  bool _isMapView = true;
  
  // Animation controller for GPS pulse
  late AnimationController _pulseController;

  final List<String> _categories = ['전체', '한식', '중식', '일식', '양식', '분식', '카페'];

  // Mock backup places if backend is empty
  final List<Place> _mockPlaces = [
    Place(
      id: 101,
      name: '상동 순대국',
      categoryCode: '한식',
      recommendedMenu: '순대국정식',
      shortRecommendation: '진한 육수와 푸짐한 건더기가 일품인 현지인 찐맛집',
      addressRoad: '경기도 부천시 원미구 상동로 117',
      addressJibun: '상동 544-1',
      latitude: 37.503456,
      longitude: 126.753456,
      currentStarLevel: 1,
      imageUrls: [],
    ),
    Place(
      id: 102,
      name: '홍대 츠케멘 라멘',
      categoryCode: '일식',
      recommendedMenu: '매운 츠케멘',
      shortRecommendation: '면발의 탄력이 차원이 다른 홍대 골목 숨은 강자',
      addressRoad: '서울특별시 마포구 와우산로 23길 9',
      addressJibun: '서교동 345-12',
      latitude: 37.556456,
      longitude: 126.924456,
      currentStarLevel: 2,
      imageUrls: [],
    ),
    Place(
      id: 103,
      name: '카페 멜로우',
      categoryCode: '카페',
      recommendedMenu: '허니 크림 라떼',
      shortRecommendation: '달콤한 크림과 아늑한 조명이 있는 서교동 최애 카페',
      addressRoad: '서울특별시 마포구 독막로 7길 15',
      addressJibun: '서교동 402-3',
      latitude: 37.549456,
      longitude: 126.921456,
      currentStarLevel: 0,
      imageUrls: [],
    ),
  ];

  @override
  void initState() {
    super.initState();
    _placeService = PlaceService(Provider.of<ApiClient>(context, listen: false));
    
    _pulseController = AnimationController(
      vsync: this,
      duration: const Duration(seconds: 2),
    )..repeat();

    _determinePosition();
  }

  @override
  void dispose() {
    _searchController.dispose();
    _pulseController.dispose();
    super.dispose();
  }

  Future<void> _determinePosition() async {
    setState(() => _isLoadingGps = true);
    try {
      bool serviceEnabled = await Geolocator.isLocationServiceEnabled();
      if (!serviceEnabled) {
        throw 'Location services are disabled.';
      }

      LocationPermission permission = await Geolocator.checkPermission();
      if (permission == LocationPermission.denied) {
        permission = await Geolocator.requestPermission();
        if (permission == LocationPermission.denied) {
          throw 'Location permissions are denied';
        }
      }

      if (permission == LocationPermission.deniedForever) {
        throw 'Location permissions are permanently denied';
      }

      Position position = await Geolocator.getCurrentPosition(
        desiredAccuracy: LocationAccuracy.high,
      );
      
      setState(() {
        _currentPosition = position;
        _isLoadingGps = false;
      });

      _loadNearbyPlaces(position);
    } catch (e) {
      debugPrint("GPS Error: $e");
      setState(() {
        _isLoadingGps = false;
        // Fallback to default coordinate (Seoul Center / Mapo)
        _currentPosition = Position(
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
      });
      _loadNearbyPlaces(_currentPosition!);
    }
  }

  Future<void> _loadNearbyPlaces(Position pos) async {
    setState(() => _isLoadingPlaces = true);
    
    // Fetch from real backend service
    List<Place> result = await _placeService.getNearbyPlaces(pos.latitude, pos.longitude);
    
    setState(() {
      _isLoadingPlaces = false;
      if (result.isNotEmpty) {
        _places = result;
      } else {
        // Use high quality mockup places in development if backend is fresh/empty
        _places = _mockPlaces;
      }
    });
  }

  void _search() async {
    final keyword = _searchController.text.trim();
    if (keyword.isEmpty) {
      if (_currentPosition != null) _loadNearbyPlaces(_currentPosition!);
      return;
    }

    setState(() => _isLoadingPlaces = true);
    List<Place> searchResult = await _placeService.searchPlaces(keyword);
    
    setState(() {
      _isLoadingPlaces = false;
      if (searchResult.isNotEmpty) {
        _places = searchResult;
      } else {
        // Filter mock places as backup search
        _places = _mockPlaces
            .where((p) => p.name.contains(keyword) || p.recommendedMenu.contains(keyword))
            .toList();
      }
    });
  }

  List<Place> _getFilteredPlaces() {
    if (_selectedCategory == '전체') return _places;
    return _places.where((p) => p.categoryCode == _selectedCategory).toList();
  }

  @override
  Widget build(BuildContext context) {
    final filtered = _getFilteredPlaces();

    return Scaffold(
      backgroundColor: const Color(0xFFFAFAFA),
      body: Stack(
        children: [
          // 1. Map/Explore Background or List view
          Positioned.fill(
            child: _isMapView 
                ? _buildSimulatedMap(filtered)
                : _buildListView(filtered),
          ),

          // 2. Search Bar at Top
          Positioned(
            top: 16,
            left: 16,
            right: 16,
            child: _buildSearchBar(),
          ),

          // 3. Category Filter List below Search Bar
          Positioned(
            top: 76,
            left: 0,
            right: 0,
            child: _buildCategoryChips(),
          ),
          // 4. GPS & Toggle & Register Floating Action Buttons
          Positioned(
            right: 16,
            bottom: _isMapView ? 150 : 20,
            child: Column(
              children: [
                FloatingActionButton(
                  heroTag: 'register_btn',
                  onPressed: () async {
                    final authProvider = Provider.of<AuthProvider>(context, listen: false);
                    if (!authProvider.isAuthenticated) {
                      _showLoginPrompt();
                      return;
                    }
                    final result = await Navigator.of(context).push(
                      MaterialPageRoute(builder: (context) => const PlaceRegisterScreen()),
                    );
                    if (result == true && _currentPosition != null) {
                      _loadNearbyPlaces(_currentPosition!);
                    }
                  },
                  backgroundColor: const Color(0xFFFF8F00),
                  foregroundColor: Colors.white,
                  tooltip: '식당 꽃 등록',
                  child: const Icon(Icons.add_location_alt_outlined),
                ),
                const SizedBox(height: 10),
                FloatingActionButton.small(
                  heroTag: 'gps_btn',
                  onPressed: _determinePosition,
                  backgroundColor: Colors.white,
                  foregroundColor: const Color(0xFFFFB300),
                  child: _isLoadingGps
                      ? const SizedBox(
                          height: 16,
                          width: 16,
                          child: CircularProgressIndicator(strokeWidth: 2, valueColor: AlwaysStoppedAnimation(Color(0xFFFFB300))),
                        )
                      : const Icon(Icons.my_location),
                ),
                const SizedBox(height: 10),
                FloatingActionButton(
                  heroTag: 'view_toggle_btn',
                  onPressed: () {
                    setState(() {
                      _isMapView = !_isMapView;
                    });
                  },
                  backgroundColor: const Color(0xFFFFB300),
                  foregroundColor: Colors.white,
                  child: Icon(_isMapView ? Icons.list : Icons.map),
                ),
              ],
            ),
          ),

          // 5. Place card slider (Only in map view at bottom)
          if (_isMapView && filtered.isNotEmpty)
            Positioned(
              left: 0,
              right: 0,
              bottom: 16,
              child: _buildPlaceCardSlider(filtered),
            ),
        ],
      ),
    );
  }

  Widget _buildSearchBar() {
    return Container(
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(30),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.06),
            blurRadius: 10,
            offset: const Offset(0, 4),
          )
        ],
      ),
      child: TextField(
        controller: _searchController,
        textInputAction: TextInputAction.search,
        onSubmitted: (_) => _search(),
        decoration: InputDecoration(
          hintText: '맛집 이름 또는 메뉴 검색...',
          hintStyle: const TextStyle(fontSize: 14, color: Colors.black38),
          prefixIcon: const Icon(Icons.search, color: Color(0xFFFFB300)),
          suffixIcon: _searchController.text.isNotEmpty
              ? IconButton(
                  icon: const Icon(Icons.clear, size: 20, color: Colors.black38),
                  onPressed: () {
                    _searchController.clear();
                    _search();
                  },
                )
              : null,
          border: InputBorder.none,
          contentPadding: const EdgeInsets.symmetric(vertical: 14, horizontal: 20),
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
          final cat = _categories[index];
          final isSelected = _selectedCategory == cat;
          return Padding(
            padding: const EdgeInsets.only(right: 8.0),
            child: ChoiceChip(
              label: Text(
                cat,
                style: TextStyle(
                  color: isSelected ? Colors.white : Colors.black87,
                  fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
                ),
              ),
              selected: isSelected,
              onSelected: (selected) {
                setState(() {
                  _selectedCategory = cat;
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

  // Simulated premium map using grid and custom painters representing nearby places
  Widget _buildSimulatedMap(List<Place> places) {
    return Container(
      color: const Color(0xFFF9F7F2), // Light cream warm background
      child: CustomPaint(
        painter: MapGridPainter(
          pulseValue: _pulseController.value,
          centerPoint: const Offset(200, 350),
        ),
        child: Stack(
          children: [
            // Center user pulse pin
            Positioned(
              left: 200 - 15,
              top: 350 - 15,
              child: AnimatedBuilder(
                animation: _pulseController,
                builder: (context, child) {
                  return Container(
                    width: 30,
                    height: 30,
                    decoration: BoxDecoration(
                      color: Colors.blueAccent.withOpacity(0.2),
                      shape: BoxShape.circle,
                      border: Border.all(color: Colors.blueAccent, width: 2),
                    ),
                    child: Center(
                      child: Container(
                        width: 10,
                        height: 10,
                        decoration: const BoxDecoration(
                          color: Colors.blueAccent,
                          shape: BoxShape.circle,
                        ),
                      ),
                    ),
                  );
                },
              ),
            ),

            // Restaurant markers
            ...places.asMap().entries.map((entry) {
              final idx = entry.key;
              final place = entry.value;
              
              // Distribute markers around the center user position
              final double angle = (idx * 2.1) + 0.5;
              final double dist = 80.0 + (idx * 45.0);
              final double left = 200 + (dist * double.parse((idx.isEven ? 1 : -1).toString()) * 0.7 * (idx % 2 == 0 ? 0.8 : 1.2));
              final double top = 350 + (dist * double.parse((idx.isOdd ? 1 : -1).toString()) * 0.5);

              return Positioned(
                left: left - 20,
                top: top - 45,
                child: InkWell(
                  onTap: () async {
                    final result = await Navigator.of(context).push(
                      MaterialPageRoute(
                        builder: (context) => PlaceDetailScreen(placeId: place.id),
                      ),
                    );
                    if (result == true && _currentPosition != null) {
                      _loadNearbyPlaces(_currentPosition!);
                    }
                  },
                  child: Column(
                    children: [
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                        decoration: BoxDecoration(
                          color: Colors.white,
                          borderRadius: BorderRadius.circular(8),
                          boxShadow: [
                            BoxShadow(color: Colors.black26, blurRadius: 4, offset: const Offset(0, 2))
                          ],
                          border: Border.all(color: const Color(0xFFFFB300), width: 1),
                        ),
                        child: Text(
                          place.name,
                          style: const TextStyle(fontSize: 10, fontWeight: FontWeight.bold),
                        ),
                      ),
                      const Icon(
                        Icons.location_on,
                        color: Color(0xFFFF8F00),
                        size: 30,
                      ),
                    ],
                  ),
                ),
              );
            }).toList(),
          ],
        ),
      ),
    );
  }

  Widget _buildListView(List<Place> places) {
    if (_isLoadingPlaces) {
      return const Center(
        child: CircularProgressIndicator(valueColor: AlwaysStoppedAnimation(Color(0xFFFFB300))),
      );
    }

    if (places.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(Icons.search_off, size: 60, color: Colors.black26),
            const SizedBox(height: 16),
            const Text('검색 결과와 가까운 꽃(맛집)이 없습니다.', style: TextStyle(color: Colors.black54)),
            const SizedBox(height: 8),
            TextButton(
              onPressed: () {
                _searchController.clear();
                _determinePosition();
              },
              child: const Text('초기화하기', style: TextStyle(color: Color(0xFFFFB300), fontWeight: FontWeight.bold)),
            )
          ],
        ),
      );
    }

    return ListView.builder(
      padding: const EdgeInsets.only(top: 130, left: 16, right: 16, bottom: 20),
      itemCount: places.length,
      itemBuilder: (context, index) {
        final place = places[index];
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
            leading: Container(
              width: 60,
              height: 60,
              decoration: BoxDecoration(
                color: const Color(0xFFFFB300).withOpacity(0.1),
                borderRadius: BorderRadius.circular(12),
              ),
              child: const Icon(Icons.restaurant, color: Color(0xFFFFB300), size: 28),
            ),
            title: Row(
              children: [
                Text(
                  place.name,
                  style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
                ),
                const SizedBox(width: 6),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                  decoration: BoxDecoration(
                    color: const Color(0xFFFFB300).withOpacity(0.15),
                    borderRadius: BorderRadius.circular(4),
                  ),
                  child: Text(
                    place.categoryCode,
                    style: const TextStyle(fontSize: 10, color: Color(0xFFFF8F00), fontWeight: FontWeight.bold),
                  ),
                ),
              ],
            ),
            subtitle: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const SizedBox(height: 4),
                Text(
                  '추천: ${place.recommendedMenu}',
                  style: const TextStyle(fontSize: 13, color: Colors.black87),
                ),
                const SizedBox(height: 2),
                Text(
                  place.addressRoad,
                  style: const TextStyle(fontSize: 12, color: Colors.black45),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
              ],
            ),
            trailing: const Icon(Icons.chevron_right),
            onTap: () async {
              final result = await Navigator.of(context).push(
                MaterialPageRoute(
                  builder: (context) => PlaceDetailScreen(placeId: place.id),
                ),
              );
              if (result == true && _currentPosition != null) {
                _loadNearbyPlaces(_currentPosition!);
              }
            },
          ),
        );
      },
    );
  }

  Widget _buildPlaceCardSlider(List<Place> places) {
    return SizedBox(
      height: 120,
      child: PageView.builder(
        itemCount: places.length,
        controller: PageController(viewportFraction: 0.85),
        itemBuilder: (context, index) {
          final place = places[index];
          return GestureDetector(
            onTap: () async {
              final result = await Navigator.of(context).push(
                MaterialPageRoute(
                  builder: (context) => PlaceDetailScreen(placeId: place.id),
                ),
              );
              if (result == true && _currentPosition != null) {
                _loadNearbyPlaces(_currentPosition!);
              }
            },
            child: Card(
              elevation: 2,
              shadowColor: Colors.black12,
              margin: const EdgeInsets.symmetric(horizontal: 8),
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
              color: Colors.white,
              child: Padding(
              padding: const EdgeInsets.all(12.0),
              child: Row(
                children: [
                  Container(
                    width: 80,
                    height: 80,
                    decoration: BoxDecoration(
                      color: const Color(0xFFFFB300).withOpacity(0.1),
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: const Icon(Icons.restaurant, color: Color(0xFFFFB300), size: 36),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Text(
                          place.name,
                          style: const TextStyle(fontSize: 15, fontWeight: FontWeight.bold),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                        const SizedBox(height: 4),
                        Text(
                          '추천: ${place.recommendedMenu}',
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
                ],
              ),
            ),
          ),
        );
        },
      ),
    );
  }

  void _showLoginPrompt() {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        title: const Text('로그인 필요', style: TextStyle(fontWeight: FontWeight.bold)),
        content: const Text('맛집 등록 기능은 로그인 후 이용하실 수 있습니다.\n로그인 화면으로 이동할까요?'),
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

// Custom Painter to draw grid lines and concentric circles representing proximity
class MapGridPainter extends CustomPainter {
  final double pulseValue;
  final Offset centerPoint;

  MapGridPainter({required this.pulseValue, required this.centerPoint});

  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = Colors.black.withOpacity(0.04)
      ..strokeWidth = 1.0
      ..style = PaintingStyle.stroke;

    // Draw grid lines
    const double step = 40.0;
    for (double i = 0; i < size.width; i += step) {
      canvas.drawLine(Offset(i, 0), Offset(i, size.height), paint);
    }
    for (double i = 0; i < size.height; i += step) {
      canvas.drawLine(Offset(0, i), Offset(size.width, i), paint);
    }

    // Draw concentric circles
    final circlePaint = Paint()
      ..color = const Color(0xFFFFB300).withOpacity(0.08)
      ..strokeWidth = 1.5
      ..style = PaintingStyle.stroke;

    canvas.drawCircle(centerPoint, 100, circlePaint);
    canvas.drawCircle(centerPoint, 200, circlePaint);
    canvas.drawCircle(centerPoint, 300, circlePaint);
  }

  @override
  bool shouldRepaint(covariant MapGridPainter oldDelegate) {
    return oldDelegate.pulseValue != pulseValue || oldDelegate.centerPoint != centerPoint;
  }
}
