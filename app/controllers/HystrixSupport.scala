package controllers

import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import com.netflix.config.scala.DynamicIntProperty
import com.netflix.hystrix.metric.consumer.HystrixDashboardStream
import com.netflix.hystrix.metric.consumer.HystrixDashboardStream.DashboardData
import com.netflix.hystrix.serial.SerialHystrixDashboardData
import play.api.Logger
import play.api.libs.EventSource
import play.api.mvc._
import rx.functions.Func1
import rx.{Observable, RxReactiveStreams}

import scala.collection.JavaConverters._
import scala.concurrent.duration._
class HystrixSupport @Inject()(system: ActorSystem) extends Controller {
  import play.api.libs.concurrent.Execution.Implicits._
  def stream(delayOpt: Option[Int]) = Action {

    val numberConnections = concurrentConnections.incrementAndGet()
    val maxConnections = maxConcurrentConnections.get

    Some(numberConnections).
      filter(_ <= maxConnections).
      map(_ => delayOpt.getOrElse(500)).
      fold(unavailable(maxConnections)) { delay =>
        val y = HystrixDashboardStream.getInstance().observe().concatMap(new Func1[DashboardData, Observable[String]]() {
           def call(dashboardData:DashboardData) = {
             Logger.info("Getting data")
             Observable.from(SerialHystrixDashboardData.toMultipleJsonStrings(dashboardData).asScala.map(x => x + "\n\n").asJava);
          }
        })

        val publisher = RxReactiveStreams.toPublisher(y);
        val source = Source.fromPublisher(publisher).via(EventSource.flow).watchTermination() { (m, f) =>
          f.onComplete{_ => Logger.info("Releasing connection"); concurrentConnections.decrementAndGet()}
          m
        }.delay(delay.milli)

        Ok.chunked(source ).withHeaders(
          "Content-Type" -> "text/event-stream;charset=UTF-8",
          "Cache-Control" -> "no-cache, no-store, max-age=0, must-revalidate",
          "Pragma" -> "no-cache"
        )
      }
  }

  private[this] def unavailable(max: Int) = {
    concurrentConnections.decrementAndGet()
    ServiceUnavailable(s"MaxConcurrentConnections reached: $maxConcurrentConnections")
  }

  private[this] final val concurrentConnections: AtomicInteger = new AtomicInteger(0)
  private[this] final val maxConcurrentConnections: DynamicIntProperty =
    new DynamicIntProperty("hystrix.stream.maxConcurrentConnections", 5)


}
