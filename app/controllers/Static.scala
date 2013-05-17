package controllers

/**
 * Created with IntelliJ IDEA.
 * User: alex
 * Date: 16.05.13
 * Time: 13:34
 * To change this template use File | Settings | File Templates.
 */

import play.api._
import play.api.mvc._
import util.{AclT, ContextT}
import views.html.static._
import views.html.help._

object Static extends Controller with ContextT with AclT {
  def  about = maybeAuthenticated { implicit pw_opt => implicit request =>
    Ok(aboutTpl())
  }
  def  showcase = maybeAuthenticated { implicit pw_opt => implicit request =>
    Ok(showcaseTpl())

  }

  def  badbrowser = maybeAuthenticated { implicit pw_opt => implicit request =>
    Ok(badbrowserTpl())

  }

  def  help = maybeAuthenticated { implicit pw_opt => implicit request =>
    Ok(indexTpl())

  }


  def helpPage(page:String) =  maybeAuthenticated { implicit pw_opt => implicit request =>
    page match {
      case "registration"     => Ok(registrationTpl())
      case "search_settings"  => Ok(searchSettingsTpl())
      case "images_settings"  => Ok(imagesSettingsTpl())
      case "design_settings"  => Ok(designSettingsTpl())
      case "setup"            => Ok(setupTpl())
      case _                  => NotFound
    }
  }
}