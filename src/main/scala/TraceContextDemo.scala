import akka.actor._
import akka.stream._
import akka.stream.scaladsl._
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import com.lightbend.cinnamon.akka.stream.CinnamonAttributes._
import io.opentracing.propagation.{Format, TextMapExtractAdapter, TextMapInjectAdapter}
import io.opentracing.util.GlobalTracer
import io.opentracing.{Scope, SpanContext, Tracer}
import java.util.{HashMap => JHashMap, Map => JMap}

import com.typesafe.scalalogging.StrictLogging

import scala.collection.JavaConverters._
import scala.io.StdIn

object TraceContextDemo extends App with StrictLogging{
  implicit val system = ActorSystem("TraceContextDemo")
  implicit val materializer = ActorMaterializer()

  // demo message type with headers for context
  case class Message[T](headers: Map[String, String], payload: T)

  // access the global tracer
  val tracer: Tracer = GlobalTracer.get

  // trace span on the producer side
  val producerScope: Scope = tracer.buildSpan("producer").startActive( /*finishOnClose =*/ true)

  // access the active context when sending a message
  val producerContext: SpanContext = tracer.activeSpan().context()

  // inject the headers for the parent context into a text map
  val contextHeaders: JMap[String, String] = new JHashMap[String, String]()
  tracer.inject(producerContext, Format.Builtin.TEXT_MAP, new TextMapInjectAdapter(contextHeaders))

  // store the trace headers in the message
  val message = Message(contextHeaders.asScala.toMap, "some payload")

  logger.info(s"Created $message")
  // close the trace scope for the producer (finish and deactivate the trace span)
  producerScope.close()

  // custom stream stage so we can wrap the downstream `push` of the message payload with traced scope
  class Extract[T] extends GraphStage[FlowShape[Message[T], T]] {
    val in = Inlet[Message[T]]("extract.in")
    val out = Outlet[T]("extract.out")
    override val shape = FlowShape(in, out)

    override def initialAttributes: Attributes = Attributes.name("extract")

    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
      new GraphStageLogic(shape) with InHandler with OutHandler {
        override def onPush(): Unit = {
          val message = grab(in)
          // extract the context and use as parent reference for the consumer trace span
          val parentContext = tracer.extract(Format.Builtin.TEXT_MAP, new TextMapExtractAdapter(message.headers.asJava))
          val consumerScope: Scope = tracer.buildSpan("consumer").asChildOf(parentContext).startActive( /*finishOnClose =*/ true)
          logger.info(s"Headers: ${message.headers}")
          // push the message payload downstream scoped by the consumer span, which is connected to the producer span
          push(out, message.payload)
          consumerScope.close()
        }

        override def onPull(): Unit = pull(in)

        setHandlers(in, out, this)
      }
  }

  // imagine the message is actually going via some message service...
  Source.single(message)
    .via(new Extract[String])
    .map(_.toUpperCase)
    .map{msg =>
      logger.info(s"Message: $msg")
      msg
    }
    .log("msg")
    .to(Sink.ignore)
    .instrumented(name = "sample", traceable = true)
    .run()

  StdIn.readLine()
  system.terminate()
}