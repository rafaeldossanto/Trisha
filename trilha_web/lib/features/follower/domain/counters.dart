/// Follower/following counters for a user.
class Counters {
  const Counters({required this.followers, required this.following});

  final int followers;
  final int following;

  factory Counters.fromJson(Map<String, dynamic> json) {
    return Counters(
      followers: (json['seguidores'] as num).toInt(),
      following: (json['seguindo'] as num).toInt(),
    );
  }
}
