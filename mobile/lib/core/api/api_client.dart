import 'package:dio/dio.dart';
import 'api_endpoints.dart';
import 'token_manager.dart';

class ApiClient {
  late final Dio dio;

  ApiClient() {
    dio = Dio(
      BaseOptions(
        baseUrl: ApiEndpoints.baseUrl,
        connectTimeout: const Duration(seconds: 5),
        receiveTimeout: const Duration(seconds: 5),
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json',
        },
      ),
    );

    // Attach request/response/refresh interceptor
    dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) async {
          // Attach Access Token if available
          final token = await TokenManager.getAccessToken();
          if (token != null) {
            options.headers['Authorization'] = 'Bearer $token';
          }
          return handler.next(options);
        },
        onError: (DioException error, handler) async {
          // If Unauthorized (401) and we have a refresh token, try to refresh it
          if (error.response?.statusCode == 401 &&
              error.requestOptions.path != ApiEndpoints.login &&
              error.requestOptions.path != ApiEndpoints.refresh) {
            final refreshToken = await TokenManager.getRefreshToken();
            if (refreshToken != null) {
              try {
                // Try to refresh token using a separate Dio instance to avoid infinite loop
                final refreshDio =
                    Dio(BaseOptions(baseUrl: ApiEndpoints.baseUrl));
                final response = await refreshDio.post(
                  ApiEndpoints.refresh,
                  data: {'refreshToken': refreshToken},
                );

                if (response.statusCode == 200 && response.data != null) {
                  final data = response.data['data'];
                  final newAccessToken = data['accessToken'];
                  final newRefreshToken = data['refreshToken'];

                  // Save new tokens
                  await TokenManager.saveTokens(
                    accessToken: newAccessToken,
                    refreshToken: newRefreshToken,
                  );

                  // Update header and retry the original request
                  final options = error.requestOptions;
                  options.headers['Authorization'] = 'Bearer $newAccessToken';

                  final retryResponse = await dio.request(
                    options.path,
                    options: Options(
                      method: options.method,
                      headers: options.headers,
                    ),
                    data: options.data,
                    queryParameters: options.queryParameters,
                  );
                  return handler.resolve(retryResponse);
                }
              } catch (e) {
                // Refresh failed, clear tokens (force logout)
                await TokenManager.clearTokens();
              }
            }
          }
          return handler.next(error);
        },
      ),
    );
  }
}
