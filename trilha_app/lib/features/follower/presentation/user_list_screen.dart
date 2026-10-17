import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../../core/network/dio_client.dart';
import '../../friendship/domain/public_user.dart';
import '../data/follower_api.dart';

/// List of followers or of who the user follows. type = 'seguidores' | 'seguindo'.
class UserListScreen extends StatefulWidget {
  const UserListScreen({
    super.key,
    required this.code,
    required this.type,
    required this.title,
  });

  final String code;
  final String type;
  final String title;

  @override
  State<UserListScreen> createState() => _UserListScreenState();
}

class _UserListScreenState extends State<UserListScreen> {
  late final FollowerApi _api = FollowerApi(context.read<DioClient>().dio);

  List<PublicUser> _users = const [];
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final list = widget.type == 'seguidores'
          ? await _api.followers(widget.code)
          : await _api.following(widget.code);
      if (!mounted) {
        return;
      }
      setState(() {
        _users = list;
        _loading = false;
      });
    } catch (_) {
      if (mounted) {
        setState(() => _loading = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text(widget.title)),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _users.isEmpty
              ? const Center(child: Text('Ninguem por aqui ainda.'))
              : ListView(
                  children: _users.map((u) {
                    return ListTile(
                      leading: const Icon(Icons.person_outline),
                      title: Text(u.name),
                      subtitle: Text(u.userCode),
                      onTap: () => context.push('/perfil', extra: u),
                    );
                  }).toList(),
                ),
    );
  }
}
