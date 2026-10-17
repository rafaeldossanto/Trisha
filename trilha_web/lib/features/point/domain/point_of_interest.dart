/// Mirrors the PontoInteresseResponse from the BFF. `confidenceLevel` (1..5) is calculated
/// by the APP from the evidence; the app only displays it (marker color).
class PointOfInterest {
  const PointOfInterest({
    required this.id,
    required this.pathId,
    required this.type,
    this.name,
    this.description,
    required this.latitude,
    required this.longitude,
    required this.confidenceLevel,
  });

  final String id;
  final String pathId;
  final String type;
  final String? name;
  final String? description;
  final double latitude;
  final double longitude;
  final int confidenceLevel;

  factory PointOfInterest.fromJson(Map<String, dynamic> json) {
    return PointOfInterest(
      id: json['id'] as String,
      pathId: json['caminhoId'] as String,
      type: json['tipo'] as String,
      name: json['nome'] as String?,
      description: json['descricao'] as String?,
      latitude: (json['latitude'] as num).toDouble(),
      longitude: (json['longitude'] as num).toDouble(),
      confidenceLevel: (json['nivelConfianca'] as num?)?.toInt() ?? 1,
    );
  }
}
