import 'package:flutter/foundation.dart';

import '../../../core/network/error_handler.dart';
import '../data/region_api.dart';
import '../domain/city.dart';
import '../domain/region.dart';

/// State for my regions (collections): list / create / edit / delete.
class RegionProvider extends ChangeNotifier {
  RegionProvider(this._api);

  final RegionApi _api;

  bool _loading = false;
  String? _error;
  List<Region> _regions = const [];

  bool get loading => _loading;
  String? get error => _error;
  List<Region> get regions => List.unmodifiable(_regions);

  Future<void> load() async {
    _loading = true;
    _error = null;
    notifyListeners();
    try {
      _regions = await _api.list();
    } catch (e, st) {
      _error = ErrorHandler.message(e, st);
    } finally {
      _loading = false;
      notifyListeners();
    }
  }

  Future<bool> save({
    String? id,
    required String name,
    String? description,
    String? coverUrl,
    required String visibility,
    required List<City> cities,
  }) async {
    try {
      if (id == null) {
        await _api.create(
            name: name, description: description, coverUrl: coverUrl, visibility: visibility, cities: cities);
      } else {
        await _api.update(id,
            name: name, description: description, coverUrl: coverUrl, visibility: visibility, cities: cities);
      }
      await load();
      return true;
    } catch (e, st) {
      _error = ErrorHandler.message(e, st);
      notifyListeners();
      return false;
    }
  }

  Future<void> remove(String id) async {
    try {
      await _api.delete(id);
      await load();
    } catch (e, st) {
      _error = ErrorHandler.message(e, st);
      notifyListeners();
    }
  }
}
