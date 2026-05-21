class Season {
  final String seasonCode;
  final String seasonName;
  final String seasonType;
  final DateTime startAt;
  final DateTime endAt;
  final String status;

  Season({
    required this.seasonCode,
    required this.seasonName,
    required this.seasonType,
    required this.startAt,
    required this.endAt,
    required this.status,
  });

  factory Season.fromJson(Map<String, dynamic> json) {
    return Season(
      seasonCode: json['seasonCode'] ?? '',
      seasonName: json['seasonName'] ?? '',
      seasonType: json['seasonType'] ?? '',
      startAt: json['startAt'] != null ? DateTime.parse(json['startAt']) : DateTime.now(),
      endAt: json['endAt'] != null ? DateTime.parse(json['endAt']) : DateTime.now(),
      status: json['status'] ?? '',
    );
  }
}

class PlaceRankingItem {
  final int rank;
  final int placeId;
  final String name;
  final int starLevel;
  final double totalScore;
  final List<String> audienceTags;

  PlaceRankingItem({
    required this.rank,
    required this.placeId,
    required this.name,
    required this.starLevel,
    required this.totalScore,
    required this.audienceTags,
  });

  factory PlaceRankingItem.fromJson(Map<String, dynamic> json) {
    var tagsJson = json['audienceTags'] as List?;
    List<String> tags = tagsJson != null ? List<String>.from(tagsJson) : [];

    return PlaceRankingItem(
      rank: json['rank'] ?? 0,
      placeId: json['placeId'] ?? 0,
      name: json['name'] ?? '',
      starLevel: json['starLevel'] ?? 0,
      totalScore: (json['totalScore'] ?? 0.0).toDouble(),
      audienceTags: tags,
    );
  }
}

class PlaceRankingResponse {
  final String seasonCode;
  final String regionType;
  final String regionName;
  final List<PlaceRankingItem> items;

  PlaceRankingResponse({
    required this.seasonCode,
    required this.regionType,
    required this.regionName,
    required this.items,
  });

  factory PlaceRankingResponse.fromJson(Map<String, dynamic> json) {
    var itemsJson = json['items'] as List?;
    List<PlaceRankingItem> itemsList = itemsJson != null
        ? itemsJson.map((item) => PlaceRankingItem.fromJson(item)).toList()
        : [];

    return PlaceRankingResponse(
      seasonCode: json['seasonCode'] ?? '',
      regionType: json['regionType'] ?? '',
      regionName: json['regionName'] ?? '',
      items: itemsList,
    );
  }
}
