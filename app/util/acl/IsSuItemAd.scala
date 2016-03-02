package util.acl

import controllers.SioController
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.item.{MItem, IMItems}
import models.req.{MReq, IReqHdr, MItemAdReq}
import play.api.mvc.{Result, Request, ActionBuilder}
import util.PlayMacroLogsI

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.03.16 11:15
  * Description: Аддон для контроллеров для поддержки получения MItem вместе со связанной mad.
  */
trait IsSuItemAd
  extends SioController
  with IsSuperuserUtilCtl
  with Csrf
  with IMItems
  with PlayMacroLogsI
{

  import mCommonDi._


  sealed trait IsSuItemAdBase
    extends ActionBuilder[MItemAdReq]
    with IsSuperuserUtil
  {

    /** Ключ item'а в таблице MItems. */
    def itemId: Gid_t

    override def invokeBlock[A](request: Request[A], block: (MItemAdReq[A]) => Future[Result]): Future[Result] = {
      val personIdOpt = sessionUtil.getPersonId(request)
      val user = mSioUsers(personIdOpt)

      def req1 = MReq(request, user)

      if (user.isSuper) {
        val mitemOptFut = slick.db.run {
          mItems.getById(itemId)
        }
        mitemOptFut.flatMap {
          case Some(mitem) =>
            val madOptFut = mNodeCache.getById( mitem.adId )
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
        supOnUnauthFut(req1)
      }
    }

    /** Реакция на отсутствующий item. */
    def itemNotFound(req: IReqHdr): Future[Result] = {
      LOGGER.debug(s"itemNotFound(): $itemId")
      errorHandler.http404Fut(req)
    }

    def madNotFound(mitem: MItem, req: IReqHdr): Future[Result] = {
      LOGGER.warn(s"madNotFound(${mitem.id.orNull}): ${mitem.adId}")
      errorHandler.http404Fut(req)
    }

  }


  abstract class IsSuItemAdAbstract
    extends IsSuItemAdBase
    with ExpireSession[MItemAdReq]

  case class IsSuItemAd(override val itemId: Gid_t)
    extends IsSuItemAdAbstract

  case class IsSuItemAdGet(override val itemId: Gid_t)
    extends IsSuItemAdAbstract
    with CsrfGet[MItemAdReq]

  case class IsSuItemAdPost(override val itemId: Gid_t)
    extends IsSuItemAdAbstract
    with CsrfPost[MItemAdReq]

}
