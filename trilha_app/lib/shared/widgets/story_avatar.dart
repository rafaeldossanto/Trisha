import 'package:flutter/material.dart';

import '../../core/theme/app_theme.dart';

/// Avatar circular com anel de gradiente (estilo story do Instagram). Sem foto
/// de perfil no backend, mostra a inicial do nome sobre a superficie escura.
class StoryAvatar extends StatelessWidget {
  const StoryAvatar({
    super.key,
    required this.name,
    this.radius = 28,
    this.showRing = true,
  });

  final String name;
  final double radius;
  final bool showRing;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final avatar = CircleAvatar(
      radius: radius,
      backgroundColor: theme.colorScheme.surfaceContainerHighest,
      child: Text(
        name.isNotEmpty ? name[0].toUpperCase() : '?',
        style: TextStyle(
          fontSize: radius * 0.9,
          fontWeight: FontWeight.w600,
          color: Colors.white,
        ),
      ),
    );

    if (!showRing) {
      return avatar;
    }

    return Container(
      padding: const EdgeInsets.all(2.5),
      decoration: const BoxDecoration(
        shape: BoxShape.circle,
        gradient: SweepGradient(colors: [...AppTheme.storyGradient, Color(0xFFFEDA75)]),
      ),
      child: Container(
        padding: const EdgeInsets.all(2.5),
        decoration: BoxDecoration(
          shape: BoxShape.circle,
          color: theme.scaffoldBackgroundColor,
        ),
        child: avatar,
      ),
    );
  }
}
