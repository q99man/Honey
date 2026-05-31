import 'dart:async';
import 'dart:typed_data';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:geolocator/geolocator.dart';
import 'package:kakao_maps_flutter/kakao_maps_flutter.dart' as kakao_map;

import '../../../core/config/app_config.dart';
import '../../../models/place.dart';
import '../../../utils/localization.dart';
import '../../place/utils/place_category.dart';
import 'home_map_marker_asset.dart';

class KakaoPlaceMap extends StatefulWidget {
  const KakaoPlaceMap({
    super.key,
    required this.places,
    required this.currentPosition,
    required this.selectedPlaceId,
    required this.recenterRequestId,
    required this.onPlaceSelected,
  });

  final List<Place> places;
  final Position? currentPosition;
  final int? selectedPlaceId;
  final int recenterRequestId;
  final ValueChanged<Place> onPlaceSelected;

  @visibleForTesting
  static kakao_map.LatLng bestCenterForTesting({
    required List<Place> places,
    required int? selectedPlaceId,
    required Position? currentPosition,
    required bool preferCurrentPosition,
  }) {
    if (preferCurrentPosition && currentPosition != null) {
      return kakao_map.LatLng(
        latitude: currentPosition.latitude,
        longitude: currentPosition.longitude,
      );
    }

    final selectedPlace = places
        .where(
          (place) => place.id == selectedPlaceId && _hasValidCoordinate(place),
        )
        .firstOrNull;
    if (selectedPlace != null) {
      return kakao_map.LatLng(
        latitude: selectedPlace.latitude,
        longitude: selectedPlace.longitude,
      );
    }

    final firstPlace = places.where(_hasValidCoordinate).firstOrNull;
    if (firstPlace != null) {
      return kakao_map.LatLng(
        latitude: firstPlace.latitude,
        longitude: firstPlace.longitude,
      );
    }

    if (currentPosition != null) {
      return kakao_map.LatLng(
        latitude: currentPosition.latitude,
        longitude: currentPosition.longitude,
      );
    }

    return kakao_map.LatLng(
      latitude: AppConfig.defaultMapLatitude,
      longitude: AppConfig.defaultMapLongitude,
    );
  }

  static bool _hasValidCoordinate(Place place) {
    return place.latitude != 0.0 && place.longitude != 0.0;
  }

  @visibleForTesting
  static kakao_map.LatLng bestInitialCenterForTesting({
    required List<Place> places,
    required int? selectedPlaceId,
    required Position? currentPosition,
  }) {
    return bestCenterForTesting(
      places: places,
      selectedPlaceId: selectedPlaceId,
      currentPosition: currentPosition,
      preferCurrentPosition: false,
    );
  }

  @visibleForTesting
  static bool shouldPreferCurrentPositionOnMapCreatedForTesting({
    required bool hasVisiblePlaces,
    required bool hasSelectedPlace,
    required bool hasCurrentPosition,
  }) {
    return hasCurrentPosition && !hasVisiblePlaces && !hasSelectedPlace;
  }

  @visibleForTesting
  static bool shouldMoveCameraForTesting({
    required bool placesChanged,
    required bool recenterRequested,
    required bool positionChanged,
    required int? oldSelectedPlaceId,
    required int? newSelectedPlaceId,
  }) {
    final selectedPlaceChanged = oldSelectedPlaceId != newSelectedPlaceId;
    final selectedPlaceOpened =
        selectedPlaceChanged && newSelectedPlaceId != null;

    return placesChanged ||
        recenterRequested ||
        positionChanged ||
        selectedPlaceOpened;
  }

  @override
  State<KakaoPlaceMap> createState() => _KakaoPlaceMapState();
}

class _KakaoPlaceMapState extends State<KakaoPlaceMap> {
  static const _currentMarkerStyleId = 'honey_current_marker';
  static const _markerLayerId = 'honey_marker_layer';

  kakao_map.KakaoMapController? _controller;
  StreamSubscription<dynamic>? _labelClickSubscription;
  Timer? _syncRetryTimer;
  final Set<String> _markerIds = {};
  bool _preferCurrentPositionOnNextSync = false;
  bool _markerStylesRegistered = false;
  int _syncGeneration = 0;
  int _syncRetryCount = 0;

  static const _syncRetryDelay = Duration(milliseconds: 200);
  static const _maxSyncRetryCount = 20;

