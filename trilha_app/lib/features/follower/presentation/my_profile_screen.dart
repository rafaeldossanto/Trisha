import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../../core/network/dio_client.dart';
import '../../../shared/widgets/adventure_grid.dart';
import '../../../shared/widgets/story_avatar.dart';
import '../../adventure/domain/adventure.dart';
import '../../adventure/presentation/adventure_provider.dart';
import '../../auth/presentation/auth_provider.dart';
import '../../point/data/media_api.dart';
import '../../point/domain/media_item.dart';
import '../data/follower_api.dart';
import '../domain/counters.dart';

/// Perfil do usuario logado, estilo Instagram: username no topo, contadores
/// (aventuras/seguidores/seguindo), atalhos e a grade 3x3 de aventuras usando
/// a primeira foto de cada uma como capa.
class MyProfileScreen extends StatefulWidget {
  const MyProfileScreen({super.key});

  @override
  State<MyProfileScreen> createState() => _MyProfileScreenState();
}

class _MyProfileScreenState extends State<MyProfileScreen> {
  late final FollowerApi _followerApi = FollowerApi(context.read<DioClient>().dio);
  late final MediaApi _mediaApi = MediaApi(context.read<DioClient>().dio);

  final Map<String, Future<List<MediaItem>>> _mediaByAdventure = {};
  Counters? _counters;

  // Botoes do perfil mais baixos e com fonte menor que o padrao do tema.
  static final _compactButtonStyle = OutlinedButton.styleFrom(
    minimumSize: const Size.fromHeight(34),
    textStyle: const TextStyle(fontSize: 13, fontWeight: FontWeight.w600),
    visualDensity: VisualDensity.compact,
  );

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _load());
  }

  Future<void> _load() async {
    final auth = context.read<AuthProvider>();
    await auth.ensureUser();
    if (!mounted) {
      return;
    }

    final userId = auth.userId;
    if (userId != null) {
      _mediaByAdventure.clear();
      await context.read<AdventureProvider>().load(userId);
    }

    final code = auth.user?.userCode ?? '';
    if (code.isNotEmpty) {
      try {
        final counters = await _followerApi.counters(code);
        if (mounted) {
          setState(() => _counters = counters);
        }
      } catch (_) {
        // Contadores sao complementares; o perfil segue sem eles.
      }
    }
  }

  Future<List<MediaItem>> _mediaOf(Adventure adventure) {
    return _mediaByAdventure.putIfAbsent(
      adventure.id,
      () => _mediaApi.listByAdventure(adventure.id, size: 1),
    );
  }

  void _openList(String type, String title) {
    final code = context.read<AuthProvider>().user?.userCode ?? '';
    if (code.isEmpty) {
      return;
    }
    context.push('/usuarios', extra: {'codigo': code, 'tipo': type, 'titulo': title});
  }

  @override
  Widget build(BuildContext context) {
    final auth = context.watch<AuthProvider>();
    final provider = context.watch<AdventureProvider>();
    final user = auth.user;

    return Scaffold(
      appBar: AppBar(
        title: Text(
          user?.userCode.isNotEmpty == true ? user!.userCode : (user?.name ?? 'Perfil'),
          style: const TextStyle(fontWeight: FontWeight.w700),
        ),
        actions: [
          IconButton(
            tooltip: 'Minhas colecoes',
            icon: const Icon(Icons.folder_outlined),
            onPressed: () => context.push('/regioes'),
          ),
          IconButton(
            tooltip: 'Sair',
            icon: const Icon(Icons.logout),
            onPressed: () => context.read<AuthProvider>().logout(),
          ),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: _load,
        child: ListView(
          children: [
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 8, 16, 0),
              child: Row(
                children: [
                  StoryAvatar(name: user?.name ?? '?', radius: 30, showRing: false),
                  const SizedBox(width: 16),
                  Expanded(
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                      children: [
                        _counter('Aventuras', provider.adventures.length, null),
                        _counter('Seguidores', _counters?.followers ?? 0,
                            () => _openList('seguidores', 'Seguidores')),
                        _counter('Seguindo', _counters?.following ?? 0,
                            () => _openList('seguindo', 'Seguindo')),
                      ],
                    ),
                  ),
                ],
              ),
            ),
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 12, 16, 4),
              child: Text(
                user?.name ?? '',
                style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 13),
              ),
            ),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
              child: Row(
                children: [
                  Expanded(
                    child: OutlinedButton(
                      style: _compactButtonStyle,
                      onPressed: () => context.push('/regioes'),
                      child: const Text('Minhas colecoes'),
                    ),
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: OutlinedButton(
                      style: _compactButtonStyle,
                      onPressed: () => context.push('/amizades'),
                      child: const Text('Amizades'),
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 4),
            const Divider(height: 1),
            const Padding(
              padding: EdgeInsets.symmetric(vertical: 6),
              child: Icon(Icons.grid_on, size: 20),
            ),
            _grid(provider),
          ],
        ),
      ),
    );
  }

  Widget _counter(String label, int value, VoidCallback? onTap) {
    return InkWell(
      onTap: onTap,
      child: Column(
        children: [
          Text('$value', style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w700)),
          Text(label, style: const TextStyle(fontSize: 11, color: Colors.white70)),
        ],
      ),
    );
  }

  Widget _grid(AdventureProvider provider) {
    if (provider.loading && provider.adventures.isEmpty) {
      return const Padding(
        padding: EdgeInsets.only(top: 48),
        child: Center(child: CircularProgressIndicator()),
      );
    }
    if (provider.adventures.isEmpty) {
      return const Padding(
        padding: EdgeInsets.only(top: 48, left: 32, right: 32),
        child: Text(
          'Nenhuma aventura ainda.\nToque no + para registrar a primeira.',
          textAlign: TextAlign.center,
          style: TextStyle(color: Colors.white54),
        ),
      );
    }
    return AdventureGrid(adventures: provider.adventures, mediaOf: _mediaOf);
  }
}
