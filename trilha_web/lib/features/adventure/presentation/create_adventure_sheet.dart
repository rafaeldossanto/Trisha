import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../../core/network/dio_client.dart';
import '../../region/data/region_api.dart';
import '../../region/domain/region.dart';
import 'adventure_provider.dart';

/// Bottom sheet for creating an adventure. The region comes from a selector loaded from
/// GET /bff/regioes; sends the selected id in the POST.
class CreateAdventureSheet extends StatefulWidget {
  const CreateAdventureSheet({super.key, required this.userId});

  final String userId;

  @override
  State<CreateAdventureSheet> createState() => _CreateAdventureSheetState();
}

class _CreateAdventureSheetState extends State<CreateAdventureSheet> {
  final _formKey = GlobalKey<FormState>();
  final _destinationController = TextEditingController();

  List<Region> _regions = const [];
  Region? _selectedRegion;
  bool _loadingRegions = true;

  String _visibility = 'PRIVADA';
  bool _saving = false;

  @override
  void initState() {
    super.initState();
    _loadRegions();
  }

  @override
  void dispose() {
    _destinationController.dispose();
    super.dispose();
  }

  Future<void> _loadRegions() async {
    try {
      final regions = await RegionApi(context.read<DioClient>().dio).list();
      if (!mounted) {
        return;
      }
      setState(() {
        _regions = regions;
        _loadingRegions = false;
      });
    } catch (_) {
      if (!mounted) {
        return;
      }
      setState(() => _loadingRegions = false);
    }
  }

  Future<void> _save() async {
    if (!_formKey.currentState!.validate()) {
      return;
    }
    setState(() => _saving = true);
    final provider = context.read<AdventureProvider>();
    final ok = await provider.create(
      userId: widget.userId,
      regionId: _selectedRegion?.id,
      destination: _destinationController.text.trim(),
      visibility: _visibility,
    );
    if (!mounted) {
      return;
    }
    setState(() => _saving = false);
    if (ok) {
      Navigator.of(context).pop();
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(provider.error ?? 'Erro ao criar aventura')),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final bottomInset = MediaQuery.of(context).viewInsets.bottom;
    return Padding(
      padding: EdgeInsets.fromLTRB(24, 24, 24, 24 + bottomInset),
      child: Form(
        key: _formKey,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text('Nova aventura', style: Theme.of(context).textTheme.titleLarge),
            const SizedBox(height: 16),
            TextFormField(
              controller: _destinationController,
              textInputAction: TextInputAction.next,
              decoration: const InputDecoration(labelText: 'Destino'),
              validator: (v) =>
                  (v == null || v.trim().isEmpty) ? 'Informe o destino' : null,
            ),
            const SizedBox(height: 16),
            _regionField(),
            const SizedBox(height: 16),
            SegmentedButton<String>(
              segments: const [
                ButtonSegment(value: 'PRIVADA', label: Text('Privada')),
                ButtonSegment(value: 'SO_GRUPO', label: Text('Grupo')),
                ButtonSegment(value: 'PUBLICA', label: Text('Publica')),
              ],
              selected: {_visibility},
              onSelectionChanged: (s) => setState(() => _visibility = s.first),
            ),
            const SizedBox(height: 24),
            FilledButton(
              onPressed: _saving ? null : _save,
              child: _saving
                  ? const SizedBox(
                      height: 20,
                      width: 20,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                  : const Text('Criar'),
            ),
          ],
        ),
      ),
    );
  }

  Widget _regionField() {
    if (_loadingRegions) {
      return const InputDecorator(
        decoration: InputDecoration(labelText: 'Regiao'),
        child: SizedBox(
          height: 20,
          width: 20,
          child: CircularProgressIndicator(strokeWidth: 2),
        ),
      );
    }

    if (_regions.isEmpty) {
      return const InputDecorator(
        decoration: InputDecoration(labelText: 'Colecao (opcional)'),
        child: Text('Nenhuma colecao — a aventura ficara solta'),
      );
    }

    return DropdownButtonFormField<Region>(
      initialValue: _selectedRegion,
      decoration: const InputDecoration(labelText: 'Colecao (opcional)'),
      items: _regions
          .map((r) => DropdownMenuItem(value: r, child: Text(r.name)))
          .toList(),
      onChanged: (r) => setState(() => _selectedRegion = r),
    );
  }
}
