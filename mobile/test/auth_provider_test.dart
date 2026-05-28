import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:honeytong_mobile/core/api/api_client.dart';
import 'package:honeytong_mobile/features/auth/providers/auth_provider.dart';
import 'package:honeytong_mobile/features/auth/services/auth_service.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    FlutterSecureStorage.setMockInitialValues({});
  });

  test('initial session restore does not disable login actions', () async {
    final provider = AuthProvider(_FakeAuthService(), autoInitialize: false);

    final initialize = provider.initialize();

    expect(provider.isInitializing, isTrue);
    expect(provider.isLoading, isFalse);

    await initialize;

    expect(provider.isInitializing, isFalse);
    expect(provider.isLoading, isFalse);
  });
}

class _FakeAuthService extends AuthService {
  _FakeAuthService() : super(ApiClient());
}
