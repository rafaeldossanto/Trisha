import 'package:flutter/material.dart';

/// Simbolo da marca: picos de montanha (sem base), em branco. Usado nas telas
/// de entrada — login e criacao de conta — acima do wordmark.
class PeakLogo extends StatelessWidget {
  const PeakLogo({super.key, required this.size});

  final double size;

  @override
  Widget build(BuildContext context) {
    return CustomPaint(
      size: Size(size, size),
      painter: _PeakLogoPainter(),
    );
  }
}

class _PeakLogoPainter extends CustomPainter {
  @override
  void paint(Canvas canvas, Size size) {
    final scale = size.width / 24;
    final paint = Paint()
      ..color = Colors.white
      ..style = PaintingStyle.stroke
      ..strokeWidth = 2.2 * scale
      ..strokeCap = StrokeCap.round
      ..strokeJoin = StrokeJoin.round;
    final path = Path()
      ..moveTo(3 * scale, 19 * scale)
      ..lineTo(11 * scale, 7 * scale)
      ..lineTo(19 * scale, 19 * scale)
      ..moveTo(13 * scale, 19 * scale)
      ..lineTo(18 * scale, 12 * scale)
      ..lineTo(23 * scale, 19 * scale);
    canvas.drawPath(path, paint);
  }

  @override
  bool shouldRepaint(covariant _PeakLogoPainter oldDelegate) => false;
}
