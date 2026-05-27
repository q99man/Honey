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
    final images = _parseImageUrls(json);

    return Place(
      id: _asInt(json['id']),
      name: json['name'] ?? '',
      categoryCode: json['categoryCode'] ?? '',
      recommendedMenu: json['recommendedMenu'] ?? '',
      shortRecommendation: json['shortRecommendation'] ?? '',
      featureText: json['featureText'],
      addressRoad: json['addressRoad'] ?? json['address'] ?? '',
      addressJibun: json['addressJibun'] ?? '',
      latitude: _asDouble(json['latitude']),
      longitude: _asDouble(json['longitude']),
      creatorNickname: json['creatorNickname'],
      regionDongName:
          json['regionDongName'] ?? json['dongName'] ?? json['regionName'],
      imageUrls: images,
      stats: _parseStats(json),
      currentStarLevel: _asInt(json['currentStarLevel'] ?? json['starLevel']),
    );
  }

  static List<String> _parseImageUrls(Map<String, dynamic> json) {
    final imagesJson = json['imageUrls'];
    if (imagesJson is List) {
      return imagesJson.whereType<String>().toList();
    }

    final representativeImageUrl = json['representativeImageUrl'];
    if (representativeImageUrl is String && representativeImageUrl.isNotEmpty) {
      return [representativeImageUrl];
    }

    return [];
  }

  static PlaceStats? _parseStats(Map<String, dynamic> json) {
    final statsJson = json['stats'];
    if (statsJson is Map<String, dynamic>) {
      return PlaceStats.fromJson(statsJson);
    }

    final hasTopLevelStats = json.containsKey('recommendCount') ||
        json.containsKey('visitCount') ||
        json.containsKey('commentCount');
    if (!hasTopLevelStats) {
      return null;
    }

    return PlaceStats.fromJson(json);
  }

  static int _asInt(dynamic value) {
    if (value is int) return value;
    if (value is num) return value.toInt();
    if (value is String) return int.tryParse(value) ?? 0;
    return 0;
  }

  static double _asDouble(dynamic value) {
    if (value is num) return value.toDouble();
    if (value is String) return double.tryParse(value) ?? 0.0;
    return 0.0;
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
      recommendCount: Place._asInt(json['recommendCount']),
      visitCount: Place._asInt(json['visitCount']),
      commentCount: Place._asInt(json['commentCount']),
      scoreTotal: Place._asDouble(json['scoreTotal']),
      trustWeightedScore: Place._asDouble(json['trustWeightedScore']),
    );
  }
}
