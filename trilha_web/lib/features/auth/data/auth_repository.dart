import '../../../core/storage/token_storage.dart';
import '../domain/user.dart';
import 'auth_api.dart';

/// Orchestrates API + storage: on login, persists token and userId; exposes the
/// session state (hasToken/userId) and logout.
class AuthRepository {
  AuthRepository(this._api, this._storage);

  final AuthApi _api;
  final TokenStorage _storage;

  Future<User> devLogin({required String email, required String name}) async {
    final result = await _api.devLogin(email: email, name: name);
    await _storage.saveSession(token: result.accessToken, userId: result.user.id);
    return result.user;
  }

  Future<bool> hasToken() async {
    final token = await _storage.readToken();
    return token != null && token.isNotEmpty;
  }

  Future<String?> userId() => _storage.readUserId();

  Future<User> getUser(String id) => _api.getUser(id);

  Future<void> logout() => _storage.clear();
}
