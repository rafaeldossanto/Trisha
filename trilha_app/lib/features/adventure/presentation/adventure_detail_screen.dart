import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../../core/network/dio_client.dart';
import '../../auth/presentation/auth_provider.dart';
import '../../path/presentation/trail_path_provider.dart';
import '../../region/presentation/region_provider.dart';
import '../data/adventure_api.dart';
import '../domain/adventure.dart';

/// Adventure detail screen: lists paths (legs), allows starting a new one,
/// finishing and opening GPS tracking or the map with points. Loads the
/// adventure itself to show its metrics (people/duration) and, when the viewer
/// is a member (not the owner), to offer leaving the group adventure.
class AdventureDetailScreen extends StatefulWidget {
  const AdventureDetailScreen({super.key, required this.adventureId});

  final String adventureId;

  @override
  State<AdventureDetailScreen> createState() => _AdventureDetailScreenState();
}

class _AdventureDetailScreenState extends State<AdventureDetailScreen> {
  Adventure? _adventure;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      context.read<TrailPathProvider>().load(widget.adventureId);
      _loadAdventure();
    });
  }

  Future<void> _loadAdventure() async {
    try {
      final adventure =
          await AdventureApi(context.read<DioClient>().dio).getById(widget.adventureId);
      if (mounted) {
        setState(() => _adventure = adventure);
      }
    } catch (_) {
      // Sem a aventura a tela ainda funciona (lista de caminhos); so nao mostra metricas/sair.
    }
  }

  Future<void> _refresh() async {
    await Future.wait([
      context.read<TrailPathProvider>().load(widget.adventureId),
      _loadAdventure(),
    ]);
  }

  bool get _canLeave {
    final adventure = _adventure;
    final myId = context.read<AuthProvider>().userId;
    return adventure != null && myId != null && adventure.userId != myId;
  }

  Future<void> _startPath() async {
    final path = await context.read<TrailPathProvider>().start(widget.adventureId);
    if (!mounted) {
      return;
    }
    if (path == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Nao foi possivel iniciar o caminho')),
      );
    }
  }

  Future<void> _finish(String pathId) async {
    await context.read<TrailPathProvider>().finish(widget.adventureId, pathId);
  }

  Future<void> _leaveAdventure() async {
    // 'manter' = vira aventura pessoal; 'descartar' = apaga; null = cancela.
    final choice = await showDialog<String>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Sair da aventura'),
        content: const Text(
          'Deseja manter seus registros? Seus caminhos viram uma aventura pessoal privada.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: const Text('Cancelar'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(ctx, 'descartar'),
            child: const Text('Descartar'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(ctx, 'manter'),
            child: const Text('Manter'),
          ),
        ],
      ),
    );
    if (choice == null || !mounted) {
      return;
    }
    try {
      final result = await AdventureApi(context.read<DioClient>().dio)
          .leave(widget.adventureId, keepData: choice == 'manter');
      if (!mounted) {
        return;
      }
      final message = result.keptData
          ? 'Voce saiu. Seus registros viraram uma aventura pessoal.'
          : 'Voce saiu da aventura.';
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));
      if (context.canPop()) {
        context.pop();
      } else {
        context.go('/aventuras');
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Nao foi possivel sair da aventura')),
        );
      }
    }
  }

  Future<void> _moveToFolder() async {
    final regionProvider = context.read<RegionProvider>();
    if (regionProvider.regions.isEmpty) {
      await regionProvider.load();
    }
    if (!mounted) {
      return;
    }
    // Result: '' = None (remove from folder); id = folder; null = cancelled.
    final choice = await showModalBottomSheet<String>(
      context: context,
      builder: (_) => ListView(
        shrinkWrap: true,
        children: [
          ListTile(
            leading: const Icon(Icons.folder_off),
            title: const Text('Nenhuma (tirar da colecao)'),
            onTap: () => Navigator.pop(context, ''),
          ),
          ...regionProvider.regions.map((r) => ListTile(
                leading: const Icon(Icons.folder),
                title: Text(r.name),
                onTap: () => Navigator.pop(context, r.id),
              )),
        ],
      ),
    );
    if (choice == null || !mounted) {
      return;
    }
    try {
      await AdventureApi(context.read<DioClient>().dio)
          .moveToRegion(widget.adventureId, choice.isEmpty ? null : choice);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Aventura movida')));
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Nao foi possivel mover')));
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<TrailPathProvider>();
    return Scaffold(
      appBar: AppBar(
        title: const Text('Aventura'),
        actions: [
          if (_canLeave)
            IconButton(
              tooltip: 'Sair da aventura',
              icon: const Icon(Icons.logout),
              onPressed: _leaveAdventure,
            ),
          IconButton(
            tooltip: 'Mover para colecao',
            icon: const Icon(Icons.drive_file_move),
            onPressed: _moveToFolder,
          ),
          IconButton(
            tooltip: 'Mapa da aventura',
            icon: const Icon(Icons.map),
            onPressed: () => context.go('/aventuras/${widget.adventureId}/mapa'),
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: provider.loading ? null : _startPath,
        icon: const Icon(Icons.add_road),
        label: const Text('Iniciar caminho'),
      ),
      body: Column(
        children: [
          if (_adventure != null) _metricsHeader(_adventure!),
          Expanded(
            child: RefreshIndicator(
              onRefresh: _refresh,
              child: _buildBody(provider),
            ),
          ),
        ],
      ),
    );
  }

  Widget _metricsHeader(Adventure adventure) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            adventure.destination,
            style: Theme.of(context).textTheme.titleMedium,
          ),
          const SizedBox(height: 6),
          Row(
            children: [
              const Icon(Icons.group, size: 16, color: Colors.white54),
              const SizedBox(width: 6),
              Text(
                adventure.metricsLabel,
                style: const TextStyle(fontSize: 12, color: Colors.white54),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildBody(TrailPathProvider provider) {
    if (provider.loading && provider.paths.isEmpty) {
      return const Center(child: CircularProgressIndicator());
    }
    if (provider.paths.isEmpty) {
      final msg = provider.error ?? 'Nenhum caminho ainda.\nInicie um para rastrear a trilha.';
      return ListView(
        children: [
          Padding(
            padding: const EdgeInsets.only(top: 120, left: 24, right: 24),
            child: Text(msg, textAlign: TextAlign.center),
          ),
        ],
      );
    }

    return ListView.separated(
      padding: const EdgeInsets.all(16),
      itemCount: provider.paths.length,
      separatorBuilder: (_, _) => const SizedBox(height: 12),
      itemBuilder: (context, index) {
        final path = provider.paths[index];
        final subtitle = path.finished
            ? 'Finalizado • ${path.totalDistanceKm?.toStringAsFixed(2) ?? '0'} km'
            : 'Em andamento';
        return Card(
          child: Column(
            children: [
              ListTile(
                leading: CircleAvatar(child: Text('${path.number ?? '-'}')),
                title: Text('Caminho ${path.number ?? ''}'),
                subtitle: Text(subtitle),
              ),
              if (!path.finished)
                OverflowBar(
                  alignment: MainAxisAlignment.end,
                  children: [
                    TextButton.icon(
                      onPressed: () => context.go('/caminhos/${path.id}/rastreio'),
                      icon: const Icon(Icons.gps_fixed),
                      label: const Text('Rastrear'),
                    ),
                    TextButton.icon(
                      onPressed: () => _finish(path.id),
                      icon: const Icon(Icons.flag),
                      label: const Text('Finalizar'),
                    ),
                  ],
                ),
            ],
          ),
        );
      },
    );
  }
}