  @override
  void didUpdateWidget(covariant KakaoPlaceMap oldWidget) {
    super.didUpdateWidget(oldWidget);
    final placesChanged = oldWidget.places != widget.places;
    final recenterRequested =
        oldWidget.recenterRequestId != widget.recenterRequestId;
    final selectedPlaceChanged =
        oldWidget.selectedPlaceId != widget.selectedPlaceId;
    final positionChanged = oldWidget.currentPosition?.latitude !=
            widget.currentPosition?.latitude ||
        oldWidget.currentPosition?.longitude !=
            widget.currentPosition?.longitude;

    if (placesChanged ||
        recenterRequested ||
        positionChanged ||
        selectedPlaceChanged) {
      final moveCamera = KakaoPlaceMap.shouldMoveCameraForTesting(
        placesChanged: placesChanged,
        recenterRequested: recenterRequested,
        positionChanged: positionChanged,
        oldSelectedPlaceId: oldWidget.selectedPlaceId,
        newSelectedPlaceId: widget.selectedPlaceId,
      );
      _requestSync(
        preferCurrentPosition:
            recenterRequested || (positionChanged && !placesChanged),
        moveCamera: moveCamera,
      );
    }
  }

  @override
  void dispose() {
    _syncRetryTimer?.cancel();
    _labelClickSubscription?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return kakao_map.KakaoMap(
      initialPosition: _initialCenter,
      initialLevel: 15,
      onMapCreated: (controller) {
        _controller = controller;
        _labelClickSubscription =
            controller.onLabelClickedStream.listen(_handleLabelClick);
        _requestSync(
          preferCurrentPosition: _shouldPreferCurrentPositionOnMapCreated,
          moveCamera: true,
        );
      },
    );
  }

  kakao_map.LatLng get _initialCenter {
    return KakaoPlaceMap.bestInitialCenterForTesting(
      places: widget.places,
      selectedPlaceId: widget.selectedPlaceId,
      currentPosition: widget.currentPosition,
    );
  }

  bool get _shouldPreferCurrentPositionOnMapCreated {
    return KakaoPlaceMap.shouldPreferCurrentPositionOnMapCreatedForTesting(
      hasVisiblePlaces: widget.places.any(_hasValidCoordinate),
      hasSelectedPlace: widget.selectedPlaceId != null,
      hasCurrentPosition: widget.currentPosition != null,
    );
  }

  void _requestSync({
    bool preferCurrentPosition = false,
    bool moveCamera = true,
  }) {
    if (!mounted) {
      return;
    }

    _preferCurrentPositionOnNextSync =
        _preferCurrentPositionOnNextSync || preferCurrentPosition;
    _syncRetryTimer?.cancel();

    final generation = ++_syncGeneration;
    _syncRetryTimer = Timer(_syncRetryDelay, () {
      if (!mounted || generation != _syncGeneration) {
        return;
      }

      final shouldPreferCurrentPosition = _preferCurrentPositionOnNextSync;
      _preferCurrentPositionOnNextSync = false;
      _syncMarkers(
        preferCurrentPosition: shouldPreferCurrentPosition,
        moveCamera: moveCamera,
        generation: generation,
      );
    });
  }

  Future<void> _syncMarkers({
    bool preferCurrentPosition = false,
    bool moveCamera = true,
    required int generation,
  }) async {
    final controller = _controller;
    if (controller == null) {
      return;
    }

    try {
      await _ensureMarkerLayer(controller);

      for (final markerId in _markerIds) {
        try {
          await controller.removeMarker(id: markerId, layerId: _markerLayerId);
        } catch (_) {
          // The native map may already have cleared the marker during a rebuild.
        }
      }
      _markerIds.clear();

      final currentPosition = widget.currentPosition;
      if (currentPosition != null) {
        await _addMarker(
          id: 'current_location',
          latitude: currentPosition.latitude,
          longitude: currentPosition.longitude,
          text: 'home.currentMarker'.tr,
          styleId: _currentMarkerStyleId,
          rank: 20000,
        );
      }

      for (final place in widget.places.where(_hasValidCoordinate)) {
        final selected = place.id == widget.selectedPlaceId;
        await _addMarker(
          id: _markerIdForPlace(place),
          latitude: place.latitude,
          longitude: place.longitude,
          text: place.name,
          styleId: PlaceCategory.markerStyleIdFor(
            place.categoryCode,
            selected: selected,
          ),
          rank: selected ? 40000 : 30000,
        );
      }

      if (moveCamera) {
        await _moveToBestCenter(preferCurrentPosition: preferCurrentPosition);
      }
      _syncRetryCount = 0;
    } catch (error) {
      _retrySyncAfterMapReady(
        error: error,
        preferCurrentPosition: preferCurrentPosition,
        moveCamera: moveCamera,
        generation: generation,
      );
    }
  }

  Future<void> _addMarker({
    required String id,
    required double latitude,
    required double longitude,
    required String text,
    required String styleId,
    required int rank,
  }) async {
    final controller = _controller;
    if (controller == null) {
      return;
    }

    await controller.addMarker(
      markerOption: kakao_map.MarkerOption(
        id: id,
        latLng: kakao_map.LatLng(latitude: latitude, longitude: longitude),
        styleId: styleId,
        rank: rank,
        text: text,
      ),
      layerId: _markerLayerId,
    );
    _markerIds.add(id);
  }

