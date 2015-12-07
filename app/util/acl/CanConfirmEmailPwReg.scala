package util.acl

import controllers.SioController
import models.req.SioReqMd
import models.usr.{IEaEmailId, EmailActivation}
import play.api.mvc.{Result, Request, ActionBuilder}
import util.PlayMacroLogsDyn
import util.acl.PersonWrapper.PwOpt_t
import util.di.IIdentUtil

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.01.15 19:34
 * Description: ActionBuilder для доступа к экшенам активации email.
 */
object CanConfirmEmailPwReg {

  /** Значение key для искомого EmailActivation. */
  def EPW_ACT_KEY = "epwAct"

}


import CanConfirmEmailPwReg._


trait CanConfirmEmailPwRegCtl
  extends SioController
  with IIdentUtil
  with OnUnauthUtilCtl
  with Csrf
{

  import mCommonDi._

  /** Код проверки возможности подтверждения регистрации по email. */
  trait CanConfirmEmailPwRegBase
    extends ActionBuilder[EmailPwRegConfirmRequest]
    with PlayMacroLogsDyn
    with OnUnauthUtil
  {

    /** Инфа по активации, присланная через URL qs. */
    def eaInfo: IEaEmailId

    override def invokeBlock[A](request: Request[A], block: (EmailPwRegConfirmRequest[A]) => Future[Result]): Future[Result] = {
      val eaFut = eaInfo.id match {
        case Some(eaId) =>
          EmailActivation.getById(eaId)
        case None =>
          Future successful None
      }
      val pwOpt = PersonWrapper.getFromRequest(request)
      val srmFut = SioReqMd.fromPwOpt(pwOpt)
      eaFut flatMap {
        // Всё срослось.
        case Some(ea) if ea.email == eaInfo.email && ea.key == EPW_ACT_KEY =>
          srmFut flatMap { srm =>
            val req1 = EmailPwRegConfirmRequest(ea, pwOpt, srm, request)
            block(req1)
          }

        // Активация не подходит. Может юзер повторно проходит по ссылке из письма? Но это не важно, по сути.
        case None =>
          LOGGER.debug(s"Client requested activation for $eaInfo , but it doesn't exists. Redirecting...")
          val rdrFut = pwOpt match {
            case None =>
              onUnauth(request)
            case Some(pw) =>
              identUtil.redirectUserSomewhere(pw.personId)
          }
          // TODO Отправлять на страницу, где описание проблема, а не туда, куда взбредёт.
          rdrFut map { rdr =>
            rdr.flashing(FLASH.ERROR -> "Activation.impossible")
          }

        // [xakep] Внезапно, кто-то пытается пропихнуть левую активацию из какого-то другого места.
        case Some(ea) =>
          LOGGER.warn(s"Client ip[${request.remoteAddress}] User[$pwOpt] tried to use foreign activation key:\n  eaInfo = $eaInfo\n  ea = $ea")
          onUnauth(request)
      }
    }
  }


  /** Реализация [[CanConfirmEmailPwRegBase]] c выставлением CSRF-token'а. */
  case class CanConfirmEmailPwRegGet(eaInfo: IEaEmailId)
    extends CanConfirmEmailPwRegBase
    with CsrfGet[EmailPwRegConfirmRequest]

  /** Реализация [[CanConfirmEmailPwRegBase]] с проверкой выставленного ранее CSRF-токена. */
  case class CanConfirmEmailPwRegPost(eaInfo: IEaEmailId)
    extends CanConfirmEmailPwRegBase
    with CsrfPost[EmailPwRegConfirmRequest]

}


case class EmailPwRegConfirmRequest[A](
  ea        : EmailActivation,
  pwOpt     : PwOpt_t,
  sioReqMd  : SioReqMd,
  request   : Request[A]
)
  extends AbstractRequestWithPwOpt(request)
