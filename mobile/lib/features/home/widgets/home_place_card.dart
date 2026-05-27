import 'package:flutter/material.dart';

import '../../../core/theme/app_theme.dart';
import '../../../models/place.dart';
import '../../place/widgets/place_thumbnail.dart';

class HomePlaceCard extends StatelessWidget {
  const HomePlaceCard({
    super.key,
    required this.place,
    required this.categoryLabel,
    this.onTap,
    this.onClose,
    this.compact = false,
  });

  final Place place;
  final String categoryLabel;
  final VoidCallback? onTap;
  final VoidCallback? onClose;
  final bool compact;

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    final address =
        place.addressRoad.isNotEmpty ? place.addressRoad : place.addressJibun;

    return Card(
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: onTap,
        child: Padding(
          padding: EdgeInsets.all(compact ? AppSpacing.sm : AppSpacing.md),
          child: Row(
            children: [
              PlaceThumbnail(
                imageUrl:
                    place.imageUrls.isNotEmpty ? place.imageUrls.first : null,
                size: compact ? 64 : 60,
              ),
              const SizedBox(width: AppSpacing.sm),
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
                            style: textTheme.titleMedium?.copyWith(
                              fontWeight: FontWeight.w800,
                              color: AppColors.ink,
                            ),
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                          ),
                        ),
                        const SizedBox(width: AppSpacing.xs),
                        _CategoryBadge(label: categoryLabel),
                      ],
                    ),
                    const SizedBox(height: AppSpacing.xs),
                    Text(
                      '추천 메뉴: ${place.recommendedMenu}',
                      style: textTheme.bodyMedium?.copyWith(
                        fontWeight: FontWeight.w700,
                        color: AppColors.ink,
                      ),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                    if (compact) ...[
                      const SizedBox(height: AppSpacing.xxs),
                      Text(
                        place.shortRecommendation,
                        style: textTheme.bodySmall?.copyWith(
                          height: 1.35,
                          color: AppColors.muted,
                        ),
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                      ),
                    ] else ...[
                      const SizedBox(height: AppSpacing.xxs),
                      Text(
                        address,
                        style: textTheme.bodySmall?.copyWith(
                          color: AppColors.muted,
                        ),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                    ],
                  ],
                ),
              ),
              if (onClose != null) ...[
                const SizedBox(width: AppSpacing.xs),
                IconButton(
                  tooltip: '닫기',
                  onPressed: onClose,
                  icon: const Icon(Icons.close_rounded),
                  color: AppColors.muted,
                ),
              ] else if (!compact) ...[
                const SizedBox(width: AppSpacing.xs),
                const Icon(Icons.chevron_right_rounded, color: AppColors.muted),
              ],
            ],
          ),
        ),
      ),
    );
  }
}

class _CategoryBadge extends StatelessWidget {
  const _CategoryBadge({required this.label});

  final String label;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.xs,
        vertical: AppSpacing.xxs,
      ),
      decoration: BoxDecoration(
        color: AppColors.surfaceWarm,
        borderRadius: BorderRadius.circular(AppRadius.sm),
      ),
      child: Text(
        label,
        style: const TextStyle(
          color: AppColors.nectar,
          fontSize: 11,
          fontWeight: FontWeight.w800,
        ),
      ),
    );
  }
}
