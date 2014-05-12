hystrix-play
============

Playground for using Hystrix together with the playframework


## Using Hystrix Commands with Plays asynchronous actions

There is an implicit method `future` available on any `HystrixCommand`, that will return a [Scala Future](http://www.scala-lang.org/api/current/#scala.concurrent.Future) rather then what Java calls a Future.
This is implemented by [`HystrixCommandWithScalaFuture`](app/util/Futures.scala#L13)


## Providing a metrics stream, consumable by the hystrix-dashboard

This is a replacement for the [`hystrix-metrics-event-stream`](https://github.com/Netflix/Hystrix/tree/master/hystrix-contrib/hystrix-metrics-event-stream) module.
This module is only available as a Servlet/WAR, which is not suitable for Play.

There is a [`HystrixSupport` Controller](app/controllers/HystrixSupport.scala), that does the same as the original Servlet
and allows for this play application to be monitored by the [Hystrix Dashboard](https://github.com/Netflix/Hystrix/wiki/Dashboard)
