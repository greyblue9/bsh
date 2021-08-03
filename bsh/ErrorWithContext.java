package bsh;

interface ErrorWithContext {
  Object getContext();

  String getMessage();

  Throwable getCause();
}
