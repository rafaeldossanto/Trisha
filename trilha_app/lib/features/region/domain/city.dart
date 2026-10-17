/// City that composes a region. Coordinates are optional (can be empty).
class City {
  const City({required this.name, this.latitude, this.longitude, this.altitude});

  final String name;
  final double? latitude;
  final double? longitude;
  final double? altitude;

  factory City.fromJson(Map<String, dynamic> json) {
    return City(
      name: json['nome'] as String,
      latitude: (json['latitude'] as num?)?.toDouble(),
      longitude: (json['longitude'] as num?)?.toDouble(),
      altitude: (json['altitude'] as num?)?.toDouble(),
    );
  }

  Map<String, dynamic> toJson() => {
        'nome': name,
        'latitude': latitude,
        'longitude': longitude,
        'altitude': altitude,
      };
}
