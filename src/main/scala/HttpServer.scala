import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest}
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext

class HttpServer(implicit executionContext: ExecutionContext, system: ActorSystem, materializer: Materializer) {

  private val logger = LoggerFactory.getLogger(getClass)

  val route =
    path("hello") {
      get {
        SpanInjector.injectSpanToMdc
        logger.info("Hello action")
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say hello to akka-http</h1>"))
      }
    } ~
  path("trigger") {
    get {
      onComplete {
        SpanInjector.injectSpanToMdc
        logger.info("Triggering hello action")
        Http().singleRequest(HttpRequest(uri = "http://localhost:8080/hello")).map { response =>
          response.discardEntityBytes()
          logger.info("Hello action triggered")
        }
      }(_ => complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Hello triggered</h1>")))

    }
  }
}
