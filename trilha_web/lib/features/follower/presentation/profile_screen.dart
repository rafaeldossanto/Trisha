import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../../core/network/dio_client.dart';
import '../../../shared/widgets/adventure_grid.dart';
import '../../../shared/widgets/story_avatar.dart';
import '../../adventure/data/adventure_api.dart';
import '../../adventure/domain/adventure.dart';
import '../../friendship/data/user_search_api.dart';
import '../../friendship/presentation/friendship_provider.dart';
import '../../point/data/media_api.dart';
import '../../point/domain/media_item.dart';
import '../data/follower_api.dart';
import '../domain/counters.dart';
import '../domain/follow_status.dart';

/// Perfil de outro usuario, estilo Instagram: cabecalho com avatar +
/// contadores, botoes Seguir/Amigo lado a lado e a grade com as aventuras
/// VISIVEIS dele (o backend filtra por visibilidade; sem nada visivel, a
/// area mostra o cadeado de conta privada).
class ProfileScreen extends StatefulWidget {
  const ProfileScreen({super.key, required this.userCode, required this.name});

  final String userCode;
  final String name;

  @override
  State<ProfileScreen> createState() => _ProfileScreenState();
}

class _ProfileScreenState extends State<ProfileScreen> {
  late final FollowerApi _api = FollowerApi(context.read<DioClient>().dio);
  late final UserSearchApi _userApi = UserSearchApi(context.read<DioClient>().dio);
  late final AdventureApi _adventureApi = AdventureApi(context.read<DioClient>().dio);
  late final MediaApi _mediaApi = MediaApi(context.read<DioClient>().dio);

  final Map<String, Future<List<MediaItem>>> _mediaByAdventure = {};

  // Botoes Seguir/Amigo: mais retangulares (cantos menos arredondados) e um
  // pouco mais baixos que o padrao do tema.
  static final _compactButtonStyle = OutlinedButton.styleFrom(
    minimumSize: const Size.fromHeight(30),
    shape: const RoundedRectangleBorder(
      borderRadius: BorderRadius.all(Radius.circular(6)),
    ),
    textStyle: const TextStyle(fontSize: 13, fontWeight: FontWeight.w600),
    visualDensity: VisualDensity.compact,
  );

