package util.acl

import javax.inject.Inject
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.item.MItems
import io.suggest.model.SlickHolder
import io.suggest.req.ReqUtil
import io.suggest.util.logs.MacroLogsImpl
import models.req.{MItemReq, MReq}
import play.api.http.{HttpErrorHandler, Status}
import play.api.inject.Injector
import play.api.mvc.{ActionBuilder, AnyContent, Request, Result}

import scala.concurrent.{ExecutionContext, Future}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.02.16 16:17
  * Description: ACL-аддон для isSuperuser + mItems.getById()
  */
final class IsSuItem @Inject() (
                                 injector    : Injector,
                               )
  extends MacroLogsImpl
{

  private lazy val aclUtil = injector.instanceOf[AclUtil]
  private lazy val reqUtil = injector.instanceOf[ReqUtil]
  private lazy val slickHolder = injector.instanceOf[SlickHolder]
  private lazy val errorHandler = injector.instanceOf[HttpErrorHandler]
  private lazy val mItems = injector.instanceOf[MItems]
  private lazy val isSu = injector.instanceOf[IsSu]
  implicit private lazy val ec = injector.instanceOf[ExecutionContext]

  import slickHolder.slick


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
