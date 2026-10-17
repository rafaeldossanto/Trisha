import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:provider/provider.dart';

import 'package:trilha_web/core/storage/token_storage.dart';
import 'package:trilha_web/features/auth/data/auth_api.dart';
import 'package:trilha_web/features/auth/data/auth_repository.dart';
import 'package:trilha_web/features/auth/presentation/auth_provider.dart';
import 'package:trilha_web/features/auth/presentation/login_screen.dart';

/// Regressao do logo do login: numa janela larga (web) a Column do formulario
/// usa CrossAxisAlignment.stretch, o que dava largura cheia ao CustomPaint do
/// logo — e como o painter escala por `size.width / 24`, ele era desenhado
/// gigante, por cima da tela inteira.
void main() {
  Widget boot() {
    final provider = AuthProvider(
      AuthRepository(AuthApi(Dio()), TokenStorage()),
    );
    return ChangeNotifierProvider<AuthProvider>.value(
      value: provider,
      child: const MaterialApp(home: LoginScreen()),
    );
  }

  final logoFinder = find.byWidgetPredicate(
    (w) => w is CustomPaint && w.painter.runtimeType.toString().contains('PeakLogo'),
  );

  testWidgets('logo do login nao estica na janela larga', (tester) async {
    tester.view.physicalSize = const Size(1400, 900);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.reset);

    await tester.pumpWidget(boot());

    expect(logoFinder, findsOneWidget);
    expect(tester.getSize(logoFinder), const Size(56, 56));
  });

  testWidgets('logo do login mantem o tamanho na janela estreita', (tester) async {
    tester.view.physicalSize = const Size(420, 900);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.reset);

    await tester.pumpWidget(boot());

    expect(tester.getSize(logoFinder), const Size(56, 56));
  });
}
