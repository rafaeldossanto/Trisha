import 'package:flutter/foundation.dart';

import '../../../core/network/error_handler.dart';
import '../data/path_api.dart';
import '../domain/path_model.dart';

/// State for the paths of an adventure (list / start / finish).
/// Named TrailPathProvider to avoid conflict with the `path_provider` Flutter package.
class TrailPathProvider extends ChangeNotifier {
  TrailPathProvider(this._api);

  final PathApi _api;

  bool _loading = false;
  String? _error;
  List<PathModel> _paths = const [];

  bool get loading => _loading;
  String? get error => _error;
  List<PathModel> get paths => List.unmodifiable(_paths);

  Future<void> load(String adventureId) async {
    _loading = true;
    _error = null;
    notifyListeners();
    try {
      final page = await _api.listByAdventure(adventureId);
      _paths = page.content;
    } catch (e, st) {
      _error = ErrorHandler.message(e, st);
    } finally {
      _loading = false;
      notifyListeners();
    }
  }

  Future<PathModel?> start(String adventureId, {String? color}) async {
    try {
      final path = await _api.start(adventureId: adventureId, color: color);
      await load(adventureId);
      return path;
    } catch (e, st) {
      _error = ErrorHandler.message(e, st);
      notifyListeners();
      return null;
    }
  }

  Future<void> finish(String adventureId, String pathId) async {
    try {
      await _api.finish(pathId);
      await load(adventureId);
    } catch (e, st) {
      _error = ErrorHandler.message(e, st);
      notifyListeners();
    }
  }
}
