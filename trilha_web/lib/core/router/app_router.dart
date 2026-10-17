import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../features/auth/presentation/auth_provider.dart';
import '../../features/auth/presentation/login_screen.dart';
import '../../features/adventure/presentation/adventure_detail_screen.dart';
import '../../features/adventure/presentation/adventures_screen.dart';
import '../../features/explore/presentation/explore_screen.dart';
import '../../features/feed/presentation/feed_screen.dart';
import '../../features/follower/presentation/my_profile_screen.dart';
import '../../features/follower/presentation/profile_screen.dart';
import '../../features/follower/presentation/user_list_screen.dart';
import '../../features/friendship/domain/public_user.dart';
import '../../features/friendship/presentation/friendships_screen.dart';
import '../../features/map/presentation/home_map_screen.dart';
import '../../features/map/presentation/map_screen.dart';
import '../../features/region/domain/region.dart';
import '../../features/region/presentation/my_regions_screen.dart';
import '../../features/region/presentation/region_detail_screen.dart';
import '../../features/tracking/domain/live_session.dart';
import '../../features/tracking/presentation/live_watch_screen.dart';
import '../../features/tracking/presentation/tracking_screen.dart';
import '../../shared/widgets/main_shell.dart';
import '../../shared/widgets/web_page.dart';

/// Builds the GoRouter with authentication guard. The main tabs (home map,
/// explore, feed, profile) live in a StatefulShellRoute; on wide windows the
/// MainShell renders the Instagram-like side nav and, on narrow ones, the
/// bottom bar. Detail screens are pushed on the root navigator wrapped in
/// [WebPage] (centered column on wide windows); map/tracking screens stay
/// full-bleed. The home is the collaborative dark map with the trails plotted
/// in colors.
GoRouter buildRouter(AuthProvider auth) {
  final rootNavigatorKey = GlobalKey<NavigatorState>();

  return GoRouter(
    navigatorKey: rootNavigatorKey,
    initialLocation: '/home',
    refreshListenable: auth,
    redirect: (context, state) {
      final goingToLogin = state.matchedLocation == '/login';

      if (!auth.isLoggedIn) {
        return goingToLogin ? null : '/login';
      }
      if (goingToLogin) {
        return '/home';
      }
      return null;
    },
    routes: [
      GoRoute(
        path: '/login',
        builder: (context, state) => const WebPage(maxWidth: 440, child: LoginScreen()),
      ),
      StatefulShellRoute.indexedStack(
        builder: (context, state, shell) => MainShell(shell: shell),
        branches: [
          StatefulShellBranch(routes: [
            GoRoute(path: '/home', builder: (context, state) => const HomeMapScreen()),
          ]),
          StatefulShellBranch(routes: [
            GoRoute(path: '/explorar', builder: (context, state) => const ExploreScreen()),
          ]),
          StatefulShellBranch(routes: [
            GoRoute(path: '/feed', builder: (context, state) => const FeedScreen()),
          ]),
          StatefulShellBranch(routes: [
            GoRoute(path: '/perfil-meu', builder: (context, state) => const MyProfileScreen()),
          ]),
        ],
      ),
      GoRoute(
        path: '/aventuras',
        builder: (context, state) => const WebPage(child: AdventuresScreen()),
      ),
      GoRoute(
        path: '/amizades',
        builder: (context, state) => const WebPage(child: FriendshipsScreen()),
      ),
      GoRoute(
        path: '/regioes',
        builder: (context, state) => const WebPage(child: MyRegionsScreen()),
      ),
      GoRoute(
        path: '/regioes/detalhe',
        builder: (context, state) =>
            WebPage(child: RegionDetailScreen(region: state.extra as Region)),
      ),
      GoRoute(
        path: '/perfil',
        builder: (context, state) {
          final user = state.extra as PublicUser;
          return WebPage(
            maxWidth: 935,
            child: ProfileScreen(userCode: user.userCode, name: user.name),
          );
        },
      ),
      GoRoute(
        path: '/usuarios',
        builder: (context, state) {
          final args = state.extra as Map<String, dynamic>;
          return WebPage(
            child: UserListScreen(
              code: args['codigo'] as String,
              type: args['tipo'] as String,
              title: args['titulo'] as String,
            ),
          );
        },
      ),
      GoRoute(
        path: '/aventuras/:id',
        builder: (context, state) => WebPage(
          child: AdventureDetailScreen(adventureId: state.pathParameters['id']!),
        ),
      ),
      // Telas de mapa em tela cheia: sem moldura mesmo na janela larga.
      GoRoute(
        path: '/aventuras/:id/mapa',
        builder: (context, state) => MapScreen(adventureId: state.pathParameters['id']),
      ),
      GoRoute(
        path: '/caminhos/:caminhoId/rastreio',
        builder: (context, state) => TrackingScreen(pathId: state.pathParameters['caminhoId']!),
      ),
      GoRoute(
        path: '/ao-vivo',
        builder: (context, state) => LiveWatchScreen(session: state.extra as LiveSession),
      ),
    ],
  );
}
