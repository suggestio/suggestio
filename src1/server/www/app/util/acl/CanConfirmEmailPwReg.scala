package util.acl

import controllers.routes
import io.suggest.es.model.EsModel
import io.suggest.model.n2.edge.MPredicates
import io.suggest.model.n2.edge.search.Criteria
import io.suggest.model.n2.node.{MNodeTypes, MNodes}
import io.suggest.model.n2.node.search.MNodeSearchDfltImpl
import javax.inject.Inject
import io.suggest.req.ReqUtil
import io.suggest.util.logs.MacroLogsImpl
import models.mproj.ICommonDi
import models.req.IReq
import models.usr.MEmailRecoverQs
import play.api.mvc._
import util.ident.IdentUtil
import japgolly.univeq._

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
                                      identUtil               : IdentUtil,
                                      isAuth                  : IsAuth,
                                      mNodes                  : MNodes,
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
  def apply(qs: MEmailRecoverQs)(eaNotFoundF: IReq[_] => Future[Result]): ActionBuilder[IReq, AnyContent] = {
    new reqUtil.SioActionBuilderImpl[IReq] {

      override def invokeBlock[A](request0: Request[A], block: (IReq[A]) => Future[Result]): Future[Result] = {
        lazy val logPrefix = s"(${qs.email}):"

        val req1 = aclUtil.reqFromRequest(request0)

        val nowDiff = MEmailRecoverQs.getNowSec() - qs.nowSec
        if (nowDiff <= 0L || nowDiff > 2.hours.toSeconds || qs.nodeId.nonEmpty) {
          LOGGER.debug(s"$logPrefix [SEC] Too old. nowDiff=$nowDiff sec, user#${req1.user.personIdOpt.orNull} remote=${req1.remoteClientAddress}")
          eaNotFoundF( req1 )

        } else if (req1.user.isAuth) {
          // Для упрощения - нельзя активировать учётку, будучи уже залогиненным.
          LOGGER.debug(s"$logPrefix User already logged in, rdr.")
          val res = Results.Redirect( routes.Ident.rdrUserSomewhere() )
          Future.successful(res)

        } else {
          // Вызываем index refresh() перед поиском, чтобы снизить риск параллельной регистрации. TODO Свести риск к нулю.
          mNodes
            .refreshIndex()
            .flatMap { _ =>
              // Свежая инфа по паролю, ищем юзера...
              val msearch = new MNodeSearchDfltImpl {
                override def limit = 1
                override val nodeTypes = MNodeTypes.Person :: Nil
                override val outEdges: Seq[Criteria] = {
                  val cr = Criteria(
                    predicates = MPredicates.Ident.Email :: Nil,
                    nodeIds    = qs.email :: Nil,
                  )
                  cr :: Nil
                }
              }
              mNodes.dynSearchIds( msearch )
            }
            .flatMap { nodeIds =>
              if (nodeIds.isEmpty) {
                LOGGER.trace(s"$logPrefix OK, email ident is free")
                block(req1)
              } else {
                // Уже есть хотя бы один узел с данным мыльником в ident'е.
                LOGGER.warn(s"$logPrefix One (or more) nodes for email ident already exists: ##[${nodeIds.mkString(", ")}]")
                eaNotFoundF( req1 )
              }
            }

        }
      }

    }
  }

}
