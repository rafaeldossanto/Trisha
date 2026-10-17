/// Follow relationship between the logged user and another (mutual enables adding friend).
class FollowStatus {
  const FollowStatus({required this.isFollowing, required this.followsMe, required this.isMutual});

  final bool isFollowing;
  final bool followsMe;
  final bool isMutual;

  factory FollowStatus.fromJson(Map<String, dynamic> json) {
    return FollowStatus(
      isFollowing: json['sigo'] as bool,
      followsMe: json['meSegue'] as bool,
      isMutual: json['mutuo'] as bool,
    );
  }
}
