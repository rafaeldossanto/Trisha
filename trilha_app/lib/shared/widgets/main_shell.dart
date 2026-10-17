import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../features/adventure/presentation/create_adventure_sheet.dart';
import '../../features/auth/presentation/auth_provider.dart';

/// Casca das abas principais com a bottom bar estilo Instagram:
/// mapa (home), explorar, criar (+), feed e perfil. O item do meio nao e uma
/// aba — abre o sheet de criar aventura por cima da aba atual.
class MainShell extends StatelessWidget {
  const MainShell({super.key, required this.shell});

  final StatefulNavigationShell shell;

  // Posicao na barra -> branch do router (o "+" nao tem branch).
  static const _branchByNavIndex = {0: 0, 1: 1, 3: 2, 4: 3};

  void _onTap(BuildContext context, int navIndex) {
    if (navIndex == 2) {
      _openCreate(context);
      return;
    }
    final branch = _branchByNavIndex[navIndex]!;
    shell.goBranch(branch, initialLocation: branch == shell.currentIndex);
  }

  Future<void> _openCreate(BuildContext context) async {
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
    final name = context.watch<AuthProvider>().user?.name ?? '';

    return Scaffold(
      body: shell,
      bottomNavigationBar: Container(
        decoration: BoxDecoration(
          color: Theme.of(context).scaffoldBackgroundColor,
          border: const Border(top: BorderSide(color: Colors.white12)),
        ),
        child: SafeArea(
          child: SizedBox(
            height: 52,
            child: Row(
              children: [
                _item(context, 0, Icons.map_outlined, Icons.map),
                _item(context, 1, Icons.search_outlined, Icons.search),
                _item(context, 2, Icons.add_box_outlined, Icons.add_box),
                _item(context, 3, Icons.photo_library_outlined, Icons.photo_library),
                _profileItem(context, name),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _item(BuildContext context, int navIndex, IconData icon, IconData activeIcon) {
    final selected = _branchByNavIndex[navIndex] == shell.currentIndex;
    return Expanded(
      child: IconButton(
        onPressed: () => _onTap(context, navIndex),
        icon: Icon(selected ? activeIcon : icon, size: 28, color: Colors.white),
      ),
    );
  }

  Widget _profileItem(BuildContext context, String name) {
    final selected = shell.currentIndex == 3;
    return Expanded(
      child: InkWell(
        onTap: () => _onTap(context, 4),
        child: Center(
          child: Container(
            padding: const EdgeInsets.all(2),
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              border: Border.all(color: selected ? Colors.white : Colors.transparent, width: 1.5),
            ),
            child: CircleAvatar(
              radius: 13,
              backgroundColor: Theme.of(context).colorScheme.surfaceContainerHighest,
              child: Text(
                name.isNotEmpty ? name[0].toUpperCase() : '?',
                style: const TextStyle(fontSize: 12, color: Colors.white),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
