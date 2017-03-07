package util.acl

import com.google.inject.Inject
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.item.{MItem, MItems}
import io.suggest.util.logs.MacroLogsImpl
import io.suggest.www.util.acl.SioActionBuilderOuter
import models.mproj.ICommonDi
import models.req.{IReqHdr, MItemAdReq, MReq}
import play.api.mvc.{ActionBuilder, Request, Result}

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.03.16 11:15
  * Description: Аддон для контроллеров для поддержки получения MItem вместе со связанной mad.
  */
class IsSuItemAd @Inject() (
                             aclUtil                : AclUtil,
                             isSu                   : IsSu,
                             mItems                 : MItems,
                             mCommonDi              : ICommonDi
                           )
  extends SioActionBuilderOuter
  with MacroLogsImpl
{

  import mCommonDi._


  /**
    * @param itemId Ключ item'а в таблице MItems.
    */
  def apply(itemId: Gid_t): ActionBuilder[MItemAdReq] = {
    new SioActionBuilderImpl[MItemAdReq] {

      override def invokeBlock[A](request: Request[A], block: (MItemAdReq[A]) => Future[Result]): Future[Result] = {
        val user = aclUtil.userFromRequest(request)

        def req1 = MReq(request, user)

        if (user.isSuper) {
          val mitemOptFut = slick.db.run {
            mItems.getById(itemId)
          }
          mitemOptFut.flatMap {
            case Some(mitem) =>
              val madOptFut = mNodesCache.getById(mitem.nodeId)
              madOptFut.flatMap {
                case Some(mad) =>
                  val req1 = MItemAdReq(mitem, mad, user, request)
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
