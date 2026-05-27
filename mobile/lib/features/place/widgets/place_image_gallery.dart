import 'package:flutter/material.dart';

import '../../../core/theme/app_theme.dart';
import '../../../core/utils/image_url_resolver.dart';

class PlaceImageGallery extends StatefulWidget {
  const PlaceImageGallery({
    super.key,
    required this.imageUrls,
  });

  final List<String> imageUrls;

  @override
  State<PlaceImageGallery> createState() => _PlaceImageGalleryState();
}

class _PlaceImageGalleryState extends State<PlaceImageGallery> {
  static const double _minDragDistance = 48;
  static const double _minDragVelocity = 200;

  late final PageController _pageController;
  int _currentIndex = 0;
  double _dragDistance = 0;

  @override
  void initState() {
    super.initState();
    _pageController = PageController();
  }

  @override
  void dispose() {
    _pageController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (widget.imageUrls.isEmpty) {
      return _buildFallback();
    }

    return Stack(
      fit: StackFit.expand,
      children: [
        PageView.builder(
          controller: _pageController,
          itemCount: widget.imageUrls.length,
          onPageChanged: (index) => setState(() => _currentIndex = index),
          itemBuilder: (context, index) {
            return Image.network(
              ImageUrlResolver.resolve(widget.imageUrls[index]),
              fit: BoxFit.cover,
              errorBuilder: (context, error, stackTrace) => _buildFallback(),
            );
          },
        ),
        const DecoratedBox(
          decoration: BoxDecoration(
            gradient: LinearGradient(
              begin: Alignment.topCenter,
              end: Alignment.bottomCenter,
              colors: [
                Colors.black26,
                Colors.transparent,
                Colors.black38,
              ],
            ),
          ),
        ),
        Positioned.fill(
          bottom: widget.imageUrls.length > 1
              ? AppSpacing.md + 48 + AppSpacing.md
              : 0,
          child: GestureDetector(
            behavior: HitTestBehavior.translucent,
            onTap: _openImageViewer,
            onHorizontalDragStart:
                widget.imageUrls.length > 1 ? (_) => _dragDistance = 0 : null,
            onHorizontalDragUpdate: widget.imageUrls.length > 1
                ? (details) {
                    _dragDistance += details.primaryDelta ?? 0;
                  }
                : null,
            onHorizontalDragEnd:
                widget.imageUrls.length > 1 ? _handleHorizontalDragEnd : null,
          ),
        ),
        Positioned(
          top: AppSpacing.md,
          right: AppSpacing.md,
          child: Container(
            padding: const EdgeInsets.symmetric(
              horizontal: AppSpacing.sm,
              vertical: AppSpacing.xxs,
            ),
            decoration: BoxDecoration(
              color: Colors.black.withValues(alpha: 0.54),
              borderRadius: BorderRadius.circular(AppRadius.pill),
            ),
            child: Text(
              '${_currentIndex + 1} / ${widget.imageUrls.length}',
              style: const TextStyle(
                color: Colors.white,
                fontSize: 12,
                fontWeight: FontWeight.w800,
              ),
            ),
          ),
        ),
        if (widget.imageUrls.length > 1)
          Positioned(
            left: AppSpacing.md,
            right: AppSpacing.md,
            bottom: AppSpacing.md,
            child: SizedBox(
              height: 48,
              child: ListView.separated(
                scrollDirection: Axis.horizontal,
                itemCount: widget.imageUrls.length,
                separatorBuilder: (context, index) =>
                    const SizedBox(width: AppSpacing.xs),
                itemBuilder: (context, index) {
                  final selected = index == _currentIndex;
                  return Tooltip(
                    message: '${index + 1}번째 사진 보기',
                    child: InkWell(
                      onTap: () => _goToPage(index),
                      borderRadius: BorderRadius.circular(AppRadius.sm),
                      child: AnimatedContainer(
                        duration: const Duration(milliseconds: 160),
                        width: 48,
                        height: 48,
                        decoration: BoxDecoration(
                          borderRadius: BorderRadius.circular(AppRadius.sm),
                          border: Border.all(
                            color: selected ? Colors.white : Colors.transparent,
                            width: 2,
                          ),
                        ),
                        child: ClipRRect(
                          borderRadius: BorderRadius.circular(AppRadius.sm),
                          child: Image.network(
                            ImageUrlResolver.resolve(widget.imageUrls[index]),
                            fit: BoxFit.cover,
                            errorBuilder: (context, error, stackTrace) {
                              return Container(
                                color: AppColors.surfaceWarm,
                                child: const Icon(
                                  Icons.broken_image_outlined,
                                  color: AppColors.muted,
                                  size: 18,
                                ),
                              );
                            },
                          ),
                        ),
                      ),
                    ),
                  );
                },
              ),
            ),
          ),
      ],
    );
  }

  void _handleHorizontalDragEnd(DragEndDetails details) {
    final velocity = details.primaryVelocity ?? 0;

    if (velocity <= -_minDragVelocity || _dragDistance <= -_minDragDistance) {
      _goToPage(_currentIndex + 1);
    } else if (velocity >= _minDragVelocity ||
        _dragDistance >= _minDragDistance) {
      _goToPage(_currentIndex - 1);
    }

    _dragDistance = 0;
  }

  void _goToPage(int index) {
    final target = index.clamp(0, widget.imageUrls.length - 1).toInt();
    if (target == _currentIndex) {
      return;
    }

    setState(() => _currentIndex = target);
    _pageController.animateToPage(
      target,
      duration: const Duration(milliseconds: 220),
      curve: Curves.easeOut,
    );
  }

