import 'package:flutter/foundation.dart';
import 'package:google_sign_in/google_sign_in.dart';
import 'package:sign_in_with_apple/sign_in_with_apple.dart';

import '../../../core/env/env.dart';

/// Provedores aceitos pelo backend — os nomes espelham o enum AuthProvider do
/// servico Cadastro, que rejeita qualquer outro valor.
enum SocialProvider {
  google('GOOGLE'),
  apple('APPLE');

  const SocialProvider(this.wireName);

  /// Valor enviado no campo `provedor` do login social.
  final String wireName;
}

/// O usuario fechou a tela do provedor. Nao e falha: a tela so volta ao estado
/// anterior, sem mensagem de erro.
class SocialSignInCancelled implements Exception {}

/// Falha ao obter o ID token no dispositivo (configuracao ausente, provedor
/// indisponivel, token sem identidade). A mensagem ja vai pronta para a tela.
class SocialSignInException implements Exception {
  const SocialSignInException(this.message);

  final String message;

  @override
  String toString() => message;
}

/// Obtem o ID token do provedor social no dispositivo. A troca desse token pela
/// sessao da aplicacao e do backend (POST /bff/usuarios/login-social) — aqui so
/// acontece a parte que exige o SDK nativo.
class SocialSignIn {
  Future<void>? _googleInitialization;

  /// A Apple exige o botao proprio e o fluxo nativo; no Android o mesmo login
  /// so existiria via fluxo web (Service ID no Apple Developer), que o projeto
  /// ainda nao tem. Fora do iOS o botao nem aparece.
  bool get appleAvailable => defaultTargetPlatform == TargetPlatform.iOS;

  Future<String> googleIdToken() async {
    await _initializeGoogle();
    try {
      final account = await GoogleSignIn.instance.authenticate();
      final idToken = account.authentication.idToken;
      if (idToken == null) {
        throw const SocialSignInException(
          'O Google nao devolveu o token de identidade. Confira o client ID configurado.',
        );
      }
      return idToken;
    } on GoogleSignInException catch (e) {
      if (e.code == GoogleSignInExceptionCode.canceled) {
        throw SocialSignInCancelled();
      }
      throw SocialSignInException(
        'Falha no login com Google: ${e.description ?? e.code.name}',
      );
    }
  }

  Future<String> appleIdToken() async {
    try {
      final credential = await SignInWithApple.getAppleIDCredential(
        scopes: [
          AppleIDAuthorizationScopes.email,
          AppleIDAuthorizationScopes.fullName,
        ],
      );
      final idToken = credential.identityToken;
      if (idToken == null) {
        throw const SocialSignInException(
          'A Apple nao devolveu o token de identidade.',
        );
      }
      return idToken;
    } on SignInWithAppleAuthorizationException catch (e) {
      if (e.code == AuthorizationErrorCode.canceled) {
        throw SocialSignInCancelled();
      }
      throw SocialSignInException('Falha no login com Apple: ${e.message}');
    } on SignInWithAppleException catch (e) {
      throw SocialSignInException('Falha no login com Apple: $e');
    }
  }

  /// O plugin do Google exige `initialize` antes de qualquer chamada. O Future
  /// fica guardado para inicializar uma vez so, mesmo com toques repetidos; se
  /// falhar, e descartado para nao congelar o botao pelo resto da sessao.
  Future<void> _initializeGoogle() async {
    final serverClientId = Env.googleServerClientId;
    if (serverClientId == null) {
      throw const SocialSignInException(
        'Login com Google ainda nao configurado: defina GOOGLE_SERVER_CLIENT_ID no .env.',
      );
    }
    final pending = _googleInitialization ??=
        GoogleSignIn.instance.initialize(serverClientId: serverClientId);
    try {
      await pending;
    } catch (_) {
      _googleInitialization = null;
      rethrow;
    }
  }
}
