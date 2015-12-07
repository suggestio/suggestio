package util.acl

import models.MNode
import models.adv.MExtTarget
import models.req.SioReqMd
import play.api.mvc.{Result, Request, ActionBuilder}
import util.PlayMacroLogsDyn
import util.acl.PersonWrapper.PwOpt_t

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.01.15 17:34
 * Description: ActionBuilder для экшенов доступа к записям [[models.adv.MExtTarget]] по id.
 */

trait CanAccessExtTargetBaseCtl
  extends OnUnauthNodeCtl
  with IsAdnNodeAdminUtilCtl
{

  import mCommonDi._

  /** Базовая логика [[CanAccessExtTarget]] живёт в этом трейте. */
  trait CanAccessExtTargetBase
    extends ActionBuilder[ExtTargetRequest]
    with PlayMacroLogsDyn
    with OnUnauthNode
    with IsAdnNodeAdminUtil
  {

    /** id ранее сохранённого экземпляра [[models.adv.MExtTarget]]. */
    def tgId: String

    override def invokeBlock[A](request: Request[A], block: (ExtTargetRequest[A]) => Future[Result]): Future[Result] = {
      val tgOptFut = MExtTarget.getById(tgId)
      val pwOpt = PersonWrapper.getFromRequest(request)
      tgOptFut flatMap {
        // Запрошенная цель существует. Нужно проверить права на её узел.
        case Some(tg) =>
          val adnNodeOptFut = isAdnNodeAdmin(tg.adnId, pwOpt)
          val srmFut = SioReqMd.fromPwOptAdn(pwOpt, tg.adnId)
          adnNodeOptFut flatMap {
            // У юзера есть права на узел. Запускаем экшен на исполнение.
            case Some(mnode) =>
              srmFut flatMap { srm =>
                val req1 = ExtTargetRequest(
                  extTarget = tg,
                  pwOpt     = pwOpt,
                  adnNode   = mnode,
                  sioReqMd  = srm,
                  request   = request
                )
                block(req1)
              }

            // Нет прав на узел.
            case None =>
              onUnauthNode(request, pwOpt)
          }

        case None =>
          LOGGER.info(s"User $pwOpt tried to access to ExtTarget[$tgId], but id does not exist.")
          tgNotFound(request, pwOpt)
      }
    }

    /** Что делать и что возвращать юзеру, если цель не найдена? */
    def tgNotFound(request: Request[_], pwOpt: PwOpt_t): Future[Result] = {
      val res = NotFound("Target does not exist: " + tgId)
      Future successful res
    }

  }


  /** Дефолтовая реализация [[CanAccessExtTargetBase]] с поддержкой проления сессии. */
  case class CanAccessExtTarget(tgId: String)
    extends CanAccessExtTargetBase
    with ExpireSession[ExtTargetRequest]

}


/** Экземпляр реквеста, касающегося таргета. */
case class ExtTargetRequest[A](
  extTarget   : MExtTarget,
  pwOpt       : PwOpt_t,
  adnNode     : MNode,
  sioReqMd    : SioReqMd,
  request     : Request[A]
)
  extends AbstractRequestForAdnNode(request)
{
  override def isMyNode = true
}
