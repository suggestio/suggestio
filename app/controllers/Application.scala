package controllers

import play.api._
import play.api.mvc._
import util.ContextT

object Application extends Controller with ContextT {
  
  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }
  
}