import 'dart:convert';

import 'package:dio/dio.dart';

/// Converts a caught exception into a user-friendly Portuguese message.
///
/// Usage in providers:
/// ```dart
/// } catch (e, st) {
///   _error = ErrorHandler.message(e, st);
/// }
/// ```
abstract final class ErrorHandler {
  ErrorHandler._();

  static String message(Object error, [StackTrace? stackTrace]) {
    if (error is DioException) {
      return _fromDio(error);
    }
    return 'Ocorreu um erro inesperado. Tente novamente.';
  }

  static String _fromDio(DioException e) {
    switch (e.type) {
      case DioExceptionType.connectionTimeout:
      case DioExceptionType.sendTimeout:
      case DioExceptionType.receiveTimeout:
        return 'Tempo de resposta esgotado. Verifique sua conexao.';
      case DioExceptionType.connectionError:
        return 'Sem conexao com o servidor. Verifique sua internet.';
      case DioExceptionType.badResponse:
        return _serverMessage(e.response) ?? _fromStatus(e.response?.statusCode);
      case DioExceptionType.cancel:
        return 'Requisicao cancelada.';
      default:
        return 'Erro de rede. Tente novamente.';
    }
  }

  /// Mensagem que o proprio backend escreveu para o usuario — "Email ou senha
  /// invalidos" ajuda muito mais que o texto generico do status. So vale para
  /// erros de cliente (4xx): em 5xx o corpo tende a ser tecnico.
  static String? _serverMessage(Response<dynamic>? response) {
    final status = response?.statusCode;
    if (status == null || status >= 500) {
      return null;
    }
    return _unwrapMessage(response?.data);
  }

  /// O GlobalExceptionHandler responde `{status, mensagem, timestamp}`. Quando o
  /// erro vem de um servico atras do BFF, o corpo original chega cru dentro de
  /// `mensagem` — por isso o desembrulho ate sobrar o texto de verdade.
  static String? _unwrapMessage(Object? data) {
    final message = data is Map ? data['mensagem'] : null;
    if (message is! String || message.isEmpty) {
      return null;
    }
    if (message.startsWith('{')) {
      try {
        return _unwrapMessage(jsonDecode(message));
      } on FormatException {
        return null;
      }
    }
    return message;
  }

  static String _fromStatus(int? status) {
    return switch (status) {
      400 => 'Dados invalidos. Confira as informacoes e tente novamente.',
      401 => 'Sessao expirada. Faca login novamente.',
      403 => 'Voce nao tem permissao para esta acao.',
      404 => 'Recurso nao encontrado.',
      409 => 'Conflito: este registro ja existe.',
      422 => 'Dados invalidos enviados ao servidor.',
      503 => 'Servico temporariamente indisponivel. Tente em instantes.',
      _ when status != null && status >= 500 => 'Erro interno do servidor. Tente novamente em instantes.',
      _ => 'Erro na comunicacao com o servidor.',
    };
  }
}
