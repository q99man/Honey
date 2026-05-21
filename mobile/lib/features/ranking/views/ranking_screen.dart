import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../../../models/ranking.dart';
import '../../../../models/region.dart';
import '../../auth/providers/auth_provider.dart';
import '../../place/services/place_service.dart';
import '../../place/views/place_detail_screen.dart';
import '../services/ranking_service.dart';

class RankingScreen extends StatefulWidget {
  const RankingScreen({super.key});

  @override
  State<RankingScreen> createState() => _RankingScreenState();
}

class _RankingScreenState extends State<RankingScreen> with SingleTickerProviderStateMixin {
  late RankingService _rankingService;
  late PlaceService _placeService;

  Season? _currentSeason;
  PlaceRankingResponse? _rankingResponse;
  
  bool _isLoadingSeason = true;
  bool _isLoadingRanking = true;
  String _regionType = 'dong'; // 'dong', 'district', 'city'

  // Selected region values
  int? _cityId;
  int? _districtId;
  int? _dongId;
  String _cityName = '';
  String _districtName = '';
  String _dongName = '';

  @override
  void initState() {
    super.initState();
    _rankingService = Provider.of<RankingService>(context, listen: false);
    _placeService = Provider.of<PlaceService>(context, listen: false);
    
    _initData();
  }

  // Initialize data: fetch season and default region
  Future<void> _initData() async {
    setState(() {
      _isLoadingSeason = true;
      _isLoadingRanking = true;
    });

    // 1. Fetch current season
    _currentSeason = await _rankingService.getCurrentSeason();
    setState(() {
      _isLoadingSeason = false;
    });

    // 2. Fetch user primary region if authenticated
    final authProvider = Provider.of<AuthProvider>(context, listen: false);
    Map<String, dynamic>? primaryRegion;
    if (authProvider.isAuthenticated) {
      primaryRegion = await _placeService.getMyRegion();
    }

    if (primaryRegion != null && primaryRegion['dongId'] != null) {
      _cityId = primaryRegion['cityId'];
      _districtId = primaryRegion['districtId'];
      _dongId = primaryRegion['dongId'];
      _cityName = primaryRegion['cityName'] ?? '';
      _districtName = primaryRegion['districtName'] ?? '';
      _dongName = primaryRegion['dongName'] ?? '';
    } else {
      // Guest or unverified user: fallback to first available region in database
      final cities = await _rankingService.getCities();
      if (cities.isNotEmpty) {
        final defaultCity = cities.first;
        _cityId = defaultCity.id;
        _cityName = defaultCity.nameKo;

        final districts = await _rankingService.getDistricts(defaultCity.id);
        if (districts.isNotEmpty) {
          final defaultDistrict = districts.first;
          _districtId = defaultDistrict.id;
          _districtName = defaultDistrict.nameKo;

          final dongs = await _rankingService.getDongs(defaultDistrict.id);
          if (dongs.isNotEmpty) {
            final defaultDong = dongs.first;
            _dongId = defaultDong.id;
            _dongName = defaultDong.nameKo;
          }
        }
      }
    }

    // 3. Load rankings for the default region
    await _loadRankings();
  }

  // Fetch rankings from API based on active region type and selected region IDs
  Future<void> _loadRankings() async {
    setState(() {
      _isLoadingRanking = true;
    });

    int? activeRegionId;
    if (_regionType == 'dong') {
      activeRegionId = _dongId;
    } else if (_regionType == 'district') {
      activeRegionId = _districtId;
    } else {
      activeRegionId = _cityId;
    }

    if (activeRegionId != null) {
      _rankingResponse = await _rankingService.getPlaceRankings(
        regionType: _regionType,
        regionId: activeRegionId,
        seasonCode: _currentSeason?.seasonCode,
      );
    } else {
      _rankingResponse = null;
    }

    setState(() {
      _isLoadingRanking = false;
    });
  }

