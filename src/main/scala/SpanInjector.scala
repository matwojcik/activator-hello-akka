object SpanInjector {
  def injectSpanToMdc= {
    val span = Option(currentSpan)
    span.foreach { span =>
      val traceId = span.asInstanceOf[com.uber.jaeger.Span].context().getTraceId.toString
      val spanId = span.asInstanceOf[com.uber.jaeger.Span].context().getSpanId.toString
      if (traceId ne null)
        org.slf4j.MDC.put("X-B3-TraceId", traceId)
      if (spanId ne null)
        org.slf4j.MDC.put("X-B3-SpanId", spanId)
    }
  }

  private def currentSpan = {
    com.lightbend.cinnamon.opentracing.ActiveSpan.get()
  }
}
