import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:geolocator/geolocator.dart';
import 'package:provider/provider.dart';

import '../../../core/api/api_client.dart';
import '../../../core/config/app_config.dart';
import '../../../core/theme/app_theme.dart';
import '../../../core/widgets/app_empty_state.dart';
import '../../../models/place.dart';
import '../../../utils/localization.dart';
import '../../auth/providers/auth_provider.dart';
import '../../auth/views/login_screen.dart';
import '../../place/utils/place_category.dart';
import '../../place/services/place_service.dart';
import '../../place/views/place_detail_screen.dart';
import '../../place/views/place_register_screen.dart';
import '../widgets/home_place_card.dart';
import '../widgets/kakao_place_map.dart';

class HomeMapScreen extends StatefulWidget {
  const HomeMapScreen({super.key});

  @visibleForTesting
  static Place? syncSelectedPlaceForTesting({
    required Place? selectedPlace,
    required List<Place> visiblePlaces,
  }) {
    if (selectedPlace == null) {
      return null;
    }

    return visiblePlaces
        .where((place) => place.id == selectedPlace.id)
        .firstOrNull;
  }

  @override
  State<HomeMapScreen> createState() => _HomeMapScreenState();
}

class _CategoryFilter {
  const _CategoryFilter(this.labelKey, this.code);

  final String labelKey;
  final String? code;
}

class _HomeEmptyStateCopy {
  const _HomeEmptyStateCopy({
    required this.icon,
    required this.titleKey,
    required this.descriptionKey,
    required this.actionKey,
    required this.action,
  });

  final IconData icon;
  final String titleKey;
  final String descriptionKey;
  final String actionKey;
  final VoidCallback action;
}

class _HomeMapScreenState extends State<HomeMapScreen> {
  static const _mapOverlayAnimationDuration = Duration(milliseconds: 220);
  static const _mapOverlayAnimationCurve = Curves.easeOutCubic;

  static final _categories = [
    const _CategoryFilter('home.category.all', null),
    ...PlaceCategory.selectable.map(
      (category) => _CategoryFilter(category.translationKey, category.code),
    ),
  ];

  late final PlaceService _placeService;
  final _searchController = TextEditingController();

  Position? _currentPosition;
  Place? _selectedPlace;
  bool _isLoadingGps = false;
  bool _isLoadingPlaces = false;
  bool _isMapView = true;
  int _recenterRequestId = 0;
  String? _selectedCategoryCode;
  String? _locationMessage;
  List<Place> _places = [];

