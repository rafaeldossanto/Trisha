/// Mirrors the MediaResponse from the BFF: media metadata already stored,
/// with the binary URL served by the storage.
class MediaItem {
  const MediaItem({
    required this.id,
    required this.url,
    required this.type,
    this.adventureId,
    this.pathId,
  });

  final String id;
  final String url;
  final String type;
  final String? adventureId;
  final String? pathId;

  factory MediaItem.fromJson(Map<String, dynamic> json) {
    return MediaItem(
      id: json['id'] as String,
      url: json['url'] as String,
      type: (json['tipo'] as String?) ?? 'FOTO',
      adventureId: json['aventuraId'] as String?,
      pathId: json['caminhoId'] as String?,
    );
  }
}
