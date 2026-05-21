class RegionCity {
  final int id;
  final String nameKo;
  final String nameEn;
  final String nameJa;
  final String code;

  RegionCity({
    required this.id,
    required this.nameKo,
    required this.nameEn,
    required this.nameJa,
    required this.code,
  });

  factory RegionCity.fromJson(Map<String, dynamic> json) {
    return RegionCity(
      id: json['id'] ?? 0,
      nameKo: json['nameKo'] ?? '',
      nameEn: json['nameEn'] ?? '',
      nameJa: json['nameJa'] ?? '',
      code: json['code'] ?? '',
    );
  }
}

class RegionDistrict {
  final int id;
  final int cityId;
  final String nameKo;
  final String nameEn;
  final String nameJa;
  final String code;

  RegionDistrict({
    required this.id,
    required this.cityId,
    required this.nameKo,
    required this.nameEn,
    required this.nameJa,
    required this.code,
  });

  factory RegionDistrict.fromJson(Map<String, dynamic> json) {
    return RegionDistrict(
      id: json['id'] ?? 0,
      cityId: json['cityId'] ?? 0,
      nameKo: json['nameKo'] ?? '',
      nameEn: json['nameEn'] ?? '',
      nameJa: json['nameJa'] ?? '',
      code: json['code'] ?? '',
    );
  }
}

class RegionDong {
  final int id;
  final int cityId;
  final int districtId;
  final String nameKo;
  final String nameEn;
  final String nameJa;
  final String code;

  RegionDong({
    required this.id,
    required this.cityId,
    required this.districtId,
    required this.nameKo,
    required this.nameEn,
    required this.nameJa,
    required this.code,
  });

  factory RegionDong.fromJson(Map<String, dynamic> json) {
    return RegionDong(
      id: json['id'] ?? 0,
      cityId: json['cityId'] ?? 0,
      districtId: json['districtId'] ?? 0,
      nameKo: json['nameKo'] ?? '',
      nameEn: json['nameEn'] ?? '',
      nameJa: json['nameJa'] ?? '',
      code: json['code'] ?? '',
    );
  }
}
