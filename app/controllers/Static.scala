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

 object Static extends Controller with ContextT with AclT {
     def  aboutTpl = maybeAuthenticated { implicit pw_opt => implicit request =>
    Ok(views.html.static.aboutTpl())
    }
    def  showcase = maybeAuthenticated { implicit pw_opt => implicit request =>
    Ok(views.html.static.showcaseTpl())

    }

 }