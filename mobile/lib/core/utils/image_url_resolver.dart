import '../api/api_endpoints.dart';

class ImageUrlResolver {
  const ImageUrlResolver._();

  static String resolve(String url) {
    final trimmed = url.trim();
    if (trimmed.isEmpty) return trimmed;

    final uri = Uri.tryParse(trimmed);
    if (uri == null || !uri.hasScheme) return trimmed;

    final isLocalBackendUrl =
        (uri.host == 'localhost' || uri.host == '127.0.0.1') &&
            uri.path.startsWith('/uploads/');
    if (!isLocalBackendUrl) return trimmed;

    final baseUri = Uri.tryParse(ApiEndpoints.baseUrl);
    if (baseUri == null || !baseUri.hasScheme) return trimmed;

    return uri
        .replace(
          scheme: baseUri.scheme,
          host: baseUri.host,
          port: baseUri.hasPort ? baseUri.port : null,
        )
        .toString();
  }
}
