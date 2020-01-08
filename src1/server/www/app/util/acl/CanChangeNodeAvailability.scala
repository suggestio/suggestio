package util.acl

import javax.inject.Inject
import io.suggest.model.n2.node.{MNode, MNodes}
import models.mproj.ICommonDi
import io.suggest.common.fut.FutureUtil.HellImplicits._
import io.suggest.es.model.EsModel
import io.suggest.model.n2.edge.MPredicates
import io.suggest.model.n2.edge.search.Criteria
import io.suggest.model.n2.node.search.MNodeSearch
import io.suggest.req.ReqUtil
import io.suggest.util.logs.MacroLogsImpl
import models.req.{IReqHdr, ISioUser, MNodeReq}
import play.api.http.Status
import play.api.mvc._
import util.billing.Bill2Util

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.03.17 22:05
  * Description: Может ли юзер глобально влиять на доступность указанного узла?
  * Т.е. удалять узел или управлять флагом isEnabled?
  *
  * Да, если он суперюзер, либо если тип узла, биллинг и наличие админства над узлом позволяют это сделать.
  */

class CanChangeNodeAvailability @Inject() (
                                            esModel           : EsModel,
                                            mNodes            : MNodes,
                                            aclUtil           : AclUtil,
                                            isNodeAdmin       : IsNodeAdmin,
                                            bill2Util         : Bill2Util,
                                            reqUtil           : ReqUtil,
                                            mCommonDi         : ICommonDi
                                          )
  extends MacroLogsImpl
{

  import mCommonDi._
  import esModel.api._


  /** Может ли админ узла влиять на availability указанного узла? */
  def adminCanChangeAvailabilityOf(mnode: MNode)(implicit req: IReqHdr): Future[Boolean] = {
    adminCanChangeAvailabilityOf(req.user, mnode)
  }
  /** Может ли админ узла влиять на availability указанного узла? */
  def adminCanChangeAvailabilityOf(nodeAdminUser: ISioUser, mnode: MNode): Future[Boolean] = {
    if (nodeAdminUser.isSuper) {
      true
    } else {
      nodeCanChangeAvailability(mnode)
    }
  }


  /** Можно ли нормальным юзерам влиять на availability данного узла? */
  def nodeCanChangeAvailability(mnode: MNode): Future[Boolean] = {
    if (
      // Тип узла должен подразумевать возможность расширенного управления со стороны юзера.
      mnode.common.ntype.userHasExtendedAcccess &&
        // Не должно быть banned-эджей на узле.
        !mnode.edges.withPredicateIter( MPredicates.ModeratedBy ).exists( _.info.flag.contains(false) )
    ) {
      val nodeId = mnode.id.get

      // Поискать размещений на узел в биллинге...
      val hasAnyBusyToNodeFut = slick.db.run {
        bill2Util.hasAnyBusyToNode(nodeId)
      }

      // Поискать какие-либо подчиненные узлы, указывающие на этот узел.
      val hasRefNodesFut = mNodes.dynCount {
        new MNodeSearch {
          override def outEdges: Seq[Criteria] = {
            val cr = Criteria(
              nodeIds    = nodeId :: Nil
            )
            cr :: Nil
          }
        }
      }

      // Сделать вывод по итогам анализа баз:
      for {
        hasAnyBusy  <- hasAnyBusyToNodeFut
        hasRefNodes <- hasRefNodesFut
      } yield {
        !(hasAnyBusy && hasRefNodes > 0L)
      }

    } else {
      LOGGER.trace(s"nodeCanChangeAvailability(${mnode.idOrNull}): Node has type ${mnode.common.ntype} or false-moderated, so availability changing is prohibited.")
      false
    }
  }


  def apply(nodeId: String): ActionBuilder[MNodeReq, AnyContent] = {
    new reqUtil.SioActionBuilderImpl[MNodeReq] {
      override def invokeBlock[A](request: Request[A], block: (MNodeReq[A]) => Future[Result]) = {
        val user = aclUtil.userFromRequest(request)
        val mnodeOptFut = isNodeAdmin.isAdnNodeAdmin(nodeId, user)

        def logPrefix = s"node$nodeId<-u#$user:"
        def forbidden: Future[Result] =
          errorHandler.onClientError(request, Status.FORBIDDEN, s"Not enought rights for node$nodeId.")

        mnodeOptFut.flatMap {
          // Уже есть админский доступ к узлу. Проверить, есть ли у юзеров возможность влиять на availability этого узла?
          case Some(mnode) =>
            adminCanChangeAvailabilityOf(user, mnode).flatMap {
              // Всё ок, пропускаем экшен на исполнение.
              case true =>
                val req1 = MNodeReq(mnode, request, user)
                block(req1)

              // Юзерам нельзя влиять на availability этого узла.
              case false =>
                LOGGER.warn(s"$logPrefix User has admin rights for node$nodeId, but user cannot change its availability.")
                forbidden
            }

          // Юзер не является админом этого узла.
          case None =>
            LOGGER.warn(s"$logPrefix User has NO admin rights over node")
            forbidden
        }
      }
    }
  }

}
