import 'package:flutter/material.dart';

/// Marker color by point confidence level: 1 (red, little evidence)
/// -> 5 (green, well confirmed). The confidence level is the app's differentiator.
Color colorByLevel(int level) {
  switch (level) {
    case 5:
      return const Color(0xFF4CAF50);
    case 4:
      return const Color(0xFF8BC34A);
    case 3:
      return const Color(0xFFFFC107);
    case 2:
      return const Color(0xFFFF9800);
    default:
      return const Color(0xFFF44336);
  }
}
