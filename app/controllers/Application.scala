package controllers

import play.api._
import play.api.mvc._
import commands.HelloWorld
import util.Futures

object Application extends Controller {
  import Futures._
  import play.api.libs.concurrent.Execution.Implicits.defaultContext

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def test = Action.async {
    new HelloWorld("Bernd").future.map(Ok(_))
  }
}
