import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../../../core/theme/app_theme.dart';
import '../../../../core/widgets/app_empty_state.dart';
import '../../../../utils/localization.dart';
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

class _RankingScreenState extends State<RankingScreen>
    with SingleTickerProviderStateMixin {
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

    final authProvider = Provider.of<AuthProvider>(context, listen: false);

    // 1. Fetch current season
    final season = await _rankingService.getCurrentSeason();
    if (!mounted) return;
    setState(() {
      _currentSeason = season;
      _isLoadingSeason = false;
    });

    // 2. Fetch user primary region if authenticated
    Map<String, dynamic>? primaryRegion;
    if (authProvider.isAuthenticated) {
      primaryRegion = await _placeService.getMyRegion();
      if (!mounted) return;
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
      if (!mounted) return;
      if (cities.isNotEmpty) {
        final defaultCity = cities.first;
        _cityId = defaultCity.id;
        _cityName = defaultCity.nameKo;

        final districts = await _rankingService.getDistricts(defaultCity.id);
        if (!mounted) return;
        if (districts.isNotEmpty) {
          final defaultDistrict = districts.first;
          _districtId = defaultDistrict.id;
          _districtName = defaultDistrict.nameKo;

          final dongs = await _rankingService.getDongs(defaultDistrict.id);
          if (!mounted) return;
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
      final rankingResponse = await _rankingService.getPlaceRankings(
        regionType: _regionType,
        regionId: activeRegionId,
        seasonCode: _currentSeason?.seasonCode,
      );
      if (!mounted) return;
      _rankingResponse = rankingResponse;
    } else {
      _rankingResponse = null;
    }

    if (!mounted) return;
    setState(() {
      _isLoadingRanking = false;
    });
  }

  // Show BottomSheet to select city -> district -> dong manually
  void _showRegionSelectionBottomSheet() {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: AppColors.surface,
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
    final displayedRegionName = _regionType == 'dong'
        ? '$_cityName $_districtName $_dongName'
        : _regionType == 'district'
            ? '$_cityName $_districtName'
            : _cityName;

    return Scaffold(
      backgroundColor: AppColors.background,
      body: SafeArea(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // Season & Title Header
            Container(
              padding: const EdgeInsets.fromLTRB(16, 20, 16, 12),
              color: AppColors.surface,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Expanded(
                        child: Row(
                          children: [
                            const Icon(Icons.emoji_nature,
                                color: AppColors.honey, size: 24),
                            const SizedBox(width: 6),
                            Expanded(
                              child: Text(
                                _currentSeason?.seasonName ??
                                    'ranking.seasonRanking'.tr,
                                style: const TextStyle(
                                  fontSize: 18,
                                  fontWeight: FontWeight.w900,
                                  color: AppColors.ink,
                                ),
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                              ),
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(width: 8),
                      Container(
                        padding: const EdgeInsets.symmetric(
                            horizontal: 10, vertical: 4),
                        decoration: BoxDecoration(
                          color: AppColors.surfaceWarm,
                          borderRadius: BorderRadius.circular(AppRadius.md),
                        ),
                        child: Text(
                          _currentSeason != null
                              ? 'ranking.liveRanking'.tr
                              : 'ranking.seasonPreparing'.tr,
                          style: const TextStyle(
                            fontSize: 12,
                            color: AppColors.nectar,
                            fontWeight: FontWeight.w800,
                          ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 6),
                  Text(
                    _currentSeason != null
                        ? '${_currentSeason!.startAt.month}월 ${_currentSeason!.startAt.day}일 ~ ${_currentSeason!.endAt.month}월 ${_currentSeason!.endAt.day}일 집계'
                        : 'ranking.seasonLoading'.tr,
                    style: const TextStyle(
                      fontSize: 12,
                      color: AppColors.muted,
                    ),
                  ),
                ],
              ),
            ),

            // Region Selector Bar
            InkWell(
              onTap: _showRegionSelectionBottomSheet,
              child: Container(
                padding:
                    const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
                decoration: const BoxDecoration(
                  color: AppColors.surface,
                  border: Border(
                    top: BorderSide(color: AppColors.outline),
                    bottom: BorderSide(color: AppColors.outline),
                  ),
                ),
                child: Row(
                  children: [
                    const Icon(Icons.location_on,
                        color: AppColors.nectar, size: 20),
                    const SizedBox(width: 8),
                    Expanded(
                      child: Text(
                        displayedRegionName.isEmpty
                            ? 'ranking.loadingRegion'.tr
                            : displayedRegionName,
                        style: const TextStyle(
                          fontSize: 15,
                          fontWeight: FontWeight.bold,
                          color: AppColors.ink,
                        ),
                      ),
                    ),
                    Text(
                      'ranking.changeRegion'.tr,
                      style: const TextStyle(
                        fontSize: 13,
                        color: AppColors.nectar,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                    const Icon(Icons.chevron_right,
                        color: AppColors.nectar, size: 16),
                  ],
                ),
              ),
            ),

            // Region Type Selector Tabs (Dong / District / City)
            Container(
              color: AppColors.surface,
              padding: const EdgeInsets.symmetric(vertical: 8, horizontal: 16),
              child: Row(
                children: [
                  _buildTabButton('ranking.tabDong'.tr, 'dong'),
                  const SizedBox(width: 8),
                  _buildTabButton('ranking.tabDistrict'.tr, 'district'),
                  const SizedBox(width: 8),
                  _buildTabButton('ranking.tabCity'.tr, 'city'),
                ],
              ),
            ),

            const SizedBox(height: 8),

            // Main Content: Ranking List / Loader / Empty State
            Expanded(
              child: _isLoadingRanking || _isLoadingSeason
                  ? const Center(
                      child: CircularProgressIndicator(
                        valueColor:
                            AlwaysStoppedAnimation<Color>(AppColors.honey),
                      ),
                    )
                  : _rankingResponse == null || _rankingResponse!.items.isEmpty
                      ? _buildEmptyState()
                      : RefreshIndicator(
                          onRefresh: _loadRankings,
                          color: AppColors.honey,
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
            color: isSelected ? AppColors.honey : AppColors.surface,
            borderRadius: BorderRadius.circular(AppRadius.md),
            border: Border.all(color: AppColors.outline),
          ),
          child: Text(
            label,
            textAlign: TextAlign.center,
            style: TextStyle(
              fontSize: 14,
              fontWeight: isSelected ? FontWeight.w800 : FontWeight.w700,
              color: isSelected ? Colors.white : AppColors.muted,
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
            AppEmptyState(
              icon: Icons.leaderboard_outlined,
              title: 'ranking.emptyTitle'.tr,
              description: 'ranking.emptyDesc'.tr,
            ),
            const SizedBox(height: AppSpacing.sm),
            FilledButton.icon(
              onPressed: _initData,
              icon: const Icon(Icons.refresh),
              label: Text('ranking.retry'.tr),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildRankingCard(PlaceRankingItem item) {
    // Rank styling config
    Color rankBadgeColor = AppColors.muted;
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
      color: AppColors.surface,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(AppRadius.lg),
        side: BorderSide(
          color: item.rank <= 3
              ? rankBadgeColor.withValues(alpha: 0.5)
              : AppColors.outline,
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
        borderRadius: BorderRadius.circular(AppRadius.lg),
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
                            color: rankBadgeColor.withValues(alpha: 0.3),
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
                        color: AppColors.ink,
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
                              (i) => const Icon(Icons.star,
                                  color: AppColors.honey, size: 16),
                            ),
                          ),
                          const SizedBox(width: 8),
                        ],
                        Text(
                          '${item.totalScore.toStringAsFixed(1)}점',
                          style: const TextStyle(
                            fontSize: 13,
                            color: AppColors.nectar,
                            fontWeight: FontWeight.w800,
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
                            padding: const EdgeInsets.symmetric(
                                horizontal: 8, vertical: 4),
                            decoration: BoxDecoration(
                              color: AppColors.background,
                              border: Border.all(color: AppColors.outline),
                              borderRadius:
                                  BorderRadius.circular(AppRadius.pill),
                            ),
                            child: Text(
                              tag,
                              style: const TextStyle(
                                fontSize: 11,
                                color: AppColors.muted,
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
                child: Icon(Icons.chevron_right, color: AppColors.muted),
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
  State<_RegionSelectorBottomSheet> createState() =>
      _RegionSelectorBottomSheetState();
}

class _RegionSelectorBottomSheetState
    extends State<_RegionSelectorBottomSheet> {
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
    if (!mounted) return;
    setState(() {
      _cities = citiesList;
      _isLoadingCities = false;

      // Select initial city if provided
      if (widget.initialCityId != null && _cities.isNotEmpty) {
        try {
          _selectedCity =
              _cities.firstWhere((c) => c.id == widget.initialCityId);
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
    if (!mounted) return;
    setState(() {
      _districts = districtsList;
      _isLoadingDistricts = false;

      // Select initial district if provided
      if (widget.initialDistrictId != null && _districts.isNotEmpty) {
        try {
          _selectedDistrict =
              _districts.firstWhere((d) => d.id == widget.initialDistrictId);
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
    if (!mounted) return;
    setState(() {
      _dongs = dongsList;
      _isLoadingDongs = false;

      // Select initial dong if provided
      if (widget.initialDongId != null && _dongs.isNotEmpty) {
        try {
          _selectedDong =
              _dongs.firstWhere((d) => d.id == widget.initialDongId);
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
              Text(
                'ranking.changePopupTitle'.tr,
                style: const TextStyle(
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
              ? const Center(
                  child: LinearProgressIndicator(color: Color(0xFFFFB300)))
              : DropdownButtonFormField<RegionCity>(
                  initialValue: _selectedCity,
                  decoration: InputDecoration(
                    labelText: 'ranking.selectCity'.tr,
                    border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(12)),
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
              ? const Center(
                  child: LinearProgressIndicator(color: Color(0xFFFFB300)))
              : DropdownButtonFormField<RegionDistrict>(
                  initialValue: _selectedDistrict,
                  decoration: InputDecoration(
                    labelText: 'ranking.selectDistrict'.tr,
                    border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(12)),
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
              ? const Center(
                  child: LinearProgressIndicator(color: Color(0xFFFFB300)))
              : DropdownButtonFormField<RegionDong>(
                  initialValue: _selectedDong,
                  decoration: InputDecoration(
                    labelText: 'ranking.selectDong'.tr,
                    border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(12)),
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
            onPressed: _selectedCity != null &&
                    _selectedDistrict != null &&
                    _selectedDong != null
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
              shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(12)),
              elevation: 0,
            ),
            child: Text('common.confirm'.tr,
                style:
                    const TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
          ),
        ],
      ),
    );
  }
}
