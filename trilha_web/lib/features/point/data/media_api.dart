import 'package:dio/dio.dart';
import 'package:image_picker/image_picker.dart';

import '../../../core/env/env.dart';
import '../../../core/network/page_response.dart';
import '../domain/media_item.dart';

/// Photo upload directly to the Media service (`:8083`, outside the BFF). Uses the same
/// Dio (the interceptor injects the Bearer even on absolute URL). Returns the URL of
/// the binary already in storage, used as evidence for the point.
/// Listing of an adventure's media goes through the BFF (`/bff/midias`).
class MediaApi {
  MediaApi(this._dio);

  final Dio _dio;

  /// Recebe o [XFile] do picker e envia os bytes: na web nao existe caminho de
  /// arquivo (`dart:io`), so o conteudo em memoria.
  Future<String> uploadMedia(XFile file) async {
    final form = FormData.fromMap({
      'arquivo': MultipartFile.fromBytes(
        await file.readAsBytes(),
        filename: file.name,
      ),
    });
    final resp = await _dio.post(
      '${Env.midiaBaseUrl}/arquivo/upload',
      queryParameters: {'tipo': 'FOTO'},
      data: form,
    );
    return (resp.data as Map<String, dynamic>)['url'] as String;
  }

  Future<List<MediaItem>> listByAdventure(String adventureId, {int size = 10}) async {
    final resp = await _dio.get(
      '/bff/midias/aventura/$adventureId',
      queryParameters: {'page': 0, 'size': size},
    );
    return PageResponse.fromJson(resp.data as Map<String, dynamic>, MediaItem.fromJson).content;
  }
}
