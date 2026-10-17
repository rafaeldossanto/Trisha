import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../../core/theme/app_theme.dart';
import 'auth_provider.dart';

/// Login screen (dev): name + e-mail and the Enter button. Without a token, the router
/// guard sends here; on login, it goes to /home.
class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final _formKey = GlobalKey<FormState>();
  final _nameController = TextEditingController();
  final _emailController = TextEditingController();

  @override
  void dispose() {
    _nameController.dispose();
    _emailController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) {
      return;
    }
    await context.read<AuthProvider>().login(
          email: _emailController.text.trim(),
          name: _nameController.text.trim(),
        );
  }

  @override
  Widget build(BuildContext context) {
    final auth = context.watch<AuthProvider>();
    final theme = Theme.of(context);

    return Scaffold(
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: Form(
              key: _formKey,
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  // Center evita que o stretch da Column estique o CustomPaint:
                  // o painter escala pela largura recebida e o logo sairia da tela.
                  const Center(child: _PeakLogo(size: 56)),
                  const SizedBox(height: 8),
                  const Text(
                    'Trisha',
                    textAlign: TextAlign.center,
                    style: AppTheme.wordmarkLarge,
                  ),
                  const SizedBox(height: 32),
                  TextFormField(
                    controller: _nameController,
                    textInputAction: TextInputAction.next,
                    decoration: const InputDecoration(labelText: 'Nome'),
                    validator: (v) =>
                        (v == null || v.trim().isEmpty) ? 'Informe seu nome' : null,
                  ),
                  const SizedBox(height: 10),
                  TextFormField(
                    controller: _emailController,
                    keyboardType: TextInputType.emailAddress,
                    textInputAction: TextInputAction.done,
                    decoration: const InputDecoration(labelText: 'E-mail'),
                    onFieldSubmitted: (_) => _submit(),
                    validator: (v) {
                      if (v == null || v.trim().isEmpty) return 'Informe seu e-mail';
                      if (!v.contains('@')) return 'E-mail invalido';
                      return null;
                    },
                  ),
                  const SizedBox(height: 20),
                  if (auth.error != null) ...[
                    Text(
                      auth.error!,
                      textAlign: TextAlign.center,
                      style: TextStyle(color: theme.colorScheme.error),
                    ),
                    const SizedBox(height: 12),
                  ],
                  FilledButton(
                    onPressed: auth.loading ? null : _submit,
                    child: auth.loading
                        ? const SizedBox(
                            height: 20,
                            width: 20,
                            child: CircularProgressIndicator(strokeWidth: 2),
                          )
                        : const Text('Entrar'),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}

/// Logo do login: picos de montanha (sem base), em branco.
class _PeakLogo extends StatelessWidget {
  const _PeakLogo({required this.size});

  final double size;

  @override
  Widget build(BuildContext context) {
    return CustomPaint(
      size: Size(size, size),
      painter: _PeakLogoPainter(),
    );
  }
}

class _PeakLogoPainter extends CustomPainter {
  @override
  void paint(Canvas canvas, Size size) {
    final scale = size.width / 24;
    final paint = Paint()
      ..color = Colors.white
      ..style = PaintingStyle.stroke
      ..strokeWidth = 2.2 * scale
      ..strokeCap = StrokeCap.round
      ..strokeJoin = StrokeJoin.round;
    final path = Path()
      ..moveTo(3 * scale, 19 * scale)
      ..lineTo(11 * scale, 7 * scale)
      ..lineTo(19 * scale, 19 * scale)
      ..moveTo(13 * scale, 19 * scale)
      ..lineTo(18 * scale, 12 * scale)
      ..lineTo(23 * scale, 19 * scale);
    canvas.drawPath(path, paint);
  }

  @override
  bool shouldRepaint(covariant _PeakLogoPainter oldDelegate) => false;
}
