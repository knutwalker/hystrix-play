package util

import scala.concurrent.{Future, Promise, ExecutionContext}
import scala.util.{Failure, Success}

import com.netflix.hystrix.{HystrixCommandGroupKey, HystrixObservable, HystrixObservableCommand}
import rx.Observable
import rx.lang.scala.subjects.ReplaySubject

object Futures {

  private class ForPromiseObserver[T](p: Promise[T]) extends rx.Observer[T] {
    def onNext(t: T): Unit = p.trySuccess(t)
    def onError(e: Throwable): Unit = p.tryFailure(e)
    def onCompleted(): Unit = ()
  }

  implicit final class HystrixCommandWithScalaFuture[T](val cmd: HystrixObservable[T]) extends AnyVal {
    def future: Future[T] = {
      val promise = Promise[T]()
      val observer = new ForPromiseObserver(promise)

      cmd.observe().subscribe(observer)

      promise.future
    }
  }

  abstract class HystrixFutureCommand[T](groupKey: HystrixCommandGroupKey)(implicit ec: ExecutionContext)
    extends HystrixObservableCommand[T](groupKey) {

    override def construct(): Observable[T] = {
      val channel = ReplaySubject[T]()

      run().onComplete {
        case Success(v) => {
          channel.onNext(v)
          channel.onCompleted()
        }
        case Failure(t) => {
          channel.onError(t)
          channel.onCompleted()
        }
      }

      channel.asJavaSubject
    }

    def run(): Future[T]

  }
}
