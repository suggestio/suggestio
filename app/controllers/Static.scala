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
     def  about = maybeAuthenticated { implicit pw_opt => implicit request =>
    Ok(views.html.static.aboutTpl())
    }
    def  showcase = maybeAuthenticated { implicit pw_opt => implicit request =>
    Ok(views.html.static.showcaseTpl())

    }

   def  badbrowser = maybeAuthenticated { implicit pw_opt => implicit request =>
     Ok(views.html.static.badbrowserTpl())

   }

   def  help = maybeAuthenticated { implicit pw_opt => implicit request =>
     Ok(views.html.help.indexTpl())

   }

   def  blog = maybeAuthenticated { implicit pw_opt => implicit request =>
     Ok(views.html.blog.indexTpl())

   }

   def helpPage(page:String) =  maybeAuthenticated { implicit pw_opt => implicit request =>
      page match {
           case "registration" => Ok(views.html.help.registrationTpl())
           case "search_settings" => Ok(views.html.help.searchSettingsTpl())
           case "images_settings" => Ok (views.html.help.imagesSettingsTpl())
           case "design_settings" => Ok(views.html.help.designSettingsTpl())
           case "setup" => Ok(views.html.help.setupTpl())
           case _ => NotFound
          }
    }
 }