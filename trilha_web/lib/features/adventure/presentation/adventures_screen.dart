import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../auth/presentation/auth_provider.dart';
import 'adventure_provider.dart';
import 'create_adventure_sheet.dart';

/// Lists the logged user's adventures, with pull-to-refresh and FAB to create.
class AdventuresScreen extends StatefulWidget {
  const AdventuresScreen({super.key});

  @override
  State<AdventuresScreen> createState() => _AdventuresScreenState();
}

class _AdventuresScreenState extends State<AdventuresScreen> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _load());
  }

  Future<void> _load() async {
    final userId = context.read<AuthProvider>().userId;
    if (userId != null) {
      await context.read<AdventureProvider>().load(userId);
    }
  }

  Future<void> _openCreate() async {
    final userId = context.read<AuthProvider>().userId;
    if (userId == null) {
      return;
    }
    await showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      builder: (_) => CreateAdventureSheet(userId: userId),
    );
  }

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<AdventureProvider>();
    return Scaffold(
      appBar: AppBar(title: const Text('Minhas aventuras')),
      floatingActionButton: FloatingActionButton(
        onPressed: _openCreate,
        child: const Icon(Icons.add),
      ),
      body: RefreshIndicator(
        onRefresh: _load,
        child: _buildBody(provider),
      ),
    );
  }

  Widget _buildBody(AdventureProvider provider) {
    if (provider.loading && provider.adventures.isEmpty) {
      return const Center(child: CircularProgressIndicator());
    }

    if (provider.adventures.isEmpty) {
      final msg = provider.error ?? 'Nenhuma aventura ainda.\nToque no + para criar.';
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
      itemCount: provider.adventures.length,
      separatorBuilder: (_, _) => const SizedBox(height: 12),
      itemBuilder: (context, index) {
        final adventure = provider.adventures[index];
        return Card(
          child: ListTile(
            leading: const Icon(Icons.terrain),
            title: Text(adventure.destination),
            subtitle: Text(
              [adventure.status, if (adventure.visibility != null) adventure.visibility!]
                  .join(' • '),
            ),
            trailing: const Icon(Icons.chevron_right),
            onTap: () => context.go('/aventuras/${adventure.id}'),
          ),
        );
      },
    );
  }
}
