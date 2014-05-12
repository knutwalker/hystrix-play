package util

import scala.concurrent.{Promise, Future}
//import scala.util.Try
import com.netflix.hystrix.HystrixCommand
import rx.lang.scala.Observer


object Futures {
  import language.implicitConversions
  import rx.lang.scala.JavaConversions.toScalaObservable

  implicit final class HystrixCommandWithScalaFuture[T](val cmd: HystrixCommand[T]) extends AnyVal {
    def future: Future[T] = {
      val promise = Promise[T]()
      val observer = Observer[T](
        (t: T) => { promise.success(t); ()},
        (t: Throwable) => { promise.failure(t); ()}
      )

      cmd.observe().apply(observer)

      promise.future
    }
  }
}
