package util.acl

import com.google.inject.Inject
import io.suggest.util.logs.MacroLogsDyn
import models.mproj.ICommonDi
import models.req.{IReq, MEmailActivationReq, MReq}
import models.usr.{EmailActivations, IEaEmailId}
import play.api.mvc.{ActionBuilder, Request, Result}
import util.ident.IdentUtil

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


class CanConfirmEmailPwReg @Inject()(
                                      identUtil               : IdentUtil,
                                      emailActivations        : EmailActivations,
                                      override val mCommonDi  : ICommonDi
                                    )
  extends Csrf
{

  import mCommonDi._

  /** Код проверки возможности подтверждения регистрации по email. */
  sealed abstract class Base
    extends ActionBuilder[MEmailActivationReq]
    with MacroLogsDyn
    with OnUnauthUtil
  {

    /** Инфа по активации, присланная через URL qs. */
    def eaInfo: IEaEmailId

    /** Что делать, когда нет искомой email activation. */
    def eaNotFoundF: IReq[_] => Future[Result]

    override def invokeBlock[A](request: Request[A], block: (MEmailActivationReq[A]) => Future[Result]): Future[Result] = {
      val eaFut = emailActivations.maybeGetById(eaInfo.id)

      val personIdOpt = sessionUtil.getPersonId(request)
      val user = mSioUsers(personIdOpt)

      eaFut.flatMap {
        // Всё срослось.
        case Some(ea) if ea.email == eaInfo.email && ea.key == EPW_ACT_KEY =>
          val req1 = MEmailActivationReq(ea, request, user)
          block(req1)

        // Активация не подходит. Может юзер повторно проходит по ссылке из письма? Но это не важно, по сути.
        case None =>
          LOGGER.debug(s"Client requested activation for $eaInfo , but it doesn't exists. Redirecting...")
          eaNotFoundF( MReq(request, user) )

        // [xakep] Внезапно, кто-то пытается пропихнуть левую активацию из какого-то другого места.
        case Some(ea) =>
          LOGGER.warn(s"Client ip[${request.remoteAddress}] User[$personIdOpt] tried to use foreign activation key:\n  eaInfo = $eaInfo\n  ea = $ea")
          onUnauth(request)
      }
    }
  }


  /** Реализация c выставлением CSRF-token'а. */
  case class Get(override val eaInfo: IEaEmailId)
                (override val eaNotFoundF: IReq[_] => Future[Result])
    extends Base
    with CsrfGet[MEmailActivationReq]

  /** Реализация с проверкой выставленного ранее CSRF-токена. */
  case class Post(override val eaInfo: IEaEmailId)
                 (override val eaNotFoundF: IReq[_] => Future[Result])
    extends Base
    with CsrfPost[MEmailActivationReq]

}
