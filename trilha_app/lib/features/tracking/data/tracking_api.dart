import 'package:dio/dio.dart';

import '../domain/gps_point.dart';
import '../domain/live_session.dart';
import '../domain/session.dart';

/// Live tracking via BFF (HTTP). The app publishes each GPS point; the BFF forwards
/// it to the Location service (no MQTT on the client).
class TrackingApi {
  TrackingApi(this._dio);

  final Dio _dio;

  Future<Session> startSession({
    required String pathId,
    required String userId,
    bool? autoFinish,
    double? finishDistanceMeters,
    String? visibility,
  }) async {
    final resp = await _dio.post(
      '/bff/localizacao/sessao',
      data: {
        'caminhoId': pathId,
        'usuarioId': userId,
        'terminoAutomatico': ?autoFinish,
        'distanciaTerminoMetros': ?finishDistanceMeters,
        'visibilidade': ?visibility,
      },
    );
    return Session.fromJson(resp.data as Map<String, dynamic>);
  }

  /// Troca quem acompanha a trilha ao vivo (PUBLICO/SEGUIDORES/AMIGOS/PRIVADO).
  Future<Session> updateVisibility({
    required String sessionId,
    required String visibility,
  }) async {
    final resp = await _dio.patch(
      '/bff/localizacao/sessao/$sessionId/visibilidade',
      queryParameters: {'visibilidade': visibility},
    );
    return Session.fromJson(resp.data as Map<String, dynamic>);
  }

  Future<void> recordPoint({
    required String sessionId,
    required double latitude,
    required double longitude,
    double? altitude,
    double? accuracy,
    double? speed,
  }) async {
    await _dio.post(
      '/bff/localizacao/ponto',
      data: {
        'sessaoId': sessionId,
        'latitude': latitude,
        'longitude': longitude,
        'altitude': ?altitude,
        'precisao': ?accuracy,
        'velocidade': ?speed,
      },
    );
  }

  /// Sessao (metadados) do caminho — quem assiste usa para saber se a trilha
  /// ainda esta em andamento.
  Future<Session> sessionByPath(String pathId) async {
    final resp = await _dio.get('/bff/localizacao/sessao/caminho/$pathId');
    return Session.fromJson(resp.data as Map<String, dynamic>);
  }

  /// Quem esta trilhando agora e o usuario pode acompanhar ao vivo.
  Future<List<LiveSession>> liveSessions() async {
    final resp = await _dio.get('/bff/localizacao/sessoes-ao-vivo');
    return (resp.data as List<dynamic>)
        .map((e) => LiveSession.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  /// Pontos ja gravados da sessao — o "catch-up" antes de assinar o WebSocket.
  Future<List<GpsPoint>> pointsBySession(String sessionId) async {
    final resp = await _dio.get('/bff/localizacao/pontos/sessao/$sessionId');
    return (resp.data as List<dynamic>)
        .map((e) => GpsPoint.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<List<GpsPoint>> pointsByPath(String pathId) async {
    final resp = await _dio.get('/bff/localizacao/pontos/caminho/$pathId');
    return (resp.data as List<dynamic>)
        .map((e) => GpsPoint.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<Session> finishSession(String sessionId) async {
    final resp = await _dio.patch('/bff/localizacao/sessao/$sessionId/finalizar');
    return Session.fromJson(resp.data as Map<String, dynamic>);
  }
}
