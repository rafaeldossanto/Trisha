import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../../core/network/dio_client.dart';
import '../../../core/theme/app_theme.dart';
import '../../../shared/widgets/story_avatar.dart';
import '../../auth/presentation/auth_provider.dart';
import '../../follower/data/follower_api.dart';
import '../../friendship/domain/public_user.dart';
import '../../point/data/media_api.dart';
import '../../point/domain/media_item.dart';
import '../data/feed_api.dart';
import '../domain/feed_adventure.dart';

/// Feed estilo Instagram: wordmark no topo, fileira de stories (quem o usuario
/// segue) e os posts do feed real do BFF — as suas aventuras e as visiveis de
/// quem voce segue, com o autor resolvido.
class FeedScreen extends StatefulWidget {
  const FeedScreen({super.key});

  @override
  State<FeedScreen> createState() => _FeedScreenState();
}

class _FeedScreenState extends State<FeedScreen> {
  late final FeedApi _feedApi = FeedApi(context.read<DioClient>().dio);
  late final FollowerApi _followerApi = FollowerApi(context.read<DioClient>().dio);
  late final MediaApi _mediaApi = MediaApi(context.read<DioClient>().dio);

  // Cache das midias por aventura para o rebuild do feed nao refazer o GET.
  final Map<String, Future<List<MediaItem>>> _mediaByAdventure = {};
  final _scrollController = ScrollController();

  List<FeedAdventure> _posts = const [];
  List<PublicUser> _following = const [];
  bool _loading = true;
  bool _loadingMore = false;
  bool _hasMore = true;
  int _page = 0;
  String? _error;

  // Stories minimizam quando o feed rola para baixo e reaparecem no topo.
  static const _storiesHeight = 104.0;
  static const _storiesCollapseOffset = 40.0;
  bool _storiesVisible = true;

  @override
  void initState() {
    super.initState();
    _scrollController.addListener(_onScroll);
    WidgetsBinding.instance.addPostFrameCallback((_) => _load());
  }

  @override
  void dispose() {
    _scrollController.dispose();
    super.dispose();
  }

  /// Paginacao infinita (perto do fim busca a proxima pagina) e colapso dos
  /// stories (longe do topo eles minimizam; de volta ao topo, reaparecem).
  void _onScroll() {
    if (_scrollController.position.pixels >=
        _scrollController.position.maxScrollExtent - 400) {
      _loadMore();
    }
    final showStories = _scrollController.position.pixels <= _storiesCollapseOffset;
    if (showStories != _storiesVisible) {
      setState(() => _storiesVisible = showStories);
    }
  }

  Future<void> _loadMore() async {
    if (_loadingMore || !_hasMore || _loading) {
      return;
    }
    _loadingMore = true;
    try {
      final next = await _feedApi.feed(page: _page + 1);
      if (mounted) {
        setState(() {
          _page += 1;
          _posts = [..._posts, ...next.content];
          _hasMore = _page + 1 < next.totalPages;
        });
      }
    } catch (_) {
      // Proximo scroll tenta de novo.
    } finally {
      _loadingMore = false;
    }
  }

  Future<void> _load() async {
    final auth = context.read<AuthProvider>();
    await auth.ensureUser();
    if (!mounted) {
      return;
    }

    try {
      _mediaByAdventure.clear();
      final page = await _feedApi.feed();
      if (mounted) {
        setState(() {
          _posts = page.content;
          _page = 0;
          _hasMore = page.totalPages > 1;
          _error = null;
          _loading = false;
        });
      }
    } catch (_) {
      if (mounted) {
        setState(() {
          _error = 'Nao foi possivel carregar o feed';
          _loading = false;
        });
      }
    }

    final code = auth.user?.userCode ?? '';
    if (code.isNotEmpty) {
      try {
        final following = await _followerApi.following(code);
        if (mounted) {
          setState(() => _following = following);
        }
      } catch (_) {
        // Stories sao opcionais; o feed continua sem eles.
      }
    }
  }

  Future<List<MediaItem>> _mediaOf(FeedAdventure post) {
    return _mediaByAdventure.putIfAbsent(
      post.id,
      () => _mediaApi.listByAdventure(post.id),
    );
  }

