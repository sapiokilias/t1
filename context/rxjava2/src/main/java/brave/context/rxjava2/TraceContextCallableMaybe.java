package brave.context.rxjava2;

import brave.context.rxjava2.TraceContextMaybe.Observer;
import brave.propagation.CurrentTraceContext;
import brave.propagation.CurrentTraceContext.Scope;
import brave.propagation.TraceContext;
import io.reactivex.Maybe;
import io.reactivex.MaybeObserver;
import io.reactivex.MaybeSource;
import java.util.concurrent.Callable;

final class TraceContextCallableMaybe<T> extends Maybe<T>
    implements Callable<T>, TraceContextGetter {
  @Override public TraceContext traceContext() {
    return assemblyContext;
  }

  final MaybeSource<T> source;
  final CurrentTraceContext currentTraceContext;
  final TraceContext assemblyContext;

  TraceContextCallableMaybe(
      MaybeSource<T> source,
      CurrentTraceContext currentTraceContext,
      TraceContext assemblyContext
  ) {
    this.source = source;
    this.currentTraceContext = currentTraceContext;
    this.assemblyContext = assemblyContext;
  }

  @Override protected void subscribeActual(MaybeObserver<? super T> s) {
    try (Scope scope = currentTraceContext.newScope(assemblyContext)) {
      source.subscribe(new Observer<>(s, currentTraceContext, assemblyContext));
    }
  }

  @SuppressWarnings("unchecked") @Override public T call() throws Exception {
    try (Scope scope = currentTraceContext.newScope(assemblyContext)) {
      return ((Callable<T>) source).call();
    }
  }
}