import 'package:dio/dio.dart';

import '../domain/public_user.dart';
import '../domain/user_summary.dart';

/// User search for adding friends (via BFF).
class UserSearchApi {
  UserSearchApi(this._dio);

  final Dio _dio;

  Future<List<PublicUser>> searchUsers(String term) async {
    final resp = await _dio.get('/bff/usuarios/busca', queryParameters: {'termo': term});
    return (resp.data as List<dynamic>)
        .map((e) => PublicUser.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  /// Resumo com o userId a partir do codigo publico — usado pelo perfil de
  /// terceiro para listar as aventuras visiveis daquele usuario.
  Future<UserSummary> summaryByCode(String userCode) async {
    final resp = await _dio.get('/bff/usuarios/resumo', queryParameters: {'codigo': userCode});
    return UserSummary.fromJson(resp.data as Map<String, dynamic>);
  }
}
