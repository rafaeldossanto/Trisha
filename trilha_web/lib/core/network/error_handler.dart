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
        return _fromStatus(e.response?.statusCode);
      case DioExceptionType.cancel:
        return 'Requisicao cancelada.';
      default:
        return 'Erro de rede. Tente novamente.';
    }
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
