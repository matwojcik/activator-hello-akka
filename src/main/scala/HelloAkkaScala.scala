import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.io.StdIn

object HelloAkkaScala extends App {
  private val logger = LoggerFactory.getLogger(getClass)

  // Create the 'helloakka' actor system
  implicit val system: ActorSystem = ActorSystem("helloakka")
  implicit val executionContext: ExecutionContext = system.dispatcher
  implicit val materializer: ActorMaterializer = ActorMaterializer()


  val bindingFuture = Http().bindAndHandle(new HttpServer().route, "localhost", 8080)

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")

  import com.lightbend.cinnamon.akka.stream.CinnamonAttributes._


  Source.fromIterator(() => (1 to 5).iterator)
    .throttle(1, 1.second)
    .map { i =>
      logger.info(s"Element: $i")
      i
    }
    .map { i =>
      logger.info(s"Another log for Element: $i")
      i
    }.mapAsync(4) { i =>
      Future {
        logger.info("Calling trigger")
      } flatMap { _ =>
        Http().singleRequest(HttpRequest(uri = "http://localhost:8080/trigger")).map { response =>
          response.discardEntityBytes()
          logger.info(s"Http call finished for element $i")
        }
      }
    }
    .to(Sink.ignore)
    // first element of stream is traced without it
//    .instrumented(name = "my-stream", traceable = true)
    .run()

  StdIn.readLine() // let it run until user presses return

}
