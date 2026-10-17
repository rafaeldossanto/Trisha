/// Generic page from the BFF. The BFF serializes with Spring Data keys
/// (`content`, `number`, `size`, `totalElements`, `totalPages`). The `fromItem`
/// converts each element of `content` into the domain type.
class PageResponse<T> {
  const PageResponse({
    required this.content,
    required this.page,
    required this.size,
    required this.total,
    required this.totalPages,
  });

  final List<T> content;
  final int page;
  final int size;
  final int total;
  final int totalPages;

  factory PageResponse.fromJson(
    Map<String, dynamic> json,
    T Function(Map<String, dynamic>) fromItem,
  ) {
    final items = (json['content'] as List<dynamic>? ?? const [])
        .map((e) => fromItem(e as Map<String, dynamic>))
        .toList();
    return PageResponse(
      content: items,
      page: (json['number'] as num?)?.toInt() ?? 0,
      size: (json['size'] as num?)?.toInt() ?? items.length,
      total: (json['totalElements'] as num?)?.toInt() ?? items.length,
      totalPages: (json['totalPages'] as num?)?.toInt() ?? 1,
    );
  }
}
