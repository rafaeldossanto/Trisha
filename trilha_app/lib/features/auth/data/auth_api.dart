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

  factory AuthResult.fromJson(Map<String, dynamic> json) {
    return AuthResult(
      user: User.fromJson(json['usuario'] as Map<String, dynamic>),
      accessToken: json['accessToken'] as String,
      expiresInSeconds: (json['expiresInSegundos'] as num).toInt(),
    );
  }
}

/// Access to authentication endpoints of the BFF.
class AuthApi {
  AuthApi(this._dio);

  final Dio _dio;

  Future<User> getUser(String id) async {
    final resp = await _dio.get('/bff/usuarios/$id');
    return User.fromJson(resp.data as Map<String, dynamic>);
  }

  /// Cadastro por email e senha. Nao devolve token: a conta nasce PENDENTE e so
  /// autentica depois que o usuario confirma o email — por isso volta o usuario
  /// criado, cujo id serve para reenviar a confirmacao.
  Future<User> signUp({
    required String name,
    required String email,
    required String password,
  }) async {
    final resp = await _dio.post(
      '/bff/usuarios',
      data: {'nome': name, 'email': email, 'senha': password},
    );
    return User.fromJson(resp.data as Map<String, dynamic>);
  }

  Future<AuthResult> login({required String email, required String password}) async {
    final resp = await _dio.post(
      '/bff/auth/login',
      data: {'email': email, 'senha': password},
    );
    return AuthResult.fromJson(resp.data as Map<String, dynamic>);
  }

  /// Login social: o ID token ja foi obtido do provedor no dispositivo. Quem o
  /// valida (assinatura, emissor e audiencia) e cria ou vincula a conta e o
  /// backend; o app so recebe de volta a sessao da aplicacao.
  Future<AuthResult> socialLogin({
    required String provider,
    required String idToken,
  }) async {
    final resp = await _dio.post(
      '/bff/usuarios/login-social',
      data: {'provedor': provider, 'idToken': idToken},
    );
    return AuthResult.fromJson(resp.data as Map<String, dynamic>);
  }

  Future<void> resendConfirmationEmail(String userId) async {
    await _dio.post('/bff/usuarios/$userId/reenviar-email');
  }

  Future<AuthResult> devLogin({required String email, required String name}) async {
    final resp = await _dio.post(
      '/bff/auth/dev-login',
      data: {'email': email, 'nome': name},
    );
    return AuthResult.fromJson(resp.data as Map<String, dynamic>);
  }
}
