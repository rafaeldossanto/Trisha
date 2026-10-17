import 'package:dio/dio.dart';

import '../../../core/network/page_response.dart';
import '../../friendship/domain/public_user.dart';
import '../domain/counters.dart';
import '../domain/follow_status.dart';

/// Follow via BFF, by userCode. The code goes in the body (follow/unfollow) or
/// in ?codigo= (gets) — avoids '#' in the code in the path.
class FollowerApi {
  FollowerApi(this._dio);

  final Dio _dio;

  Future<void> follow(String code) async {
    await _dio.post('/bff/seguidores', data: {'seguidoCodigo': code});
  }

  Future<void> unfollow(String code) async {
    await _dio.delete('/bff/seguidores', data: {'seguidoCodigo': code});
  }

  Future<FollowStatus> status(String code) async {
    final resp = await _dio.get('/bff/seguidores/status', queryParameters: {'codigo': code});
    return FollowStatus.fromJson(resp.data as Map<String, dynamic>);
  }

  Future<Counters> counters(String code) async {
    final resp = await _dio.get('/bff/seguidores/contadores', queryParameters: {'codigo': code});
    return Counters.fromJson(resp.data as Map<String, dynamic>);
  }

  Future<List<PublicUser>> followers(String code) async {
    final resp = await _dio.get('/bff/seguidores/seguidores',
        queryParameters: {'codigo': code, 'page': 0, 'size': 100});
    return PageResponse.fromJson(resp.data as Map<String, dynamic>, PublicUser.fromJson).content;
  }

  Future<List<PublicUser>> following(String code) async {
    final resp = await _dio.get('/bff/seguidores/seguindo',
        queryParameters: {'codigo': code, 'page': 0, 'size': 100});
    return PageResponse.fromJson(resp.data as Map<String, dynamic>, PublicUser.fromJson).content;
  }
}
