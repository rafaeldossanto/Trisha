import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import 'core/theme/app_theme.dart';

/// Raiz do app web: MaterialApp.router com o tema escuro e o GoRouter (criado
/// no main, com o guard de autenticacao). O scrollBehavior libera arrastar
/// listas e carrosseis com o mouse/trackpad — na web o padrao do Flutter so
/// permite toque.
class TrilhaApp extends StatelessWidget {
  const TrilhaApp({super.key, required this.router});

  final GoRouter router;

  @override
  Widget build(BuildContext context) {
    return MaterialApp.router(
      title: 'Trisha',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.dark,
      darkTheme: AppTheme.dark,
      themeMode: ThemeMode.dark,
      scrollBehavior: const _WebScrollBehavior(),
      routerConfig: router,
    );
  }
}

class _WebScrollBehavior extends MaterialScrollBehavior {
  const _WebScrollBehavior();

  @override
  Set<PointerDeviceKind> get dragDevices => const {
        PointerDeviceKind.touch,
        PointerDeviceKind.mouse,
        PointerDeviceKind.trackpad,
        PointerDeviceKind.stylus,
      };
}
