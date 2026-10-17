import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:provider/provider.dart';
import 'package:trilha_app/core/storage/token_storage.dart';
import 'package:trilha_app/features/auth/data/auth_api.dart';
import 'package:trilha_app/features/auth/data/auth_repository.dart';
import 'package:trilha_app/features/auth/data/social_sign_in.dart';
import 'package:trilha_app/features/auth/presentation/auth_provider.dart';
import 'package:trilha_app/features/auth/presentation/login_screen.dart';

void main() {
  Future<void> pumpLogin(WidgetTester tester) {
    final auth = AuthProvider(
      AuthRepository(AuthApi(Dio()), TokenStorage()),
      SocialSignIn(),
    );

    return tester.pumpWidget(
      ChangeNotifierProvider<AuthProvider>.value(
        value: auth,
        child: const MaterialApp(home: LoginScreen()),
      ),
    );
  }

  testWidgets('login screen mostra os campos e o botao Entrar', (tester) async {
    await pumpLogin(tester);

    expect(find.text('E-mail'), findsOneWidget);
    expect(find.text('Senha'), findsOneWidget);
    expect(find.text('Entrar'), findsOneWidget);
  });

  // Sem essas saidas o usuario novo fica sem caminho: era o estado da tela antes
  // de existirem cadastro e login social.
  testWidgets('login screen oferece cadastro e login social', (tester) async {
    await pumpLogin(tester);

    expect(find.text('Criar conta'), findsOneWidget);
    expect(find.text('Continuar com Google'), findsOneWidget);
  });

  // Regressao: a Column do formulario usa CrossAxisAlignment.stretch, o que dava
  // largura cheia ao CustomPaint do logo — e como o painter escala por
  // `size.width / 24`, ele era desenhado por cima da tela inteira.
  testWidgets('logo do login nao estica com a largura da tela', (tester) async {
    final logo = find.byWidgetPredicate(
      (w) => w is CustomPaint && w.painter.runtimeType.toString().contains('PeakLogo'),
    );

    tester.view.physicalSize = const Size(420, 900);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.reset);

    await pumpLogin(tester);

    expect(logo, findsOneWidget);
    expect(tester.getSize(logo), const Size(56, 56));
  });
}
