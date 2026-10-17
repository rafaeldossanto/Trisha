/// Mirrors the UsuarioResponse from the BFF (subset the app uses).
class User {
  const User({
    required this.id,
    required this.name,
    required this.email,
    required this.userCode,
  });

  final String id;
  final String name;
  final String email;
  final String userCode;

  factory User.fromJson(Map<String, dynamic> json) {
    return User(
      id: json['id'] as String,
      name: json['nome'] as String,
      email: json['email'] as String,
      userCode: (json['codigoUsuario'] as String?) ?? '',
    );
  }
}
