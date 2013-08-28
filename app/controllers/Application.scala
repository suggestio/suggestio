package controllers

import play.api.mvc._
import util.{AclT, ContextT}
import views.html.crawl.indexTpl
import play.api.data._
import play.api.data.Forms._
import java.net.URL
import play.api.i18n.Lang
import play.api.Logger
import gnu.inet.encoding.IDNA
import io.suggest.util.UrlUtil
import play.api.Play.current

object Application extends Controller with ContextT with AclT {

  val addSiteFormM = Form(
    "url" -> nonEmptyText(minLength = 7, maxLength = 40)
      .verifying("Invalid URL", {result => result match {
        case url =>
          try {
            new URL(url)
            true
          } catch {
            case ex:Exception => false
          }
      }})
  )
 
  /** 
   * Выдача главной страницы
   */
  def index = maybeAuthenticated { implicit pw_opt => implicit request =>
    Ok(indexTpl())
  }
  
  def change_locale(locale:String) = maybeAuthenticated { implicit pw_opt => implicit request =>
    
    val referrer = request.headers.get(REFERER).getOrElse("/")
    
    Logger.logger.debug("Change user lang to : " + locale)
    Redirect(referrer).withLang(Lang(locale)) // TODO Check if the lang is handled by the application
  
  }
  
  /**
   * Добавление сайта, шаг 1. Нужно подготовить qi и сгенерить данные для генерации js-кода.
   * @return Переменные для генерации js-кода на клиенте.
   */
  def siteAddGenerateJs = maybeAuthenticated { implicit pw_opt => implicit request =>
    val bindedForm = addSiteFormM.bindFromRequest
    bindedForm.fold(
      formWithErrors =>
        NotAcceptable("cannot parse POST")
      ,
      {url =>
        val u = new URL(url)
        val host = u.getHost
        val hostIDN = IDNA.toASCII(host)
        if (UrlUtil.isHostnameValid(hostIDN)) {
          // Хостнейм валиден


        } else {
          // Хостнейм невалиден.
          "hostname_prohibited"
        }
        ???
      }
    )
  }
  
}
