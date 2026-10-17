/// Status pessoal de progressao sobre um ponto de interesse. O valor `wire`
/// e o do contrato do BFF; `label` e o texto exibido no app.
enum PointStatus {
  noRadar('NO_RADAR', 'No radar'),
  naMira('NA_MIRA', 'Na mira'),
  conquistado('CONQUISTADO', 'Conquistado');

  const PointStatus(this.wire, this.label);

  final String wire;
  final String label;

  static PointStatus? fromWire(String? value) {
    for (final status in PointStatus.values) {
      if (status.wire == value) {
        return status;
      }
    }
    return null;
  }
}

/// Marcacao do usuario num ponto: status de progressao (nulo se nao marcado)
/// e a flag de objetivo, independente do status.
class PointUserStatus {
  const PointUserStatus({
    required this.pointId,
    this.status,
    required this.goal,
  });

  final String pointId;
  final PointStatus? status;
  final bool goal;

  bool get isEmpty => status == null && !goal;

  factory PointUserStatus.fromJson(Map<String, dynamic> json) {
    return PointUserStatus(
      pointId: json['pontoId'] as String,
      status: PointStatus.fromWire(json['status'] as String?),
      goal: json['objetivo'] as bool? ?? false,
    );
  }
}
