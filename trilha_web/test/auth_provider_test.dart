import 'dart:async';

import 'package:flutter_test/flutter_test.dart';

import 'package:trilha_web/features/auth/data/auth_repository.dart';
import 'package:trilha_web/features/auth/domain/user.dart';
import 'package:trilha_web/features/auth/presentation/auth_provider.dart';

/// Sem tela de login, o bootstrap decide a sessao sozinho: restaura a salva,
/// entra com o usuario de teste ou, sem API, abre o app sem sessao.
void main() {
  const testUser = User(id: 'u-1', name: 'Teste', email: 'teste@trisha.dev', userCode: 'teste');

  test('com sessao salva restaura o userId sem chamar o dev-login', () async {
    final repository = _FakeRepository(storedUserId: 'u-salvo');
    final provider = AuthProvider(repository);

    await provider.bootstrap();

    expect(provider.userId, 'u-salvo');
    expect(repository.devLoginCalls, 0);
  });

  test('sem sessao entra com o usuario de teste', () async {
    final repository = _FakeRepository(onDevLogin: () async => testUser);
    final provider = AuthProvider(repository);

    await provider.bootstrap();

    expect(provider.userId, 'u-1');
    expect(provider.user?.email, 'teste@trisha.dev');
  });

  test('sem API o app abre sem sessao', () async {
    final provider = AuthProvider(
      _FakeRepository(onDevLogin: () async => throw Exception('connection refused')),
    );

    await provider.bootstrap();

    expect(provider.userId, isNull);
    expect(provider.user, isNull);
  });

  testWidgets('dev-login parado nao segura a abertura alem de 3s', (tester) async {
    final provider = AuthProvider(
      _FakeRepository(onDevLogin: () => Completer<User>().future),
    );
    var opened = false;

    unawaited(provider.bootstrap().then((_) => opened = true));
    await tester.pump(const Duration(seconds: 2));
    expect(opened, isFalse);

    await tester.pump(const Duration(seconds: 1));
    expect(opened, isTrue);
    expect(provider.userId, isNull);
  });
}

class _FakeRepository implements AuthRepository {
  _FakeRepository({this.storedUserId, this.onDevLogin});

  final String? storedUserId;
  final Future<User> Function()? onDevLogin;
  int devLoginCalls = 0;

  @override
  Future<bool> hasToken() async => storedUserId != null;

  @override
  Future<String?> userId() async => storedUserId;

  @override
  Future<User> devLogin({required String email, required String name}) {
    devLoginCalls++;
    return onDevLogin!();
  }

  @override
  Future<User> getUser(String id) => throw UnimplementedError();
}
