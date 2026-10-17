import 'package:flutter/foundation.dart';

import '../../../core/network/error_handler.dart';
import '../data/adventure_api.dart';
import '../domain/adventure.dart';

/// State for the list of adventures of the logged user.
class AdventureProvider extends ChangeNotifier {
  AdventureProvider(this._api);

  final AdventureApi _api;

  bool _loading = false;
  String? _error;
  List<Adventure> _adventures = const [];

  bool get loading => _loading;
  String? get error => _error;
  List<Adventure> get adventures => List.unmodifiable(_adventures);

  Future<void> load(String userId) async {
    _loading = true;
    _error = null;
    notifyListeners();
    try {
      final page = await _api.listByUser(userId);
      _adventures = page.content;
    } catch (e, st) {
      _error = ErrorHandler.message(e, st);
    } finally {
      _loading = false;
      notifyListeners();
    }
  }

  /// Creates and reloads the list. The region (folder) is optional. Returns true on
  /// success; on error stores the message.
  Future<bool> create({
    required String userId,
    String? regionId,
    required String destination,
    String? visibility,
  }) async {
    try {
      await _api.create(regionId: regionId, destination: destination, visibility: visibility);
      await load(userId);
      return true;
    } catch (e, st) {
      _error = ErrorHandler.message(e, st);
      notifyListeners();
      return false;
    }
  }
}
