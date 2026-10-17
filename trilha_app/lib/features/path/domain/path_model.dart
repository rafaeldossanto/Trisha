/// Mirrors the CaminhoResponse from the BFF. A path is a "leg" of the adventure,
/// with a sequential number; stays in progress until finalized (distance comes from GPS).
class PathModel {
  const PathModel({
    required this.id,
    required this.adventureId,
    required this.color,
    this.number,
    this.startedAt,
    this.finishedAt,
    this.totalDistanceKm,
  });

  final String id;
  final String adventureId;
  final String color;
  final int? number;
  final DateTime? startedAt;
  final DateTime? finishedAt;
  final double? totalDistanceKm;

  bool get finished => finishedAt != null;

  factory PathModel.fromJson(Map<String, dynamic> json) {
    return PathModel(
      id: json['id'] as String,
      adventureId: json['aventuraId'] as String,
      color: (json['cor'] as String?) ?? 'ROXO',
      number: (json['numero'] as num?)?.toInt(),
      startedAt: _parseDate(json['iniciadoEm']),
      finishedAt: _parseDate(json['finalizadoEm']),
      totalDistanceKm: (json['distanciaTotalKm'] as num?)?.toDouble(),
    );
  }

  static DateTime? _parseDate(dynamic value) =>
      value == null ? null : DateTime.tryParse(value as String);
}
