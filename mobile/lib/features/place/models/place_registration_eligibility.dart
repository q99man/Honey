class PlaceRegistrationEligibility {
  const PlaceRegistrationEligibility._({
    required this.allowed,
    this.message,
  });

  final bool allowed;
  final String? message;

  static PlaceRegistrationEligibility evaluate({
    required bool phoneVerified,
    required bool hasVerifiedRegion,
  }) {
    if (!phoneVerified) {
      return const PlaceRegistrationEligibility._(
        allowed: false,
        message: '맛집 등록은 휴대폰 인증을 완료한 뒤 이용할 수 있습니다.',
      );
    }

    if (!hasVerifiedRegion) {
      return const PlaceRegistrationEligibility._(
        allowed: false,
        message: '맛집 등록은 동네 인증을 완료한 뒤 이용할 수 있습니다.',
      );
    }

    return const PlaceRegistrationEligibility._(allowed: true);
  }
}
