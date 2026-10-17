import 'package:flutter_dotenv/flutter_dotenv.dart';

/// Acesso tipado as variaveis do `.env`. Carregado uma vez no `main` antes de
/// `runApp`. Centraliza as chaves para o resto do app nao mexer no dotenv direto.
class Env {
  const Env._();

  static String get apiBaseUrl => dotenv.get('API_BASE_URL');

  static String get midiaBaseUrl => dotenv.get('MIDIA_BASE_URL');

  /// Endpoint STOMP/SockJS do loc — o acompanhamento ao vivo nao passa pelo BFF.
  static String get locWsUrl => dotenv.get('LOC_WS_URL');

  /// Web client ID do Google, usado como `serverClientId` no login social: e
  /// ele que vira o `aud` do ID token que o Cadastro valida. Null enquanto nao
  /// estiver configurado — o login com Google avisa em vez de falhar mudo.
  static String? get googleServerClientId {
    final value = dotenv.maybeGet('GOOGLE_SERVER_CLIENT_ID');
    return (value == null || value.isEmpty) ? null : value;
  }
}
