/// Mirrors the SessaoResponse from the BFF (subset used in tracking).
class Session {
  const Session({
    required this.id,
    required this.pathId,
    required this.status,
    this.visibility,
    this.totalDistanceKm,
  });

  final String id;
  final String pathId;
  final String status;

  /// Quem acompanha ao vivo: PUBLICO/SEGUIDORES/AMIGOS/PRIVADO.
  final String? visibility;
  final double? totalDistanceKm;

  factory Session.fromJson(Map<String, dynamic> json) {
    return Session(
      id: json['id'] as String,
      pathId: json['caminhoId'] as String,
      status: json['status'] as String,
      visibility: json['visibilidade'] as String?,
      totalDistanceKm: (json['distanciaTotalKm'] as num?)?.toDouble(),
    );
  }
}
