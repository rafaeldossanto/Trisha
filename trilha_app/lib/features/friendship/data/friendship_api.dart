import 'package:dio/dio.dart';

import '../../../core/network/page_response.dart';
import '../domain/friendship.dart';

/// Accesses friendships via BFF. The requester comes from the token; the target
/// is identified by userCode (public handle), resolved to id in the APP.
class FriendshipApi {
  FriendshipApi(this._dio);

  final Dio _dio;

  Future<PageResponse<Friendship>> pending({int page = 0, int size = 50}) async {
    final resp = await _dio.get(
      '/bff/amizades/pendentes',
      queryParameters: {'page': page, 'size': size},
    );
    return PageResponse.fromJson(resp.data as Map<String, dynamic>, Friendship.fromJson);
  }

  Future<PageResponse<Friendship>> listFriends({int page = 0, int size = 50}) async {
    final resp = await _dio.get(
      '/bff/amizades/amigos',
      queryParameters: {'page': page, 'size': size},
    );
    return PageResponse.fromJson(resp.data as Map<String, dynamic>, Friendship.fromJson);
  }

  Future<void> sendRequest(String receiverCode) async {
    await _dio.post('/bff/amizades', data: {'receptorCodigo': receiverCode});
  }

  Future<void> respond(String friendshipId, String status) async {
    await _dio.patch(
      '/bff/amizades/$friendshipId/responder',
      queryParameters: {'status': status},
    );
  }
}
