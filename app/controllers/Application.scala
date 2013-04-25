package controllers

import play.api._
import play.api.mvc._
import util.{AclT, ContextT}

object Application extends Controller with ContextT with AclT {
  
  def index = maybeAuthenticated { implicit pw_opt => implicit request =>
    Ok(views.html.index("Your new application is ready."))
  }
  
}