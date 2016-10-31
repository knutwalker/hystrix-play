package commands

import scala.concurrent.{Future, ExecutionContext}

import akka.actor.{Actor, Props, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import java.util.concurrent.TimeUnit

import com.netflix.hystrix.HystrixCommandGroupKey
import rx.Observable

import util.Futures.HystrixFutureCommand


class HelloWorldAsync(name: String)(implicit system: ActorSystem, ec: ExecutionContext)
  extends HystrixFutureCommand[String](HelloWorldAsync.key) {

  implicit val timeout = Timeout(5000, TimeUnit.MILLISECONDS)

  override def run(): Future[String] = {
    val actor = system.actorOf(HelloWorldAsync.actor(name))
    val f = actor ? HelloWorldAsync.ReturnMessage
    f.asInstanceOf[Future[String]]
  }

  override protected def resumeWithFallback(): Observable[String] = {
    Observable.just("Fallback (Async)")
  }

}

object HelloWorldAsync {
  private final val key = HystrixCommandGroupKey.Factory.asKey("HelloWorldAsync")

  def actor(name: String) = Props(new HelloWorldActor(name))

  private class HelloWorldActor(name: String) extends Actor {

    def receive = {
      case ReturnMessage => {
        Thread.sleep((math.random * 1500).toLong)
        sender ! s"Hello, $name! (Async)"
      }
    }

  }

  case object ReturnMessage
}
