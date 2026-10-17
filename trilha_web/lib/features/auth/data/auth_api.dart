import 'package:dio/dio.dart';

import '../domain/user.dart';

/// Login result: user + app token and its validity.
class AuthResult {
  const AuthResult({
    required this.user,
    required this.accessToken,
    required this.expiresInSeconds,
  });

  final User user;
  final String accessToken;
  final int expiresInSeconds;
}

/// Access to authentication endpoints of the BFF.
class AuthApi {
  AuthApi(this._dio);

  final Dio _dio;

  Future<User> getUser(String id) async {
    final resp = await _dio.get('/bff/usuarios/$id');
    return User.fromJson(resp.data as Map<String, dynamic>);
  }

  Future<AuthResult> devLogin({required String email, required String name}) async {
    final resp = await _dio.post(
      '/bff/auth/dev-login',
      data: {'email': email, 'nome': name},
    );
    final data = resp.data as Map<String, dynamic>;
    return AuthResult(
      user: User.fromJson(data['usuario'] as Map<String, dynamic>),
      accessToken: data['accessToken'] as String,
      expiresInSeconds: (data['expiresInSegundos'] as num).toInt(),
    );
  }
}
