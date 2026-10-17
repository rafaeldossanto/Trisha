/// Mirrors the AmizadeResponse from the BFF.
class Friendship {
  const Friendship({
    required this.id,
    required this.requesterId,
    required this.receiverId,
    required this.status,
    this.requestedAt,
    this.respondedAt,
  });

  final String id;
  final String requesterId;
  final String receiverId;
  final String status;
  final DateTime? requestedAt;
  final DateTime? respondedAt;

  factory Friendship.fromJson(Map<String, dynamic> json) {
    return Friendship(
      id: json['id'] as String,
      requesterId: json['solicitanteId'] as String,
      receiverId: json['receptorId'] as String,
      status: json['status'] as String,
      requestedAt: _parseDate(json['solicitadoEm']),
      respondedAt: _parseDate(json['respondidoEm']),
    );
  }

  static DateTime? _parseDate(dynamic v) => v == null ? null : DateTime.tryParse(v as String);
}
