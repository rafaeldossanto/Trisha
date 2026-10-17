/// Mirrors the LiveSessionResponse from the BFF: a session being tracked right
/// now that the user may watch live, with the last known position for the
/// map marker. The live feed itself comes from the loc WebSocket.
class LiveSession {
  const LiveSession({
    required this.sessionId,
    required this.pathId,
    required this.userId,
    required this.userName,
    required this.userCode,
    required this.visibility,
    required this.latitude,
    required this.longitude,
  });

  final String sessionId;
  final String pathId;
  final String userId;
  final String userName;
  final String userCode;
  final String visibility;
  final double latitude;
  final double longitude;

  factory LiveSession.fromJson(Map<String, dynamic> json) {
    return LiveSession(
      sessionId: json['sessaoId'] as String,
      pathId: json['caminhoId'] as String,
      userId: json['usuarioId'] as String,
      userName: (json['usuarioNome'] as String?) ?? 'Trilheiro',
      userCode: (json['usuarioCodigo'] as String?) ?? '',
      visibility: (json['visibilidade'] as String?) ?? 'PUBLICO',
      latitude: (json['latitude'] as num).toDouble(),
      longitude: (json['longitude'] as num).toDouble(),
    );
  }
}
