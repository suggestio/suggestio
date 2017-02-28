package util.acl

import com.google.inject.Inject
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.item.MItems
import io.suggest.www.util.acl.SioActionBuilderOuter
import models.mproj.ICommonDi
import models.req.{IReqHdr, MItemReq, MReq}
import play.api.mvc.{ActionBuilder, Request, Result}

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.02.16 16:17
  * Description: ACL-аддон для isSuperuser + mItems.getById()
  */
class IsSuItem @Inject() (
                           mItems      : MItems,
                           isSu        : IsSu,
                           mCommonDi   : ICommonDi
                         )
  extends SioActionBuilderOuter
{

  import mCommonDi._

  /**
    * @param itemId Ключ item'а в таблице MItems.
    */
  def apply(itemId: Gid_t): ActionBuilder[MItemReq] = {
    new SioActionBuilderImpl[MItemReq] {

      override def invokeBlock[A](request: Request[A], block: (MItemReq[A]) => Future[Result]): Future[Result] = {
        val personIdOpt = sessionUtil.getPersonId(request)
        val user = mSioUsers(personIdOpt)
        if (user.isSuper) {
          val mitemOptFut = slick.db.run {
            mItems.getById(itemId)
          }
          mitemOptFut.flatMap {
            case Some(mitem) =>
              val req1 = MItemReq(mitem, request, user)
              block(req1)

            case None =>
              val req1 = MReq(request, user)
              itemNotFound(req1)
          }

        } else {
          val req1 = MReq(request, user)
          isSu.supOnUnauthFut(req1)
        }
      }

      /** Реакция на отсутствующий item. */
      def itemNotFound(req: IReqHdr): Future[Result] = {
        errorHandler.http404Fut(req)
      }

    }
  }

}
