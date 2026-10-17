import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:trilha_web/core/theme/app_theme.dart';

void main() {
  test('tema escuro usa fundo preto puro', () {
    expect(AppTheme.dark.scaffoldBackgroundColor, const Color(0xFF000000));
  });
}
