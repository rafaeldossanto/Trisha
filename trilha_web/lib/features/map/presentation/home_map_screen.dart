import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:go_router/go_router.dart';
import 'package:latlong2/latlong.dart';
import 'package:provider/provider.dart';

import '../../../core/network/dio_client.dart';
import '../../../core/theme/app_theme.dart';
import '../../../shared/widgets/story_avatar.dart';
import '../../adventure/data/adventure_api.dart';
import '../../adventure/domain/adventure.dart';
import '../../auth/presentation/auth_provider.dart';
import '../../friendship/domain/public_user.dart';
import '../../path/data/path_api.dart';
import '../../path/domain/discovered_trail.dart';
import '../../tracking/data/tracking_api.dart';
import '../../tracking/domain/live_session.dart';
import '../data/location_service.dart';
import '../data/place_search_service.dart';
import '../domain/place.dart';

/// Home do app (estilo Strava/helios): mapa escuro em tela cheia com as
/// trilhas rastreadas das aventuras plotadas em cores vivas. Embaixo, um
/// carrossel de cards — deslizar centraliza o mapa na trilha daquela aventura.
/// As trilhas da comunidade (aventuras publicas/do grupo) sao carregadas por
/// viewport: a cada pan/zoom o app busca so a area visivel, nunca o mapa todo.
class HomeMapScreen extends StatefulWidget {
  const HomeMapScreen({super.key});

  @override
  State<HomeMapScreen> createState() => _HomeMapScreenState();
}

/// Uma aventura pronta para o mapa: cor propria, segmentos (um por caminho
/// rastreado) e a distancia somada dos caminhos.
class _Trail {
  const _Trail(this.adventure, this.color, this.segments, this.totalKm, this.pathCount);

  final Adventure adventure;
  final Color color;
  final List<List<LatLng>> segments;
  final double totalKm;
  final int pathCount;

  bool get hasTrack => segments.isNotEmpty;

  List<LatLng> get allPoints => [for (final s in segments) ...s];
}

class _HomeMapScreenState extends State<HomeMapScreen> {
  static const _fallbackCenter = LatLng(-20.4350, -41.7920);

  // Abaixo deste zoom a bbox fica grande demais — nao busca a comunidade.
  static const _minDiscoverZoom = 10.0;

  // Camadas de tile (mundo real, OSM): escura padrao e topografica com relevo
  // e curvas de nivel (OpenTopoMap), para leitura de altitude na trilha.
  static const _darkTiles = 'https://a.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}.png';
  static const _topoTiles = 'https://a.tile.opentopomap.org/{z}/{x}/{y}.png';

  // Paleta de trilhas sobre o mapa escuro (uma cor por aventura, ciclando).
  static const _palette = [
    Color(0xFF4CAF7D),
    Color(0xFF7C4DFF),
    Color(0xFF00E5FF),
    Color(0xFFFFC400),
    Color(0xFFFF5252),
    Color(0xFF69F0AE),
    Color(0xFFFF80AB),
    Color(0xFF40C4FF),
  ];

  final _mapController = MapController();
  final _locationService = LocationService();
  final _pageController = PageController(viewportFraction: 0.88);
  final _placeSearchService = PlaceSearchService();
  final _placeSearchController = TextEditingController();

  late final PathApi _pathApi = PathApi(context.read<DioClient>().dio);
  late final TrackingApi _trackingApi = TrackingApi(context.read<DioClient>().dio);

  LatLng? _position;
  bool _loading = true;
  bool _mapReady = false;
  bool _topoLayer = false;
  List<_Trail> _trails = const [];
  List<DiscoveredTrail> _community = const [];
  List<LiveSession> _live = const [];
  Timer? _discoverDebounce;

  // Busca de lugar (luneta): campo sobre o mapa, sugestoes do geocoding e um
  // pin temporario no lugar escolhido.
  bool _placeSearchOpen = false;
  bool _placeSearching = false;
  List<Place> _places = const [];
  Place? _foundPlace;
  Timer? _placeSearchDebounce;

