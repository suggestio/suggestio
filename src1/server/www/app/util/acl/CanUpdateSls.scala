package util.acl

import io.suggest.es.model.EsModel
import io.suggest.n2.edge.MPredicates
import io.suggest.n2.node.{MNodeTypes, MNodes}
import javax.inject.Inject
import io.suggest.req.ReqUtil
import io.suggest.util.logs.MacroLogsImpl
import models.mproj.ICommonDi
import models.req.{MAdProdReq, MReq}
import play.api.mvc._
import util.n2u.N2NodesUtil

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.10.15 19:39
 * Description: Аддон для контроллера для поддержки проверки прав
 * на редактирование в рамках узла ad show levels.
 */

class CanUpdateSls @Inject() (
                               esModel                : EsModel,
                               mNodes                 : MNodes,
                               aclUtil                : AclUtil,
                               isNodeAdmin            : IsNodeAdmin,
                               canEditAd              : CanEditAd,
                               n2NodesUtil            : N2NodesUtil,
                               reqUtil                : ReqUtil,
                               mCommonDi              : ICommonDi
                             )
  extends MacroLogsImpl
{

  import mCommonDi._
  import esModel.api._

  def apply(adId1: String): ActionBuilder[MAdProdReq, AnyContent] = {
    new reqUtil.SioActionBuilderImpl[MAdProdReq] with canEditAd.AdEditBase {

      override final def adId = adId1

      override def invokeBlock[A](request: Request[A], block: (MAdProdReq[A]) => Future[Result]): Future[Result] = {
        val madOptFut = mNodes
          .getByIdCache(adId)
          .withNodeType(MNodeTypes.Ad)

        val user = aclUtil.userFromRequest(request)

        def reqErr = MReq(request, user)

        if (user.isAnon) {
          LOGGER.trace("invokeBlock(): Anonymous access prohibited to " + adId)
          isNodeAdmin.onUnauthNode(reqErr)

        } else {
          madOptFut.flatMap {
            // Найдена запрошенная рекламная карточка
            case Some(mad) =>
              // Модер может запретить бесплатное размещение карточки. Если стоит черная метка, то на этом можно закончить.
              val isMdrProhibited = mad.edges
                .withPredicateIter( MPredicates.ModeratedBy )
                .flatMap(_.info.flag)
                .contains(false)

              if (isMdrProhibited) {
                // Админы s.io когда-то запретили бесплатно размещать эту карточку. Пока бан не снять, карточку публиковать бесплатно нельзя.
                LOGGER.debug("invokeBlock(): cannot update sls for false-moderated ad " + adId)
                forbiddenFut("false-moderated ad", request)

              } else {

                val producerIdOpt = n2NodesUtil.madProducerId(mad)
                mNodes.maybeGetByIdCached(producerIdOpt).flatMap { producerOpt =>
                  val userIsNodeAdmin = producerOpt.exists { producer =>
                    isNodeAdmin.isNodeAdminCheck(producer, user)
                  }
                  if (userIsNodeAdmin) {
                    // Юзер является админом. Всё ок.
                    val req1 = MAdProdReq(mad, producerOpt.get, request, user)
                    block(req1)
                  } else {
                    // Юзер не является админом, либо (маловероятно) producer-узел был удалён (и нельзя проверить права).
                    LOGGER.debug(s"invokeBlock(): No node-admin rights for update sls for ad=$adId producer=${producerOpt.flatMap(_.id)}")
                    forbiddenFut("No edit rights", request)
                  }
                }
              }

            // Рекламная карточка не найдена.
            case None =>
              adNotFound(reqErr)
          }
        }
      }

    }
  }

}
