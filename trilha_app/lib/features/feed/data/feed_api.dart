import 'package:dio/dio.dart';

import '../../../core/network/page_response.dart';
import '../domain/feed_adventure.dart';

/// Feed do app via BFF: minhas aventuras + as visiveis de quem eu sigo,
/// mais recentes primeiro, ja com o autor resolvido.
class FeedApi {
  FeedApi(this._dio);

  final Dio _dio;

  Future<PageResponse<FeedAdventure>> feed({int page = 0, int size = 20}) async {
    final resp = await _dio.get(
      '/bff/aventuras/feed',
      queryParameters: {'page': page, 'size': size},
    );
    return PageResponse.fromJson(resp.data as Map<String, dynamic>, FeedAdventure.fromJson);
  }
}
