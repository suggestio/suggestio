package util.acl

import io.suggest.es.model.EsModel
import io.suggest.i18n.MsgCodes
import io.suggest.mbill2.m.ott.MOneTimeTokens
import io.suggest.n2.node.{MNodeTypes, MNodes}
import javax.inject.Inject
import io.suggest.req.ReqUtil
import io.suggest.util.logs.MacroLogsImpl
import models.mproj.ICommonDi
import models.req.MNodeReq
import models.usr.MEmailRecoverQs
import play.api.mvc._
import japgolly.univeq._
import models.mctx.Context
import play.api.http.Status

import scala.concurrent.Future
import scala.concurrent.duration._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.01.15 19:34
 * Description: ActionBuilder для доступа к экшенам активации email.
 */
class CanConfirmEmailPwReg @Inject()(
                                      aclUtil                 : AclUtil,
                                      mNodes                  : MNodes,
                                      reqUtil                 : ReqUtil,
                                      mOneTimeTokens          : MOneTimeTokens,
                                      esModel                 : EsModel,
                                      mCommonDi               : ICommonDi,
                                    )
  extends MacroLogsImpl
{

  import mCommonDi.{ec, slick, errorHandler}
  import esModel.api._


  /** Максимальное время жизни. */
  def MAX_AGE = 12.hours


  def activationImpossible(implicit ctx: Context): Future[Result] = {
    errorHandler.onClientError(
      request     = ctx.request,
      statusCode  = Status.CONFLICT,
      message     = ctx.messages( MsgCodes.`Activation.impossible` )
    )
  }


  /** Сборка ActionBuilder'а, проверяющего права доступа на подтверждение реги по email.
    *
    * @param qs Инфа по активации, присланная через URL qs.
    * @return
    */
  def apply(qs: MEmailRecoverQs): ActionBuilder[MNodeReq, AnyContent] = {
    new reqUtil.SioActionBuilderImpl[MNodeReq] {

      override def invokeBlock[A](request0: Request[A], block: (MNodeReq[A]) => Future[Result]): Future[Result] = {
        lazy val logPrefix = s"(${qs.email}):"

        val req1 = aclUtil.reqFromRequest(request0)

        val nowDiff = MEmailRecoverQs.getNowSec() - qs.nowSec

        implicit lazy val ctx = errorHandler.getContext2(req1)

        if (nowDiff <= 0L || nowDiff > MAX_AGE.toSeconds || qs.nodeId.isEmpty) {
          LOGGER.debug(s"$logPrefix [SEC] Too old. nowDiff=$nowDiff sec, user#${req1.user.personIdOpt.orNull} remote=${req1.remoteClientAddress}")
          activationImpossible( ctx )

        } else {
          val personNodeId = qs.nodeId.get

          // Нужно поискать ott если уже использовано:
          val mottOptFut = slick.db.run {
            mOneTimeTokens.getById( qs.nonce )
          }

          // Поискать указанный узел юзера:
          val personNodeOptFut = mNodes.getByIdCache( personNodeId )
            .withNodeType( MNodeTypes.Person )

          for {

            mottOpt <- mottOptFut
            if {
              val r = mottOpt.isEmpty
              if (!r) LOGGER.warn(s"$logPrefix OTT#${mottOpt.orNull} already exists. EaQs is already used.")
              r
            }

            // Юзер должен уже существовать.
            personNodeOpt <- personNodeOptFut
            if {
              val r = personNodeOpt.nonEmpty
              if (!r) LOGGER.error(s"$logPrefix Person#$personNodeId missing or invalid.")
              r
            }
            personNode = personNodeOpt.get

            mreq2 = MNodeReq( personNode, req1, req1.user )
            res <- block(mreq2)
          } yield {
            res
          }
        }
      }

    }
  }

}
