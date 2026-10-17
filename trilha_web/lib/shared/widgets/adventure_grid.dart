import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../features/adventure/domain/adventure.dart';
import '../../features/point/domain/media_item.dart';

/// Grade 3x3 estilo Instagram de aventuras: a primeira foto vira a capa;
/// sem foto, um placeholder com o destino. Usada no perfil proprio e no de
/// terceiros (que ja chega filtrado por visibilidade pelo backend).
class AdventureGrid extends StatelessWidget {
  const AdventureGrid({
    super.key,
    required this.adventures,
    required this.mediaOf,
  });

  final List<Adventure> adventures;
  final Future<List<MediaItem>> Function(Adventure adventure) mediaOf;

  @override
  Widget build(BuildContext context) {
    return GridView.builder(
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
        crossAxisCount: 3,
        mainAxisSpacing: 2,
        crossAxisSpacing: 2,
      ),
      itemCount: adventures.length,
      itemBuilder: (context, index) {
        final adventure = adventures[index];
        return InkWell(
          onTap: () => context.push('/aventuras/${adventure.id}'),
          child: FutureBuilder<List<MediaItem>>(
            future: mediaOf(adventure),
            builder: (context, snapshot) {
              final photos = snapshot.data ?? const <MediaItem>[];
              if (photos.isEmpty) {
                return _placeholder(context, adventure);
              }
              return Image.network(
                photos.first.url,
                fit: BoxFit.cover,
                errorBuilder: (context, _, _) => _placeholder(context, adventure),
              );
            },
          ),
        );
      },
    );
  }

  Widget _placeholder(BuildContext context, Adventure adventure) {
    final theme = Theme.of(context);
    return Container(
      color: theme.colorScheme.surface,
      padding: const EdgeInsets.all(6),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.terrain, color: theme.colorScheme.primary, size: 28),
          const SizedBox(height: 4),
          Text(
            adventure.destination,
            maxLines: 2,
            overflow: TextOverflow.ellipsis,
            textAlign: TextAlign.center,
            style: const TextStyle(fontSize: 10, color: Colors.white70),
          ),
        ],
      ),
    );
  }
}
