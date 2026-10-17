import 'package:flutter/foundation.dart';

import '../../../core/network/error_handler.dart';
import '../data/auth_repository.dart';
import '../data/social_sign_in.dart';
import '../domain/user.dart';

/// Authentication state of the app. The router listens to this ChangeNotifier
/// (refreshListenable) to re-evaluate the guard when the session changes.
class AuthProvider extends ChangeNotifier {
  AuthProvider(this._repository, this._socialSignIn);

  final AuthRepository _repository;
  final SocialSignIn _socialSignIn;

  bool _loading = false;
  String? _error;
  User? _user;
  String? _userId;
  bool _isLoggedIn = false;

  bool get loading => _loading;
  String? get error => _error;
  User? get user => _user;
  String? get userId => _userId;
  bool get isLoggedIn => _isLoggedIn;

  /// Fora do iOS o login com Apple nao e oferecido — ver [SocialSignIn].
  bool get appleAvailable => _socialSignIn.appleAvailable;

  /// Restores the session from storage (called before runApp).
  Future<void> bootstrap() async {
    _isLoggedIn = await _repository.hasToken();
    if (_isLoggedIn) {
      _userId = await _repository.userId();
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

  Future<void> login({required String email, required String password}) {
    return _authenticate(() => _repository.login(email: email, password: password));
  }

  /// Login de desenvolvimento (nome + email, sem senha). O endpoint so existe no
  /// profile `dev` do backend, e a tela so oferece a opcao em build de debug.
  Future<void> devLogin({required String email, required String name}) {
    return _authenticate(() => _repository.devLogin(email: email, name: name));
  }

  Future<void> loginWithGoogle() => _socialLogin(SocialProvider.google);

  Future<void> loginWithApple() => _socialLogin(SocialProvider.apple);

  Future<void> logout() async {
    await _repository.logout();
    _user = null;
    _userId = null;
    _isLoggedIn = false;
    notifyListeners();
  }

  Future<void> _socialLogin(SocialProvider provider) async {
    _startLoading();
    try {
      final idToken = switch (provider) {
        SocialProvider.google => await _socialSignIn.googleIdToken(),
        SocialProvider.apple => await _socialSignIn.appleIdToken(),
      };
      _onAuthenticated(
        await _repository.socialLogin(
          provider: provider.wireName,
          idToken: idToken,
        ),
      );
    } on SocialSignInCancelled {
      // Desistir do login nao e erro: a tela volta ao estado anterior.
    } on SocialSignInException catch (e) {
      _error = e.message;
      _isLoggedIn = false;
    } catch (e, st) {
      _error = ErrorHandler.message(e, st);
      _isLoggedIn = false;
    } finally {
      _loading = false;
      notifyListeners();
    }
  }

  Future<void> _authenticate(Future<User> Function() action) async {
    _startLoading();
    try {
      _onAuthenticated(await action());
    } catch (e, st) {
      _error = ErrorHandler.message(e, st);
      _isLoggedIn = false;
    } finally {
      _loading = false;
      notifyListeners();
    }
  }

  void _startLoading() {
    _loading = true;
    _error = null;
    notifyListeners();
  }

  void _onAuthenticated(User user) {
    _user = user;
    _userId = user.id;
    _isLoggedIn = true;
  }
}
