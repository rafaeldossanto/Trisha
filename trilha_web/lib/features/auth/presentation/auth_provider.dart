import 'package:flutter/foundation.dart';

import '../data/auth_repository.dart';
import '../domain/user.dart';

/// Session state of the app. There is no login screen while the front is being
/// tested: without a stored session, [bootstrap] enters with a fixed test user
/// through the BFF dev-login. Without the API (e.g.: front published while the
/// back is not online) the app still opens, only without a session.
class AuthProvider extends ChangeNotifier {
  AuthProvider(this._repository);

  static const _testEmail = 'teste@trisha.dev';
  static const _testName = 'Teste';

  /// Sem o back no ar o pedido pode ficar parado (ex.: o navegador perguntando
  /// se libera o acesso ao localhost): o app nao espera mais que isso para abrir.
  static const _testLoginTimeout = Duration(seconds: 3);

  final AuthRepository _repository;

  User? _user;
  String? _userId;

  User? get user => _user;
  String? get userId => _userId;

  /// Restores the session from storage or, without one, enters with the test
  /// user (called before runApp).
  Future<void> bootstrap() async {
    if (await _repository.hasToken()) {
      _userId = await _repository.userId();
    } else {
      await _testLogin().timeout(_testLoginTimeout, onTimeout: () {});
    }
    notifyListeners();
  }

  /// Guarantees `user` is filled after a session restore (bootstrap only reads
  /// the token/userId from storage; the rest comes from the BFF).
  Future<void> ensureUser() async {
    if (_user != null || _userId == null) {
      return;
    }
    try {
      _user = await _repository.getUser(_userId!);
      notifyListeners();
    } catch (_) {
      // Sem o perfil o app segue funcionando com o userId do storage.
    }
  }

  Future<void> _testLogin() async {
    try {
      final user = await _repository.devLogin(email: _testEmail, name: _testName);
      _user = user;
      _userId = user.id;
      notifyListeners();
    } catch (_) {
      // Sem a API o app abre sem sessao; cada tela mostra o proprio erro.
    }
  }
}
