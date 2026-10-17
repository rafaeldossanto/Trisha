import 'package:flutter/material.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:go_router/go_router.dart';
import 'package:latlong2/latlong.dart';
import 'package:provider/provider.dart';

import '../../../core/network/dio_client.dart';
import '../../adventure/domain/adventure.dart';
import '../data/region_api.dart';
import '../domain/region.dart';

/// Detail of another user's folder: cities as pins on the map and visible
/// adventures (the backend already filters by the composition rule).
class RegionDetailScreen extends StatefulWidget {
  const RegionDetailScreen({super.key, required this.region});

  final Region region;

  @override
  State<RegionDetailScreen> createState() => _RegionDetailScreenState();
}

class _RegionDetailScreenState extends State<RegionDetailScreen> {
  late final RegionApi _api = RegionApi(context.read<DioClient>().dio);

  List<Adventure> _adventures = const [];
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final adventures = await _api.adventuresByRegion(widget.region.id);
      if (!mounted) {
        return;
      }
      setState(() {
        _adventures = adventures;
        _loading = false;
      });
    } catch (_) {
      if (mounted) {
        setState(() => _loading = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final citiesWithCoords = widget.region.cities
        .where((c) => c.latitude != null && c.longitude != null)
        .toList();
    final center = citiesWithCoords.isNotEmpty
        ? LatLng(citiesWithCoords.first.latitude!, citiesWithCoords.first.longitude!)
        : const LatLng(-20.4350, -41.7920);

    return Scaffold(
      appBar: AppBar(title: Text(widget.region.name)),
      body: Column(
        children: [
          SizedBox(
            height: 260,
            child: FlutterMap(
              options: MapOptions(initialCenter: center, initialZoom: citiesWithCoords.isNotEmpty ? 9 : 6),
              children: [
                TileLayer(
                  urlTemplate: 'https://a.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}.png',
                  userAgentPackageName: 'com.trilha.trilha_app',
                ),
                MarkerLayer(
                  markers: [
                    for (final c in citiesWithCoords)
                      Marker(
                        point: LatLng(c.latitude!, c.longitude!),
                        width: 40,
                        height: 40,
                        child: Tooltip(
                          message: c.name,
                          child: Icon(Icons.location_city, color: Theme.of(context).colorScheme.primary, size: 30),
                        ),
                      ),
                  ],
                ),
              ],
            ),
          ),
          Expanded(child: _adventureList()),
        ],
      ),
    );
  }

  Widget _adventureList() {
    if (_loading) {
      return const Center(child: CircularProgressIndicator());
    }
    if (_adventures.isEmpty) {
      return const Center(child: Text('Nenhuma aventura visivel nesta colecao.'));
    }
    return ListView.separated(
      padding: const EdgeInsets.all(16),
      itemCount: _adventures.length,
      separatorBuilder: (_, _) => const SizedBox(height: 12),
      itemBuilder: (context, index) {
        final a = _adventures[index];
        return Card(
          child: ListTile(
            leading: const Icon(Icons.terrain),
            title: Text(a.destination),
            subtitle: Text(a.status),
            trailing: const Icon(Icons.map),
            onTap: () => context.push('/aventuras/${a.id}/mapa'),
          ),
        );
      },
    );
  }
}
