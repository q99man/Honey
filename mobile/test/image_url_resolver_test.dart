import 'package:flutter_test/flutter_test.dart';
import 'package:honeytong_mobile/core/config/app_config.dart';
import 'package:honeytong_mobile/core/utils/image_url_resolver.dart';

void main() {
  test('ImageUrlResolver rewrites local uploaded image urls to API base origin',
      () {
    final resolved = ImageUrlResolver.resolve(
      'http://localhost:8080/uploads/images/places/sample.jpg',
    );

    expect(
      resolved,
      '${AppConfig.apiBaseUrl}/uploads/images/places/sample.jpg',
    );
  });

  test('ImageUrlResolver keeps external image urls unchanged', () {
    const external = 'https://cdn.example.com/images/sample.jpg';

    expect(ImageUrlResolver.resolve(external), external);
  });
}
