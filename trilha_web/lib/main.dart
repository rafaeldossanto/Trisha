import 'package:flutter/material.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:provider/provider.dart';

import 'app.dart';
import 'core/network/dio_client.dart';
import 'core/router/app_router.dart';
import 'core/storage/token_storage.dart';
import 'features/auth/data/auth_api.dart';
import 'features/auth/data/auth_repository.dart';
import 'features/auth/presentation/auth_provider.dart';
import 'features/adventure/data/adventure_api.dart';
import 'features/adventure/presentation/adventure_provider.dart';
import 'features/friendship/data/friendship_api.dart';
import 'features/friendship/data/user_search_api.dart';
import 'features/friendship/presentation/friendship_provider.dart';
import 'features/path/data/path_api.dart';
import 'features/path/presentation/trail_path_provider.dart';
import 'features/region/data/region_api.dart';
import 'features/region/presentation/region_provider.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await dotenv.load(fileName: '.env');

  final tokenStorage = TokenStorage();
  final dioClient = DioClient(tokenStorage);
  final authRepository = AuthRepository(AuthApi(dioClient.dio), tokenStorage);
  final authProvider = AuthProvider(authRepository);
  await authProvider.bootstrap();

  // Router created once, outside build, listening to AuthProvider.
  final router = buildRouter(authProvider);

  runApp(
    MultiProvider(
      providers: [
        Provider<DioClient>.value(value: dioClient),
        // O socket do ao vivo precisa do token cru (Bearer no CONNECT do STOMP).
        Provider<TokenStorage>.value(value: tokenStorage),
        ChangeNotifierProvider<AuthProvider>.value(value: authProvider),
        ChangeNotifierProvider<AdventureProvider>(
          create: (_) => AdventureProvider(AdventureApi(dioClient.dio)),
        ),
        ChangeNotifierProvider<TrailPathProvider>(
          create: (_) => TrailPathProvider(PathApi(dioClient.dio)),
        ),
        ChangeNotifierProvider<FriendshipProvider>(
          create: (_) => FriendshipProvider(
            FriendshipApi(dioClient.dio),
            UserSearchApi(dioClient.dio),
          ),
        ),
        ChangeNotifierProvider<RegionProvider>(
          create: (_) => RegionProvider(RegionApi(dioClient.dio)),
        ),
      ],
      child: TrilhaApp(router: router),
    ),
  );
}