  @override
  void initState() {
    super.initState();
    _placeService =
        PlaceService(Provider.of<ApiClient>(context, listen: false));
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
    setState(() {
      _isLoadingGps = true;
      _locationMessage = null;
    });

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
      if (!mounted) return;
      setState(() {
        _currentPosition = null;
        _selectedPlace = null;
        _isLoadingGps = false;
        _locationMessage = error.toString();
      });
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
      final position = _currentPosition;
      if (position != null) {
        await _loadNearbyPlaces(position);
      } else {
        setState(() {
          _places = [];
          _selectedPlace = null;
        });
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
    _selectedPlace = HomeMapScreen.syncSelectedPlaceForTesting(
      selectedPlace: _selectedPlace,
      visiblePlaces: _getFilteredPlaces(),
    );
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
          if (_isMapView && _locationMessage != null)
            Positioned(
              top: 126,
              left: 16,
              right: 16,
              child: _buildLocationNotice(_locationMessage!),
            ),
          AnimatedPositioned(
            key: const ValueKey('home-map-floating-actions'),
            duration: _mapOverlayAnimationDuration,
            curve: _mapOverlayAnimationCurve,
            right: 16,
            bottom: _isMapView && selectedPlace != null ? 132 : 20,
            child: _buildFloatingActions(),
          ),
          if (_isMapView)
            Positioned(
              left: 16,
              right: 16,
              bottom: 16,
              child: AnimatedSwitcher(
                key: const ValueKey('home-map-selected-card-switcher'),
                duration: _mapOverlayAnimationDuration,
                reverseDuration: const Duration(milliseconds: 160),
                switchInCurve: _mapOverlayAnimationCurve,
                switchOutCurve: Curves.easeInCubic,
                transitionBuilder: (child, animation) {
                  final offsetAnimation = Tween<Offset>(
                    begin: const Offset(0, 0.08),
                    end: Offset.zero,
                  ).animate(animation);

                  return FadeTransition(
                    opacity: animation,
                    child: SlideTransition(
                      position: offsetAnimation,
                      child: child,
                    ),
                  );
                },
                child: selectedPlace == null
                    ? const SizedBox.shrink(key: ValueKey('empty-place-card'))
                    : _buildSelectedPlaceCard(selectedPlace),
              ),
            ),
          if (_isMapView && (_isLoadingGps || _isLoadingPlaces))
            Positioned(
              left: 0,
              right: 0,
              bottom: 28,
              child: Center(
                child: _buildMapStatusPill(
                  _isLoadingGps
                      ? 'home.locatingTitle'.tr
                      : 'home.loadingNearbyTitle'.tr,
                ),
              ),
            ),
        ],
      ),
    );
  }

  Widget _buildSearchBar() {
    return Material(
      color: AppColors.surface,
      elevation: 3,
      shadowColor: AppColors.ink.withValues(alpha: 0.10),
      borderRadius: BorderRadius.circular(AppRadius.pill),
      child: TextField(
        controller: _searchController,
        textInputAction: TextInputAction.search,
        onSubmitted: (_) => _search(),
        decoration: InputDecoration(
          hintText: 'home.searchHint'.tr,
          hintStyle: const TextStyle(fontSize: 14, color: AppColors.muted),
          prefixIcon: const Icon(Icons.search, color: AppColors.honey),
          suffixIcon: _searchController.text.isNotEmpty
              ? IconButton(
                  icon: const Icon(Icons.clear, size: 20),
                  color: AppColors.muted,
                  tooltip: 'home.clearSearch'.tr,
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
            child: FilterChip(
              label: Text(category.labelKey.tr),
              selected: isSelected,
              onSelected: (_) {
                setState(() {
                  _selectedCategoryCode = category.code;
                  _syncSelectedPlace();
                });
              },
              selectedColor: AppColors.honey,
              checkmarkColor: Colors.white,
              labelStyle: TextStyle(
                color: isSelected ? Colors.white : AppColors.ink,
                fontWeight: isSelected ? FontWeight.w800 : FontWeight.w700,
              ),
              backgroundColor: AppColors.surface,
              side: const BorderSide(color: AppColors.outline),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(AppRadius.pill),
              ),
            ),
          );
        },
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
          tooltip: 'home.registerPlace'.tr,
          child: const Icon(Icons.add_location_alt_outlined),
        ),
        const SizedBox(height: 10),
        FloatingActionButton.small(
          heroTag: 'gps_btn',
          onPressed: _determinePosition,
          backgroundColor: AppColors.surface,
          foregroundColor: AppColors.honey,
          tooltip: 'home.currentLocation'.tr,
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
          tooltip: _isMapView ? 'home.listView'.tr : 'home.mapView'.tr,
          child: Icon(_isMapView ? Icons.list : Icons.map),
        ),
      ],
    );
  }

  Widget _buildMapView(List<Place> places) {
    if (!AppConfig.isKakaoNativeConfigured) {
      return _buildMapConfigurationState();
    }

    return KakaoPlaceMap(
      places: places,
      currentPosition: _currentPosition,
      selectedPlaceId: _selectedPlace?.id,
      recenterRequestId: _recenterRequestId,
      onPlaceSelected: (place) {
        setState(() => _selectedPlace = place);
      },
    );
  }

  Widget _buildMapConfigurationState() {
    return ColoredBox(
      color: AppColors.background,
      child: AppEmptyState(
        icon: Icons.map_outlined,
        title: 'home.mapConfigurationTitle'.tr,
        description: 'home.mapConfigurationDescription'.tr,
      ),
    );
  }

  Widget _buildLocationNotice(String message) {
    return Material(
      color: AppColors.surface,
      elevation: 2,
      shadowColor: AppColors.ink.withValues(alpha: 0.12),
      borderRadius: BorderRadius.circular(AppRadius.md),
      child: Padding(
        padding: const EdgeInsets.all(AppSpacing.sm),
        child: Row(
          children: [
            const Icon(Icons.location_off_outlined, color: AppColors.nectar),
            const SizedBox(width: AppSpacing.xs),
            Expanded(
              child: Text(
                '$message\n${'home.locationRetryHint'.tr}',
                style: const TextStyle(
                  color: AppColors.ink,
                  fontSize: 12,
                  height: 1.35,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildMapStatusPill(String label) {
    return Material(
      color: AppColors.surface,
      elevation: 2,
      shadowColor: AppColors.ink.withValues(alpha: 0.12),
      borderRadius: BorderRadius.circular(AppRadius.pill),
      child: Padding(
        padding: const EdgeInsets.symmetric(
          horizontal: AppSpacing.sm,
          vertical: AppSpacing.xs,
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            const SizedBox(
              width: 16,
              height: 16,
              child: CircularProgressIndicator(
                strokeWidth: 2,
                valueColor: AlwaysStoppedAnimation(AppColors.honey),
              ),
            ),
            const SizedBox(width: AppSpacing.xs),
            Text(
              label,
              style: const TextStyle(
                color: AppColors.ink,
                fontSize: 12,
                fontWeight: FontWeight.w700,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildListView(List<Place> places) {
    if (_isLoadingGps) {
      return _buildProgressListState(
        title: 'home.locatingTitle'.tr,
        description: 'home.locatingDescription'.tr,
      );
    }

    if (_isLoadingPlaces) {
      return _buildProgressListState(
        title: 'home.loadingNearbyTitle'.tr,
        description: 'home.loadingNearbyDescription'.tr,
      );
    }

    if (places.isEmpty) {
      final emptyState = _emptyStateCopy();
      return AppEmptyState(
        icon: emptyState.icon,
        title: emptyState.titleKey.tr,
        description: emptyState.descriptionKey.tr,
        actionLabel: emptyState.actionKey.tr,
        onActionPressed: emptyState.action,
      );
    }

    return ListView.builder(
      padding: const EdgeInsets.only(top: 130, left: 16, right: 16, bottom: 20),
      itemCount: places.length,
      itemBuilder: (context, index) => _buildPlaceListItem(places[index]),
    );
  }

  Widget _buildProgressListState({
    required String title,
    required String description,
  }) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(AppSpacing.lg),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const CircularProgressIndicator(
              valueColor: AlwaysStoppedAnimation(AppColors.honey),
            ),
            const SizedBox(height: AppSpacing.md),
            Text(
              title,
              style: const TextStyle(
                color: AppColors.ink,
                fontSize: 18,
                fontWeight: FontWeight.w800,
              ),
            ),
            const SizedBox(height: AppSpacing.xs),
            Text(
              description,
              textAlign: TextAlign.center,
              style: const TextStyle(
                color: AppColors.muted,
                height: 1.4,
              ),
            ),
          ],
        ),
      ),
    );
  }

  _HomeEmptyStateCopy _emptyStateCopy() {
    final currentPosition = _currentPosition;
    if (_locationMessage != null && currentPosition == null) {
      return _HomeEmptyStateCopy(
        icon: Icons.location_off_outlined,
        titleKey: 'home.permissionEmptyTitle',
        descriptionKey: 'home.permissionEmptyDescription',
        actionKey: 'home.findByLocation',
        action: _determinePosition,
      );
    }

    if (_searchController.text.trim().isNotEmpty) {
      return _HomeEmptyStateCopy(
        icon: Icons.search_off_rounded,
        titleKey: 'home.searchEmptyTitle',
        descriptionKey: 'home.searchEmptyDescription',
        actionKey: 'home.reload',
        action: _search,
      );
    }

    if (_selectedCategoryCode != null && currentPosition != null) {
      return _HomeEmptyStateCopy(
        icon: Icons.filter_alt_off_outlined,
        titleKey: 'home.filterEmptyTitle',
        descriptionKey: 'home.filterEmptyDescription',
        actionKey: 'home.reload',
        action: () => _loadNearbyPlaces(currentPosition!),
      );
    }

    if (currentPosition != null) {
      return _HomeEmptyStateCopy(
        icon: Icons.explore_off_outlined,
        titleKey: 'home.nearbyEmptyTitle',
        descriptionKey: 'home.nearbyEmptyDescription',
        actionKey: 'home.reload',
        action: () => _loadNearbyPlaces(currentPosition),
      );
    }

    return _HomeEmptyStateCopy(
      icon: Icons.my_location_outlined,
      titleKey: 'home.emptyTitle',
      descriptionKey: 'home.emptyNoLocation',
      actionKey: 'home.findByLocation',
      action: _determinePosition,
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
      key: ValueKey('selected-place-card-${place.id}'),
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
    return PlaceCategory.labelFor(categoryCode);
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
        title: Text(
          'home.loginRequiredTitle'.tr,
          style: TextStyle(fontWeight: FontWeight.w800),
        ),
        content: Text(
          'home.loginRequiredDescription'.tr,
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: Text('common.cancel'.tr),
          ),
          FilledButton(
            onPressed: () {
              Navigator.pop(context);
              Navigator.of(context).push(
                MaterialPageRoute(builder: (context) => const LoginScreen()),
              );
            },
            child: Text('home.login'.tr),
          ),
        ],
      ),
    );
  }
}