  // Show BottomSheet to select city -> district -> dong manually
  void _showRegionSelectionBottomSheet() {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.white,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (context) {
        return _RegionSelectorBottomSheet(
          rankingService: _rankingService,
          initialCityId: _cityId,
          initialDistrictId: _districtId,
          initialDongId: _dongId,
          onRegionSelected: (city, district, dong) {
            setState(() {
              _cityId = city.id;
              _cityName = city.nameKo;
              _districtId = district.id;
              _districtName = district.nameKo;
              _dongId = dong.id;
              _dongName = dong.nameKo;
            });
            _loadRankings();
          },
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final displayedRegionName = _regionType == 'dong'
        ? '$_cityName $_districtName $_dongName'
        : _regionType == 'district'
            ? '$_cityName $_districtName'
            : _cityName;

    return Scaffold(
      backgroundColor: const Color(0xFFFAFAFA),
      body: SafeArea(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // Season & Title Header
            Container(
              padding: const EdgeInsets.fromLTRB(16, 20, 16, 12),
              color: Colors.white,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Row(
                        children: [
                          const Icon(Icons.emoji_nature, color: Color(0xFFFFB300), size: 24),
                          const SizedBox(width: 6),
                          Text(
                            _currentSeason?.seasonName ?? '시즌 랭킹',
                            style: const TextStyle(
                              fontSize: 20,
                              fontWeight: FontWeight.w900,
                              color: Colors.black87,
                            ),
                          ),
                        ],
                      ),
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                        decoration: BoxDecoration(
                          color: const Color(0xFFFFB300).withOpacity(0.1),
                          borderRadius: BorderRadius.circular(12),
                        ),
                        child: Text(
                          _currentSeason != null ? '실시간 랭킹' : '시즌 준비중',
                          style: const TextStyle(
                            fontSize: 12,
                            color: Color(0xFFFF8F00),
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 6),
                  Text(
                    _currentSeason != null
                        ? '${_currentSeason!.startAt.month}월 ${_currentSeason!.startAt.day}일 ~ ${_currentSeason!.endAt.month}월 ${_currentSeason!.endAt.day}일 집계'
                        : '종합 랭킹 집계 중입니다.',
                    style: const TextStyle(
                      fontSize: 12,
                      color: Colors.black45,
                    ),
                  ),
                ],
              ),
            ),

            // Region Selector Bar
            InkWell(
              onTap: _showRegionSelectionBottomSheet,
              child: Container(
                padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
                decoration: const BoxDecoration(
                  color: Colors.white,
                  border: Border(
                    top: BorderSide(color: Color(0xFFF0F0F0)),
                    bottom: BorderSide(color: Color(0xFFE5E5E5)),
                  ),
                ),
                child: Row(
                  children: [
                    const Icon(Icons.location_on, color: Color(0xFFFF8F00), size: 20),
                    const SizedBox(width: 8),
                    Expanded(
                      child: Text(
                        displayedRegionName.isEmpty ? '지역을 로딩 중입니다...' : displayedRegionName,
                        style: const TextStyle(
                          fontSize: 15,
                          fontWeight: FontWeight.bold,
                          color: Colors.black87,
                        ),
                      ),
                    ),
                    const Text(
                      '지역 변경',
                      style: TextStyle(
                        fontSize: 13,
                        color: Color(0xFFFF8F00),
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                    const Icon(Icons.chevron_right, color: Color(0xFFFF8F00), size: 16),
                  ],
                ),
              ),
            ),

            // Region Type Selector Tabs (Dong / District / City)
            Container(
              color: Colors.white,
              padding: const EdgeInsets.symmetric(vertical: 8, horizontal: 16),
              child: Row(
                children: [
                  _buildTabButton('읍면동', 'dong'),
                  const SizedBox(width: 8),
                  _buildTabButton('시군구', 'district'),
                  const SizedBox(width: 8),
                  _buildTabButton('시도', 'city'),
                ],
              ),
            ),

            const SizedBox(height: 8),

            // Main Content: Ranking List / Loader / Empty State
            Expanded(
              child: _isLoadingRanking || _isLoadingSeason
                  ? const Center(
                      child: CircularProgressIndicator(
                        valueColor: AlwaysStoppedAnimation<Color>(Color(0xFFFFB300)),
                      ),
                    )
                  : _rankingResponse == null || _rankingResponse!.items.isEmpty
                      ? _buildEmptyState()
                      : RefreshIndicator(
                          onRefresh: _loadRankings,
                          color: const Color(0xFFFFB300),
                          child: ListView.builder(
                            physics: const AlwaysScrollableScrollPhysics(),
                            padding: const EdgeInsets.all(16),
                            itemCount: _rankingResponse!.items.length,
                            itemBuilder: (context, index) {
                              final item = _rankingResponse!.items[index];
                              return _buildRankingCard(item);
                            },
                          ),
                        ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildTabButton(String label, String value) {
    final isSelected = _regionType == value;
    return Expanded(
      child: GestureDetector(
        onTap: () {
          if (!isSelected) {
            setState(() {
              _regionType = value;
            });
            _loadRankings();
          }
        },
        child: Container(
          padding: const EdgeInsets.symmetric(vertical: 10),
          decoration: BoxDecoration(
            color: isSelected ? const Color(0xFFFFB300) : const Color(0xFFF5F5F5),
            borderRadius: BorderRadius.circular(12),
          ),
          child: Text(
            label,
            textAlign: TextAlign.center,
            style: TextStyle(
              fontSize: 14,
              fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
              color: isSelected ? Colors.white : Colors.black54,
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildEmptyState() {
    return SingleChildScrollView(
      physics: const AlwaysScrollableScrollPhysics(),
      child: Container(
        height: MediaQuery.of(context).size.height * 0.5,
        alignment: Alignment.center,
        padding: const EdgeInsets.symmetric(horizontal: 24.0),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(Icons.leaderboard_outlined, size: 64, color: Colors.black12),
            const SizedBox(height: 16),
            const Text(
              '랭킹 정보가 없습니다',
              style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: Colors.black87),
            ),
            const SizedBox(height: 8),
            const Text(
              '이 지역에서 아직 수집된 맛집 데이터가 없거나\n새로운 시즌 랭킹 계산 대기 중입니다.',
              textAlign: TextAlign.center,
              style: TextStyle(fontSize: 13, color: Colors.black45, height: 1.4),
            ),
            const SizedBox(height: 24),
            ElevatedButton.icon(
              onPressed: _initData,
              icon: const Icon(Icons.refresh),
              label: const Text('다시 시도'),
              style: ElevatedButton.styleFrom(
                backgroundColor: const Color(0xFFFFB300),
                foregroundColor: Colors.white,
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildRankingCard(PlaceRankingItem item) {
    // Rank styling config
    Color rankBadgeColor = Colors.grey.shade400;
    Color rankTextColor = Colors.white;
    double cardElevation = 0;

    if (item.rank == 1) {
      rankBadgeColor = const Color(0xFFFFD700); // Gold
      cardElevation = 2;
    } else if (item.rank == 2) {
      rankBadgeColor = const Color(0xFFC0C0C0); // Silver
      cardElevation = 1;
    } else if (item.rank == 3) {
      rankBadgeColor = const Color(0xFFCD7F32); // Bronze
      cardElevation = 0.5;
    }

    return Card(
      elevation: cardElevation,
      margin: const EdgeInsets.only(bottom: 12),
      color: Colors.white,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
        side: BorderSide(
          color: item.rank <= 3 ? rankBadgeColor.withOpacity(0.5) : Colors.black12,
          width: item.rank <= 3 ? 1.5 : 1,
        ),
      ),
      child: InkWell(
        onTap: () {
          Navigator.of(context).push(
            MaterialPageRoute(
              builder: (context) => PlaceDetailScreen(placeId: item.placeId),
            ),
          );
        },
        borderRadius: BorderRadius.circular(16),
        child: Padding(
          padding: const EdgeInsets.all(16.0),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Rank Badge
              Container(
                width: 32,
                height: 32,
                decoration: BoxDecoration(
                  color: rankBadgeColor,
                  shape: BoxShape.circle,
                  boxShadow: item.rank <= 3
                      ? [
                          BoxShadow(
                            color: rankBadgeColor.withOpacity(0.3),
                            blurRadius: 4,
                            offset: const Offset(0, 2),
                          )
                        ]
                      : null,
                ),
                alignment: Alignment.center,
                child: Text(
                  '${item.rank}',
                  style: TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.bold,
                    color: rankTextColor,
                  ),
                ),
              ),
              const SizedBox(width: 16),

              // Restaurant Info
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      item.name,
                      style: const TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.bold,
                        color: Colors.black87,
                      ),
                    ),
                    const SizedBox(height: 6),

                    // Stars & Score
                    Row(
                      children: [
                        if (item.starLevel > 0) ...[
                          Row(
                            children: List.generate(
                              item.starLevel,
                              (i) => const Icon(Icons.star, color: Color(0xFFFFB300), size: 16),
                            ),
                          ),
                          const SizedBox(width: 8),
                        ],
                        Text(
                          '${item.totalScore.toStringAsFixed(1)}점',
                          style: const TextStyle(
                            fontSize: 13,
                            color: Color(0xFFFF8F00),
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 8),

                    // Demographic tags (audienceTags)
                    if (item.audienceTags.isNotEmpty)
                      Wrap(
                        spacing: 6,
                        runSpacing: 4,
                        children: item.audienceTags.map((tag) {
                          return Container(
                            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                            decoration: BoxDecoration(
                              color: const Color(0xFFFAFAFA),
                              border: Border.all(color: Colors.black12),
                              borderRadius: BorderRadius.circular(20),
                            ),
                            child: Text(
                              tag,
                              style: const TextStyle(
                                fontSize: 11,
                                color: Colors.black54,
                              ),
                            ),
                          );
                        }).toList(),
                      ),
                  ],
                ),
              ),

              // Arrow Icon
              const Align(
                alignment: Alignment.center,
                child: Icon(Icons.chevron_right, color: Colors.black26),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

// Region selector bottom sheet logic
class _RegionSelectorBottomSheet extends StatefulWidget {
  final RankingService rankingService;
  final int? initialCityId;
  final int? initialDistrictId;
  final int? initialDongId;
  final Function(RegionCity, RegionDistrict, RegionDong) onRegionSelected;

  const _RegionSelectorBottomSheet({
    required this.rankingService,
    this.initialCityId,
    this.initialDistrictId,
    this.initialDongId,
    required this.onRegionSelected,
  });

  @override
  State<_RegionSelectorBottomSheet> createState() => _RegionSelectorBottomSheetState();
}

class _RegionSelectorBottomSheetState extends State<_RegionSelectorBottomSheet> {
  List<RegionCity> _cities = [];
  List<RegionDistrict> _districts = [];
  List<RegionDong> _dongs = [];

  RegionCity? _selectedCity;
  RegionDistrict? _selectedDistrict;
  RegionDong? _selectedDong;

  bool _isLoadingCities = true;
  bool _isLoadingDistricts = false;
  bool _isLoadingDongs = false;

  @override
  void initState() {
    super.initState();
    _loadCities();
  }

  // Load cities list
  Future<void> _loadCities() async {
    setState(() => _isLoadingCities = true);
    final citiesList = await widget.rankingService.getCities();
    setState(() {
      _cities = citiesList;
      _isLoadingCities = false;

      // Select initial city if provided
      if (widget.initialCityId != null && _cities.isNotEmpty) {
        try {
          _selectedCity = _cities.firstWhere((c) => c.id == widget.initialCityId);
          _loadDistricts(_selectedCity!.id);
        } catch (_) {}
      }
    });
  }

  // Load districts list based on cityId
  Future<void> _loadDistricts(int cityId) async {
    setState(() {
      _isLoadingDistricts = true;
      _districts = [];
      _dongs = [];
      _selectedDistrict = null;
      _selectedDong = null;
    });

    final districtsList = await widget.rankingService.getDistricts(cityId);
    setState(() {
      _districts = districtsList;
      _isLoadingDistricts = false;

      // Select initial district if provided
      if (widget.initialDistrictId != null && _districts.isNotEmpty) {
        try {
          _selectedDistrict = _districts.firstWhere((d) => d.id == widget.initialDistrictId);
          _loadDongs(_selectedDistrict!.id);
        } catch (_) {}
      }
    });
  }

  // Load dongs list based on districtId
  Future<void> _loadDongs(int districtId) async {
    setState(() {
      _isLoadingDongs = true;
      _dongs = [];
      _selectedDong = null;
    });

    final dongsList = await widget.rankingService.getDongs(districtId);
    setState(() {
      _dongs = dongsList;
      _isLoadingDongs = false;

      // Select initial dong if provided
      if (widget.initialDongId != null && _dongs.isNotEmpty) {
        try {
          _selectedDong = _dongs.firstWhere((d) => d.id == widget.initialDongId);
        } catch (_) {}
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.fromLTRB(
        20,
        20,
        20,
        MediaQuery.of(context).viewInsets.bottom + 30,
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              const Text(
                '랭킹 조회 지역 변경',
                style: TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                  color: Colors.black87,
                ),
              ),
              IconButton(
                onPressed: () => Navigator.pop(context),
                icon: const Icon(Icons.close),
              ),
            ],
          ),
          const SizedBox(height: 16),

          // Dropdown 1: 시/도 (City)
          _isLoadingCities
              ? const Center(child: LinearProgressIndicator(color: Color(0xFFFFB300)))
              : DropdownButtonFormField<RegionCity>(
                  value: _selectedCity,
                  decoration: InputDecoration(
                    labelText: '시/도 선택',
                    border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
                  ),
                  items: _cities.map((city) {
                    return DropdownMenuItem<RegionCity>(
                      value: city,
                      child: Text(city.nameKo),
                    );
                  }).toList(),
                  onChanged: (city) {
                    if (city != null) {
                      setState(() {
                        _selectedCity = city;
                      });
                      _loadDistricts(city.id);
                    }
                  },
                ),
          const SizedBox(height: 16),

          // Dropdown 2: 시/군/구 (District)
          _isLoadingDistricts
              ? const Center(child: LinearProgressIndicator(color: Color(0xFFFFB300)))
              : DropdownButtonFormField<RegionDistrict>(
                  value: _selectedDistrict,
                  decoration: InputDecoration(
                    labelText: '시/군/구 선택',
                    border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
                  ),
                  items: _districts.map((district) {
                    return DropdownMenuItem<RegionDistrict>(
                      value: district,
                      child: Text(district.nameKo),
                    );
                  }).toList(),
                  onChanged: _selectedCity == null
                      ? null
                      : (district) {
                          if (district != null) {
                            setState(() {
                              _selectedDistrict = district;
                            });
                            _loadDongs(district.id);
                          }
                        },
                ),
          const SizedBox(height: 16),

          // Dropdown 3: 읍/면/동 (Dong)
          _isLoadingDongs
              ? const Center(child: LinearProgressIndicator(color: Color(0xFFFFB300)))
              : DropdownButtonFormField<RegionDong>(
                  value: _selectedDong,
                  decoration: InputDecoration(
                    labelText: '동/읍/면 선택',
                    border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
                  ),
                  items: _dongs.map((dong) {
                    return DropdownMenuItem<RegionDong>(
                      value: dong,
                      child: Text(dong.nameKo),
                    );
                  }).toList(),
                  onChanged: _selectedDistrict == null
                      ? null
                      : (dong) {
                          setState(() {
                            _selectedDong = dong;
                          });
                        },
                ),
          const SizedBox(height: 24),

          // Action Button: Confirm Selection
          ElevatedButton(
            onPressed: _selectedCity != null && _selectedDistrict != null && _selectedDong != null
                ? () {
                    widget.onRegionSelected(
                      _selectedCity!,
                      _selectedDistrict!,
                      _selectedDong!,
                    );
                    Navigator.pop(context);
                  }
                : null,
            style: ElevatedButton.styleFrom(
              backgroundColor: const Color(0xFFFFB300),
              foregroundColor: Colors.white,
              padding: const EdgeInsets.symmetric(vertical: 16),
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
              elevation: 0,
            ),
            child: const Text('확인', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
          ),
        ],
      ),
    );
  }
}
