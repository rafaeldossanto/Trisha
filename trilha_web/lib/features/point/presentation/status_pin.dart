import 'package:flutter/material.dart';

import '../domain/point_status.dart';

/// Cores dos pins de status (mockup aprovado em 2026-07-20).
abstract final class PinColors {
  static const blue = Color(0xFF2196F3);
  static const blueBorder = Color(0xFF1565C0);
  static const bluePupil = Color(0xFF0D47A1);
  static const green = Color(0xFF22C55E);
  static const greenDark = Color(0xFF15803D);
  static const gold = Color(0xFFFFC400);
  static const goldDark = Color(0xFFC88A00);
  static const black = Color(0xFF1B1B19);
  static const gray = Color(0xFF888780);
  static const grayDark = Color(0xFF5F5E5A);
  static const grayLight = Color(0xFFB4B2A9);
}

/// Variante visual do pin. O objetivo domina o status: enquanto nao
/// conquistado o pin e dourado com borda preta; conquistado, inverte
/// (preto com borda dourada) e ganha estrela + sparkles.
enum PinVariant {
  noRadar,
  naMira,
  conquistado,
  objetivo,
  objetivoConquistado;

  static PinVariant? of(PointUserStatus? mark) {
    if (mark == null || mark.isEmpty) {
      return null;
    }
    if (mark.goal) {
      return mark.status == PointStatus.conquistado
          ? PinVariant.objetivoConquistado
          : PinVariant.objetivo;
    }
    return switch (mark.status) {
      PointStatus.noRadar => PinVariant.noRadar,
      PointStatus.naMira => PinVariant.naMira,
      PointStatus.conquistado => PinVariant.conquistado,
      null => null,
    };
  }
}

/// Pin de mapa desenhado no visual aprovado, com a ponta no centro inferior.
class StatusPin extends StatelessWidget {
  const StatusPin({super.key, required this.variant, this.width = 34});

  final PinVariant variant;
  final double width;

  @override
  Widget build(BuildContext context) {
    return CustomPaint(
      size: Size(width, width * 54 / 40),
      painter: _StatusPinPainter(variant),
    );
  }
}

class _StatusPinPainter extends CustomPainter {
  const _StatusPinPainter(this.variant);

  final PinVariant variant;

  @override
  void paint(Canvas canvas, Size size) {
    canvas.scale(size.width / 40);
    switch (variant) {
      case PinVariant.noRadar:
        _body(canvas, PinColors.gray, PinColors.grayDark);
        _radar(canvas);
      case PinVariant.naMira:
        _body(canvas, PinColors.blue, PinColors.blueBorder);
        _eye(canvas);
      case PinVariant.conquistado:
        _body(canvas, PinColors.green, PinColors.greenDark);
        _check(canvas, PinColors.greenDark, onWhiteCircle: true);
      case PinVariant.objetivo:
        _body(canvas, PinColors.gold, PinColors.black, borderWidth: 2.8);
      case PinVariant.objetivoConquistado:
        _body(canvas, PinColors.black, PinColors.gold, borderWidth: 2.8);
        canvas.drawPath(_star(const Offset(20, 20), 6.5), Paint()..color = PinColors.gold);
        _sparkles(canvas);
    }
  }

  /// Gota do pin (coordenadas base 40x54, ponta em 20,53).
  Path _pinPath() {
    return Path()
      ..moveTo(20, 1)
      ..cubicTo(9.5, 1, 1, 9.5, 1, 20)
      ..cubicTo(1, 32, 20, 53, 20, 53)
      ..cubicTo(20, 53, 39, 32, 39, 20)
      ..cubicTo(39, 9.5, 30.5, 1, 20, 1)
      ..close();
  }

  void _body(Canvas canvas, Color fill, Color border, {double borderWidth = 1}) {
    final pin = _pinPath();
    canvas.drawPath(pin, Paint()..color = fill);
    canvas.drawPath(
      pin,
      Paint()
        ..color = border
        ..style = PaintingStyle.stroke
        ..strokeWidth = borderWidth,
    );
  }

  void _radar(Canvas canvas) {
    const center = Offset(20, 20);
    canvas.drawCircle(center, 10, Paint()..color = Colors.white);
    final ring = Paint()
      ..color = PinColors.gray
      ..style = PaintingStyle.stroke
      ..strokeWidth = 1;
    canvas.drawCircle(center, 7.5, ring);
    canvas.drawCircle(center, 4, ring);
    final sweep = Path()
      ..moveTo(20, 20)
      ..lineTo(20, 10.5)
      ..arcToPoint(const Offset(26.7, 13.3), radius: const Radius.circular(9.5))
      ..close();
    canvas.drawPath(sweep, Paint()..color = PinColors.grayLight.withValues(alpha: 0.8));
    canvas.drawCircle(center, 1.5, Paint()..color = PinColors.grayDark);
    canvas.drawCircle(const Offset(24.2, 15.8), 1.3, Paint()..color = PinColors.grayDark);
  }

  void _eye(Canvas canvas) {
    final eye = Path()
      ..moveTo(10.5, 20)
      ..quadraticBezierTo(20, 12, 29.5, 20)
      ..quadraticBezierTo(20, 28, 10.5, 20)
      ..close();
    canvas.drawPath(eye, Paint()..color = Colors.white);
    canvas.drawCircle(const Offset(20, 20), 4, Paint()..color = PinColors.bluePupil);
    canvas.drawCircle(const Offset(21.4, 18.6), 1.2, Paint()..color = Colors.white);
  }

  void _check(Canvas canvas, Color color, {required bool onWhiteCircle}) {
    if (onWhiteCircle) {
      canvas.drawCircle(const Offset(20, 20), 10, Paint()..color = Colors.white);
    }
    final check = Path()
      ..moveTo(14.8, 20.5)
      ..lineTo(18.4, 24)
      ..lineTo(25.4, 16.5);
    canvas.drawPath(
      check,
      Paint()
        ..color = color
        ..style = PaintingStyle.stroke
        ..strokeWidth = 2.5
        ..strokeCap = StrokeCap.round
        ..strokeJoin = StrokeJoin.round,
    );
  }

  /// Estrela de 4 pontas usada no centro e nos sparkles da conquista.
  Path _star(Offset center, double radius) {
    final inner = radius * 0.3;
    return Path()
      ..moveTo(center.dx, center.dy - radius)
      ..lineTo(center.dx + inner, center.dy - inner)
      ..lineTo(center.dx + radius, center.dy)
      ..lineTo(center.dx + inner, center.dy + inner)
      ..lineTo(center.dx, center.dy + radius)
      ..lineTo(center.dx - inner, center.dy + inner)
      ..lineTo(center.dx - radius, center.dy)
      ..lineTo(center.dx - inner, center.dy - inner)
      ..close();
  }

  void _sparkles(Canvas canvas) {
    final fill = Paint()..color = PinColors.gold;
    final outline = Paint()
      ..color = PinColors.goldDark
      ..style = PaintingStyle.stroke
      ..strokeWidth = 0.5;
    for (final (center, radius) in [
      (const Offset(5, 11.5), 3.5),
      (const Offset(35.5, 6.3), 2.8),
      (const Offset(37, 19.4), 2.4),
    ]) {
      final star = _star(center, radius);
      canvas.drawPath(star, fill);
      canvas.drawPath(star, outline);
    }
  }

  @override
  bool shouldRepaint(_StatusPinPainter oldDelegate) => oldDelegate.variant != variant;
}
