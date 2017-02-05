package util.acl

import controllers.SioController
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.item.IMItems
import models.req.{IReqHdr, MReq, MItemReq}
import play.api.mvc.{Result, Request, ActionBuilder}

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.02.16 16:17
  * Description: ACL-аддон для isSuperuser + mItems.getById()
  */
trait IsSuItem
  extends SioController
  with Csrf
  with IMItems
{

  import mCommonDi._

  sealed trait IsSuItemBase
    extends ActionBuilder[MItemReq]
    with IsSuperuserUtil
  {

    /** Ключ item'а в таблице MItems. */
    def itemId: Gid_t

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
        supOnUnauthFut(req1)
      }
    }

    /** Реакция на отсутствующий item. */
    def itemNotFound(req: IReqHdr): Future[Result] = {
      errorHandler.http404Fut(req)
    }

  }


  sealed abstract class IsSuItemAbstract
    extends IsSuItemBase
    with ExpireSession[MItemReq]

  case class IsSuItem(override val itemId: Gid_t)
    extends IsSuItemAbstract

  case class IsSuItemGet(override val itemId: Gid_t)
    extends IsSuItemAbstract
    with CsrfGet[MItemReq]

  case class IsSuItemPost(override val itemId: Gid_t)
    extends IsSuItemAbstract
    with CsrfPost[MItemReq]

}
