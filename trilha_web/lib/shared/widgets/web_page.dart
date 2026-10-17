import 'package:flutter/material.dart';

/// Moldura das telas empurradas no root navigator quando a janela e larga:
/// centraliza o conteudo numa coluna com largura maxima e bordas laterais
/// sutis, em vez de esticar a tela inteira. Em janela estreita (ou para telas
/// de mapa, que nao usam este wrapper) a tela ocupa tudo, como no app.
class WebPage extends StatelessWidget {
  const WebPage({super.key, required this.child, this.maxWidth = 800});

  final Widget child;
  final double maxWidth;

  /// Mesmo breakpoint do MainShell: abaixo disso o layout e o do celular.
  static const double _desktopBreakpoint = 900;

  @override
  Widget build(BuildContext context) {
    final width = MediaQuery.sizeOf(context).width;
    if (width < _desktopBreakpoint) {
      return child;
    }
    return ColoredBox(
      color: Theme.of(context).scaffoldBackgroundColor,
      child: Center(
        child: Container(
          constraints: BoxConstraints(maxWidth: maxWidth),
          decoration: const BoxDecoration(
            border: Border.symmetric(vertical: BorderSide(color: Colors.white12)),
          ),
          child: child,
        ),
      ),
    );
  }
}
