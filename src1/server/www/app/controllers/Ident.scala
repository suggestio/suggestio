package controllers

import com.google.inject.Inject
import controllers.ident._
import io.suggest.model.n2.node.MNodes
import io.suggest.util.logs.MacroLogsImpl
import models._
import models.jsm.init.{MTarget, MTargets}
import models.mctx.{Context, CtxData}
import models.mproj.ICommonDi
import models.msession.Keys
import models.req.IReqHdr
import models.usr.{EmailActivations, EmailPwIdents, MExtIdents, MPersonIdents}
import play.api.mvc._
import util.acl._
import util.adn.NodesUtil
import util.captcha.CaptchaUtil
import util.ident.IdentUtil
import util.mail.IMailerWrapper
import util.secure.ScryptUtil
import views.html.ident._
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
                        override val mNodes               : MNodes,
                        override val mailer               : IMailerWrapper,
                        override val identUtil            : IdentUtil,
                        override val nodesUtil            : NodesUtil,
                        override val captchaUtil          : CaptchaUtil,
                        override val mPersonIdents        : MPersonIdents,
                        override val emailPwIdents        : EmailPwIdents,
                        override val emailActivations     : EmailActivations,
                        override val mExtIdents           : MExtIdents,
                        override val scryptUtil           : ScryptUtil,
                        override val isAnon            : IsAnon,
                        override val mCommonDi            : ICommonDi
)
  extends SioController
  with MacroLogsImpl
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
    identUtil.redirectUserSomewhere(request.user.personIdOpt.get)
  }

  /**
   * Стартовая страница my.suggest.io. Здесь лежит предложение логина/регистрации и возможно что-то ещё.
   * @param r Возврат после логина куда?
   * @return 200 Ok для анонимуса.
   *         Иначе редирект в личный кабинет.
   */
  def mySioStartPage(r: Option[String]) = isAnon.Get.async { implicit request =>
    implicit val ctxData = CtxData(
      jsiTgs = Seq(MTargets.CaptchaForm, MTargets.HiddenCaptcha)
    )
    // TODO Затолкать это в отдельный шаблон!
    val ctx = implicitly[Context]
    val formFut = emailPwLoginFormStubM
    val title = ctx.messages("Login.page.title")
    val rc = _regColumnTpl(emailRegFormM, captchaShown = false)(ctx)
    for (lf <- formFut) yield {
      val lc = _loginColumnTpl(lf, r)(ctx)
      Ok( mySioStartTpl(title, Seq(lc, rc))(ctx) )
    }
  }


  /** Вернуть список целей инициализации js.
    * Страницы ident-контроллера нуждаются в доп.центровке колонок по вертикали. */
  override def jsiTgs(req: IReqHdr): List[MTarget] = {
    MTargets.IdentVCenterContent :: super.jsiTgs(req)
  }

}

