import 'package:flutter/material.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:image_picker/image_picker.dart';
import 'package:latlong2/latlong.dart';
import 'package:provider/provider.dart';

import '../../../core/network/dio_client.dart';
import '../../point/data/media_api.dart';
import '../../point/data/point_api.dart';
import '../../point/domain/point_of_interest.dart';
import '../../point/domain/point_status.dart';
import '../../point/presentation/create_point_sheet.dart';
import '../../point/presentation/color_level.dart';
import '../../point/presentation/status_pin.dart';
import '../data/location_service.dart';

/// Dark map centered on the current location. With [adventureId], plots the
/// points of the adventure (colored by confidence level), allows creating a point
/// (long press) and adding evidence by photo (tap on point).
class MapScreen extends StatefulWidget {
  const MapScreen({super.key, this.adventureId});

  final String? adventureId;

  @override
  State<MapScreen> createState() => _MapScreenState();
}

class _MapScreenState extends State<MapScreen> {
  static const _fallbackCenter = LatLng(-20.4350, -41.7920);

  final _mapController = MapController();
  final _locationService = LocationService();
  final _picker = ImagePicker();

  LatLng? _position;
  bool _locating = true;
  List<PointOfInterest> _points = const [];

  // Marcacao do usuario por ponto (status/objetivo); ponto sem entrada
  // continua com o pin comum colorido pelo nivel de confianca.
  Map<String, PointUserStatus> _marks = const {};

  PointApi get _pointApi => PointApi(context.read<DioClient>().dio);

  @override
  void initState() {
    super.initState();
    _locate();
    if (widget.adventureId != null) {
      _loadPoints(widget.adventureId!);
    }
  }

  Future<void> _locate() async {
    setState(() => _locating = true);
    final pos = await _locationService.currentPosition();
    if (!mounted) {
      return;
    }
    setState(() {
      _position = pos == null ? null : LatLng(pos.latitude, pos.longitude);
      _locating = false;
    });
    if (_position != null) {
      _mapController.move(_position!, 15);
    }
  }

  Future<void> _loadPoints(String adventureId) async {
    try {
      final points = await _pointApi.pointsByAdventure(adventureId);
      if (!mounted) {
        return;
      }
      setState(() => _points = points);
      if (_position == null && points.isNotEmpty) {
        _mapController.move(LatLng(points.first.latitude, points.first.longitude), 14);
      }
      await _loadMarks(points);
    } catch (_) {
      // No points on the map — adventure may not have a path/point yet.
    }
  }

  Future<void> _loadMarks(List<PointOfInterest> points) async {
    try {
      final marks = await _pointApi.statuses([for (final p in points) p.id]);
      if (mounted) {
        setState(() => _marks = {for (final mark in marks) mark.pointId: mark});
      }
    } catch (_) {
      // Status e opcional; sem ele os pontos ficam com o pin comum.
    }
  }

  void _updateMark(String pointId, PointUserStatus mark) {
    setState(() {
      final updated = Map<String, PointUserStatus>.of(_marks);
      if (mark.isEmpty) {
        updated.remove(pointId);
      } else {
        updated[pointId] = mark;
      }
      _marks = updated;
    });
  }

