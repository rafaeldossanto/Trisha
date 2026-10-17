import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:geolocator/geolocator.dart';
import 'package:go_router/go_router.dart';
import 'package:latlong2/latlong.dart';
import 'package:provider/provider.dart';

import '../../../core/network/dio_client.dart';
import '../../auth/presentation/auth_provider.dart';
import '../../path/data/path_api.dart';
import '../data/tracking_api.dart';

/// Live GPS tracking of a path: opens a session, follows `geolocator`,
/// publishes each point and draws the route. On finish, closes the session and the
/// path (actual distance is calculated by the backend from the points).
/// Before starting, the user chooses who can watch the trail live (PRIVADO/
/// AMIGOS/SEGUIDORES/PUBLICO) — changeable mid-session via the AppBar icon.
class TrackingScreen extends StatefulWidget {
  const TrackingScreen({super.key, required this.pathId});

  final String pathId;

  @override
  State<TrackingScreen> createState() => _TrackingScreenState();
}

class _TrackingScreenState extends State<TrackingScreen> {
  static const _locationSettings = LocationSettings(
    accuracy: LocationAccuracy.high,
    distanceFilter: 5,
  );

  /// Opcoes de visibilidade do acompanhamento ao vivo (contrato do BFF).
  static const _visibilityOptions = [
    (code: 'PRIVADO', label: 'Ninguem', hint: 'So voce ve a trilha ao vivo', icon: Icons.lock_outline),
    (code: 'AMIGOS', label: 'Amigos', hint: 'Somente seus amigos acompanham', icon: Icons.people_outline),
    (code: 'SEGUIDORES', label: 'Seguidores', hint: 'Quem te segue acompanha', icon: Icons.person_add_alt),
    (code: 'PUBLICO', label: 'Publico', hint: 'Qualquer pessoa acompanha', icon: Icons.public),
  ];

  final _mapController = MapController();
  late final TrackingApi _api = TrackingApi(context.read<DioClient>().dio);
  late final PathApi _pathApi = PathApi(context.read<DioClient>().dio);

