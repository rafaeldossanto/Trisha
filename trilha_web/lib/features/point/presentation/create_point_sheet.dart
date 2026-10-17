import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../../core/network/dio_client.dart';
import '../../path/data/path_api.dart';
import '../../path/domain/path_model.dart';
import '../data/point_api.dart';

const _pointTypes = ['CACHOEIRA', 'MIRANTE', 'ESCALADA', 'ACAMPAMENTO', 'OUTRO'];

/// Form to create a point of interest at the location tapped on the map. Starts with
/// confidence level 1; gains level as it receives evidence.
class CreatePointSheet extends StatefulWidget {
  const CreatePointSheet({
    super.key,
    required this.adventureId,
    required this.latitude,
    required this.longitude,
  });

  final String adventureId;
  final double latitude;
  final double longitude;

  @override
  State<CreatePointSheet> createState() => _CreatePointSheetState();
}

class _CreatePointSheetState extends State<CreatePointSheet> {
  final _formKey = GlobalKey<FormState>();
  final _nameController = TextEditingController();
  final _descriptionController = TextEditingController();

  late final PathApi _pathApi = PathApi(context.read<DioClient>().dio);
  late final PointApi _pointApi = PointApi(context.read<DioClient>().dio);

  List<PathModel> _paths = const [];
  PathModel? _selectedPath;
  String _type = _pointTypes.first;
  bool _loading = true;
  bool _saving = false;

  @override
  void initState() {
    super.initState();
    _loadPaths();
  }

  @override
  void dispose() {
    _nameController.dispose();
    _descriptionController.dispose();
    super.dispose();
  }

  Future<void> _loadPaths() async {
    try {
      final page = await _pathApi.listByAdventure(widget.adventureId);
      if (!mounted) {
        return;
      }
      setState(() {
        _paths = page.content;
        _selectedPath = _paths.isNotEmpty ? _paths.first : null;
        _loading = false;
      });
    } catch (_) {
      if (mounted) {
        setState(() => _loading = false);
      }
    }
  }

  Future<void> _save() async {
    if (!_formKey.currentState!.validate() || _selectedPath == null) {
      return;
    }
    setState(() => _saving = true);
    try {
      await _pointApi.create(
        pathId: _selectedPath!.id,
        type: _type,
        name: _nameController.text.trim().isEmpty ? null : _nameController.text.trim(),
        description: _descriptionController.text.trim().isEmpty ? null : _descriptionController.text.trim(),
        latitude: widget.latitude,
        longitude: widget.longitude,
      );
      if (mounted) {
        Navigator.of(context).pop(true);
      }
    } catch (_) {
      if (mounted) {
        setState(() => _saving = false);
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Nao foi possivel criar o ponto')),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final bottomInset = MediaQuery.of(context).viewInsets.bottom;
    return Padding(
      padding: EdgeInsets.fromLTRB(24, 24, 24, 24 + bottomInset),
      child: _buildBody(),
    );
  }

  Widget _buildBody() {
    if (_loading) {
      return const Padding(
        padding: EdgeInsets.all(24),
        child: Center(child: CircularProgressIndicator()),
      );
    }
    if (_paths.isEmpty) {
      return const Padding(
        padding: EdgeInsets.all(16),
        child: Text('Inicie um caminho na aventura antes de marcar pontos.'),
      );
    }

    return Form(
      key: _formKey,
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text('Novo ponto', style: Theme.of(context).textTheme.titleLarge),
          const SizedBox(height: 16),
          DropdownButtonFormField<PathModel>(
            initialValue: _selectedPath,
            decoration: const InputDecoration(labelText: 'Caminho'),
            items: _paths
                .map((p) => DropdownMenuItem(value: p, child: Text('Caminho ${p.number ?? ''}')))
                .toList(),
            onChanged: (p) => setState(() => _selectedPath = p),
          ),
          const SizedBox(height: 16),
          DropdownButtonFormField<String>(
            initialValue: _type,
            decoration: const InputDecoration(labelText: 'Tipo'),
            items: _pointTypes
                .map((t) => DropdownMenuItem(value: t, child: Text(t)))
                .toList(),
            onChanged: (t) => setState(() => _type = t ?? _type),
          ),
          const SizedBox(height: 16),
          TextFormField(
            controller: _nameController,
            decoration: const InputDecoration(labelText: 'Nome (opcional)'),
          ),
          const SizedBox(height: 16),
          TextFormField(
            controller: _descriptionController,
            decoration: const InputDecoration(labelText: 'Descricao (opcional)'),
            maxLines: 2,
          ),
          const SizedBox(height: 24),
          FilledButton(
            onPressed: _saving ? null : _save,
            child: _saving
                ? const SizedBox(height: 20, width: 20, child: CircularProgressIndicator(strokeWidth: 2))
                : const Text('Criar ponto'),
          ),
        ],
      ),
    );
  }
}
