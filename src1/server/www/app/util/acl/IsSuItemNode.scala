package util.acl

import javax.inject.Inject

import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.item.{MItem, MItems}
import io.suggest.req.ReqUtil
import io.suggest.util.logs.MacroLogsImpl
import models.mproj.ICommonDi
import models.req.{IReqHdr, MItemNodeReq, MReq}
import play.api.mvc.{ActionBuilder, AnyContent, Request, Result}

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.03.16 11:15
  * Description: Аддон для контроллеров для поддержки получения MItem вместе со связанной mad.
  */
class IsSuItemNode @Inject()(
                              aclUtil                : AclUtil,
                              isSu                   : IsSu,
                              mItems                 : MItems,
                              reqUtil                : ReqUtil,
                              mCommonDi              : ICommonDi
                            )
  extends MacroLogsImpl
{

  import mCommonDi._


  /**
    * @param itemId Ключ item'а в таблице MItems.
    */
  def apply(itemId: Gid_t): ActionBuilder[MItemNodeReq, AnyContent] = {
    new reqUtil.SioActionBuilderImpl[MItemNodeReq] {

      override def invokeBlock[A](request: Request[A], block: (MItemNodeReq[A]) => Future[Result]): Future[Result] = {
        val user = aclUtil.userFromRequest(request)

        def req1 = MReq(request, user)

        if (user.isSuper) {
          val mitemOptFut = slick.db.run {
            mItems.getById(itemId)
          }
          mitemOptFut.flatMap {
            case Some(mitem) =>
              val mnodeOptFut = mNodesCache.getById(mitem.nodeId)
              mnodeOptFut.flatMap {
                case Some(mad) =>
                  val req1 = MItemNodeReq(mitem, mad, user, request)
                  block(req1)

                case None =>
                  madNotFound(mitem, req1)
              }

            case None =>
              itemNotFound(req1)
          }

        } else {
          isSu.supOnUnauthFut(req1)
        }
      }

      /** Реакция на отсутствующий item. */
      def itemNotFound(req: IReqHdr): Future[Result] = {
        LOGGER.debug(s"itemNotFound(): $itemId")
        errorHandler.http404Fut(req)
      }

      def madNotFound(mitem: MItem, req: IReqHdr): Future[Result] = {
        LOGGER.warn(s"madNotFound(${mitem.id.orNull}): ${mitem.nodeId}")
        errorHandler.http404Fut(req)
      }

    }
  }

}