  StreamSubscription<Position>? _subscription;
  String? _sessionId;
  final List<LatLng> _route = [];
  String _status = 'Iniciando...';
  String _visibility = 'PRIVADO';
  bool _finishing = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _chooseVisibilityAndStart());
  }

  /// Antes de rastrear, o usuario escolhe quem acompanha ao vivo. Fechar o
  /// sheet sem escolher mantem o default seguro (PRIVADO).
  Future<void> _chooseVisibilityAndStart() async {
    final choice = await _showVisibilitySheet();
    if (!mounted) {
      return;
    }
    if (choice != null) {
      setState(() => _visibility = choice);
    }
    await _start();
  }

  Future<String?> _showVisibilitySheet() {
    return showModalBottomSheet<String>(
      context: context,
      builder: (_) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Padding(
              padding: EdgeInsets.all(16),
              child: Text(
                'Quem pode acompanhar sua trilha ao vivo?',
                style: TextStyle(fontWeight: FontWeight.w600),
              ),
            ),
            for (final option in _visibilityOptions)
              ListTile(
                leading: Icon(option.icon),
                title: Text(option.label),
                subtitle: Text(option.hint, style: const TextStyle(fontSize: 12)),
                trailing: _visibility == option.code
                    ? Icon(Icons.check, color: Theme.of(context).colorScheme.primary)
                    : null,
                onTap: () => Navigator.pop(context, option.code),
              ),
          ],
        ),
      ),
    );
  }

  /// Durante a sessao: troca a visibilidade no backend (PATCH no BFF).
  Future<void> _changeVisibility() async {
    final choice = await _showVisibilitySheet();
    final sessionId = _sessionId;
    if (choice == null || !mounted) {
      return;
    }
    if (sessionId == null) {
      setState(() => _visibility = choice);
      return;
    }
    try {
      await _api.updateVisibility(sessionId: sessionId, visibility: choice);
      if (mounted) {
        setState(() => _visibility = choice);
        _showMessage('Agora ${_labelOf(choice).toLowerCase()} podem acompanhar');
      }
    } catch (_) {
      _showMessage('Nao foi possivel alterar a visibilidade');
    }
  }

  String _labelOf(String code) =>
      _visibilityOptions.firstWhere((o) => o.code == code).label;

  IconData _iconOf(String code) =>
      _visibilityOptions.firstWhere((o) => o.code == code).icon;

  void _showMessage(String msg) {
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(msg)));
    }
  }

  @override
  void dispose() {
    _subscription?.cancel();
    super.dispose();
  }

  Future<void> _start() async {
    final userId = context.read<AuthProvider>().userId;
    if (userId == null) {
      return;
    }

    final hasPermission = await _ensurePermission();
    if (!mounted) {
      return;
    }
    if (!hasPermission) {
      setState(() => _status = 'Permissao de localizacao negada');
      return;
    }

    try {
      final session = await _api.startSession(
        pathId: widget.pathId,
        userId: userId,
        visibility: _visibility,
      );
      if (!mounted) {
        return;
      }
      setState(() {
        _sessionId = session.id;
        _status = 'Rastreando';
      });
      _subscription = Geolocator.getPositionStream(locationSettings: _locationSettings).listen(_onMove);
    } catch (_) {
      setState(() => _status = 'Nao foi possivel iniciar a sessao');
    }
  }

  Future<bool> _ensurePermission() async {
    if (!await Geolocator.isLocationServiceEnabled()) {
      return false;
    }
    var permission = await Geolocator.checkPermission();
    if (permission == LocationPermission.denied) {
      permission = await Geolocator.requestPermission();
    }
    return permission != LocationPermission.denied &&
        permission != LocationPermission.deniedForever;
  }

  void _onMove(Position pos) {
    final point = LatLng(pos.latitude, pos.longitude);
    setState(() => _route.add(point));
    _mapController.move(point, 16);

    final sessionId = _sessionId;
    if (sessionId != null) {
      _api
          .recordPoint(
            sessionId: sessionId,
            latitude: pos.latitude,
            longitude: pos.longitude,
            altitude: pos.altitude,
            accuracy: pos.accuracy,
            speed: pos.speed,
          )
          .ignore();
    }
  }

  Future<void> _finish() async {
    final sessionId = _sessionId;
    if (sessionId == null) {
      return;
    }
    setState(() => _finishing = true);
    await _subscription?.cancel();
    try {
      await _api.finishSession(sessionId);
      await _pathApi.finish(widget.pathId);
    } catch (_) {
      // Even with a network error, close the screen; the user reopens if needed.
    }
    if (!mounted) {
      return;
    }
    context.pop();
  }

  @override
  Widget build(BuildContext context) {
    final center = _route.isNotEmpty ? _route.last : const LatLng(-20.4350, -41.7920);
    return Scaffold(
      appBar: AppBar(
        title: Text(_status),
        actions: [
          IconButton(
            tooltip: 'Ao vivo: ${_labelOf(_visibility)}',
            icon: Icon(_iconOf(_visibility)),
            onPressed: _finishing ? null : _changeVisibility,
          ),
          if (_finishing)
            const Padding(
              padding: EdgeInsets.all(14),
              child: SizedBox(height: 20, width: 20, child: CircularProgressIndicator(strokeWidth: 2)),
            )
          else
            TextButton.icon(
              onPressed: _sessionId == null ? null : _finish,
              icon: const Icon(Icons.flag),
              label: const Text('Finalizar'),
            ),
        ],
      ),
      body: FlutterMap(
        mapController: _mapController,
        options: MapOptions(initialCenter: center, initialZoom: 16),
        children: [
          TileLayer(
            urlTemplate: 'https://a.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}.png',
            userAgentPackageName: 'com.trilha.trilha_app',
          ),
          if (_route.length >= 2)
            PolylineLayer(
              polylines: [
                Polyline(points: _route, strokeWidth: 4, color: Theme.of(context).colorScheme.primary),
              ],
            ),
          if (_route.isNotEmpty)
            MarkerLayer(
              markers: [
                Marker(
                  point: _route.last,
                  width: 36,
                  height: 36,
                  child: Icon(Icons.my_location, color: Theme.of(context).colorScheme.primary),
                ),
              ],
            ),
        ],
      ),
    );
  }
}
