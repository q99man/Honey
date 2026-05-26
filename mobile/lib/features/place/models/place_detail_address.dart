class PlaceDetailAddress {
  const PlaceDetailAddress({
    required this.road,
    required this.jibun,
  });

  final String? road;
  final String? jibun;

  String get primary => road ?? jibun ?? '주소 정보 없음';

  String? get jibunLabel {
    if (jibun == null) return null;
    return '[지번] $jibun';
  }

  factory PlaceDetailAddress.fromJson(Map<String, dynamic> json) {
    return PlaceDetailAddress(
      road: _clean(json['addressRoad']),
      jibun: _clean(json['addressJibun']),
    );
  }

  static String? _clean(Object? value) {
    if (value == null) return null;

    final text = value.toString().trim();
    if (text.isEmpty || text.toLowerCase() == 'null') {
      return null;
    }

    return text;
  }
}