  @override
  Widget build(BuildContext context) {
    final auth = context.watch<AuthProvider>();

    return Scaffold(
      appBar: AppBar(
        title: const Text('Trisha', style: AppTheme.wordmark),
        actions: [
          IconButton(
            tooltip: 'Amizades',
            icon: const Icon(Icons.favorite_border),
            onPressed: () => context.push('/amizades'),
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
        child: _buildBody(auth),
      ),
    );
  }

  Widget _buildBody(AuthProvider auth) {
    if (_loading && _posts.isEmpty) {
      return const Center(child: CircularProgressIndicator());
    }

    return Column(
      children: [
        _collapsibleStories(auth),
        const Divider(height: 1),
        Expanded(
          child: ListView(
            controller: _scrollController,
            children: [
              if (_posts.isEmpty)
                Padding(
                  padding: const EdgeInsets.only(top: 96, left: 32, right: 32),
                  child: Column(
                    children: [
                      Icon(Icons.photo_camera_outlined, size: 56, color: Colors.white38),
                      const SizedBox(height: 12),
                      Text(
                        _error ??
                            'Nada por aqui ainda.\nSiga trilheiros ou toque no + para criar uma aventura.',
                        textAlign: TextAlign.center,
                        style: const TextStyle(color: Colors.white70),
                      ),
                    ],
                  ),
                )
              else ...[
                ..._posts.map(
                  (post) => _PostCard(
                    post: post,
                    isMine: post.userId == auth.userId,
                    media: _mediaOf(post),
                  ),
                ),
                if (_hasMore)
                  const Padding(
                    padding: EdgeInsets.symmetric(vertical: 16),
                    child: Center(
                      child: SizedBox(
                        height: 22,
                        width: 22,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      ),
                    ),
                  ),
              ],
            ],
          ),
        ),
      ],
    );
  }

  /// Stories fora da lista, num cabecalho que encolhe (altura -> 0) quando o
  /// usuario rola o feed para baixo e reabre quando ele volta ao topo.
  Widget _collapsibleStories(AuthProvider auth) {
    return ClipRect(
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 220),
        curve: Curves.easeInOut,
        height: _storiesVisible ? _storiesHeight : 0,
        child: OverflowBox(
          alignment: Alignment.topCenter,
          minHeight: _storiesHeight,
          maxHeight: _storiesHeight,
          child: _storiesRow(auth),
        ),
      ),
    );
  }

  Widget _storiesRow(AuthProvider auth) {
    final myName = auth.user?.name ?? 'Voce';
    return SizedBox(
      height: _storiesHeight,
      child: ListView.separated(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
        itemCount: _following.length + 1,
        separatorBuilder: (_, _) => const SizedBox(width: 12),
        itemBuilder: (context, index) {
          if (index == 0) {
            return _story(
              name: 'Seu perfil',
              avatarName: myName,
              showRing: false,
              onTap: () => context.go('/perfil-meu'),
            );
          }
          final user = _following[index - 1];
          return _story(
            name: user.name,
            avatarName: user.name,
            showRing: true,
            onTap: () => context.push('/perfil', extra: user),
          );
        },
      ),
    );
  }

  Widget _story({
    required String name,
    required String avatarName,
    required bool showRing,
    required VoidCallback onTap,
  }) {
    return InkWell(
      onTap: onTap,
      child: SizedBox(
        width: 68,
        child: Column(
          children: [
            StoryAvatar(name: avatarName, radius: 26, showRing: showRing),
            const SizedBox(height: 4),
            Text(
              name,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: const TextStyle(fontSize: 11),
            ),
          ],
        ),
      ),
    );
  }
}

/// Um post do feed: cabecalho com o autor real, carrossel de fotos da aventura
/// (placeholder quando ainda nao tem midia), acoes e legenda.
class _PostCard extends StatefulWidget {
  const _PostCard({
    required this.post,
    required this.isMine,
    required this.media,
  });

  final FeedAdventure post;
  final bool isMine;
  final Future<List<MediaItem>> media;

  @override
  State<_PostCard> createState() => _PostCardState();
}

class _PostCardState extends State<_PostCard> {
  int _page = 0;
  bool _liked = false;