  Future<void> _createPoint(LatLng location) async {
    final adventureId = widget.adventureId;
    if (adventureId == null) {
      return;
    }
    final created = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      builder: (_) => CreatePointSheet(
        adventureId: adventureId,
        latitude: location.latitude,
        longitude: location.longitude,
      ),
    );
    if (created == true) {
      await _loadPoints(adventureId);
    }
  }

  Future<void> _addEvidence(PointOfInterest point) async {
    final photo = await _picker.pickImage(source: ImageSource.camera, imageQuality: 70);
    if (photo == null) {
      return;
    }
    final pos = await _locationService.currentPosition();
    if (!mounted) {
      return;
    }
    if (pos == null) {
      _showMessage('Ative a localizacao para registrar a evidencia');
      return;
    }
    _showMessage('Enviando evidencia...');
    try {
      final dio = context.read<DioClient>().dio;
      final url = await MediaApi(dio).uploadMedia(photo.path);
      await PointApi(dio).addEvidence(
        pointId: point.id,
        photoUrl: url,
        evidenceType: 'VISTA',
        captureLatitude: pos.latitude,
        captureLongitude: pos.longitude,
      );
      if (widget.adventureId != null) {
        await _loadPoints(widget.adventureId!);
      }
      _showMessage('Evidencia adicionada');
    } catch (_) {
      _showMessage('Nao foi possivel adicionar a evidencia (precisa estar a <50m)');
    }
  }

  void _showMessage(String msg) {
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(msg)));
    }
  }

  /// Ponto marcado ganha o pin de status (com a ponta no local exato);
  /// sem marcacao, mantem o pin comum colorido pelo nivel de confianca.
  Marker _pointMarker(PointOfInterest point) {
    final variant = PinVariant.of(_marks[point.id]);
    if (variant == null) {
      return Marker(
        point: LatLng(point.latitude, point.longitude),
        width: 36,
        height: 36,
        child: GestureDetector(
          onTap: () => _showPoint(point),
          child: Icon(Icons.place, color: colorByLevel(point.confidenceLevel), size: 32),
        ),
      );
    }
    return Marker(
      point: LatLng(point.latitude, point.longitude),
      width: 34,
      height: 46,
      alignment: Alignment.topCenter,
      child: GestureDetector(
        onTap: () => _showPoint(point),
        child: StatusPin(variant: variant),
      ),
    );
  }

  /// Marca/desmarca o status pelo bottom sheet, refletindo direto no pin.
  Future<void> _applyStatus(
    PointOfInterest point,
    PointStatus? status,
    StateSetter setSheetState,
  ) async {
    final current = _marks[point.id];
    try {
      if (status == null) {
        await _pointApi.clearStatus(point.id);
        _updateMark(point.id, PointUserStatus(pointId: point.id, goal: current?.goal ?? false));
      } else {
        _updateMark(point.id, await _pointApi.setStatus(pointId: point.id, status: status.wire));
      }
      setSheetState(() {});
    } catch (_) {
      _showMessage('Nao foi possivel atualizar o status');
    }
  }

  Future<void> _applyGoal(
    PointOfInterest point,
    bool goal,
    StateSetter setSheetState,
  ) async {
    try {
      _updateMark(point.id, await _pointApi.setGoal(pointId: point.id, goal: goal));
      setSheetState(() {});
    } catch (_) {
      _showMessage('Nao foi possivel atualizar o objetivo');
    }
  }

  Color _statusColor(PointStatus status) {
    return switch (status) {
      PointStatus.noRadar => PinColors.gray,
      PointStatus.naMira => PinColors.blue,
      PointStatus.conquistado => PinColors.green,
    };
  }

  void _showPoint(PointOfInterest point) {
    showModalBottomSheet<void>(
      context: context,
      builder: (_) => StatefulBuilder(
        builder: (sheetContext, setSheetState) {
          final mark = _marks[point.id];
          return Padding(
            padding: const EdgeInsets.all(24),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Icon(Icons.place, color: colorByLevel(point.confidenceLevel)),
                    const SizedBox(width: 8),
                    Expanded(
                      child: Text(point.name ?? point.type, style: Theme.of(context).textTheme.titleLarge),
                    ),
                  ],
                ),
                const SizedBox(height: 12),
                Text('Tipo: ${point.type}'),
                if (point.description != null && point.description!.isNotEmpty) ...[
                  const SizedBox(height: 8),
                  Text(point.description!),
                ],
                const SizedBox(height: 16),
                Row(
                  children: [
                    const Text('Confianca: '),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                      decoration: BoxDecoration(
                        color: colorByLevel(point.confidenceLevel),
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: Text(
                        'Nivel ${point.confidenceLevel}/5',
                        style: const TextStyle(color: Colors.black, fontWeight: FontWeight.bold),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 16),
                const Text('Minha marcacao', style: TextStyle(fontWeight: FontWeight.w600)),
                const SizedBox(height: 8),
                Wrap(
                  spacing: 8,
                  children: [
                    for (final status in PointStatus.values)
                      ChoiceChip(
                        label: Text(status.label),
                        selected: mark?.status == status,
                        selectedColor: _statusColor(status).withValues(alpha: 0.4),
                        onSelected: (selected) =>
                            _applyStatus(point, selected ? status : null, setSheetState),
                      ),
                  ],
                ),
                SwitchListTile(
                  contentPadding: EdgeInsets.zero,
                  title: const Row(
                    children: [
                      Icon(Icons.star, color: PinColors.gold, size: 20),
                      SizedBox(width: 8),
                      Text('Objetivo'),
                    ],
                  ),
                  activeThumbColor: PinColors.gold,
                  value: mark?.goal ?? false,
                  onChanged: (goal) => _applyGoal(point, goal, setSheetState),
                ),
                const SizedBox(height: 8),
                FilledButton.icon(
                  onPressed: () {
                    Navigator.of(context).pop();
                    _addEvidence(point);
                  },
                  icon: const Icon(Icons.add_a_photo),
                  label: const Text('Adicionar evidencia'),
                ),
              ],
            ),
          );
        },
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final hasAdventure = widget.adventureId != null;
    return Scaffold(
      appBar: AppBar(
        title: Text(hasAdventure ? 'Pontos da aventura' : 'Mapa'),
        actions: [
          IconButton(
            tooltip: 'Minha localizacao',
            onPressed: _locating ? null : _locate,
            icon: const Icon(Icons.my_location),
          ),
        ],
      ),
      body: Stack(
        children: [
          FlutterMap(
            mapController: _mapController,
            options: MapOptions(
              initialCenter: _position ?? _fallbackCenter,
              initialZoom: _position != null ? 15 : 11,
              onLongPress: hasAdventure ? (_, location) => _createPoint(location) : null,
            ),
            children: [
              TileLayer(
                urlTemplate: 'https://a.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}.png',
                userAgentPackageName: 'com.trilha.trilha_app',
              ),
              if (_points.isNotEmpty)
                MarkerLayer(
                  markers: [for (final point in _points) _pointMarker(point)],
                ),
              if (_position != null)
                MarkerLayer(
                  markers: [
                    Marker(
                      point: _position!,
                      width: 40,
                      height: 40,
                      child: Icon(Icons.my_location, color: Theme.of(context).colorScheme.primary, size: 32),
                    ),
                  ],
                ),
            ],
          ),
          if (hasAdventure)
            const Positioned(
              bottom: 16,
              left: 0,
              right: 0,
              child: Center(
                child: Card(
                  child: Padding(
                    padding: EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                    child: Text('Toque e segure no mapa para marcar um ponto'),
                  ),
                ),
              ),
            ),
          if (_locating)
            const Positioned(
              top: 12,
              left: 0,
              right: 0,
              child: Center(
                child: Card(
                  child: Padding(
                    padding: EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                    child: Text('Localizando...'),
                  ),
                ),
              ),
            ),
        ],
      ),
    );
  }
}
