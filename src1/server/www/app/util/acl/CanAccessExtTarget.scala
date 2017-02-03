package util.acl

import io.suggest.util.logs.MacroLogsDyn
import models.adv.IMExtTargets
import models.req.{MExtTargetNodeReq, MReq}
import play.api.mvc.{ActionBuilder, Request, Result}

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.01.15 17:34
 * Description: ActionBuilder для экшенов доступа к записям [[models.adv.MExtTarget]] по id.
 */

trait CanAccessExtTarget
  extends OnUnauthNodeCtl
  with IsAdnNodeAdminUtilCtl
  with IMExtTargets
{

  import mCommonDi._

  /** Базовая логика [[CanAccessExtTarget]] живёт в этом трейте. */
  trait CanAccessExtTargetBase
    extends ActionBuilder[MExtTargetNodeReq]
    with MacroLogsDyn
    with OnUnauthNode
    with IsAdnNodeAdminUtil
  {

    /** id ранее сохранённого экземпляра [[models.adv.MExtTarget]]. */
    def tgId: String

    override def invokeBlock[A](request: Request[A], block: (MExtTargetNodeReq[A]) => Future[Result]): Future[Result] = {
      val tgOptFut = mExtTargets.getById(tgId)

      val personIdOpt = sessionUtil.getPersonId(request)

      tgOptFut.flatMap {
        // Запрошенная цель существует. Нужно проверить права на её узел.
        case Some(tg) =>
          val user = mSioUsers(personIdOpt)
          val adnNodeOptFut = isAdnNodeAdmin(tg.adnId, user)
          adnNodeOptFut flatMap {
            // У юзера есть права на узел. Запускаем экшен на исполнение.
            case Some(mnode) =>
              val req1 = MExtTargetNodeReq(tg, mnode, request, user)
              block(req1)

            // Нет прав на узел.
            case None =>
              val req1 = MReq(request, user)
              onUnauthNode(req1)
          }

        case None =>
          LOGGER.info(s"User $personIdOpt tried to access to ExtTarget[$tgId], but id does not exist.")
          tgNotFound(request)
      }
    }

    /** Что делать и что возвращать юзеру, если цель не найдена? */
    def tgNotFound(request: Request[_]): Future[Result] = {
      val res = NotFound("Target does not exist: " + tgId)
      Future successful res
    }

  }


  /** Дефолтовая реализация [[CanAccessExtTargetBase]] с поддержкой проления сессии. */
  case class CanAccessExtTarget(override val tgId: String)
    extends CanAccessExtTargetBase
    with ExpireSession[MExtTargetNodeReq]

}
