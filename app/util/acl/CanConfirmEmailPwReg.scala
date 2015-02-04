package util.acl

import models.usr.{IEaEmailId, EmailActivation}
import play.api.mvc.{Result, Request, ActionBuilder}
import util.PlayMacroLogsDyn
import util.acl.PersonWrapper.PwOpt_t
import util.ident.IdentUtil
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client

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


trait CanConfirmEmailPwRegBase extends ActionBuilder[EmailPwRegConfirmRequest] with PlayMacroLogsDyn {

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
            IsAuth.onUnauth(request)
          case Some(pw) =>
            IdentUtil.redirectUserSomewhere(pw.personId)
        }
        rdrFut map { rdr =>
          rdr.flashing("error" -> "Активация невозможна.")    // TODO Задействовать тут Messages(), а не хардкод строки.
        }

      // [xakep] Внезапно, кто-то пытается пропихнуть левую активацию из какого-то другого места.
      case Some(ea) =>
        LOGGER.warn(s"Client ip[${request.remoteAddress}] User[$pwOpt] tried to use foreign activation key:\n  eaInfo = $eaInfo\n  ea = $ea")
        IsAuth.onUnauth(request)
    }
  }
}

case class EmailPwRegConfirmRequest[A](
  ea        : EmailActivation,
  pwOpt     : PwOpt_t,
  sioReqMd  : SioReqMd,
  request   : Request[A]
) extends AbstractRequestWithPwOpt(request)


/** Дефолтовая реализация [[CanConfirmEmailPwRegBase]].
  * ExpireSession тут нет, т.к. сюда попадают незалогиненные. */
case class CanConfirmEmailPwReg(eaInfo: IEaEmailId)
  extends CanConfirmEmailPwRegBase


/** Реализация [[CanConfirmEmailPwRegBase]] c выставлением CSRF-token'а. */
case class CanConfirmEmailPwRegGet(eaInfo: IEaEmailId)
  extends CanConfirmEmailPwRegBase
  with CsrfGet[AbstractRequestWithPwOpt]

/** Реализация [[CanConfirmEmailPwRegBase]] с проверкой выставленного ранее CSRF-токена. */
case class CanConfirmEmailPwRegPost(eaInfo: IEaEmailId)
  extends CanConfirmEmailPwRegBase
  with CsrfPost[AbstractRequestWithPwOpt]

