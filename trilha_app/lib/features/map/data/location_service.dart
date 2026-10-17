import 'package:geolocator/geolocator.dart';

/// Gets the current location handling permission and disabled service.
/// Returns null when it's not possible to locate (permission denied / GPS off) —
/// the screen falls back to the fallback center.
class LocationService {
  Future<Position?> currentPosition() async {
    final serviceEnabled = await Geolocator.isLocationServiceEnabled();
    if (!serviceEnabled) {
      return null;
    }

    var permission = await Geolocator.checkPermission();
    if (permission == LocationPermission.denied) {
      permission = await Geolocator.requestPermission();
    }
    if (permission == LocationPermission.denied ||
        permission == LocationPermission.deniedForever) {
      return null;
    }

    return Geolocator.getCurrentPosition();
  }
}
