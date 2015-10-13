package controllers

import com.google.inject.Inject
import controllers.ident._
import io.suggest.event.SioNotifierStaticClientI
import models.jsm.init.{MTargets, MTarget}
import models.msession.Keys
import org.elasticsearch.client.Client
import play.api.cache.CacheApi
import play.api.i18n.MessagesApi
import util.acl._
import util._
import play.api.mvc._
import util.ident.IdentUtil
import util.mail.IMailerWrapper
import views.html.ident._
import models._
import views.html.ident.login.epw._loginColumnTpl
import views.html.ident.reg.email._regColumnTpl

import scala.concurrent.ExecutionContext

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.04.13 11:47
 * Description: Контроллер обычного логина в систему.
 * Обычно логинятся через email+password.
 * 2015.jan.27: вынос разжиревших кусков контроллера в util.acl.*, controllers.ident.* и рефакторинг.
 */

class Ident @Inject() (
  override val messagesApi          : MessagesApi,
  override val mailer               : IMailerWrapper,
  override val current              : play.api.Application,
  override val cache                : CacheApi,
  override val identUtil            : IdentUtil,
  override implicit val ec          : ExecutionContext,
  override implicit val esClient    : Client,
  override implicit val sn          : SioNotifierStaticClientI
)
  extends SioController
  with PlayMacroLogsImpl
  with EmailPwLogin
  with CaptchaValidator
  with ChangePw
  with PwRecover
  with EmailPwReg
  with ExternalLogin
  with IEsClient
{

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
    val rc = _regColumnTpl(EmailPwReg.emailRegFormM, captchaShown = false)(ctx)
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

