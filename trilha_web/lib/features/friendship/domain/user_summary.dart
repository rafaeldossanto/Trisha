/// Mirrors the UserSummaryResponse from the BFF: name/code plus the internal
/// userId — needed to list another user's (visible) adventures.
class UserSummary {
  const UserSummary({required this.id, required this.name, required this.userCode});

  final String id;
  final String name;
  final String userCode;

  factory UserSummary.fromJson(Map<String, dynamic> json) {
    return UserSummary(
      id: json['id'] as String,
      name: (json['nome'] as String?) ?? '',
      userCode: (json['codigoUsuario'] as String?) ?? '',
    );
  }
}
