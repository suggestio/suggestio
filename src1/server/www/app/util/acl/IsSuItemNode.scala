package util.acl

import io.suggest.es.model.EsModel
import javax.inject.Inject
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.item.MItems
import io.suggest.model.n2.node.MNodes
import io.suggest.req.ReqUtil
import io.suggest.util.logs.MacroLogsImpl
import models.mproj.ICommonDi
import models.req.{MItemNodeReq, MReq}
import play.api.http.Status
import play.api.mvc.{ActionBuilder, AnyContent, Request, Result}

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.03.16 11:15
  * Description: Аддон для контроллеров для поддержки получения MItem вместе со связанной mad.
  */
class IsSuItemNode @Inject()(
                              esModel                : EsModel,
                              mNodes                 : MNodes,
                              aclUtil                : AclUtil,
                              isSu                   : IsSu,
                              mItems                 : MItems,
                              reqUtil                : ReqUtil,
                              mCommonDi              : ICommonDi
                            )
  extends MacroLogsImpl
{

  import mCommonDi._
  import esModel.api._


  /**
    * @param itemId Ключ item'а в таблице MItems.
    */
  def apply(itemId: Gid_t): ActionBuilder[MItemNodeReq, AnyContent] = {
    new reqUtil.SioActionBuilderImpl[MItemNodeReq] {

      override def invokeBlock[A](request: Request[A], block: (MItemNodeReq[A]) => Future[Result]): Future[Result] = {
        val user = aclUtil.userFromRequest(request)

        def req1 = MReq(request, user)
        lazy val logPrefix = s"item#$itemId:"

        if (user.isSuper) {
          val mitemOptFut = slick.db.run {
            mItems.getById(itemId)
          }
          mitemOptFut.flatMap {
            case Some(mitem) =>
              val mnodeOptFut = mNodes.getByIdCache(mitem.nodeId)
              mnodeOptFut.flatMap {
                case Some(mad) =>
                  val req1 = MItemNodeReq(mitem, mad, user, request)
                  block(req1)

                case None =>
                  LOGGER.warn(s"$logPrefix item node#${mitem.nodeId} not found")
                  errorHandler.onClientError(req1, Status.NOT_FOUND)
              }

            case None =>
              LOGGER.debug(s"$logPrefix Item does not exist")
              errorHandler.onClientError(req1, Status.NOT_FOUND)
          }

        } else {
          LOGGER.trace(s"$logPrefix SU expected, but user#${user.personIdOpt.orNull} is here.")
          isSu.supOnUnauthFut(req1)
        }
      }

    }
  }

}
