class SavedPlace {
  final int recommendationId;
  final int placeId;
  final String placeName;
  final DateTime recommendedAt;

  SavedPlace({
    required this.recommendationId,
    required this.placeId,
    required this.placeName,
    required this.recommendedAt,
  });

  factory SavedPlace.fromJson(Map<String, dynamic> json) {
    return SavedPlace(
      recommendationId: json['recommendationId'] ?? 0,
      placeId: json['placeId'] ?? 0,
      placeName: json['placeName'] ?? '',
      recommendedAt: json['recommendedAt'] != null
          ? DateTime.parse(json['recommendedAt'])
          : DateTime.now(),
    );
  }
}
