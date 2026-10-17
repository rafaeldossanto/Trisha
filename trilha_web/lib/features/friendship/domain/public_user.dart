/// Public view of a user (search to add friends). Only the userCode
/// (public handle) and the name — no internal id.
class PublicUser {
  const PublicUser({required this.userCode, required this.name});

  final String userCode;
  final String name;

  factory PublicUser.fromJson(Map<String, dynamic> json) {
    return PublicUser(
      userCode: json['codigoUsuario'] as String,
      name: json['nome'] as String,
    );
  }
}
