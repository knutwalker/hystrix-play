package controllers

import play.api.mvc._
import java.util.concurrent.atomic.{AtomicReference, AtomicInteger}
import com.netflix.config.scala.DynamicIntProperty
import com.netflix.hystrix.contrib.metrics.eventstream.HystrixMetricsPoller
import play.api.Logger
import play.api.libs.concurrent.Akka
import play.api.libs.iteratee.Enumerator
import java.util.concurrent.TimeUnit
import scala.annotation.tailrec
import scala.concurrent.duration.FiniteDuration
import akka.actor.ActorSystem
import java.io.OutputStream
import scala.util.Try

object HystrixSupport extends Controller {
  import play.api.Play.current
  import play.api.libs.concurrent.Execution.Implicits.defaultContext

  def stream(delayOpt: Option[Int]) = Action {

    val numberConnections = concurrentConnections.incrementAndGet()
    val maxConnections = maxConcurrentConnections.get()

    Some(numberConnections).
      filter(_ <= maxConnections).
      map(_ => delayOpt.getOrElse(500)).
      fold(unavailable(maxConnections)) { delay =>

        Ok.chunked(streamRequest(delay)).withHeaders(
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

  private[this] def streamRequest(delay: Int): Enumerator[Array[Byte]] = {
    val listener = new MetricJsonListener(1000)
    val poller = new HystrixMetricsPoller(listener, delay)
    poller.start()
    Logger.info("Starting poller")

    val delayDuration = FiniteDuration(delay, TimeUnit.MILLISECONDS)
    val system = Akka.system

    val streamer = (out: OutputStream) => produceStream(poller, listener, delayDuration, system, out)
    val closer = () => {
      Logger.info("Closing poller")
      poller.shutdown()
      concurrentConnections.decrementAndGet()
    }

    val enum = Enumerator.outputStream(streamer)
    enum.onDoneEnumerating(closer())
  }

  private[this] def produceStream(poller: HystrixMetricsPoller, listener: MetricJsonListener, delay: FiniteDuration, system: ActorSystem, out: OutputStream): Unit = {
    val strings = produce(poller, listener)
    if (strings.isEmpty) {
      out.flush()
      out.close()
    }
    else {
      strings.foreach(s => out.write(s"$s\n\n".getBytes("UTF-8")))
      out.flush()
      Try(system.scheduler.scheduleOnce(delay)(produceStream(poller, listener, delay, system, out)))
    }
  }

  private[this] def produce(poller: HystrixMetricsPoller, listener: MetricJsonListener): Vector[String] = {
    if (!poller.isRunning) Vector()
    else {
      val jsonMessages = listener.getJsonMetrics

      if (jsonMessages.isEmpty) Vector("ping: ")
      else jsonMessages.map(j => s"data: $j")
    }
  }

  private class MetricJsonListener(capacity: Int) extends HystrixMetricsPoller.MetricsAsJsonPollerListener {

    private[this] final val metrics = new AtomicReference[Vector[String]](Vector())

    @tailrec
    private[this] final def set(oldValue: Vector[String], newValue: Vector[String]): Boolean = {
      metrics.compareAndSet(oldValue, newValue) || set(oldValue, newValue)
    }

    private[this] final def getAndSet(newValue: Vector[String]): Vector[String] = {
      val oldValue = metrics.get
      set(oldValue, newValue)
      oldValue
    }

    def handleJsonMetric(json: String): Unit = {
      val oldMetrics = metrics.get()
      if (oldMetrics.size >= capacity) throw new IllegalStateException("Queue full")

      val newMetrics = oldMetrics :+ json
      set(oldMetrics, newMetrics)
    }

    def getJsonMetrics: Vector[String] = getAndSet(Vector())
  }
}
