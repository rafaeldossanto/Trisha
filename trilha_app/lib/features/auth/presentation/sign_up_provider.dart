import 'package:flutter/foundation.dart';

import '../../../core/network/error_handler.dart';
import '../data/auth_repository.dart';
import '../domain/user.dart';

/// Estado da criacao de conta. Fica separado do [AuthProvider] de proposito: o
/// cadastro nao abre sessao (a conta nasce PENDENTE ate a confirmacao do email),
/// entao seu erro e seu loading nao podem vazar para a tela de login.
class SignUpProvider extends ChangeNotifier {
  SignUpProvider(this._repository);

  final AuthRepository _repository;

  bool _loading = false;
  String? _error;
  User? _createdUser;
  bool _emailResent = false;

  bool get loading => _loading;
  String? get error => _error;

  /// Preenchido quando a conta foi criada — a tela troca o formulario pelo
  /// aviso de confirmacao e usa o id para reenviar o email.
  User? get createdUser => _createdUser;

  bool get emailResent => _emailResent;

  Future<void> submit({
    required String name,
    required String email,
    required String password,
  }) async {
    _loading = true;
    _error = null;
    notifyListeners();
    try {
      _createdUser = await _repository.signUp(
        name: name,
        email: email,
        password: password,
      );
    } catch (e, st) {
      _error = ErrorHandler.message(e, st);
    } finally {
      _loading = false;
      notifyListeners();
    }
  }

  Future<void> resendConfirmation() async {
    final user = _createdUser;
    if (user == null) {
      return;
    }
    _loading = true;
    _error = null;
    notifyListeners();
    try {
      await _repository.resendConfirmationEmail(user.id);
      _emailResent = true;
    } catch (e, st) {
      _error = ErrorHandler.message(e, st);
    } finally {
      _loading = false;
      notifyListeners();
    }
  }
}
