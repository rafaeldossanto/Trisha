import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';
import 'package:sign_in_with_apple/sign_in_with_apple.dart';

import '../../../core/theme/app_theme.dart';
import '../../../shared/widgets/peak_logo.dart';
import 'auth_provider.dart';

/// Tela de entrada: email + senha, criacao de conta e login pelos provedores
/// sociais. Sem token, o guard do router manda para ca; ao autenticar, vai para
/// /home. Em build de debug ainda ha o atalho do login sem senha, que usa o
/// endpoint exclusivo do profile `dev` do backend.
class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final _formKey = GlobalKey<FormState>();
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();
  bool _passwordHidden = true;

  @override
  void dispose() {
    _emailController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) {
      return;
    }
    await context.read<AuthProvider>().login(
          email: _emailController.text.trim(),
          password: _passwordController.text,
        );
  }

  @override
  Widget build(BuildContext context) {
    final auth = context.watch<AuthProvider>();
    final theme = Theme.of(context);
    final busy = auth.loading;

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
                  const Center(child: PeakLogo(size: 56)),
                  const SizedBox(height: 8),
                  const Text(
                    'Trisha',
                    textAlign: TextAlign.center,
                    style: AppTheme.wordmarkLarge,
                  ),
                  const SizedBox(height: 32),
                  TextFormField(
                    controller: _emailController,
                    keyboardType: TextInputType.emailAddress,
                    textInputAction: TextInputAction.next,
                    autofillHints: const [AutofillHints.email],
                    decoration: const InputDecoration(labelText: 'E-mail'),
                    validator: (v) {
                      if (v == null || v.trim().isEmpty) return 'Informe seu e-mail';
                      if (!v.contains('@')) return 'E-mail invalido';
                      return null;
                    },
                  ),
                  const SizedBox(height: 10),
                  TextFormField(
                    controller: _passwordController,
                    obscureText: _passwordHidden,
                    textInputAction: TextInputAction.done,
                    autofillHints: const [AutofillHints.password],
                    decoration: InputDecoration(
                      labelText: 'Senha',
                      suffixIcon: IconButton(
                        icon: Icon(_passwordHidden ? Icons.visibility_off : Icons.visibility),
                        onPressed: () => setState(() => _passwordHidden = !_passwordHidden),
                        tooltip: _passwordHidden ? 'Mostrar senha' : 'Ocultar senha',
                      ),
                    ),
                    onFieldSubmitted: (_) => _submit(),
                    validator: (v) =>
                        (v == null || v.isEmpty) ? 'Informe sua senha' : null,
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
                    onPressed: busy ? null : _submit,
                    child: busy
                        ? const SizedBox(
                            height: 20,
                            width: 20,
                            child: CircularProgressIndicator(strokeWidth: 2),
                          )
                        : const Text('Entrar'),
                  ),
                  const SizedBox(height: 12),
                  OutlinedButton(
                    onPressed: busy ? null : () => context.push('/cadastro'),
                    child: const Text('Criar conta'),
                  ),
                  const SizedBox(height: 24),
                  const _OrDivider(),
                  const SizedBox(height: 16),
                  OutlinedButton.icon(
                    onPressed: busy ? null : () => context.read<AuthProvider>().loginWithGoogle(),
                    icon: const Icon(Icons.g_mobiledata, size: 28),
                    label: const Text('Continuar com Google'),
                  ),
                  if (auth.appleAvailable) ...[
                    const SizedBox(height: 10),
                    SignInWithAppleButton(
                      text: 'Continuar com Apple',
                      style: SignInWithAppleButtonStyle.white,
                      borderRadius: BorderRadius.circular(10),
                      onPressed: busy ? null : () => context.read<AuthProvider>().loginWithApple(),
                    ),
                  ],
                  if (kDebugMode) ...[
                    const SizedBox(height: 8),
                    TextButton(
                      onPressed: busy ? null : _openDevLogin,
                      child: const Text('Entrar sem senha (dev)'),
                    ),
                  ],
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }

  /// Atalho de desenvolvimento: cria ou recupera a conta pelo email e emite o
  /// token sem senha nem confirmacao. Depende do profile `dev` no BFF.
  Future<void> _openDevLogin() async {
    final credentials = await showDialog<({String name, String email})>(
      context: context,
      builder: (_) => _DevLoginDialog(email: _emailController.text.trim()),
    );
    if (credentials == null || !mounted) {
      return;
    }
    await context.read<AuthProvider>().devLogin(
          email: credentials.email,
          name: credentials.name,
        );
  }
}

/// Linha divisoria com o rotulo entre o login por senha e os provedores sociais.
class _OrDivider extends StatelessWidget {
  const _OrDivider();

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        const Expanded(child: Divider()),
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 12),
          child: Text(
            'ou entre com',
            style: TextStyle(color: Theme.of(context).hintColor),
          ),
        ),
        const Expanded(child: Divider()),
      ],
    );
  }
}

class _DevLoginDialog extends StatefulWidget {
  const _DevLoginDialog({required this.email});

  final String email;

  @override
  State<_DevLoginDialog> createState() => _DevLoginDialogState();
}

class _DevLoginDialogState extends State<_DevLoginDialog> {
  late final _nameController = TextEditingController();
  late final _emailController = TextEditingController(text: widget.email);

  @override
  void dispose() {
    _nameController.dispose();
    _emailController.dispose();
    super.dispose();
  }

  void _confirm() {
    final name = _nameController.text.trim();
    final email = _emailController.text.trim();
    if (name.isEmpty || !email.contains('@')) {
      return;
    }
    Navigator.pop(context, (name: name, email: email));
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: const Text('Login de desenvolvimento'),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          TextField(
            controller: _nameController,
            decoration: const InputDecoration(labelText: 'Nome'),
            textInputAction: TextInputAction.next,
          ),
          const SizedBox(height: 10),
          TextField(
            controller: _emailController,
            keyboardType: TextInputType.emailAddress,
            decoration: const InputDecoration(labelText: 'E-mail'),
            onSubmitted: (_) => _confirm(),
          ),
        ],
      ),
      actions: [
        TextButton(onPressed: () => Navigator.pop(context), child: const Text('Cancelar')),
        FilledButton(onPressed: _confirm, child: const Text('Entrar')),
      ],
    );
  }
}