  FollowStatus? _followStatus;
  Counters? _counters;
  List<Adventure> _adventures = const [];
  bool _loading = true;
  bool _processing = false;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final followStatus = await _api.status(widget.userCode);
      final counters = await _api.counters(widget.userCode);
      if (!mounted) {
        return;
      }
      setState(() {
        _followStatus = followStatus;
        _counters = counters;
        _loading = false;
      });
    } catch (_) {
      if (mounted) {
        setState(() => _loading = false);
      }
    }
    await _loadAdventures();
  }

  /// Aventuras visiveis deste usuario (o backend filtra pela visibilidade).
  /// O resumo resolve o codigo publico -> userId; qualquer falha mantem so
  /// o cadeado de conta privada.
  Future<void> _loadAdventures() async {
    try {
      final summary = await _userApi.summaryByCode(widget.userCode);
      final page = await _adventureApi.listByUser(summary.id, size: 60);
      if (mounted) {
        setState(() => _adventures = page.content);
      }
    } catch (_) {
      // Grade e complementar; o perfil segue com o cadeado.
    }
  }

  Future<List<MediaItem>> _mediaOf(Adventure adventure) {
    return _mediaByAdventure.putIfAbsent(
      adventure.id,
      () => _mediaApi.listByAdventure(adventure.id, size: 1),
    );
  }

  Future<void> _toggleFollow() async {
    final followStatus = _followStatus;
    if (followStatus == null) {
      return;
    }
    setState(() => _processing = true);
    try {
      if (followStatus.isFollowing) {
        await _api.unfollow(widget.userCode);
      } else {
        await _api.follow(widget.userCode);
      }
      await _load();
    } catch (_) {
      _showMessage('Nao foi possivel atualizar');
    } finally {
      if (mounted) {
        setState(() => _processing = false);
      }
    }
  }

  Future<void> _addFriend() async {
    final error = await context.read<FriendshipProvider>().sendRequest(widget.userCode);
    if (!mounted) {
      return;
    }
    _showMessage(error ?? 'Solicitacao de amizade enviada');
  }

  void _showMessage(String msg) {
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(msg)));
    }
  }

  void _openList(String type, String title) {
    context.push('/usuarios', extra: {
      'codigo': widget.userCode,
      'tipo': type,
      'titulo': title,
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.userCode, style: const TextStyle(fontWeight: FontWeight.w700)),
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : RefreshIndicator(
              onRefresh: _load,
              child: ListView(
                children: [
                  Padding(
                    padding: const EdgeInsets.fromLTRB(16, 8, 16, 0),
                    child: Row(
                      children: [
                        StoryAvatar(name: widget.name, radius: 30),
                        const SizedBox(width: 16),
                        Expanded(
                          child: Row(
                            mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                            children: [
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
                    padding: const EdgeInsets.fromLTRB(16, 12, 16, 8),
                    child: Text(
                      widget.name,
                      style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 13),
                    ),
                  ),
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 16),
                    child: Row(
                      children: [
                        Expanded(child: _followButton()),
                        const SizedBox(width: 8),
                        Expanded(child: _addFriendButton()),
                      ],
                    ),
                  ),
                  const SizedBox(height: 16),
                  const Divider(height: 1),
                  if (_adventures.isNotEmpty) ...[
                    const Padding(
                      padding: EdgeInsets.symmetric(vertical: 6),
                      child: Icon(Icons.grid_on, size: 20),
                    ),
                    AdventureGrid(adventures: _adventures, mediaOf: _mediaOf),
                  ] else
                    Padding(
                      padding: const EdgeInsets.only(top: 64, left: 32, right: 32),
                      child: Column(
                        children: [
                          Container(
                            padding: const EdgeInsets.all(14),
                            decoration: BoxDecoration(
                              shape: BoxShape.circle,
                              border: Border.all(color: Colors.white24, width: 2),
                            ),
                            child: const Icon(Icons.lock_outline, size: 28),
                          ),
                          const SizedBox(height: 12),
                          const Text(
                            'As aventuras deste trilheiro sao privadas',
                            style: TextStyle(fontWeight: FontWeight.w600, fontSize: 13),
                          ),
                          const SizedBox(height: 4),
                          const Text(
                            'Siga e vire amigo para acompanhar as trilhas dele.',
                            textAlign: TextAlign.center,
                            style: TextStyle(fontSize: 12, color: Colors.white54),
                          ),
                        ],
                      ),
                    ),
                ],
              ),
            ),
    );
  }

  Widget _counter(String label, int value, VoidCallback onTap) {
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

  Widget _followButton() {
    final isFollowing = _followStatus?.isFollowing ?? false;
    final child = _processing
        ? const SizedBox(height: 20, width: 20, child: CircularProgressIndicator(strokeWidth: 2))
        : Text(isFollowing ? 'Seguindo' : 'Seguir');
    return isFollowing
        ? OutlinedButton(
            style: _compactButtonStyle,
            onPressed: _processing ? null : _toggleFollow,
            child: child,
          )
        : FilledButton(
            style: _compactButtonStyle,
            onPressed: _processing ? null : _toggleFollow,
            child: child,
          );
  }

  Widget _addFriendButton() {
    final isMutual = _followStatus?.isMutual ?? false;
    return OutlinedButton.icon(
      style: _compactButtonStyle,
      onPressed: isMutual ? _addFriend : null,
      icon: const Icon(Icons.person_add_alt, size: 16),
      label: Text(isMutual ? 'Adicionar amigo' : 'Amigo (siga mutuo)'),
    );
  }
}
