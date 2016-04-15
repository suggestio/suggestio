package util.acl

import controllers.SioController
import models.req.MEmailActivationReq
import models.usr.{EmailActivation, IEaEmailId}
import play.api.mvc.{ActionBuilder, Request, Result}
import util.PlayMacroLogsDyn
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


import util.acl.CanConfirmEmailPwReg._


trait CanConfirmEmailPwRegCtl
  extends SioController
  with IIdentUtil
  with OnUnauthUtilCtl
  with Csrf
{

  import mCommonDi._

  /** Код проверки возможности подтверждения регистрации по email. */
  trait CanConfirmEmailPwRegBase
    extends ActionBuilder[MEmailActivationReq]
    with PlayMacroLogsDyn
    with OnUnauthUtil
  {

    /** Инфа по активации, присланная через URL qs. */
    def eaInfo: IEaEmailId

    override def invokeBlock[A](request: Request[A], block: (MEmailActivationReq[A]) => Future[Result]): Future[Result] = {
      val eaFut = EmailActivation.maybeGetById(eaInfo.id)

      val personIdOpt = sessionUtil.getPersonId(request)
      val user = mSioUsers(personIdOpt)

      eaFut flatMap {
        // Всё срослось.
        case Some(ea) if ea.email == eaInfo.email && ea.key == EPW_ACT_KEY =>
          val req1 = MEmailActivationReq(ea, request, user)
          block(req1)

        // Активация не подходит. Может юзер повторно проходит по ссылке из письма? Но это не важно, по сути.
        case None =>
          LOGGER.debug(s"Client requested activation for $eaInfo , but it doesn't exists. Redirecting...")
          val rdrFut = personIdOpt.fold {
            onUnauth(request)
          } { personId =>
            identUtil.redirectUserSomewhere(personId)
          }
          // TODO Отправлять на страницу, где описание проблема, а не туда, куда взбредёт.
          rdrFut map { rdr =>
            rdr.flashing(FLASH.ERROR -> "Activation.impossible")
          }

        // [xakep] Внезапно, кто-то пытается пропихнуть левую активацию из какого-то другого места.
        case Some(ea) =>
          LOGGER.warn(s"Client ip[${request.remoteAddress}] User[$personIdOpt] tried to use foreign activation key:\n  eaInfo = $eaInfo\n  ea = $ea")
          onUnauth(request)
      }
    }
  }


  /** Реализация [[CanConfirmEmailPwRegBase]] c выставлением CSRF-token'а. */
  case class CanConfirmEmailPwRegGet(override val eaInfo: IEaEmailId)
    extends CanConfirmEmailPwRegBase
    with CsrfGet[MEmailActivationReq]

  /** Реализация [[CanConfirmEmailPwRegBase]] с проверкой выставленного ранее CSRF-токена. */
  case class CanConfirmEmailPwRegPost(override val eaInfo: IEaEmailId)
    extends CanConfirmEmailPwRegBase
    with CsrfPost[MEmailActivationReq]

}
