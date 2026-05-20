class Place {
  final int id;
  final String name;
  final String categoryCode;
  final String recommendedMenu;
  final String shortRecommendation;
  final String? featureText;
  final String addressRoad;
  final String addressJibun;
  final double latitude;
  final double longitude;
  final String? creatorNickname;
  final String? regionDongName;
  final List<String> imageUrls;
  final PlaceStats? stats;
  final int currentStarLevel;

  Place({
    required this.id,
    required this.name,
    required this.categoryCode,
    required this.recommendedMenu,
    required this.shortRecommendation,
    this.featureText,
    required this.addressRoad,
    required this.addressJibun,
    required this.latitude,
    required this.longitude,
    this.creatorNickname,
    this.regionDongName,
    required this.imageUrls,
    this.stats,
    required this.currentStarLevel,
  });

  factory Place.fromJson(Map<String, dynamic> json) {
    var imagesJson = json['imageUrls'] as List?;
    List<String> images = imagesJson != null ? List<String>.from(imagesJson) : [];

    return Place(
      id: json['id'],
      name: json['name'] ?? '',
      categoryCode: json['categoryCode'] ?? '',
      recommendedMenu: json['recommendedMenu'] ?? '',
      shortRecommendation: json['shortRecommendation'] ?? '',
      featureText: json['featureText'],
      addressRoad: json['addressRoad'] ?? '',
      addressJibun: json['addressJibun'] ?? '',
      latitude: (json['latitude'] ?? 0.0).toDouble(),
      longitude: (json['longitude'] ?? 0.0).toDouble(),
      creatorNickname: json['creatorNickname'],
      regionDongName: json['regionDongName'],
      imageUrls: images,
      stats: json['stats'] != null ? PlaceStats.fromJson(json['stats']) : null,
      currentStarLevel: json['currentStarLevel'] ?? 0,
    );
  }
}

class PlaceStats {
  final int recommendCount;
  final int visitCount;
  final int commentCount;
  final double scoreTotal;
  final double trustWeightedScore;

  PlaceStats({
    required this.recommendCount,
    required this.visitCount,
    required this.commentCount,
    required this.scoreTotal,
    required this.trustWeightedScore,
  });

  factory PlaceStats.fromJson(Map<String, dynamic> json) {
    return PlaceStats(
      recommendCount: json['recommendCount'] ?? 0,
      visitCount: json['visitCount'] ?? 0,
      commentCount: json['commentCount'] ?? 0,
      scoreTotal: (json['scoreTotal'] ?? 0.0).toDouble(),
      trustWeightedScore: (json['trustWeightedScore'] ?? 0.0).toDouble(),
    );
  }
}
