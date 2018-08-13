import io.opentracing.util.GlobalTracer

object SpanInjector {
  def injectSpanToMdc= {
    val span = Option(currentSpan) getOrElse createSpan
      val traceId = span.asInstanceOf[com.uber.jaeger.Span].context().getTraceId.toString
      val spanId = span.asInstanceOf[com.uber.jaeger.Span].context().getSpanId.toString
      if (traceId ne null)
        org.slf4j.MDC.put("X-B3-TraceId", traceId)
      if (spanId ne null)
        org.slf4j.MDC.put("X-B3-SpanId", spanId)
    }

  private def createSpan = {
    val tracer = GlobalTracer.get
    tracer.buildSpan("custom-span").startActive(false).span()
  }

  private def currentSpan = {
    com.lightbend.cinnamon.opentracing.ActiveSpan.get()
  }
}
