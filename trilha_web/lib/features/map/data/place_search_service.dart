import 'package:dio/dio.dart';

import '../domain/place.dart';

/// Busca lugares por nome no Nominatim (OpenStreetMap) para a luneta da home.
/// Vai direto no servico publico, fora do BFF: geocoding nao passa pelo backend.
class PlaceSearchService {
  PlaceSearchService([Dio? dio])
      : _dio = dio ??
            Dio(
              BaseOptions(
                baseUrl: 'https://nominatim.openstreetmap.org',
                // Politica do Nominatim: identificar o app e no maximo 1 req/s
                // (o debounce da tela garante o ritmo).
                headers: {'User-Agent': 'trilha-app/1.0'},
              ),
            );

  final Dio _dio;

  Future<List<Place>> search(String query) async {
    final resp = await _dio.get('/search', queryParameters: {
      'q': query,
      'format': 'jsonv2',
      'limit': 5,
      'accept-language': 'pt-BR',
    });
    return (resp.data as List<dynamic>)
        .map((e) => Place.fromJson(e as Map<String, dynamic>))
        .toList();
  }
}
