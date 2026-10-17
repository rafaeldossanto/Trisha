/// Um lugar devolvido pelo geocoding (Nominatim/OpenStreetMap): nome completo
/// e coordenada para centralizar o mapa da home.
class Place {
  const Place({required this.name, required this.latitude, required this.longitude});

  factory Place.fromJson(Map<String, dynamic> json) => Place(
        name: json['display_name'] as String? ?? '',
        latitude: double.parse(json['lat'].toString()),
        longitude: double.parse(json['lon'].toString()),
      );

  final String name;
  final double latitude;
  final double longitude;

  /// Primeira parte do display_name (ex: "Pico das Agulhas Negras").
  String get title => name.split(',').first.trim();

  /// Resto do display_name (ex: "Itatiaia, RJ, Brasil"); vazio quando nao ha.
  String get subtitle {
    final comma = name.indexOf(',');
    return comma < 0 ? '' : name.substring(comma + 1).trim();
  }
}
