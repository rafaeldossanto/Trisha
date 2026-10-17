import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../domain/region.dart';
import 'region_form_sheet.dart';
import 'region_provider.dart';

/// My collections (regions): list, create, edit and delete.
class MyRegionsScreen extends StatefulWidget {
  const MyRegionsScreen({super.key});

  @override
  State<MyRegionsScreen> createState() => _MyRegionsScreenState();
}

class _MyRegionsScreenState extends State<MyRegionsScreen> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => context.read<RegionProvider>().load());
  }

  Future<void> _openForm({Region? region}) async {
    await showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      builder: (_) => RegionFormSheet(region: region),
    );
  }

  Future<void> _delete(Region region) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (_) => AlertDialog(
        title: const Text('Excluir colecao'),
        content: Text('Excluir "${region.name}"? As aventuras nao sao apagadas, so saem da colecao.'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Cancelar')),
          FilledButton(onPressed: () => Navigator.pop(context, true), child: const Text('Excluir')),
        ],
      ),
    );
    if (confirmed == true && mounted) {
      await context.read<RegionProvider>().remove(region.id);
    }
  }

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<RegionProvider>();
    return Scaffold(
      appBar: AppBar(title: const Text('Minhas colecoes')),
      floatingActionButton: FloatingActionButton(
        onPressed: () => _openForm(),
        child: const Icon(Icons.create_new_folder),
      ),
      body: RefreshIndicator(
        onRefresh: () => context.read<RegionProvider>().load(),
        child: _buildBody(provider),
      ),
    );
  }

  Widget _buildBody(RegionProvider provider) {
    if (provider.loading && provider.regions.isEmpty) {
      return const Center(child: CircularProgressIndicator());
    }
    if (provider.regions.isEmpty) {
      return ListView(children: [
        Padding(
          padding: const EdgeInsets.only(top: 120, left: 24, right: 24),
          child: Text(provider.error ?? 'Nenhuma colecao ainda.\nToque no + para criar.', textAlign: TextAlign.center),
        ),
      ]);
    }
    return ListView.separated(
      padding: const EdgeInsets.all(16),
      itemCount: provider.regions.length,
      separatorBuilder: (_, _) => const SizedBox(height: 12),
      itemBuilder: (context, index) {
        final r = provider.regions[index];
        return Card(
          child: ListTile(
            leading: _cover(r),
            title: Text(r.name),
            subtitle: Text('${r.visibility} • ${r.cities.length} cidade(s)'),
            onTap: () => _openForm(region: r),
            trailing: IconButton(
              icon: const Icon(Icons.delete_outline),
              onPressed: () => _delete(r),
            ),
          ),
        );
      },
    );
  }

  /// Cover thumbnail; without a photo, falls back to the folder icon.
  Widget _cover(Region region) {
    if (region.coverUrl == null) {
      return const Icon(Icons.folder);
    }
    return ClipRRect(
      borderRadius: BorderRadius.circular(8),
      child: Image.network(
        region.coverUrl!,
        width: 44,
        height: 44,
        fit: BoxFit.cover,
        errorBuilder: (_, _, _) => const Icon(Icons.folder),
      ),
    );
  }
}
