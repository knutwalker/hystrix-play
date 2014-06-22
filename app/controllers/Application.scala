package controllers

import play.api.mvc._
import commands.{HelloWorld, HelloWorldAsync}
import util.Futures

object Application extends Controller {
  import play.api.libs.concurrent.Execution.Implicits.defaultContext

import util.Futures._

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def test = Action.async {
    new HelloWorld("Bernd").future.map(Ok(_))
  }

  def async = Action.async {
    new HelloWorldAsync("Bernd").future.map(Ok(_))
  }
}
