import 'dart:async';
import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:geolocator/geolocator.dart';
import 'package:kakao_maps_flutter/kakao_maps_flutter.dart' as kakao_map;

import '../../../models/place.dart';

class KakaoPlaceMap extends StatefulWidget {
  const KakaoPlaceMap({
    super.key,
    required this.places,
    required this.currentPosition,
    required this.recenterRequestId,
    required this.onPlaceSelected,
  });

  final List<Place> places;
  final Position? currentPosition;
  final int recenterRequestId;
  final ValueChanged<Place> onPlaceSelected;

  @override
  State<KakaoPlaceMap> createState() => _KakaoPlaceMapState();
}

class _KakaoPlaceMapState extends State<KakaoPlaceMap> {
  static const _fallbackCenter = kakao_map.LatLng(
    latitude: 37.556456,
    longitude: 126.924456,
  );
  static const _placeMarkerStyleId = 'honey_place_marker';
  static const _currentMarkerStyleId = 'honey_current_marker';
  static const _markerLayerId = 'honey_marker_layer';
  static const _markerPngBase64 =
      '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';

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
    final positionChanged = oldWidget.currentPosition?.latitude !=
            widget.currentPosition?.latitude ||
        oldWidget.currentPosition?.longitude !=
            widget.currentPosition?.longitude;

    if (placesChanged || recenterRequested || positionChanged) {
      _requestSync(
        preferCurrentPosition:
            recenterRequested || (positionChanged && !placesChanged),
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
        _requestSync(preferCurrentPosition: true);
      },
    );
  }

  kakao_map.LatLng get _initialCenter {
    final currentPosition = widget.currentPosition;
    if (currentPosition != null) {
      return kakao_map.LatLng(
        latitude: currentPosition.latitude,
        longitude: currentPosition.longitude,
      );
    }

    final firstPlace = widget.places.where(_hasValidCoordinate).firstOrNull;
    if (firstPlace != null) {
      return kakao_map.LatLng(
        latitude: firstPlace.latitude,
        longitude: firstPlace.longitude,
      );
    }

    return _fallbackCenter;
  }

  void _requestSync({bool preferCurrentPosition = false}) {
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
        generation: generation,
      );
    });
  }

  Future<void> _syncMarkers({
    bool preferCurrentPosition = false,
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
          text: '내 위치',
          styleId: _currentMarkerStyleId,
          rank: 20000,
        );
      }

      for (final place in widget.places.where(_hasValidCoordinate)) {
        await _addMarker(
          id: _markerIdForPlace(place),
          latitude: place.latitude,
          longitude: place.longitude,
          text: place.name,
          styleId: _placeMarkerStyleId,
          rank: 30000,
        );
      }

      await _moveToBestCenter(preferCurrentPosition: preferCurrentPosition);
      _syncRetryCount = 0;
    } catch (error) {
      _retrySyncAfterMapReady(
        error: error,
        preferCurrentPosition: preferCurrentPosition,
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
      await controller.registerMarkerStyles(
        styles: [
          _buildMarkerStyle(_placeMarkerStyleId, textColor: 0xFF111111),
          _buildMarkerStyle(_currentMarkerStyleId, textColor: 0xFF1565C0),
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
    required int textColor,
  }) {
    return kakao_map.MarkerStyle(
      styleId: styleId,
      perLevels: [
        kakao_map.MarkerPerLevelStyle.fromBytes(
          bytes: base64Decode(_markerPngBase64),
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
        generation: generation,
      );
    });
  }

  kakao_map.LatLng _bestCenter({required bool preferCurrentPosition}) {
    final currentPosition = widget.currentPosition;
    if (preferCurrentPosition && currentPosition != null) {
      return kakao_map.LatLng(
        latitude: currentPosition.latitude,
        longitude: currentPosition.longitude,
      );
    }

    final firstPlace = widget.places.where(_hasValidCoordinate).firstOrNull;
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

    return _fallbackCenter;
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
