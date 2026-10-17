import 'dart:convert';

import 'package:stomp_dart_client/stomp_dart_client.dart';

import '../../../core/env/env.dart';
import '../domain/gps_point.dart';

/// Assinatura da trilha ao vivo: conecta no STOMP/SockJS do loc
/// (`/ws-localizacao`, Bearer no CONNECT) e assina `/topic/sessao/{id}`.
/// O backend autoriza o SUBSCRIBE pela visibilidade da sessao — aqui so
/// conectamos e entregamos cada ponto recebido ao callback.
/// Reconexao e automatica (reconnectDelay): sinal fraco na trilha e a regra,
/// nao a excecao — a cada reconexao o onConnect reassina o topico.
class LiveLocationSocket {
  LiveLocationSocket(this._token);

  final String _token;
  StompClient? _client;
  bool _disposed = false;

  void connect({
    required String sessionId,
    required void Function(GpsPoint point) onPoint,
    void Function()? onConnected,
    void Function()? onDisconnected,
  }) {
    _client = StompClient(
      config: StompConfig.sockJS(
        url: Env.locWsUrl,
        reconnectDelay: const Duration(seconds: 5),
        stompConnectHeaders: {'Authorization': 'Bearer $_token'},
        onConnect: (frame) {
          onConnected?.call();
          _client?.subscribe(
            destination: '/topic/sessao/$sessionId',
            callback: (frame) {
              final body = frame.body;
              if (body != null) {
                onPoint(GpsPoint.fromJson(jsonDecode(body) as Map<String, dynamic>));
              }
            },
          );
        },
        onWebSocketError: (_) => _notifyDown(onDisconnected),
        onStompError: (_) => _notifyDown(onDisconnected),
        onWebSocketDone: () => _notifyDown(onDisconnected),
      ),
    );
    _client!.activate();
  }

  /// O deactivate do dispose tambem dispara onWebSocketDone — o flag evita
  /// avisar "reconectando" para uma desconexao intencional.
  void _notifyDown(void Function()? onDisconnected) {
    if (!_disposed) {
      onDisconnected?.call();
    }
  }

  void dispose() {
    _disposed = true;
    _client?.deactivate();
    _client = null;
  }
}
