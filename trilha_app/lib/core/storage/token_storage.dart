import 'package:flutter_secure_storage/flutter_secure_storage.dart';

/// Stores the user session securely (Keystore on Android). The token and the
/// userId are kept here: the token goes in the request header; the userId is
/// needed to build screens that depend on the owner (e.g.: adventures).
class TokenStorage {
  TokenStorage([FlutterSecureStorage? storage])
      : _storage = storage ?? const FlutterSecureStorage();

  final FlutterSecureStorage _storage;

  static const _kToken = 'access_token';
  static const _kUserId = 'usuario_id';

  Future<void> saveSession({required String token, required String userId}) async {
    await _storage.write(key: _kToken, value: token);
    await _storage.write(key: _kUserId, value: userId);
  }

  Future<String?> readToken() => _storage.read(key: _kToken);

  Future<String?> readUserId() => _storage.read(key: _kUserId);

  Future<void> clear() async {
    await _storage.delete(key: _kToken);
    await _storage.delete(key: _kUserId);
  }
}
