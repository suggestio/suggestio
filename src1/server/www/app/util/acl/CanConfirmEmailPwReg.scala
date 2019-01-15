package util.acl

import io.suggest.es.model.EsModel
import javax.inject.Inject
import io.suggest.req.ReqUtil
import io.suggest.util.logs.MacroLogsImpl
import models.mproj.ICommonDi
import models.req.{IReq, MEmailActivationReq, MReq}
import models.usr.{EmailActivations, IEaEmailId}
import play.api.mvc.{ActionBuilder, AnyContent, Request, Result}
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
                                      aclUtil                 : AclUtil,
                                      identUtil               : IdentUtil,
                                      emailActivations        : EmailActivations,
                                      isAuth                  : IsAuth,
                                      reqUtil                 : ReqUtil,
                                      esModel                 : EsModel,
                                      mCommonDi               : ICommonDi
                                    )
  extends MacroLogsImpl
{

  import mCommonDi._
  import esModel.api._

  /** Сборка ActionBuilder'а, проверяющего права доступа на подтверждение реги по email.
    *
    * @param eaInfo Инфа по активации, присланная через URL qs.
    * @param eaNotFoundF Что делать, когда нет искомой email activation.
    * @return
    */
  def apply(eaInfo: IEaEmailId)(eaNotFoundF: IReq[_] => Future[Result]): ActionBuilder[MEmailActivationReq, AnyContent] = {
    new reqUtil.SioActionBuilderImpl[MEmailActivationReq] {

      override def invokeBlock[A](request0: Request[A], block: (MEmailActivationReq[A]) => Future[Result]): Future[Result] = {
        val eaFut = emailActivations.maybeGetById(eaInfo.id)

        val request = aclUtil.reqFromRequest( request0 )
        val user = aclUtil.userFromRequest(request)

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
            LOGGER.warn(s"Client ip[${request.remoteClientAddress}] User#${user.personIdOpt.orNull} tried to use foreign activation key:\n  eaInfo = $eaInfo\n  ea = $ea")
            isAuth.onUnauth(request)
        }
      }

    }
  }

}
