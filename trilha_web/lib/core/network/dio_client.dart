import 'package:dio/dio.dart';

import '../env/env.dart';
import '../storage/token_storage.dart';
import 'auth_interceptor.dart';

/// Cliente HTTP unico do app: baseUrl vem do .env (BFF), timeouts razoaveis,
/// AuthInterceptor e RetryInterceptor (503 → 1 retry com delay de 1 s).
class DioClient {
  DioClient(TokenStorage tokenStorage)
      : dio = Dio(
          BaseOptions(
            baseUrl: Env.apiBaseUrl,
            connectTimeout: const Duration(seconds: 10),
            receiveTimeout: const Duration(seconds: 10),
            contentType: 'application/json',
          ),
        ) {
    dio.interceptors.add(AuthInterceptor(tokenStorage));
    dio.interceptors.add(_RetryInterceptor(dio));
  }

  final Dio dio;
}

/// Retries a failed request once when the server responds with 503 (service
/// unavailable / circuit-breaker open). Waits 1 second before retrying to give
/// the downstream service a moment to recover.
class _RetryInterceptor extends Interceptor {
  _RetryInterceptor(this._dio);

  final Dio _dio;

  static const _retryHeader = 'x-retry-attempt';

  @override
  Future<void> onError(
    DioException err,
    ErrorInterceptorHandler handler,
  ) async {
    final isRetry = err.requestOptions.headers.containsKey(_retryHeader);
    final is503 = err.response?.statusCode == 503;

    if (is503 && !isRetry) {
      await Future<void>.delayed(const Duration(seconds: 1));
      try {
        final options = err.requestOptions;
        options.headers[_retryHeader] = '1';
        final response = await _dio.fetch(options);
        handler.resolve(response);
        return;
      } on DioException catch (retryErr) {
        handler.next(retryErr);
        return;
      }
    }

    handler.next(err);
  }
}
