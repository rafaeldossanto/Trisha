/// Mirrors the TrailDiscoveryResponse from the BFF: a trail from another user
/// visible in the map viewport, with the decimated geometry ready to draw.
class DiscoveredTrail {
  const DiscoveredTrail({
    required this.pathId,
    required this.adventureId,
    required this.userId,
    required this.userName,
    required this.userCode,
    required this.destination,
    required this.color,
    required this.points,
  });

  final String pathId;
  final String adventureId;
  final String userId;
  final String userName;
  final String userCode;
  final String destination;
  final String color;
  final List<TrailPoint> points;

  factory DiscoveredTrail.fromJson(Map<String, dynamic> json) {
    return DiscoveredTrail(
      pathId: json['caminhoId'] as String,
      adventureId: json['aventuraId'] as String,
      userId: json['usuarioId'] as String,
      userName: (json['usuarioNome'] as String?) ?? 'Trilheiro',
      userCode: (json['usuarioCodigo'] as String?) ?? '',
      destination: (json['destino'] as String?) ?? '',
      color: (json['cor'] as String?) ?? 'ROXO',
      points: ((json['pontos'] as List<dynamic>?) ?? const [])
          .map((e) => TrailPoint.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }
}

/// Ponto da trilha descoberta; altitude vem junto para perfis de elevacao.
class TrailPoint {
  const TrailPoint({required this.latitude, required this.longitude, this.altitude});

  final double latitude;
  final double longitude;
  final double? altitude;

  factory TrailPoint.fromJson(Map<String, dynamic> json) {
    return TrailPoint(
      latitude: (json['latitude'] as num).toDouble(),
      longitude: (json['longitude'] as num).toDouble(),
      altitude: (json['altitude'] as num?)?.toDouble(),
    );
  }
}
