sealed class AppFailure implements Exception {
  const AppFailure(this.message);
  final String message;
}

class NetworkFailure extends AppFailure {
  const NetworkFailure(super.message);
}

class BackendFailure extends AppFailure {
  const BackendFailure(super.message);
}

class ValidationFailure extends AppFailure {
  const ValidationFailure(super.message);
}
