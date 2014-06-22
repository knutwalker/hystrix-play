package commands

import scala.concurrent.duration._

import akka.actor.{Actor, Props}

import com.netflix.hystrix.{HystrixCommandGroupKey, HystrixObservableCommand}
import rx.Observable
import rx.lang.scala.Observer
import rx.lang.scala.subjects.ReplaySubject


class HelloWorldAsync(name: String) extends HystrixObservableCommand[String](HelloWorldAsync.key) {
  import play.api.Play.current
  import play.api.libs.concurrent.Akka

  def run(): Observable[String] = {
    val channel = ReplaySubject[String]()
    Akka.system.actorOf(HelloWorldAsync.actor(channel, name))
    channel.asJavaSubject
  }
}
object HelloWorldAsync {
  private final val key = HystrixCommandGroupKey.Factory.asKey("HelloWorldAsync")

  def actor(subj: Observer[String], name: String) = Props(new HelloWorldActor(subj, name))

  private class HelloWorldActor(subj: Observer[String], name: String) extends Actor {
    import context.dispatcher

    override def preStart(): Unit = {
      context.system.scheduler.scheduleOnce(500.millis, self, ReturnMessage)
      super.preStart()
    }

    def receive = {
      case ReturnMessage =>
        subj.onNext(s"Hello, $name! (Async)")
        subj.onCompleted()
        context stop self
    }
  }

  case object ReturnMessage
}
