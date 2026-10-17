import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../core/theme/app_theme.dart';
import '../../features/adventure/presentation/create_adventure_sheet.dart';
import '../../features/auth/presentation/auth_provider.dart';

/// Casca das abas principais, responsiva:
/// - janela larga (web/desktop): side nav a esquerda estilo Instagram
///   (wordmark + itens com rotulo; encolhe para so-icones em larguras medias)
///   e o conteudo central com largura maxima por aba (o mapa segue full-bleed);
/// - janela estreita: a bottom bar original do app (mapa, explorar, criar (+),
///   feed e perfil). O item de criar nao e uma aba — abre o sheet de criar
///   aventura por cima da aba atual.
class MainShell extends StatelessWidget {
  const MainShell({super.key, required this.shell});

  final StatefulNavigationShell shell;

  /// A partir daqui a navegacao vira side nav.
  static const double _desktopBreakpoint = 900;

  /// A partir daqui a side nav mostra os rotulos (abaixo, so icones).
  static const double _labelsBreakpoint = 1150;

  // Posicao na barra -> branch do router (o "+" nao tem branch).
  static const _branchByNavIndex = {0: 0, 1: 1, 3: 2, 4: 3};

  // Largura maxima do conteudo por branch: mapa full-bleed, feed em coluna
  // estreita (estilo Instagram) e o resto em pagina larga.
  static const _maxWidthByBranch = {
    0: double.infinity,
    1: 935.0,
    2: 630.0,
    3: 935.0,
  };

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
    final width = MediaQuery.sizeOf(context).width;

    if (width >= _desktopBreakpoint) {
      return _desktop(context, name, showLabels: width >= _labelsBreakpoint);
    }
    return _mobile(context, name);
  }

  // ---------------------------------------------------------------- desktop

  Widget _desktop(BuildContext context, String name, {required bool showLabels}) {
    final maxWidth = _maxWidthByBranch[shell.currentIndex]!;

    return Scaffold(
      body: Row(
        children: [
          _sideNav(context, name, showLabels: showLabels),
          Expanded(
            child: Center(
              child: Container(
                constraints: BoxConstraints(maxWidth: maxWidth),
                decoration: maxWidth.isFinite
                    ? const BoxDecoration(
                        border: Border.symmetric(
                          vertical: BorderSide(color: Colors.white12),
                        ),
                      )
                    : null,
                child: shell,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _sideNav(BuildContext context, String name, {required bool showLabels}) {
    return Container(
      width: showLabels ? 244 : 76,
      decoration: BoxDecoration(
        color: Theme.of(context).scaffoldBackgroundColor,
        border: const Border(right: BorderSide(color: Colors.white12)),
      ),
      child: SafeArea(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Padding(
              padding: const EdgeInsets.fromLTRB(20, 24, 20, 28),
              child: showLabels
                  ? const Text('Trisha', style: AppTheme.wordmark)
                  : const Icon(Icons.terrain, size: 28, color: Colors.white),
            ),
            _navTile(context, 0, Icons.map_outlined, Icons.map, 'Mapa', showLabels),
            _navTile(context, 1, Icons.search_outlined, Icons.search, 'Explorar', showLabels),
            _navTile(context, 2, Icons.add_box_outlined, Icons.add_box, 'Criar', showLabels),
            _navTile(context, 3, Icons.photo_library_outlined, Icons.photo_library, 'Feed', showLabels),
            _profileTile(context, name, showLabels),
          ],
        ),
      ),
    );
  }

  Widget _navTile(
    BuildContext context,
    int navIndex,
    IconData icon,
    IconData activeIcon,
    String label,
    bool showLabels,
  ) {
    final selected = _branchByNavIndex[navIndex] == shell.currentIndex;
    return _tile(
      context: context,
      navIndex: navIndex,
      selected: selected,
      showLabels: showLabels,
      label: label,
      leading: Icon(selected ? activeIcon : icon, size: 26, color: Colors.white),
    );
  }

  Widget _profileTile(BuildContext context, String name, bool showLabels) {
    final selected = shell.currentIndex == 3;
    return _tile(
      context: context,
      navIndex: 4,
      selected: selected,
      showLabels: showLabels,
      label: 'Perfil',
      leading: _avatar(context, name, selected),
    );
  }

  Widget _tile({
    required BuildContext context,
    required int navIndex,
    required bool selected,
    required bool showLabels,
    required String label,
    required Widget leading,
  }) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
      child: InkWell(
        onTap: () => _onTap(context, navIndex),
        borderRadius: BorderRadius.circular(10),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
          child: Row(
            mainAxisAlignment:
                showLabels ? MainAxisAlignment.start : MainAxisAlignment.center,
            children: [
              leading,
              if (showLabels) ...[
                const SizedBox(width: 14),
                Text(
                  label,
                  style: TextStyle(
                    fontSize: 15,
                    fontWeight: selected ? FontWeight.w700 : FontWeight.w400,
                  ),
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }

  // ----------------------------------------------------------------- mobile

  Widget _mobile(BuildContext context, String name) {
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
        child: Center(child: _avatar(context, name, selected)),
      ),
    );
  }

  Widget _avatar(BuildContext context, String name, bool selected) {
    return Container(
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
    );
  }
}
