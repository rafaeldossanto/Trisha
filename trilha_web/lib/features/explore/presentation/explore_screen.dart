import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../../core/network/dio_client.dart';
import '../../friendship/presentation/friendship_provider.dart';
import '../../region/data/region_api.dart';
import '../../region/domain/region.dart';
import '../../../shared/widgets/story_avatar.dart';

/// Aba de busca estilo Instagram: campo no topo procura trilheiros; sem termo,
/// mostra a grade "descobrir" com as colecoes publicas e de amigos.
class ExploreScreen extends StatefulWidget {
  const ExploreScreen({super.key});

  @override
  State<ExploreScreen> createState() => _ExploreScreenState();
}

class _ExploreScreenState extends State<ExploreScreen> {
  late final RegionApi _regionApi = RegionApi(context.read<DioClient>().dio);
  final _searchController = TextEditingController();

  List<Region> _regions = const [];
  bool _loading = true;
  String _term = '';

  @override
  void initState() {
    super.initState();
    _loadRegions();
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  Future<void> _loadRegions() async {
    try {
      final regions = await _regionApi.discover();
      if (mounted) {
        setState(() {
          _regions = regions;
          _loading = false;
        });
      }
    } catch (_) {
      if (mounted) {
        setState(() => _loading = false);
      }
    }
  }

  void _onSearch(String term) {
    setState(() => _term = term.trim());
    context.read<FriendshipProvider>().search(term);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Column(
          children: [
            Padding(
              padding: const EdgeInsets.fromLTRB(12, 8, 12, 4),
              child: TextField(
                controller: _searchController,
                decoration: InputDecoration(
                  hintText: 'Buscar trilheiros (ex: rafael#1)',
                  prefixIcon: const Icon(Icons.search),
                  suffixIcon: _term.isEmpty
                      ? null
                      : IconButton(
                          icon: const Icon(Icons.close),
                          onPressed: () {
                            _searchController.clear();
                            _onSearch('');
                          },
                        ),
                ),
                onChanged: _onSearch,
              ),
            ),
            Expanded(child: _term.isEmpty ? _discover() : _userResults()),
          ],
        ),
      ),
    );
  }

  Widget _discover() {
    if (_loading) {
      return const Center(child: CircularProgressIndicator());
    }
    if (_regions.isEmpty) {
      return const Center(
        child: Text(
          'Nenhuma colecao publica ou de amigos por aqui.',
          style: TextStyle(color: Colors.white54),
        ),
      );
    }
    return RefreshIndicator(
      onRefresh: _loadRegions,
      child: GridView.builder(
        padding: const EdgeInsets.all(12),
        gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
          crossAxisCount: 2,
          mainAxisSpacing: 8,
          crossAxisSpacing: 8,
          childAspectRatio: 1.1,
        ),
        itemCount: _regions.length,
        itemBuilder: (context, index) => _regionTile(_regions[index]),
      ),
    );
  }

  Widget _regionTile(Region region) {
    final theme = Theme.of(context);
    return InkWell(
      onTap: () => context.push('/regioes/detalhe', extra: region),
      borderRadius: BorderRadius.circular(12),
      child: Container(
        clipBehavior: Clip.antiAlias,
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(12),
          gradient: LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [
              theme.colorScheme.primary.withValues(alpha: 0.3),
              theme.colorScheme.surface,
            ],
          ),
        ),
        child: Stack(
          fit: StackFit.expand,
          children: [
            // Capa da colecao como fundo, escurecida para o texto continuar legivel.
            if (region.coverUrl != null)
              Image.network(
                region.coverUrl!,
                fit: BoxFit.cover,
                color: Colors.black38,
                colorBlendMode: BlendMode.darken,
                errorBuilder: (_, _, _) => const SizedBox.shrink(),
              ),
            Padding(
              padding: const EdgeInsets.all(12),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Icon(Icons.public, color: theme.colorScheme.primary),
                  const Spacer(),
                  Text(
                    region.name,
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(fontWeight: FontWeight.w600),
                  ),
                  const SizedBox(height: 2),
                  Text(
                    '${region.cities.length} cidade(s) • ${region.visibility}',
                    style: const TextStyle(fontSize: 11, color: Colors.white70),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _userResults() {
    final provider = context.watch<FriendshipProvider>();
    if (provider.searchResults.isEmpty) {
      return const Center(
        child: Text('Nenhum trilheiro encontrado.', style: TextStyle(color: Colors.white54)),
      );
    }
    return ListView(
      children: provider.searchResults.map((user) {
        return ListTile(
          leading: StoryAvatar(name: user.name, radius: 20, showRing: false),
          title: Text(user.name, style: const TextStyle(fontWeight: FontWeight.w600)),
          subtitle: Text(user.userCode, style: const TextStyle(color: Colors.white54)),
          onTap: () => context.push('/perfil', extra: user),
        );
      }).toList(),
    );
  }
}
