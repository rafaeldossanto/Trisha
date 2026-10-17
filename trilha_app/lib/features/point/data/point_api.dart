import 'package:dio/dio.dart';

import '../../../core/network/page_response.dart';
import '../domain/point_of_interest.dart';
import '../domain/point_status.dart';

/// Accesses points of interest via BFF. The hierarchy is
/// Adventure -> Path -> Point, so for the points of an adventure
/// we get its paths (from /detalhe) and join the points of each one.
class PointApi {
  PointApi(this._dio);

  final Dio _dio;

  Future<List<String>> pathsByAdventure(String adventureId) async {
    final resp = await _dio.get('/bff/aventuras/$adventureId/detalhe');
    final data = resp.data as Map<String, dynamic>;
    final paths = (data['caminhos'] as List<dynamic>? ?? const []);
    return paths
        .map((c) => (c as Map<String, dynamic>)['id'] as String)
        .toList();
  }

  Future<List<PointOfInterest>> pointsByPath(String pathId) async {
    final resp = await _dio.get('/bff/pontos-interesse/caminho/$pathId');
    final page = PageResponse.fromJson(
      resp.data as Map<String, dynamic>,
      PointOfInterest.fromJson,
    );
    return page.content;
  }

  Future<List<PointOfInterest>> pointsByAdventure(String adventureId) async {
    final paths = await pathsByAdventure(adventureId);
    final all = <PointOfInterest>[];
    for (final pathId in paths) {
      all.addAll(await pointsByPath(pathId));
    }
    return all;
  }

  Future<PointOfInterest> create({
    required String pathId,
    required String type,
    String? name,
    String? description,
    required double latitude,
    required double longitude,
  }) async {
    final resp = await _dio.post(
      '/bff/pontos-interesse',
      data: {
        'caminhoId': pathId,
        'tipo': type,
        'nome': ?name,
        'descricao': ?description,
        'latitude': latitude,
        'longitude': longitude,
      },
    );
    return PointOfInterest.fromJson(resp.data as Map<String, dynamic>);
  }

  /// Statuses of the authenticated user for the visible points, in batch.
  /// Only marked points come back.
  Future<List<PointUserStatus>> statuses(List<String> pointIds) async {
    if (pointIds.isEmpty) {
      return const [];
    }
    final resp = await _dio.get(
      '/bff/pontos-interesse/status',
      queryParameters: {'ids': pointIds.join(',')},
    );
    final list = resp.data as List<dynamic>;
    return [
      for (final item in list)
        PointUserStatus.fromJson(item as Map<String, dynamic>),
    ];
  }

  Future<PointUserStatus> setStatus({
    required String pointId,
    required String status,
  }) async {
    final resp = await _dio.patch(
      '/bff/pontos-interesse/$pointId/status',
      queryParameters: {'status': status},
    );
    return PointUserStatus.fromJson(resp.data as Map<String, dynamic>);
  }

  Future<void> clearStatus(String pointId) async {
    await _dio.delete('/bff/pontos-interesse/$pointId/status');
  }

  Future<PointUserStatus> setGoal({
    required String pointId,
    required bool goal,
  }) async {
    final resp = await _dio.patch(
      '/bff/pontos-interesse/$pointId/objetivo',
      queryParameters: {'objetivo': goal},
    );
    return PointUserStatus.fromJson(resp.data as Map<String, dynamic>);
  }

  /// Adds evidence (photo already in storage). The APP validates proximity (<50m)
  /// and recalculates the confidence level of the point.
  Future<void> addEvidence({
    required String pointId,
    required String photoUrl,
    required String evidenceType,
    required double captureLatitude,
    required double captureLongitude,
  }) async {
    await _dio.post(
      '/bff/pontos-interesse/evidencia',
      data: {
        'pontoId': pointId,
        'fotoUrl': photoUrl,
        'tipoEvidencia': evidenceType,
        'latCaptura': captureLatitude,
        'lngCaptura': captureLongitude,
      },
    );
  }
}
