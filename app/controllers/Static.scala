package controllers

/**
 * User: Alexander Pestrikov <alexander.pestrikov@cbca.ru>
 * Date: 16.05.13
 * Time: 13:34
 * Статика всякая.
 */

import play.api.mvc._
import util.ContextT
import util.acl._
import views.html.static._
import views.html.help._
import Application.http404AdHoc

object Static extends Controller with ContextT {

  def about = MaybeAuth { implicit request =>
    Ok(aboutTpl())
  }


  def showcase = MaybeAuth { implicit request =>
    Ok(showcaseTpl())
  }


  def badbrowser = MaybeAuth { implicit request =>
    Ok(badbrowserTpl())

  }


  def help = MaybeAuth { implicit request =>
    Ok(indexTpl())
  }


  def helpPage(page:String) = MaybeAuth { implicit request =>
    page match {
      case "registration"     => Ok(registrationTpl())
      case "search_settings"  => Ok(searchSettingsTpl())
      case "images_settings"  => Ok(imagesSettingsTpl())
      case "design_settings"  => Ok(designSettingsTpl())
      case "setup"            => Ok(setupTpl())
      case _                  => http404AdHoc
    }
  }

  /**
   * Костыль в связи с проблемами в play-html-compressor в play-2.3 https://github.com/mohiva/play-html-compressor/issues/20
   * Без этого костыля, запрос html'ки просто подвисает.
   */
  def tinymceColorpicker = Action { implicit request =>
    Ok(tinymce.colorpicker.indexTpl())
      .withHeaders(CACHE_CONTROL -> "public, max-age=3600")
  }

}