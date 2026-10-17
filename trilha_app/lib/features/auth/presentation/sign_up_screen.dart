import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../../core/theme/app_theme.dart';
import '../../../shared/widgets/peak_logo.dart';
import 'sign_up_provider.dart';

/// Criacao de conta por email e senha. A conta nasce PENDENTE: em vez de entrar
/// direto no app, a tela termina no aviso de confirmacao — o login so funciona
/// depois que o usuario clica no link enviado por email.
class SignUpScreen extends StatefulWidget {
  const SignUpScreen({super.key});

  @override
  State<SignUpScreen> createState() => _SignUpScreenState();
}

class _SignUpScreenState extends State<SignUpScreen> {
  final _formKey = GlobalKey<FormState>();
  final _nameController = TextEditingController();
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();
  final _confirmationController = TextEditingController();
  bool _passwordHidden = true;

  @override
  void dispose() {
    _nameController.dispose();
    _emailController.dispose();
    _passwordController.dispose();
    _confirmationController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) {
      return;
    }
    await context.read<SignUpProvider>().submit(
          name: _nameController.text.trim(),
          email: _emailController.text.trim(),
          password: _passwordController.text,
        );
  }

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<SignUpProvider>();

    return Scaffold(
      appBar: AppBar(title: const Text('Criar conta')),
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: provider.createdUser == null
                ? _buildForm(provider)
                : _buildConfirmationNotice(provider),
          ),
        ),
      ),
    );
  }

  Widget _buildForm(SignUpProvider provider) {
    final theme = Theme.of(context);
    final busy = provider.loading;

    return Form(
      key: _formKey,
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          // Center pelo mesmo motivo da tela de login: o painter escala pela
          // largura recebida e o stretch da Column deformaria o logo.
          const Center(child: PeakLogo(size: 44)),
          const SizedBox(height: 8),
          const Text(
            'Trisha',
            textAlign: TextAlign.center,
            style: AppTheme.wordmarkLarge,
          ),
          const SizedBox(height: 28),
          TextFormField(
            controller: _nameController,
            textInputAction: TextInputAction.next,
            textCapitalization: TextCapitalization.words,
            autofillHints: const [AutofillHints.name],
            decoration: const InputDecoration(labelText: 'Nome'),
            validator: (v) =>
                (v == null || v.trim().isEmpty) ? 'Informe seu nome' : null,
          ),
          const SizedBox(height: 10),
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
            textInputAction: TextInputAction.next,
            autofillHints: const [AutofillHints.newPassword],
            decoration: InputDecoration(
              labelText: 'Senha',
              suffixIcon: IconButton(
                icon: Icon(_passwordHidden ? Icons.visibility_off : Icons.visibility),
                onPressed: () => setState(() => _passwordHidden = !_passwordHidden),
                tooltip: _passwordHidden ? 'Mostrar senha' : 'Ocultar senha',
              ),
            ),
            validator: (v) {
              if (v == null || v.isEmpty) return 'Informe uma senha';
              if (v.length < 8) return 'A senha precisa de pelo menos 8 caracteres';
              return null;
            },
          ),
          const SizedBox(height: 10),
          TextFormField(
            controller: _confirmationController,
            obscureText: _passwordHidden,
            textInputAction: TextInputAction.done,
            decoration: const InputDecoration(labelText: 'Confirmar senha'),
            onFieldSubmitted: (_) => _submit(),
            validator: (v) =>
                (v != _passwordController.text) ? 'As senhas nao conferem' : null,
          ),
          const SizedBox(height: 20),
          if (provider.error != null) ...[
            Text(
              provider.error!,
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
                : const Text('Criar conta'),
          ),
          const SizedBox(height: 12),
          TextButton(
            onPressed: busy ? null : () => Navigator.of(context).pop(),
            child: const Text('Ja tenho conta'),
          ),
        ],
      ),
    );
  }

  Widget _buildConfirmationNotice(SignUpProvider provider) {
    final theme = Theme.of(context);
    final email = provider.createdUser!.email;

    return Column(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const Icon(Icons.mark_email_unread_outlined, size: 56),
        const SizedBox(height: 16),
        Text(
          'Confira seu e-mail',
          textAlign: TextAlign.center,
          style: theme.textTheme.titleLarge,
        ),
        const SizedBox(height: 12),
        Text(
          'Enviamos um link de confirmacao para $email. '
          'Confirme a conta para poder entrar.',
          textAlign: TextAlign.center,
          style: TextStyle(color: theme.hintColor),
        ),
        const SizedBox(height: 24),
        if (provider.error != null) ...[
          Text(
            provider.error!,
            textAlign: TextAlign.center,
            style: TextStyle(color: theme.colorScheme.error),
          ),
          const SizedBox(height: 12),
        ],
        FilledButton(
          onPressed: () => Navigator.of(context).pop(),
          child: const Text('Voltar para o login'),
        ),
        const SizedBox(height: 8),
        TextButton(
          onPressed: provider.loading || provider.emailResent
              ? null
              : () => context.read<SignUpProvider>().resendConfirmation(),
          child: Text(provider.emailResent ? 'E-mail reenviado' : 'Reenviar e-mail'),
        ),
      ],
    );
  }
}
