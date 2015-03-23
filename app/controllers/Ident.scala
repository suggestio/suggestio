package controllers

import controllers.ident._
import play.api.i18n.Messages
import util.acl._
import util._
import play.api.mvc._
import util.ident.IdentUtil
import views.html.ident._
import play.api.libs.concurrent.Execution.Implicits._
import models._
import views.html.ident.login.epw._loginColumnTpl
import views.html.ident.reg.email._regColumnTpl

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.04.13 11:47
 * Description: Контроллер обычного логина в систему.
 * Обычно логинятся через email+password.
 * 2015.jan.27: вынос разжиревших кусков контроллера в util.acl.*, controllers.ident.* и рефакторинг.
 */

object Ident extends SioController with PlayMacroLogsImpl with EmailPwLogin with CaptchaValidator
with ChangePw with PwRecover with EmailPwReg with ExternalLogin {

  import LOGGER._

  /**
   * Юзер разлогинивается. Выпилить из сессии данные о его логине.
   * @return Редирект на главную, ибо анонимусу идти больше некуда.
   */
  def logout = Action { implicit request =>
    Redirect(MAIN_PAGE_CALL)
      .withNewSession
  }



  /** Отредиректить юзера куда-нибудь. */
  def rdrUserSomewhere = IsAuth.async { implicit request =>
    IdentUtil.redirectUserSomewhere(request.pwOpt.get.personId)
  }

  /**
   * Стартовая страница my.suggest.io. Здесь лежит предложение логина/регистрации и возможно что-то ещё.
   * @param r Возврат после логина куда?
   * @return 200 Ok для анонимуса.
   *         Иначе редирект в личный кабинет.
   */
  def mySioStartPage(r: Option[String]) = IsAnonGet.async { implicit request =>
    val formFut = EmailPwSubmit.emailPwLoginFormStubM
    val ctx = implicitly[Context]
    val title = Messages("Login.page.title")(ctx.lang)
    val rc = _regColumnTpl(EmailPwReg.emailRegFormM, captchaShown = true)(ctx)
    formFut.map { lf =>
      val lc = _loginColumnTpl(lf, r)(ctx)
      Ok( mySioStartTpl(title, Seq(lc, rc))(ctx) )
    }
  }

}

