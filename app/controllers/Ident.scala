package controllers

import com.google.inject.Inject
import controllers.ident._
import models.jsm.init.{MTargets, MTarget}
import models.mproj.MCommonDi
import models.msession.Keys
import util.acl._
import util._
import play.api.mvc._
import util.adn.NodesUtil
import util.captcha.CaptchaUtil
import util.ident.IdentUtil
import util.mail.IMailerWrapper
import views.html.ident._
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

class Ident @Inject() (
  override val mailer               : IMailerWrapper,
  override val identUtil            : IdentUtil,
  override val nodesUtil            : NodesUtil,
  override val captchaUtil          : CaptchaUtil,
  override val mCommonDi            : MCommonDi
)
  extends SioController
  with PlayMacroLogsImpl
  with EmailPwLogin
  with CaptchaValidator
  with ChangePw
  with PwRecover
  with EmailPwReg
  with ExternalLogin
  with IsAuth
{

  import mCommonDi._

  /**
   * Юзер разлогинивается. Выпилить из сессии данные о его логине.
   * @return Редирект на главную, ибо анонимусу идти больше некуда.
   */
  // TODO Добавить CSRF
  def logout = Action { implicit request =>
    Redirect(MAIN_PAGE_CALL)
      .removingFromSession(Keys.PersonId.name, Keys.Timestamp.name)
  }


  /** Отредиректить юзера куда-нибудь. */
  def rdrUserSomewhere = IsAuth.async { implicit request =>
    identUtil.redirectUserSomewhere(request.pwOpt.get.personId)
  }

  /**
   * Стартовая страница my.suggest.io. Здесь лежит предложение логина/регистрации и возможно что-то ещё.
   * @param r Возврат после логина куда?
   * @return 200 Ok для анонимуса.
   *         Иначе редирект в личный кабинет.
   */
  def mySioStartPage(r: Option[String]) = IsAnonGet.async { implicit request =>
    // TODO Затолкать это в отдельный шаблон!
    implicit val jsInitTgs = Seq(MTargets.CaptchaForm, MTargets.HiddenCaptcha)
    val ctx = implicitly[Context]
    val formFut = emailPwLoginFormStubM
    val title = ctx.messages("Login.page.title")
    val rc = _regColumnTpl(emailRegFormM, captchaShown = false)(ctx)
    formFut.map { lf =>
      val lc = _loginColumnTpl(lf, r)(ctx)
      Ok( mySioStartTpl(title, Seq(lc, rc))(ctx) )
    }
  }

  /** Страницы ident-контроллера нуждаются в доп.центровке колонок по вертикали. */
  override protected def _jsInitTargets0: List[MTarget] = {
    MTargets.IdentVCenterContent :: super._jsInitTargets0
  }

}