  void _openImageViewer() {
    showDialog<void>(
      context: context,
      barrierColor: Colors.black,
      useSafeArea: false,
      builder: (context) {
        return _PlaceImageViewerDialog(
          imageUrls: widget.imageUrls,
          initialIndex: _currentIndex,
        );
      },
    );
  }

  Widget _buildFallback() {
    return Container(
      decoration: const BoxDecoration(
        gradient: LinearGradient(
          colors: [AppColors.surfaceWarm, AppColors.honey],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
      ),
      child: const Center(
        child: Icon(
          Icons.restaurant_rounded,
          size: 70,
          color: Colors.white,
        ),
      ),
    );
  }
}

class _PlaceImageViewerDialog extends StatefulWidget {
  const _PlaceImageViewerDialog({
    required this.imageUrls,
    required this.initialIndex,
  });

  final List<String> imageUrls;
  final int initialIndex;

  @override
  State<_PlaceImageViewerDialog> createState() =>
      _PlaceImageViewerDialogState();
}

class _PlaceImageViewerDialogState extends State<_PlaceImageViewerDialog> {
  late int _currentIndex;
  int? _activePointer;
  double _pointerDragDistance = 0;
  bool _isImageZoomed = false;

  @override
  void initState() {
    super.initState();
    _currentIndex = widget.initialIndex;
  }

  @override
  Widget build(BuildContext context) {
    return Material(
      key: const Key('place-image-viewer'),
      color: Colors.black,
      child: SafeArea(
        child: Stack(
          children: [
            Listener(
              key: const Key('place-image-viewer-page-view'),
              onPointerDown: _handlePointerDown,
              onPointerMove: _handlePointerMove,
              onPointerUp: _handlePointerUp,
              onPointerCancel: (_) => _resetPointerDrag(),
              child: _ZoomableNetworkImage(
                key: ValueKey(_currentIndex),
                imageUrl: widget.imageUrls[_currentIndex],
                onZoomChanged: (isZoomed) {
                  if (isZoomed != _isImageZoomed) {
                    setState(() => _isImageZoomed = isZoomed);
                  }
                },
              ),
            ),
            Positioned(
              top: AppSpacing.sm,
              left: AppSpacing.sm,
              child: IconButton.filled(
                tooltip: '사진 닫기',
                onPressed: () => Navigator.of(context).pop(),
                icon: const Icon(Icons.close_rounded),
              ),
            ),
            Positioned(
              top: AppSpacing.md,
              left: 0,
              right: 0,
              child: Center(
                child: Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: AppSpacing.sm,
                    vertical: AppSpacing.xxs,
                  ),
                  decoration: BoxDecoration(
                    color: Colors.black.withValues(alpha: 0.54),
                    borderRadius: BorderRadius.circular(AppRadius.pill),
                  ),
                  child: Text(
                    '${_currentIndex + 1} / ${widget.imageUrls.length}',
                    style: const TextStyle(
                      color: Colors.white,
                      fontSize: 13,
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  void _handlePointerDown(PointerDownEvent event) {
    if (widget.imageUrls.length < 2 || _isImageZoomed) {
      return;
    }

    _activePointer = event.pointer;
    _pointerDragDistance = 0;
  }

  void _handlePointerMove(PointerMoveEvent event) {
    if (event.pointer != _activePointer || _isImageZoomed) {
      return;
    }

    _pointerDragDistance += event.delta.dx;
  }

  void _handlePointerUp(PointerUpEvent event) {
    if (event.pointer != _activePointer || _isImageZoomed) {
      _resetPointerDrag();
      return;
    }

    if (_pointerDragDistance <= -_PlaceImageGalleryState._minDragDistance) {
      _goToPage(_currentIndex + 1);
    } else if (_pointerDragDistance >=
        _PlaceImageGalleryState._minDragDistance) {
      _goToPage(_currentIndex - 1);
    }

    _resetPointerDrag();
  }

  void _resetPointerDrag() {
    _activePointer = null;
    _pointerDragDistance = 0;
  }

  void _goToPage(int index) {
    final target = index.clamp(0, widget.imageUrls.length - 1).toInt();
    if (target == _currentIndex) {
      return;
    }

    setState(() {
      _currentIndex = target;
      _isImageZoomed = false;
    });
  }
}

class _ZoomableNetworkImage extends StatefulWidget {
  const _ZoomableNetworkImage({
    super.key,
    required this.imageUrl,
    required this.onZoomChanged,
  });

  final String imageUrl;
  final ValueChanged<bool> onZoomChanged;

  @override
  State<_ZoomableNetworkImage> createState() => _ZoomableNetworkImageState();
}

class _ZoomableNetworkImageState extends State<_ZoomableNetworkImage> {
  late final TransformationController _transformationController;
  bool _isZoomed = false;

  @override
  void initState() {
    super.initState();
    _transformationController = TransformationController();
    _transformationController.addListener(_syncZoomState);
  }

  @override
  void dispose() {
    _transformationController.removeListener(_syncZoomState);
    _transformationController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return InteractiveViewer(
      transformationController: _transformationController,
      minScale: 1,
      maxScale: 4,
      panEnabled: _isZoomed,
      child: Center(
        child: Image.network(
          ImageUrlResolver.resolve(widget.imageUrl),
          fit: BoxFit.contain,
          errorBuilder: (context, error, stackTrace) {
            return const Icon(
              Icons.broken_image_outlined,
              color: Colors.white70,
              size: 56,
            );
          },
        ),
      ),
    );
  }

  void _syncZoomState() {
    final isZoomed = _transformationController.value.getMaxScaleOnAxis() > 1.01;
    if (isZoomed != _isZoomed) {
      setState(() => _isZoomed = isZoomed);
      widget.onZoomChanged(isZoomed);
    }
  }
}
