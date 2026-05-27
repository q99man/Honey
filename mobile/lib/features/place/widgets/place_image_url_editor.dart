import 'package:flutter/material.dart';

import '../../../core/theme/app_theme.dart';
import '../../../core/utils/image_url_resolver.dart';
import '../../../core/widgets/app_surface_card.dart';

class PlaceImageUrlEditor extends StatelessWidget {
  const PlaceImageUrlEditor({
    super.key,
    required this.imageUrls,
    required this.controller,
    required this.onAdd,
    required this.onRemove,
    required this.onMoveUp,
    required this.onMoveDown,
    this.onPickImage,
    this.isPickingImage = false,
  });

  final List<String> imageUrls;
  final TextEditingController controller;
  final VoidCallback onAdd;
  final ValueChanged<int> onRemove;
  final ValueChanged<int> onMoveUp;
  final ValueChanged<int> onMoveDown;
  final VoidCallback? onPickImage;
  final bool isPickingImage;

  @override
  Widget build(BuildContext context) {
    return AppSurfaceCard(
      padding: const EdgeInsets.all(AppSpacing.md),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const Row(
            children: [
              Icon(Icons.photo_library_outlined, color: AppColors.honey),
              SizedBox(width: AppSpacing.xs),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      '맛집 사진',
                      style: TextStyle(
                        fontSize: 15,
                        fontWeight: FontWeight.w800,
                        color: AppColors.ink,
                      ),
                    ),
                    SizedBox(height: 2),
                    Text(
                      '첫 번째 이미지가 대표 사진으로 사용됩니다.',
                      style: TextStyle(fontSize: 12, color: AppColors.muted),
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: AppSpacing.md),
          if (imageUrls.isEmpty)
            Container(
              padding: const EdgeInsets.symmetric(
                horizontal: AppSpacing.md,
                vertical: AppSpacing.lg,
              ),
              decoration: BoxDecoration(
                color: AppColors.surfaceWarm,
                borderRadius: BorderRadius.circular(AppRadius.md),
                border: Border.all(color: AppColors.outline),
              ),
              child: const Column(
                children: [
                  Icon(
                    Icons.add_photo_alternate_outlined,
                    color: AppColors.nectar,
                    size: 32,
                  ),
                  SizedBox(height: AppSpacing.xs),
                  Text(
                    '사진 URL을 추가하면 상세 화면과 저장 목록에 표시됩니다.',
                    textAlign: TextAlign.center,
                    style: TextStyle(fontSize: 12, color: AppColors.muted),
                  ),
                ],
              ),
            )
          else
            ...imageUrls.indexed.map((entry) {
              final index = entry.$1;
              final url = entry.$2;
              return Padding(
                padding: EdgeInsets.only(
                  bottom: index == imageUrls.length - 1 ? 0 : AppSpacing.sm,
                ),
                child: _ImageUrlTile(
                  index: index,
                  url: url,
                  isFirst: index == 0,
                  canMoveUp: index > 0,
                  canMoveDown: index < imageUrls.length - 1,
                  onRemove: () => onRemove(index),
                  onMoveUp: () => onMoveUp(index),
                  onMoveDown: () => onMoveDown(index),
                ),
              );
            }),
          const SizedBox(height: AppSpacing.md),
          if (onPickImage != null) ...[
            OutlinedButton.icon(
              onPressed: isPickingImage ? null : onPickImage,
              icon: isPickingImage
                  ? const SizedBox(
                      width: 16,
                      height: 16,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                  : const Icon(Icons.photo_camera_back_outlined),
              label: Text(isPickingImage ? '사진 업로드 중' : '갤러리에서 사진 선택'),
            ),
            const SizedBox(height: AppSpacing.sm),
          ],
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(
                child: TextFormField(
                  controller: controller,
                  keyboardType: TextInputType.url,
                  textInputAction: TextInputAction.done,
                  decoration: const InputDecoration(
                    labelText: '사진 URL',
                    hintText: 'https://example.com/place.jpg',
                    prefixIcon: Icon(
                      Icons.link,
                      color: AppColors.honey,
                      size: 20,
                    ),
                  ),
                  onFieldSubmitted: (_) => onAdd(),
                ),
              ),
              const SizedBox(width: AppSpacing.xs),
              IconButton.filled(
                onPressed: onAdd,
                tooltip: '사진 추가',
                icon: const Icon(Icons.add_photo_alternate_outlined),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _ImageUrlTile extends StatelessWidget {
  const _ImageUrlTile({
    required this.index,
    required this.url,
    required this.isFirst,
    required this.canMoveUp,
    required this.canMoveDown,
    required this.onRemove,
    required this.onMoveUp,
    required this.onMoveDown,
  });

  final int index;
  final String url;
  final bool isFirst;
  final bool canMoveUp;
  final bool canMoveDown;
  final VoidCallback onRemove;
  final VoidCallback onMoveUp;
  final VoidCallback onMoveDown;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(AppSpacing.xs),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(AppRadius.md),
        border: Border.all(color: AppColors.outline),
      ),
      child: Row(
        children: [
          ClipRRect(
            borderRadius: BorderRadius.circular(AppRadius.sm),
            child: SizedBox(
              width: 72,
              height: 72,
              child: Image.network(
                ImageUrlResolver.resolve(url),
                fit: BoxFit.cover,
                errorBuilder: (context, error, stackTrace) {
                  return Container(
                    color: AppColors.surfaceWarm,
                    child: const Icon(
                      Icons.broken_image_outlined,
                      color: AppColors.muted,
                    ),
                  );
                },
              ),
            ),
          ),
          const SizedBox(width: AppSpacing.sm),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Wrap(
                  spacing: AppSpacing.xs,
                  crossAxisAlignment: WrapCrossAlignment.center,
                  children: [
                    Text(
                      '${index + 1}번 사진',
                      style: const TextStyle(
                        fontWeight: FontWeight.w800,
                        color: AppColors.ink,
                      ),
                    ),
                    if (isFirst)
                      const Chip(
                        visualDensity: VisualDensity.compact,
                        label: Text('대표'),
                      ),
                  ],
                ),
                const SizedBox(height: 2),
                Text(
                  url,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(fontSize: 12, color: AppColors.muted),
                ),
              ],
            ),
          ),
          Column(
            children: [
              IconButton(
                onPressed: canMoveUp ? onMoveUp : null,
                tooltip: '위로 이동',
                icon: const Icon(Icons.keyboard_arrow_up),
              ),
              IconButton(
                onPressed: canMoveDown ? onMoveDown : null,
                tooltip: '아래로 이동',
                icon: const Icon(Icons.keyboard_arrow_down),
              ),
              IconButton(
                onPressed: onRemove,
                tooltip: '삭제',
                color: AppColors.berry,
                icon: const Icon(Icons.delete_outline),
              ),
            ],
          ),
        ],
      ),
    );
  }
}
