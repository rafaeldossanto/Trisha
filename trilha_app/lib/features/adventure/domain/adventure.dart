/// Mirrors the AventuraResponse from the BFF. `regionId` can be null (adventure
/// without a resolved region); `visibility` is optional. `participantsCount` and
/// `durationHours` are derived metrics (duration is null while nothing finished).
class Adventure {
  const Adventure({
    required this.id,
    required this.userId,
    required this.regionId,
    required this.destination,
    required this.status,
    this.visibility,
    this.participantsCount = 0,
    this.durationHours,
  });

  final String id;
  final String userId;
  final String regionId;
  final String destination;
  final String status;
  final String? visibility;
  final int participantsCount;
  final double? durationHours;

  /// Rotulo curto "N pessoas · Xh" (esconde a duracao quando ainda nula).
  String get metricsLabel => adventureMetricsLabel(participantsCount, durationHours);

  factory Adventure.fromJson(Map<String, dynamic> json) {
    return Adventure(
      id: json['id'] as String,
      userId: json['usuarioId'] as String,
      regionId: (json['regiaoId'] as String?) ?? '',
      destination: json['destino'] as String,
      status: json['status'] as String,
      visibility: json['visibilidade'] as String?,
      participantsCount: (json['participantes'] as int?) ?? 0,
      durationHours: (json['duracaoHoras'] as num?)?.toDouble(),
    );
  }
}

/// "N pessoas" mais "· Xh" quando ha duracao. Compartilhado entre a aventura e
/// o item de feed para o rotulo ficar igual nas duas telas.
String adventureMetricsLabel(int participantsCount, double? durationHours) {
  final people = '$participantsCount ${participantsCount == 1 ? 'pessoa' : 'pessoas'}';
  if (durationHours == null) {
    return people;
  }
  return '$people · ${durationHours.toStringAsFixed(1)}h';
}
