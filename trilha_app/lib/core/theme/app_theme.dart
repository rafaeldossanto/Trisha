import 'package:flutter/material.dart';

/// Tema escuro do Trilha, inspirado no Instagram dark: fundo preto puro,
/// superficies quase pretas e divisores sutis. O verde continua como cor de
/// marca (botoes, destaques); o anel de stories usa o gradiente classico.
class AppTheme {
  const AppTheme._();

  static const Color _verde = Color(0xFF4CAF7D);
  static const Color _fundo = Color(0xFF000000);
  static const Color _superficie = Color(0xFF121212);

  /// Gradiente do anel de stories (estilo Instagram).
  static const List<Color> storyGradient = [
    Color(0xFFFEDA75),
    Color(0xFFFA7E1E),
    Color(0xFFD62976),
    Color(0xFF962FBF),
    Color(0xFF4F5BD5),
  ];

  static const String _wordmarkFamily = 'Yellowtail';

  /// Wordmark da marca (AppBar do feed e barra do mapa).
  ///
  /// Yellowtail e um script de pincel embarcado em `assets/fonts`. Duas regras
  /// que o desenho impoe:
  /// - sem `fontWeight`/`fontStyle`: a fonte tem peso unico e ja vem inclinada;
  ///   pedir bold ou italic faz o Flutter sintetizar e deformar o traco;
  /// - `letterSpacing` zero: as letras sao conectadas, e qualquer espacamento
  ///   abre buracos na emenda.
  static const TextStyle wordmark = TextStyle(
    fontFamily: _wordmarkFamily,
    fontSize: 30,
    letterSpacing: 0,
  );

  /// Mesma marca no tamanho da tela de entrar.
  static const TextStyle wordmarkLarge = TextStyle(
    fontFamily: _wordmarkFamily,
    fontSize: 52,
    letterSpacing: 0,
  );

  static ThemeData get dark {
    final base = ThemeData.dark(useMaterial3: true);
    final colors = ColorScheme.fromSeed(
      seedColor: _verde,
      brightness: Brightness.dark,
    ).copyWith(
      surface: _superficie,
    );

    return base.copyWith(
      colorScheme: colors,
      scaffoldBackgroundColor: _fundo,
      dividerColor: Colors.white12,
      appBarTheme: const AppBarTheme(
        backgroundColor: _fundo,
        elevation: 0,
        centerTitle: false,
      ),
      filledButtonTheme: FilledButtonThemeData(
        style: FilledButton.styleFrom(
          minimumSize: const Size.fromHeight(44),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(10),
          ),
        ),
      ),
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          minimumSize: const Size.fromHeight(44),
          side: const BorderSide(color: Colors.white24),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(10),
          ),
        ),
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: _superficie,
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: BorderSide.none,
        ),
      ),
      cardTheme: CardThemeData(
        color: _superficie,
        elevation: 0,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(16),
        ),
      ),
      tabBarTheme: const TabBarThemeData(
        indicatorColor: Colors.white,
        labelColor: Colors.white,
        unselectedLabelColor: Colors.white54,
      ),
    );
  }
}