  // Hit-test das polylines da comunidade: toque abre o card da trilha.
  final LayerHitNotifier<DiscoveredTrail> _communityHit = ValueNotifier(null);

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _locate();
      _load();
    });
  }

  @override
  void dispose() {
    _discoverDebounce?.cancel();
    _placeSearchDebounce?.cancel();
    _placeSearchController.dispose();
    _pageController.dispose();
    super.dispose();
  }

  /// Debounce dos gestos de pan/zoom: so consulta a comunidade quando o mapa
  /// "assenta", buscando apenas a bbox visivel no zoom atual.
  void _onMapEvent(MapEvent event) {
    _discoverDebounce?.cancel();
    _discoverDebounce = Timer(const Duration(milliseconds: 700), _discoverVisible);
  }

  Future<void> _discoverVisible() async {
    if (!_mapReady || !mounted) {
      return;
    }
    final camera = _mapController.camera;
    if (camera.zoom < _minDiscoverZoom) {
      if (_community.isNotEmpty) {
        setState(() => _community = const []);
      }
      return;
    }
    final bounds = camera.visibleBounds;
    try {
      final trails = await _pathApi.discover(
        minLat: bounds.south,
        minLng: bounds.west,
        maxLat: bounds.north,
        maxLng: bounds.east,
      );
      if (mounted) {
        setState(() => _community = trails);
      }
    } catch (_) {
      // Comunidade e opcional; as trilhas proprias continuam no mapa.
    }
    await _refreshLive();
  }

  /// Quem esta trilhando agora (e o usuario pode assistir). Atualizado junto
  /// com a descoberta por viewport — lista pequena, sem bbox.
  Future<void> _refreshLive() async {
    try {
      final live = await _trackingApi.liveSessions();
      if (mounted) {
        setState(() => _live = live);
      }
    } catch (_) {
      // Ao vivo e opcional; o mapa segue com as trilhas gravadas.
    }
  }

  Future<void> _locate() async {
    final pos = await _locationService.currentPosition();
    if (!mounted) {
      return;
    }
    setState(() => _position = pos == null ? null : LatLng(pos.latitude, pos.longitude));
  }

  Future<void> _load() async {
    setState(() => _loading = true);
    final auth = context.read<AuthProvider>();
    await auth.ensureUser();
    if (!mounted) {
      return;
    }
    final userId = auth.userId;
    if (userId == null) {
      setState(() => _loading = false);
      return;
    }

    final dio = context.read<DioClient>().dio;
    final adventureApi = AdventureApi(dio);
    final pathApi = PathApi(dio);
    final trackingApi = TrackingApi(dio);

    final trails = <_Trail>[];
    try {
      final adventures = (await adventureApi.listByUser(userId, size: 50)).content;
      for (var i = 0; i < adventures.length; i++) {
        final adventure = adventures[i];
        final color = _palette[i % _palette.length];
        try {
          final paths = (await pathApi.listByAdventure(adventure.id)).content;
          final segments = <List<LatLng>>[];
          var km = 0.0;
          for (final path in paths) {
            km += path.totalDistanceKm ?? 0;
            final points = await trackingApi.pointsByPath(path.id);
            if (points.length >= 2) {
              points.sort((a, b) => (a.order ?? 0).compareTo(b.order ?? 0));
              segments.add([for (final p in points) LatLng(p.latitude, p.longitude)]);
            }
          }
          trails.add(_Trail(adventure, color, segments, km, paths.length));
        } catch (_) {
          trails.add(_Trail(adventure, color, const [], 0, 0));
        }
      }
    } catch (_) {
      // Sem aventuras carregadas o mapa fica so com a localizacao atual.
    }

    if (!mounted) {
      return;
    }
    setState(() {
      _trails = trails;
      _loading = false;
    });
    _fitAll();
  }

  void _fit(List<LatLng> points) {
    if (!_mapReady || points.isEmpty) {
      return;
    }
    if (points.length == 1) {
      _mapController.move(points.first, 14);
      return;
    }
    _mapController.fitCamera(
      CameraFit.bounds(
        bounds: LatLngBounds.fromPoints(points),
        padding: const EdgeInsets.fromLTRB(48, 120, 48, 220),
      ),
    );
  }

  void _fitAll() {
    final all = [for (final t in _trails) ...t.allPoints];
    if (all.isNotEmpty) {
      _fit(all);
    } else if (_position != null && _mapReady) {
      _mapController.move(_position!, 13);
    }
  }

  void _onCardChanged(int index) {
    final trail = _trails[index];
    if (trail.hasTrack) {
      _fit(trail.allPoints);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Stack(
        children: [
          FlutterMap(
            mapController: _mapController,
            options: MapOptions(
              initialCenter: _position ?? _fallbackCenter,
              initialZoom: 11,
              onMapReady: () {
                _mapReady = true;
                _fitAll();
                _discoverVisible();
              },
              onMapEvent: _onMapEvent,
            ),
            children: [
              TileLayer(
                urlTemplate: _topoLayer ? _topoTiles : _darkTiles,
                userAgentPackageName: 'com.trilha.trilha_app',
              ),
              GestureDetector(
                onTap: () {
                  final hit = _communityHit.value;
                  final trail = hit?.hitValues.firstOrNull;
                  if (trail != null) {
                    _showCommunityTrail(trail);
                  }
                },
                child: PolylineLayer(
                  hitNotifier: _communityHit,
                  polylines: [
                    for (final trail in _community)
                      if (trail.points.length >= 2) ...[
                        Polyline(
                          points: [for (final p in trail.points) LatLng(p.latitude, p.longitude)],
                          strokeWidth: 6,
                          color: _communityColor(trail).withValues(alpha: 0.18),
                          hitValue: trail,
                        ),
                        Polyline(
                          points: [for (final p in trail.points) LatLng(p.latitude, p.longitude)],
                          strokeWidth: 2.5,
                          color: _communityColor(trail).withValues(alpha: 0.85),
                          hitValue: trail,
                        ),
                      ],
                  ],
                ),
              ),
              PolylineLayer(
                polylines: [
                  for (final trail in _trails)
                    for (final segment in trail.segments) ...[
                      Polyline(
                        points: segment,
                        strokeWidth: 8,
                        color: trail.color.withValues(alpha: 0.25),
                      ),
                      Polyline(
                        points: segment,
                        strokeWidth: 3,
                        color: trail.color,
                      ),
                    ],
                ],
              ),
              if (_live.isNotEmpty)
                MarkerLayer(
                  markers: [
                    for (final session in _live)
                      Marker(
                        point: LatLng(session.latitude, session.longitude),
                        width: 88,
                        height: 46,
                        child: GestureDetector(
                          onTap: () => context.push('/ao-vivo', extra: session),
                          child: Column(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              Container(
                                width: 14,
                                height: 14,
                                decoration: BoxDecoration(
                                  shape: BoxShape.circle,
                                  color: const Color(0xFFFF5252),
                                  border: Border.all(color: Colors.white, width: 2),
                                ),
                              ),
                              const SizedBox(height: 2),
                              Container(
                                padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 1),
                                decoration: BoxDecoration(
                                  color: const Color(0xFFFF5252),
                                  borderRadius: BorderRadius.circular(4),
                                ),
                                child: Text(
                                  '● ${session.userName}',
                                  maxLines: 1,
                                  overflow: TextOverflow.ellipsis,
                                  style: const TextStyle(
                                    fontSize: 9,
                                    fontWeight: FontWeight.w700,
                                    color: Colors.white,
                                  ),
                                ),
                              ),
                            ],
                          ),
                        ),
                      ),
                  ],
                ),
              if (_position != null)
                MarkerLayer(
                  markers: [
                    Marker(
                      point: _position!,
                      width: 36,
                      height: 36,
                      child: Icon(
                        Icons.my_location,
                        color: Theme.of(context).colorScheme.primary,
                        size: 28,
                      ),
                    ),
                  ],
                ),
              if (_foundPlace != null)
                MarkerLayer(
                  markers: [
                    Marker(
                      point: LatLng(_foundPlace!.latitude, _foundPlace!.longitude),
                      width: 44,
                      height: 44,
                      alignment: Alignment.topCenter,
                      child: GestureDetector(
                        onTap: () => setState(() => _foundPlace = null),
                        child: Icon(
                          Icons.place,
                          color: Theme.of(context).colorScheme.primary,
                          size: 38,
                        ),
                      ),
                    ),
                  ],
                ),
            ],
          ),
          if (_placeSearchOpen) _placeSearchOverlay(context) else _topBar(context),
          if (_loading)
            const Positioned(
              top: 100,
              left: 0,
              right: 0,
              child: Center(
                child: Card(
                  child: Padding(
                    padding: EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                    child: Text('Carregando trilhas...'),
                  ),
                ),
              ),
            ),
          if (!_loading && _trails.isEmpty) _emptyOverlay(context),
          if (_trails.isNotEmpty) _carousel(),
          if (_community.isNotEmpty) _communityChip(context),
          Positioned(
            right: 12,
            bottom: _trails.isEmpty ? 24 : 148,
            child: Column(
              children: [
                FloatingActionButton.small(
                  heroTag: 'layers',
                  tooltip: _topoLayer ? 'Mapa escuro' : 'Relevo (topografico)',
                  backgroundColor: Theme.of(context).colorScheme.surface,
                  foregroundColor: _topoLayer ? Theme.of(context).colorScheme.primary : Colors.white,
                  onPressed: () => setState(() => _topoLayer = !_topoLayer),
                  child: const Icon(Icons.layers_outlined),
                ),
                const SizedBox(height: 8),
                FloatingActionButton.small(
                  heroTag: 'locate',
                  backgroundColor: Theme.of(context).colorScheme.surface,
                  foregroundColor: Colors.white,
                  onPressed: () async {
                    await _locate();
                    if (_position != null && _mapReady) {
                      _mapController.move(_position!, 15);
                    }
                  },
                  child: const Icon(Icons.my_location),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _topBar(BuildContext context) {
    return Positioned(
      top: 0,
      left: 0,
      right: 0,
      child: Container(
        decoration: BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
            colors: [Colors.black.withValues(alpha: 0.7), Colors.transparent],
          ),
        ),
        child: SafeArea(
          bottom: false,
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
            child: Row(
              children: [
                const Text('Trisha', style: AppTheme.wordmark),
                const Spacer(),
                IconButton(
                  tooltip: 'Buscar lugar',
                  icon: const Icon(Icons.search),
                  onPressed: () => setState(() => _placeSearchOpen = true),
                ),
                IconButton(
                  tooltip: 'Sair',
                  icon: const Icon(Icons.logout),
                  onPressed: () => context.read<AuthProvider>().logout(),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  /// Debounce da digitacao: evita buscar a cada tecla e respeita o limite de
  /// 1 req/s do Nominatim.
  void _onPlaceTermChanged(String term) {
    _placeSearchDebounce?.cancel();
    if (term.trim().length < 3) {
      setState(() {
        _places = const [];
        _placeSearching = false;
      });
      return;
    }
    setState(() => _placeSearching = true);
    _placeSearchDebounce = Timer(
      const Duration(milliseconds: 400),
      () => _searchPlaces(term),
    );
  }

  Future<void> _searchPlaces(String term) async {
    try {
      final places = await _placeSearchService.search(term.trim());
      if (mounted && _placeSearchOpen) {
        setState(() {
          _places = places;
          _placeSearching = false;
        });
      }
    } catch (_) {
      // Geocoding e opcional; sem resultado a lista simplesmente nao aparece.
      if (mounted) {
        setState(() => _placeSearching = false);
      }
    }
  }

  /// Escolheu uma sugestao: fecha a busca, voa ate o lugar e solta um pin
  /// temporario (tocar no pin remove).
  void _selectPlace(Place place) {
    _placeSearchDebounce?.cancel();
    _placeSearchController.clear();
    setState(() {
      _foundPlace = place;
      _placeSearchOpen = false;
      _places = const [];
      _placeSearching = false;
    });
    if (_mapReady) {
      _mapController.move(LatLng(place.latitude, place.longitude), 13);
    }
  }

  /// X do campo: com texto limpa; vazio fecha a busca.
  void _clearOrClosePlaceSearch() {
    _placeSearchDebounce?.cancel();
    final hadText = _placeSearchController.text.isNotEmpty;
    _placeSearchController.clear();
    setState(() {
      _places = const [];
      _placeSearching = false;
      if (!hadText) {
        _placeSearchOpen = false;
      }
    });
  }

  /// Luneta da home: campo sobre o mapa com sugestoes do geocoding, no lugar
  /// da barra do topo enquanto a busca esta aberta.
  Widget _placeSearchOverlay(BuildContext context) {
    return Positioned(
      top: 0,
      left: 0,
      right: 0,
      child: Container(
        decoration: BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
            colors: [Colors.black.withValues(alpha: 0.85), Colors.transparent],
          ),
        ),
        child: SafeArea(
          bottom: false,
          child: Padding(
            padding: const EdgeInsets.fromLTRB(12, 4, 12, 20),
            child: Column(
              children: [
                TextField(
                  controller: _placeSearchController,
                  autofocus: true,
                  textInputAction: TextInputAction.search,
                  onChanged: _onPlaceTermChanged,
                  onSubmitted: (term) {
                    _placeSearchDebounce?.cancel();
                    if (term.trim().length >= 3) {
                      _searchPlaces(term);
                    }
                  },
                  decoration: InputDecoration(
                    hintText: 'Buscar lugar (ex: Pico das Agulhas Negras)',
                    prefixIcon: const Icon(Icons.search),
                    suffixIcon: IconButton(
                      tooltip: 'Limpar / fechar',
                      icon: const Icon(Icons.close),
                      onPressed: _clearOrClosePlaceSearch,
                    ),
                  ),
                ),
                if (_placeSearching)
                  const Padding(
                    padding: EdgeInsets.only(top: 10),
                    child: LinearProgressIndicator(minHeight: 2),
                  ),
                if (_places.isNotEmpty)
                  Container(
                    margin: const EdgeInsets.only(top: 8),
                    decoration: BoxDecoration(
                      color: Theme.of(context).colorScheme.surface,
                      borderRadius: BorderRadius.circular(14),
                    ),
                    child: Column(
                      children: [
                        for (final place in _places)
                          ListTile(
                            dense: true,
                            leading: Icon(
                              Icons.place_outlined,
                              color: Theme.of(context).colorScheme.primary,
                            ),
                            title: Text(
                              place.title,
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                            ),
                            subtitle: place.subtitle.isEmpty
                                ? null
                                : Text(
                                    place.subtitle,
                                    maxLines: 1,
                                    overflow: TextOverflow.ellipsis,
                                    style: const TextStyle(fontSize: 11, color: Colors.white54),
                                  ),
                            onTap: () => _selectPlace(place),
                          ),
                      ],
                    ),
                  ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  /// Cor da trilha de outro usuario: respeita a cor escolhida no caminho e,
  /// sem correspondencia, deriva uma cor estavel da aventura.
  Color _communityColor(DiscoveredTrail trail) {
    const byName = {
      'ROXO': Color(0xFFB388FF),
      'VERDE': Color(0xFF69F0AE),
      'AZUL': Color(0xFF40C4FF),
      'AMARELO': Color(0xFFFFD740),
      'VERMELHO': Color(0xFFFF8A80),
    };
    return byName[trail.color] ?? _palette[trail.adventureId.hashCode.abs() % _palette.length];
  }

  /// Card da trilha tocada: quem fez, destino e atalhos pro perfil/aventura.
  void _showCommunityTrail(DiscoveredTrail trail) {
    showModalBottomSheet<void>(
      context: context,
      builder: (_) => SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Row(
                children: [
                  StoryAvatar(name: trail.userName, radius: 20),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(trail.userName,
                            style: const TextStyle(fontWeight: FontWeight.w600)),
                        Text(
                          trail.destination,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: const TextStyle(fontSize: 12, color: Colors.white54),
                        ),
                      ],
                    ),
                  ),
                  Container(
                    width: 14,
                    height: 14,
                    decoration: BoxDecoration(
                      shape: BoxShape.circle,
                      color: _communityColor(trail),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 16),
              FilledButton.icon(
                onPressed: () {
                  Navigator.pop(context);
                  context.push('/aventuras/${trail.adventureId}');
                },
                icon: const Icon(Icons.route_outlined),
                label: const Text('Ver aventura'),
              ),
              const SizedBox(height: 8),
              OutlinedButton.icon(
                onPressed: trail.userCode.isEmpty
                    ? null
                    : () {
                        Navigator.pop(context);
                        context.push(
                          '/perfil',
                          extra: PublicUser(userCode: trail.userCode, name: trail.userName),
                        );
                      },
                icon: const Icon(Icons.person_outline),
                label: const Text('Ver perfil'),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _communityChip(BuildContext context) {
    return Positioned(
      top: 0,
      left: 0,
      right: 0,
      child: SafeArea(
        bottom: false,
        child: Padding(
          padding: const EdgeInsets.only(top: 52),
          child: Center(
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 5),
              decoration: BoxDecoration(
                color: Colors.black.withValues(alpha: 0.75),
                borderRadius: BorderRadius.circular(999),
              ),
              child: Text(
                '${_community.length} trilha(s) da comunidade por aqui',
                style: const TextStyle(fontSize: 11, color: Colors.white70),
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _emptyOverlay(BuildContext context) {
    return Positioned(
      bottom: 32,
      left: 24,
      right: 24,
      child: Card(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(Icons.terrain, color: Theme.of(context).colorScheme.primary, size: 36),
              const SizedBox(height: 8),
              const Text(
                'Seu mapa ainda esta vazio.\nToque no + para criar uma aventura e rastrear a primeira trilha.',
                textAlign: TextAlign.center,
                style: TextStyle(color: Colors.white70, fontSize: 13),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _carousel() {
    return Positioned(
      bottom: 16,
      left: 0,
      right: 0,
      child: SizedBox(
        height: 118,
        child: PageView.builder(
          controller: _pageController,
          itemCount: _trails.length,
          onPageChanged: _onCardChanged,
          itemBuilder: (context, index) => _card(_trails[index]),
        ),
      ),
    );
  }

  Widget _card(_Trail trail) {
    final adventure = trail.adventure;
    final subtitle = trail.hasTrack
        ? '${trail.totalKm.toStringAsFixed(1)} km • ${trail.pathCount} caminho(s)'
        : 'Sem trilha rastreada ainda';
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 6),
      child: Material(
        color: Colors.black.withValues(alpha: 0.82),
        borderRadius: BorderRadius.circular(16),
        child: InkWell(
          borderRadius: BorderRadius.circular(16),
          onTap: () => context.push('/aventuras/${adventure.id}'),
          child: Container(
            padding: const EdgeInsets.all(14),
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(16),
              border: Border.all(color: trail.color.withValues(alpha: 0.6)),
            ),
            child: Row(
              children: [
                Container(
                  width: 14,
                  height: 14,
                  decoration: BoxDecoration(shape: BoxShape.circle, color: trail.color),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        adventure.destination,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 15),
                      ),
                      const SizedBox(height: 4),
                      Text(subtitle, style: const TextStyle(fontSize: 12, color: Colors.white70)),
                      const SizedBox(height: 4),
                      Text(
                        adventure.status,
                        style: TextStyle(fontSize: 11, color: trail.color),
                      ),
                    ],
                  ),
                ),
                const Icon(Icons.chevron_right, color: Colors.white54),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
