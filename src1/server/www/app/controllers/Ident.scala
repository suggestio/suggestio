package controllers

import javax.inject.{Inject, Singleton}

import controllers.ident._
import io.suggest.ctx.CtxData
import io.suggest.init.routed.{MJsInitTarget, MJsInitTargets}
import io.suggest.model.n2.node.MNodes
import io.suggest.sec.m.msession.Keys
import io.suggest.sec.util.ScryptUtil
import io.suggest.util.logs.MacroLogsImpl
import models._
import models.mctx.Context
import models.mproj.ICommonDi
import models.req.IReqHdr
import models.usr.{EmailActivations, EmailPwIdents, MExtIdents, MPersonIdents}
import util.acl._
import util.adn.NodesUtil
import util.captcha.CaptchaUtil
import util.ident.IdentUtil
import util.mail.IMailerWrapper
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

@Singleton
class Ident @Inject() (
                        override val mNodes               : MNodes,
                        override val mailer               : IMailerWrapper,
                        override val identUtil            : IdentUtil,
                        override val nodesUtil            : NodesUtil,
                        override val captchaUtil          : CaptchaUtil,
                        override val mPersonIdents        : MPersonIdents,
                        override val emailPwIdents        : EmailPwIdents,
                        override val emailActivations     : EmailActivations,
                        override val canConfirmEmailPwReg : CanConfirmEmailPwReg,
                        override val bruteForceProtect    : BruteForceProtect,
                        override val mExtIdents           : MExtIdents,
                        override val scryptUtil           : ScryptUtil,
                        override val maybeAuth            : MaybeAuth,
                        override val canConfirmIdpReg     : CanConfirmIdpReg,
                        override val canRecoverPw         : CanRecoverPw,
                        override val isAnon               : IsAnon,
                        override val isAuth               : IsAuth,
                        override val mCommonDi            : ICommonDi
                      )
  extends SioControllerImpl
  with MacroLogsImpl
  with EmailPwLogin
  with CaptchaValidator
  with ChangePw
  with PwRecover
  with EmailPwReg
  with ExternalLogin
{

  import mCommonDi._

  /**
   * Юзер разлогинивается. Выпилить из сессии данные о его логине.
   * @return Редирект на главную, ибо анонимусу идти больше некуда.
   */
  // TODO Добавить CSRF
  def logout = Action { implicit request =>
    Redirect(MAIN_PAGE_CALL)
      .removingFromSession(Keys.PersonId.value, Keys.Timestamp.value)
  }


  /** Отредиректить юзера куда-нибудь. */
  def rdrUserSomewhere = isAuth().async { implicit request =>
    identUtil.redirectUserSomewhere(request.user.personIdOpt.get)
  }

  /**
   * Стартовая страница my.suggest.io. Здесь лежит предложение логина/регистрации и возможно что-то ещё.
   * @param r Возврат после логина куда?
   * @return 200 Ok для анонимуса.
   *         Иначе редирект в личный кабинет.
   */
  def mySioStartPage(r: Option[String]) = csrf.AddToken {
    isAnon().async { implicit request =>
      implicit val ctxData = CtxData(
        jsInitTargets = MJsInitTargets.CaptchaForm :: MJsInitTargets.HiddenCaptcha :: Nil
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
  }


  /** Вернуть список целей инициализации js.
    * Страницы ident-контроллера нуждаются в доп.центровке колонок по вертикали. */
  override def jsiTgs(req: IReqHdr): List[MJsInitTarget] = {
    MJsInitTargets.IdentVCenterContent :: super.jsiTgs(req)
  }

}