  Future<void> _moveToBestCenter({bool preferCurrentPosition = false}) async {
    final controller = _controller;
    if (controller == null) {
      return;
    }

    await controller.moveCamera(
      cameraUpdate: kakao_map.CameraUpdate.fromLatLng(
        _bestCenter(preferCurrentPosition: preferCurrentPosition),
      ),
    );
  }

  Future<void> _ensureMarkerLayer(
    kakao_map.KakaoMapController controller,
  ) async {
    if (!_markerStylesRegistered) {
      final placeStyles = <kakao_map.MarkerStyle>[
        _buildMarkerStyle(
          PlaceCategory.defaultMarkerStyleId,
          bytes: await HomeMapMarkerAsset.place(categoryCode: ''),
          textColor: 0xFF111111,
        ),
        _buildMarkerStyle(
          PlaceCategory.markerStyleIdFor('', selected: true),
          bytes:
              await HomeMapMarkerAsset.place(categoryCode: '', selected: true),
          textColor: 0xFF111111,
        ),
      ];
      for (final category in PlaceCategory.selectable) {
        placeStyles.add(
          _buildMarkerStyle(
            PlaceCategory.markerStyleIdFor(category.code),
            bytes: await HomeMapMarkerAsset.place(categoryCode: category.code),
            textColor: 0xFF111111,
          ),
        );
        placeStyles.add(
          _buildMarkerStyle(
            PlaceCategory.markerStyleIdFor(category.code, selected: true),
            bytes: await HomeMapMarkerAsset.place(
              categoryCode: category.code,
              selected: true,
            ),
            textColor: 0xFF111111,
          ),
        );
      }

      await controller.registerMarkerStyles(
        styles: [
          ...placeStyles,
          _buildMarkerStyle(
            _currentMarkerStyleId,
            bytes: await HomeMapMarkerAsset.currentLocation(),
            textColor: 0xFF1565C0,
          ),
        ],
      );
      _markerStylesRegistered = true;
    }

    await controller.addMarkerLayer(
      layerId: _markerLayerId,
      zOrder: 1000,
      clickable: true,
    );
  }

  kakao_map.MarkerStyle _buildMarkerStyle(
    String styleId, {
    required Uint8List bytes,
    required int textColor,
  }) {
    return kakao_map.MarkerStyle(
      styleId: styleId,
      perLevels: [
        kakao_map.MarkerPerLevelStyle.fromBytes(
          bytes: bytes,
          textStyle: kakao_map.MarkerTextStyle(
            fontSize: 22,
            fontColorArgb: textColor,
            strokeThickness: 4,
            strokeColorArgb: 0xFFFFFFFF,
          ),
          level: 6,
        ),
      ],
    );
  }

  void _retrySyncAfterMapReady({
    required Object error,
    required bool preferCurrentPosition,
    required bool moveCamera,
    required int generation,
  }) {
    if (!mounted || generation != _syncGeneration) {
      return;
    }

    if (_syncRetryCount >= _maxSyncRetryCount) {
      debugPrint('Kakao map sync failed after readiness retries: $error');
      return;
    }

    _syncRetryCount += 1;
    _preferCurrentPositionOnNextSync =
        _preferCurrentPositionOnNextSync || preferCurrentPosition;
    _syncRetryTimer?.cancel();
    _syncRetryTimer = Timer(_syncRetryDelay, () {
      if (!mounted || generation != _syncGeneration) {
        return;
      }

      final shouldPreferCurrentPosition = _preferCurrentPositionOnNextSync;
      _preferCurrentPositionOnNextSync = false;
      _syncMarkers(
        preferCurrentPosition: shouldPreferCurrentPosition,
        moveCamera: moveCamera,
        generation: generation,
      );
    });
  }

  kakao_map.LatLng _bestCenter({required bool preferCurrentPosition}) {
    return KakaoPlaceMap.bestCenterForTesting(
      places: widget.places,
      selectedPlaceId: widget.selectedPlaceId,
      currentPosition: widget.currentPosition,
      preferCurrentPosition: preferCurrentPosition,
    );
  }

  void _handleLabelClick(dynamic event) {
    final labelId = event.labelId as String?;
    if (labelId == null || !labelId.startsWith('place_')) {
      return;
    }

    final placeId = int.tryParse(labelId.substring('place_'.length));
    if (placeId == null) {
      return;
    }

    final place =
        widget.places.where((place) => place.id == placeId).firstOrNull;
    if (place != null) {
      widget.onPlaceSelected(place);
    }
  }

  static bool _hasValidCoordinate(Place place) {
    return place.latitude != 0.0 && place.longitude != 0.0;
  }

  static String _markerIdForPlace(Place place) => 'place_${place.id}';
}
