package util.acl

import javax.inject.Inject
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.item.MItems
import io.suggest.req.ReqUtil
import io.suggest.util.logs.MacroLogsImpl
import models.mproj.ICommonDi
import models.req.{MItemReq, MReq}
import play.api.http.Status
import play.api.mvc.{ActionBuilder, AnyContent, Request, Result}

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.02.16 16:17
  * Description: ACL-аддон для isSuperuser + mItems.getById()
  */
final class IsSuItem @Inject() (
                                 aclUtil     : AclUtil,
                                 reqUtil     : ReqUtil,
                                 mCommonDi   : ICommonDi
                               )
  extends MacroLogsImpl
{

  import mCommonDi.{ec, slick, errorHandler}
  import mCommonDi.current.injector

  private lazy val mItems = injector.instanceOf[MItems]
  private lazy val isSu = injector.instanceOf[IsSu]

  /**
    * @param itemId Ключ item'а в таблице MItems.
    */
  def apply(itemId: Gid_t): ActionBuilder[MItemReq, AnyContent] = {
    new reqUtil.SioActionBuilderImpl[MItemReq] {

      override def invokeBlock[A](request: Request[A], block: (MItemReq[A]) => Future[Result]): Future[Result] = {
        val user = aclUtil.userFromRequest(request)
        def req1 = MReq(request, user)

        if (user.isSuper) {
          val mitemOptFut = slick.db.run {
            mItems.getById(itemId)
          }
          mitemOptFut.flatMap {
            case Some(mitem) =>
              val req1 = MItemReq(mitem, request, user)
              block(req1)

            case None =>
              val msg = s"Item#$itemId not found."
              LOGGER.debug(msg)
              errorHandler.onClientError(req1, Status.NOT_FOUND, msg)
          }

        } else {
          isSu.supOnUnauthFut(req1)
        }
      }

    }
  }

}
