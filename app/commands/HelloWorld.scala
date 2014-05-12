package commands

import com.netflix.hystrix.{HystrixCommandGroupKey, HystrixCommand}

class HelloWorld(name: String) extends HystrixCommand[String](HelloWorld.key) {
  def run(): String = {
    if (math.random > 0.6) throw new RuntimeException("haha")
    Thread.sleep((math.random * 1500).toLong)
    s"Hello, $name!"
  }

  override def getFallback: String =
    new HelloWorld2().execute()
}

class HelloWorld2 extends HystrixCommand[String](HelloWorld.key) {
  def run(): String = {
    if (math.random > 0.8) throw new RuntimeException("wow, we're pretty fucked up")
    "Booya! Fallback here!"
  }

  override def getFallback: String = new HelloWorld3().execute()
}

class HelloWorld3 extends HystrixCommand[String](HelloWorld.key) {
  def run(): String = "Always working"
}

object HelloWorld {
  private[commands] final val key = HystrixCommandGroupKey.Factory.asKey("ExampleGroup")
}
