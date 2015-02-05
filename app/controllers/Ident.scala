package controllers

import controllers.ident._
import util.acl._
import util._
import play.api.mvc._
import util.ident.IdentUtil
import views.html.ident._
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.Future
import models._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.04.13 11:47
 * Description: Контроллер обычного логина в систему.
 * Обычно логинятся через email+password.
 * 2015.jan.27: вынос разжиревших кусков контроллера в util.acl.*, controllers.ident.* и рефакторинг.
 */

object Ident extends SioController with PlayMacroLogsImpl with EmailPwSubmit with CaptchaValidator
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


  /** Рендер страницы с возможностью логина по email и паролю. */
  def emailPwLoginForm(r: Option[String]) = IsAnonGet { implicit request =>
    Ok(emailPwLoginFormTpl(EmailPwSubmit.emailPwLoginFormM, r))
  }


  override def emailSubmitError(lf: EmailPwLoginForm_t, r: Option[String])
                               (implicit request: AbstractRequestWithPwOpt[_]): Future[Result] = {
    Forbidden(emailPwLoginFormTpl(lf, r))
  }

  /** Отредиректить юзера куда-нибудь. */
  def rdrUserSomewhere = IsAuth.async { implicit request =>
    IdentUtil.redirectUserSomewhere(request.pwOpt.get.personId)
  }

  /**
   * Стартовая страница my.suggest.io. Здесь лежит предложение логина/регистрации и возможно что-то ещё.
   * @return 200 Ok для анонимуса.
   *         Иначе редирект в личный кабинет.
   */
  def mySioStartPage = IsAnonGet { implicit request =>
    val ctx = implicitly[Context]
    val lc = _loginColumnTpl( EmailPwSubmit.emailPwLoginFormM )(ctx)
    val rc = _regColumnTpl( EmailPwReg.emailRegFormM )(ctx)
    Ok( mySioStartTpl( Seq(lc, rc) )(ctx) )
  }

}

