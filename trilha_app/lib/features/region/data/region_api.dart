import 'package:dio/dio.dart';

import '../../../core/network/page_response.dart';
import '../../adventure/domain/adventure.dart';
import '../domain/city.dart';
import '../domain/region.dart';

/// CRUD of regions (collections) + discovery, over /bff/regioes.
class RegionApi {
  RegionApi(this._dio);

  final Dio _dio;

  Map<String, dynamic> _body(
          String name, String? description, String? coverUrl, String visibility, List<City> cities) =>
      {
        'nome': name,
        'descricao': description,
        'capaUrl': coverUrl,
        'visibilidade': visibility,
        'cidades': cities.map((c) => c.toJson()).toList(),
      };

  Future<List<Region>> list() async {
    final resp = await _dio.get('/bff/regioes', queryParameters: {'page': 0, 'size': 100});
    return PageResponse.fromJson(resp.data as Map<String, dynamic>, Region.fromJson).content;
  }

  Future<Region> create({
    required String name,
    String? description,
    String? coverUrl,
    required String visibility,
    required List<City> cities,
  }) async {
    final resp = await _dio.post('/bff/regioes', data: _body(name, description, coverUrl, visibility, cities));
    return Region.fromJson(resp.data as Map<String, dynamic>);
  }

  Future<Region> update(
    String id, {
    required String name,
    String? description,
    String? coverUrl,
    required String visibility,
    required List<City> cities,
  }) async {
    final resp = await _dio.put('/bff/regioes/$id', data: _body(name, description, coverUrl, visibility, cities));
    return Region.fromJson(resp.data as Map<String, dynamic>);
  }

  Future<void> delete(String id) async {
    await _dio.delete('/bff/regioes/$id');
  }

  Future<List<Region>> discover() async {
    final resp = await _dio.get('/bff/regioes/descobrir', queryParameters: {'page': 0, 'size': 100});
    return PageResponse.fromJson(resp.data as Map<String, dynamic>, Region.fromJson).content;
  }

  Future<List<Adventure>> adventuresByRegion(String id) async {
    final resp = await _dio.get('/bff/regioes/$id/aventuras', queryParameters: {'page': 0, 'size': 100});
    return PageResponse.fromJson(resp.data as Map<String, dynamic>, Adventure.fromJson).content;
  }
}
