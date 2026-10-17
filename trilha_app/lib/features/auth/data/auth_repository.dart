import '../../../core/storage/token_storage.dart';
import '../domain/user.dart';
import 'auth_api.dart';

/// Orchestrates API + storage: on login, persists token and userId; exposes the
/// session state (hasToken/userId) and logout.
class AuthRepository {
  AuthRepository(this._api, this._storage);

  final AuthApi _api;
  final TokenStorage _storage;

  Future<User> login({required String email, required String password}) async {
    final result = await _api.login(email: email, password: password);
    return _persist(result);
  }

  Future<User> socialLogin({required String provider, required String idToken}) async {
    final result = await _api.socialLogin(provider: provider, idToken: idToken);
    return _persist(result);
  }

  Future<User> devLogin({required String email, required String name}) async {
    final result = await _api.devLogin(email: email, name: name);
    return _persist(result);
  }

  /// Cria a conta sem abrir sessao: o acesso so vale depois da confirmacao do
  /// email, entao nao ha token para guardar aqui.
  Future<User> signUp({
    required String name,
    required String email,
    required String password,
  }) {
    return _api.signUp(name: name, email: email, password: password);
  }

  Future<void> resendConfirmationEmail(String userId) =>
      _api.resendConfirmationEmail(userId);

  Future<bool> hasToken() async {
    final token = await _storage.readToken();
    return token != null && token.isNotEmpty;
  }

  Future<String?> userId() => _storage.readUserId();

  Future<User> getUser(String id) => _api.getUser(id);

  Future<void> logout() => _storage.clear();

  Future<User> _persist(AuthResult result) async {
    await _storage.saveSession(token: result.accessToken, userId: result.user.id);
    return result.user;
  }
}
