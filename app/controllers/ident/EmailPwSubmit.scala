package controllers.ident

import controllers.SioController
import io.suggest.model.n2.node.IMNodes
import models.req.IReq
import models.MNodeTypes
import models.msession.{Keys, LongTtl, ShortTtl, Ttl}
import models.usr._
import play.api.data._
import play.api.data.Forms._
import util.acl._
import util._
import play.api.mvc._
import util.di.IIdentUtil
import util.xplay.SetLangCookieUtil
import views.html.ident.login.epw._

import scala.concurrent.Future
import util.FormUtil.passwordM

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.01.15 16:18
 * Description: Поддержка сабмита формы логина по email и паролю.
 */

trait EmailPwSubmit
  extends SioController
  with PlayMacroLogsI
  with BruteForceProtectCtl
  with SetLangCookieUtil
  with IsAnon
  with IIdentUtil
  with IMNodes
{

  import mCommonDi._

  /** Форма логина по email и паролю. */
  def emailPwLoginFormM: EmailPwLoginForm_t = {
    Form(
      mapping(
        "email"       -> email,
        "password"    -> passwordM,
        "remember_me" -> boolean.transform[Ttl](
          { rme => if (rme) LongTtl else ShortTtl },
          { _ == LongTtl }
        )
      )
      { EpwLoginFormBind.apply }
      { EpwLoginFormBind.unapply }
    )
  }

  /** Маппинг формы epw-логина с забиванием дефолтовых значений. */
  def emailPwLoginFormStubM(implicit request: RequestHeader): Future[EmailPwLoginForm_t] = {
    // Пытаемся извлечь email из сессии.
    val emailFut: Future[String] = request.session.get(Keys.PersonId.name) match {
      case Some(personId) =>
        EmailPwIdent.findByPersonId(personId)
          .map { _.headOption.fold("")(_.email) }
      case None =>
        Future successful ""
    }
    val ttl = Ttl(request.session)
    emailFut map { email1 =>
      emailPwLoginFormM fill EpwLoginFormBind(email1, "", ttl)
    }
  }


  def emailSubmitOkCall(personId: String)(implicit request: IReq[_]): Future[Call] = {
    identUtil.redirectCallUserSomewhere(personId)
  }

  def emailSubmitError(lf: EmailPwLoginForm_t, r: Option[String])(implicit request: IReq[_]): Future[Result]

  /** Самбит формы логина по email и паролю. */
  def emailPwLoginFormSubmit(r: Option[String]) = IsAnonPost.async { implicit request =>
    bruteForceProtected {
      val formBinded = emailPwLoginFormM.bindFromRequest()
      formBinded.fold(
        {formWithErrors =>
          LOGGER.debug("emailPwLoginFormSubmit(): Form bind failed:\n" + formatFormErrors(formWithErrors))
          emailSubmitError(formWithErrors, r)
        },
        {binded =>
          EmailPwIdent.getByEmail(binded.email) flatMap { epwOpt =>
            if (epwOpt.exists(_.checkPassword(binded.password))) {
              // Логин удался.
              val personId = epwOpt.get.personId
              val mpersonOptFut = mNodes.getByIdType(personId, MNodeTypes.Person)
              val rdrFut = RdrBackOrFut(r) { emailSubmitOkCall(personId) }
              var addToSession: List[(String, String)] = List(
                Keys.PersonId.name -> personId
              )
              // Реализация длинной сессии при наличии флага rememberMe.
              addToSession = binded.ttl.addToSessionAcc(addToSession)
              val rdrFut2 = rdrFut.map { rdr =>
                rdr.addingToSession(addToSession : _*)
              }
              // Выставить язык, сохраненный ранее в MPerson
              setLangCookie2(rdrFut2, mpersonOptFut)

            } else {
              val binded1 = binded.copy(password = "")
              val lf = formBinded
                .fill(binded1)
                .withGlobalError("error.unknown.email_pw")
              emailSubmitError(lf, r)
            }
          }
        }
      )
    }
  }

}


/** Экшены для Ident-контроллера. */
trait EmailPwLogin extends EmailPwSubmit {

  import mCommonDi._

  /** Рендер страницы с возможностью логина по email и паролю. */
  def emailPwLoginForm(r: Option[String]) = IsAnonGet.async { implicit request =>
    for (lf <- emailPwLoginFormStubM) yield {
      epwLoginPage(lf, r)
    }
  }

  /** Общий код методов emailPwLoginForm() и emailSubmitError(). */
  protected def epwLoginPage(lf: EmailPwLoginForm_t, r: Option[String])
                            (implicit request: IReq[_]): Result = {
    Ok( loginTpl(lf, r) )
  }

  override def emailSubmitError(lf: EmailPwLoginForm_t, r: Option[String])
                               (implicit request: IReq[_]): Future[Result] = {
    epwLoginPage(lf, r)
  }

}
