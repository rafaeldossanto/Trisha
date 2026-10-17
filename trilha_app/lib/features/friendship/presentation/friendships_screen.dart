import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../auth/presentation/auth_provider.dart';
import 'friendship_provider.dart';

/// Friendships in tabs: friends, pending requests and search to add.
class FriendshipsScreen extends StatefulWidget {
  const FriendshipsScreen({super.key});

  @override
  State<FriendshipsScreen> createState() => _FriendshipsScreenState();
}

class _FriendshipsScreenState extends State<FriendshipsScreen> {
  final _searchController = TextEditingController();

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => context.read<FriendshipProvider>().load());
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return DefaultTabController(
      length: 3,
      child: Scaffold(
        appBar: AppBar(
          title: const Text('Amizades'),
          bottom: const TabBar(
            tabs: [
              Tab(text: 'Amigos'),
              Tab(text: 'Pendentes'),
              Tab(text: 'Buscar'),
            ],
          ),
        ),
        body: TabBarView(
          children: [_friendsTab(), _pendingTab(), _searchTab()],
        ),
      ),
    );
  }

  Widget _friendsTab() {
    final provider = context.watch<FriendshipProvider>();
    final myId = context.read<AuthProvider>().userId;
    if (provider.friends.isEmpty) {
      return _empty('Voce ainda nao tem amigos.');
    }
    return ListView(
      padding: const EdgeInsets.all(16),
      children: provider.friends.map((f) {
        final other = f.requesterId == myId ? f.receiverId : f.requesterId;
        return Card(
          child: ListTile(
            leading: const Icon(Icons.person),
            title: Text(other),
            subtitle: const Text('Amigo'),
          ),
        );
      }).toList(),
    );
  }

  Widget _pendingTab() {
    final provider = context.watch<FriendshipProvider>();
    if (provider.pending.isEmpty) {
      return _empty('Nenhuma solicitacao pendente.');
    }
    return ListView(
      padding: const EdgeInsets.all(16),
      children: provider.pending.map((f) {
        return Card(
          child: ListTile(
            leading: const Icon(Icons.person_add),
            title: Text(f.requesterId),
            subtitle: const Text('Quer ser seu amigo'),
            trailing: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                IconButton(
                  tooltip: 'Aceitar',
                  icon: const Icon(Icons.check, color: Colors.green),
                  onPressed: () => context.read<FriendshipProvider>().respond(f.id, 'ACEITA'),
                ),
                IconButton(
                  tooltip: 'Recusar',
                  icon: const Icon(Icons.close, color: Colors.red),
                  onPressed: () => context.read<FriendshipProvider>().respond(f.id, 'RECUSADA'),
                ),
              ],
            ),
          ),
        );
      }).toList(),
    );
  }

  Widget _searchTab() {
    final provider = context.watch<FriendshipProvider>();
    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.all(16),
          child: TextField(
            controller: _searchController,
            decoration: const InputDecoration(
              labelText: 'Buscar por codigo (ex: rafael#1)',
              prefixIcon: Icon(Icons.search),
            ),
            onChanged: (term) => context.read<FriendshipProvider>().search(term),
          ),
        ),
        Expanded(
          child: ListView(
            children: provider.searchResults.map((u) {
              return ListTile(
                leading: const Icon(Icons.person_outline),
                title: Text(u.name),
                subtitle: Text(u.userCode),
                trailing: const Icon(Icons.chevron_right),
                onTap: () => context.push('/perfil', extra: u),
              );
            }).toList(),
          ),
        ),
      ],
    );
  }

  Widget _empty(String msg) => Center(
        child: Padding(padding: const EdgeInsets.all(24), child: Text(msg, textAlign: TextAlign.center)),
      );
}
