import 'package:flutter/foundation.dart';

import '../../../core/network/error_handler.dart';
import '../data/auth_repository.dart';
import '../domain/user.dart';

/// Authentication state of the app. The router listens to this ChangeNotifier
/// (refreshListenable) to re-evaluate the guard when the session changes.
class AuthProvider extends ChangeNotifier {
  AuthProvider(this._repository);

  final AuthRepository _repository;

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

  Future<void> login({required String email, required String name}) async {
    _loading = true;
    _error = null;
    notifyListeners();
    try {
      final user = await _repository.devLogin(email: email, name: name);
      _user = user;
      _userId = user.id;
      _isLoggedIn = true;
    } catch (e, st) {
      _error = ErrorHandler.message(e, st);
      _isLoggedIn = false;
    } finally {
      _loading = false;
      notifyListeners();
    }
  }

  Future<void> logout() async {
    await _repository.logout();
    _user = null;
    _userId = null;
    _isLoggedIn = false;
    notifyListeners();
  }
}
