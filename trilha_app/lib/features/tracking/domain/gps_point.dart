/// Mirrors the GpsPointResponse from the BFF (subset the map uses): a tracked
/// point of a path, ordered by `order` to draw the polyline.
class GpsPoint {
  const GpsPoint({
    required this.latitude,
    required this.longitude,
    this.order,
  });

  final double latitude;
  final double longitude;
  final int? order;

  factory GpsPoint.fromJson(Map<String, dynamic> json) {
    return GpsPoint(
      latitude: (json['latitude'] as num).toDouble(),
      longitude: (json['longitude'] as num).toDouble(),
      order: (json['ordem'] as num?)?.toInt(),
    );
  }
}
