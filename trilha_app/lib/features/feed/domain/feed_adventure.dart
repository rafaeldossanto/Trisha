import '../../adventure/domain/adventure.dart' show adventureMetricsLabel;

/// Mirrors the FeedAdventureResponse from the BFF: an adventure in the feed
/// with the author's name/code already resolved (a post). Carries the derived
/// metrics (people/duration) so the card can show "N pessoas · Xh".
class FeedAdventure {
  const FeedAdventure({
    required this.id,
    required this.userId,
    required this.userName,
    required this.userCode,
    required this.destination,
    required this.status,
    this.participantsCount = 0,
    this.durationHours,
  });

  final String id;
  final String userId;
  final String userName;
  final String userCode;
  final String destination;
  final String status;
  final int participantsCount;
  final double? durationHours;

  String get metricsLabel => adventureMetricsLabel(participantsCount, durationHours);

  factory FeedAdventure.fromJson(Map<String, dynamic> json) {
    return FeedAdventure(
      id: json['id'] as String,
      userId: json['usuarioId'] as String,
      userName: (json['usuarioNome'] as String?) ?? 'Trilheiro',
      userCode: (json['usuarioCodigo'] as String?) ?? '',
      destination: json['destino'] as String,
      status: json['status'] as String,
      participantsCount: (json['participantes'] as int?) ?? 0,
      durationHours: (json['duracaoHoras'] as num?)?.toDouble(),
    );
  }
}
