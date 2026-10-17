import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:latlong2/latlong.dart';
import 'package:provider/provider.dart';

import '../../../core/network/dio_client.dart';
import '../../../core/storage/token_storage.dart';
import '../data/live_location_socket.dart';
import '../data/tracking_api.dart';
import '../domain/gps_point.dart';
import '../domain/live_session.dart';

/// Acompanha uma trilha ao vivo: carrega os pontos ja gravados (catch-up),
/// assina o WebSocket do loc e desenha a linha crescendo conforme o trilheiro
/// anda. A autorizacao (amigos/seguidores/publico) e do backend — se o
/// SUBSCRIBE for negado, a tela avisa e mostra so o trajeto ja carregado.
/// O socket reconecta sozinho (status "Reconectando...") e um poll periodico
/// detecta o fim da trilha para encerrar a tela graciosamente.
class LiveWatchScreen extends StatefulWidget {
  const LiveWatchScreen({super.key, required this.session});

  final LiveSession session;

  @override
  State<LiveWatchScreen> createState() => _LiveWatchScreenState();
}

class _LiveWatchScreenState extends State<LiveWatchScreen> {
  static const _liveColor = Color(0xFFFF5252);

  final _mapController = MapController();
  late final TrackingApi _api = TrackingApi(context.read<DioClient>().dio);

  LiveLocationSocket? _socket;
  Timer? _finishPoll;
  final List<LatLng> _route = [];
  String _status = 'Conectando...';
  bool _mapReady = false;
  bool _finished = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _start());
  }

  @override
  void dispose() {
    _finishPoll?.cancel();
    _socket?.dispose();
    super.dispose();
  }

  /// O topico simplesmente para de emitir quando a sessao acaba — este poll
  /// diferencia "trilha finalizada" de "sinal caiu" para o espectador.
  void _startFinishPoll() {
    _finishPoll = Timer.periodic(const Duration(seconds: 30), (_) async {
      try {
        final session = await _api.sessionByPath(widget.session.pathId);
        if (mounted && session.status != 'EM_ANDAMENTO') {
          _onSessionFinished();
        }
      } catch (_) {
        // Sem resposta o poll tenta de novo no proximo tick.
      }
    });
  }

  void _onSessionFinished() {
    _finishPoll?.cancel();
    _socket?.dispose();
    if (mounted) {
      setState(() {
        _finished = true;
        _status = 'Trilha finalizada';
      });
    }
  }

  Future<void> _start() async {
    try {
      final points = await _api.pointsBySession(widget.session.sessionId);
      points.sort((a, b) => (a.order ?? 0).compareTo(b.order ?? 0));
      if (!mounted) {
        return;
      }
      setState(() {
        _route.addAll(points.map((p) => LatLng(p.latitude, p.longitude)));
      });
      _moveToLast();
    } catch (_) {
      // Sem catch-up a linha comeca no proximo ponto recebido ao vivo.
    }

    final token = await context.read<TokenStorage>().readToken();
    if (!mounted || token == null) {
      return;
    }

    _socket = LiveLocationSocket(token)
      ..connect(
        sessionId: widget.session.sessionId,
        onPoint: _onLivePoint,
        onConnected: () {
          if (mounted && !_finished) {
            setState(() => _status = 'Ao vivo');
          }
        },
        onDisconnected: () {
          if (mounted && !_finished) {
            setState(() => _status = 'Reconectando...');
          }
        },
      );
    _startFinishPoll();
  }

  void _onLivePoint(GpsPoint point) {
    if (!mounted) {
      return;
    }
    setState(() => _route.add(LatLng(point.latitude, point.longitude)));
    _moveToLast();
  }

  void _moveToLast() {
    if (_mapReady && _route.isNotEmpty) {
      _mapController.move(_route.last, 16);
    }
  }

  @override
  Widget build(BuildContext context) {
    final session = widget.session;
    final center = _route.isNotEmpty
        ? _route.last
        : LatLng(session.latitude, session.longitude);

    return Scaffold(
      appBar: AppBar(
        title: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            if (_finished)
              const Icon(Icons.flag, size: 16, color: _liveColor)
            else
              const _PulsingDot(color: _liveColor),
            const SizedBox(width: 8),
            Flexible(
              child: Text(
                '${session.userName} • $_status',
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
              ),
            ),
          ],
        ),
      ),
      body: FlutterMap(
        mapController: _mapController,
        options: MapOptions(
          initialCenter: center,
          initialZoom: 16,
          onMapReady: () {
            _mapReady = true;
            _moveToLast();
          },
        ),
        children: [
          TileLayer(
            urlTemplate: 'https://a.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}.png',
            userAgentPackageName: 'com.trilha.trilha_app',
          ),
          if (_route.length >= 2)
            PolylineLayer(
              polylines: [
                Polyline(
                  points: _route,
                  strokeWidth: 8,
                  color: _liveColor.withValues(alpha: 0.25),
                ),
                Polyline(points: _route, strokeWidth: 3, color: _liveColor),
              ],
            ),
          MarkerLayer(
            markers: [
              Marker(
                point: center,
                width: 40,
                height: 40,
                child: const _PulsingDot(color: _liveColor, size: 16),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

/// Bolinha pulsante do "ao vivo" (AppBar e ponta da trilha no mapa).
class _PulsingDot extends StatefulWidget {
  const _PulsingDot({required this.color, this.size = 10});

  final Color color;
  final double size;

  @override
  State<_PulsingDot> createState() => _PulsingDotState();
}

class _PulsingDotState extends State<_PulsingDot> with SingleTickerProviderStateMixin {
  late final AnimationController _controller = AnimationController(
    vsync: this,
    duration: const Duration(seconds: 1),
  )..repeat(reverse: true);

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: _controller,
      builder: (context, _) => Container(
        width: widget.size,
        height: widget.size,
        decoration: BoxDecoration(
          shape: BoxShape.circle,
          color: widget.color,
          boxShadow: [
            BoxShadow(
              color: widget.color.withValues(alpha: 0.6 * (1 - _controller.value)),
              blurRadius: 4,
              spreadRadius: 6 * _controller.value,
            ),
          ],
        ),
      ),
    );
  }
}
