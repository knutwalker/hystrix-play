package util

import scala.concurrent.{Future, Promise}

import com.netflix.hystrix.HystrixExecutable


object Futures {

  private class ForPromiseObserver[T](p: Promise[T]) extends rx.Observer[T] {
    def onNext(t: T): Unit = p.trySuccess(t)
    def onError(e: Throwable): Unit = p.tryFailure(e)
    def onCompleted(): Unit = ()
  }

  implicit final class HystrixCommandWithScalaFuture[T](val cmd: HystrixExecutable[T]) extends AnyVal {
    def future: Future[T] = {
      val promise = Promise[T]()
      val observer = new ForPromiseObserver(promise)

      cmd.observe().subscribe(observer)

      promise.future
    }
  }
}
