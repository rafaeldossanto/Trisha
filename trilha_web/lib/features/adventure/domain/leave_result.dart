/// Mirrors the LeaveAdventureResponse from the BFF: result of leaving (or being
/// removed from) a group adventure. When data is kept, `personalAdventureId`
/// points to the new private personal adventure; when discarded, it is null.
class LeaveResult {
  const LeaveResult({
    required this.personalAdventureId,
    required this.movedPaths,
    required this.deletedPaths,
  });

  final String? personalAdventureId;
  final int movedPaths;
  final int deletedPaths;

  bool get keptData => personalAdventureId != null;

  factory LeaveResult.fromJson(Map<String, dynamic> json) {
    return LeaveResult(
      personalAdventureId: json['aventuraPessoalId'] as String?,
      movedPaths: (json['caminhosMovidos'] as int?) ?? 0,
      deletedPaths: (json['caminhosExcluidos'] as int?) ?? 0,
    );
  }
}