  void _openAuthor() {
    if (widget.isMine) {
      context.go('/perfil-meu');
      return;
    }
    if (widget.post.userCode.isNotEmpty) {
      context.push(
        '/perfil',
        extra: PublicUser(userCode: widget.post.userCode, name: widget.post.userName),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final post = widget.post;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _header(context),
        FutureBuilder<List<MediaItem>>(
          future: widget.media,
          builder: (context, snapshot) {
            final photos = snapshot.data ?? const <MediaItem>[];
            if (photos.isEmpty) {
              return _placeholder(context);
            }
            return _carousel(photos);
          },
        ),
        _actions(context),
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 12),
          child: Text.rich(
            TextSpan(
              children: [
                TextSpan(
                  text: '${post.userName} ',
                  style: const TextStyle(fontWeight: FontWeight.w600),
                ),
                TextSpan(text: post.destination),
              ],
            ),
          ),
        ),
        Padding(
          padding: const EdgeInsets.only(left: 12, top: 4, bottom: 16),
          child: InkWell(
            onTap: () => context.push('/aventuras/${post.id}'),
            child: const Text(
              'Ver caminhos da aventura',
              style: TextStyle(color: Colors.white54, fontSize: 13),
            ),
          ),
        ),
      ],
    );
  }

  Widget _header(BuildContext context) {
    final post = widget.post;
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      child: Row(
        children: [
          InkWell(
            onTap: _openAuthor,
            child: StoryAvatar(name: post.userName, radius: 16),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: InkWell(
              onTap: _openAuthor,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    widget.isMine ? '${post.userName} (voce)' : post.userName,
                    style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 13),
                  ),
                  Text(
                    post.destination,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(fontSize: 11, color: Colors.white54),
                  ),
                  Text(
                    post.metricsLabel,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(fontSize: 10, color: Colors.white38),
                  ),
                ],
              ),
            ),
          ),
          _statusChip(context, post.status),
        ],
      ),
    );
  }

  Widget _statusChip(BuildContext context, String status) {
    final color = Theme.of(context).colorScheme.primary;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
      decoration: BoxDecoration(
        border: Border.all(color: color),
        borderRadius: BorderRadius.circular(999),
      ),
      child: Text(status, style: TextStyle(fontSize: 10, color: color)),
    );
  }

  Widget _carousel(List<MediaItem> photos) {
    return Column(
      children: [
        AspectRatio(
          aspectRatio: 1,
          child: PageView.builder(
            itemCount: photos.length,
            onPageChanged: (page) => setState(() => _page = page),
            itemBuilder: (context, index) => Image.network(
              photos[index].url,
              fit: BoxFit.cover,
              errorBuilder: (context, _, _) => _placeholder(context),
              loadingBuilder: (context, child, progress) => progress == null
                  ? child
                  : Container(
                      color: Theme.of(context).colorScheme.surface,
                      child: const Center(child: CircularProgressIndicator(strokeWidth: 2)),
                    ),
            ),
          ),
        ),
        if (photos.length > 1)
          Padding(
            padding: const EdgeInsets.only(top: 8),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: List.generate(
                photos.length,
                (i) => Container(
                  width: 6,
                  height: 6,
                  margin: const EdgeInsets.symmetric(horizontal: 2),
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    color: i == _page ? Theme.of(context).colorScheme.primary : Colors.white24,
                  ),
                ),
              ),
            ),
          ),
      ],
    );
  }

  Widget _placeholder(BuildContext context) {
    final theme = Theme.of(context);
    return AspectRatio(
      aspectRatio: 1,
      child: Container(
        decoration: BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [
              theme.colorScheme.primary.withValues(alpha: 0.25),
              theme.colorScheme.surface,
            ],
          ),
        ),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.terrain, size: 64, color: theme.colorScheme.primary),
            const SizedBox(height: 8),
            const Text(
              'Sem fotos ainda — registre pontos na trilha',
              style: TextStyle(color: Colors.white54, fontSize: 12),
            ),
          ],
        ),
      ),
    );
  }

  Widget _actions(BuildContext context) {
    final post = widget.post;
    return Row(
      children: [
        IconButton(
          onPressed: () => setState(() => _liked = !_liked),
          icon: Icon(
            _liked ? Icons.favorite : Icons.favorite_border,
            color: _liked ? Colors.redAccent : Colors.white,
          ),
        ),
        IconButton(
          tooltip: 'Caminhos',
          onPressed: () => context.push('/aventuras/${post.id}'),
          icon: const Icon(Icons.route_outlined),
        ),
        IconButton(
          tooltip: 'Mapa da aventura',
          onPressed: () => context.push('/aventuras/${post.id}/mapa'),
          icon: const Icon(Icons.location_on_outlined),
        ),
      ],
    );
  }
}
