package util.acl

import models.MAdnNode
import models.adv.MExtTarget
import play.api.mvc.{Results, Result, Request, ActionBuilder}
import util.PlayMacroLogsDyn
import util.acl.PersonWrapper.PwOpt_t
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.01.15 17:34
 * Description: ActionBuilder для экшенов доступа к записям [[models.adv.MExtTarget]] по id.
 */

trait CanAccessExtTargetBase extends ActionBuilder[ExtTargetRequest] with PlayMacroLogsDyn {

  /** id ранее сохранённого экземпляра [[models.adv.MExtTarget]]. */
  def tgId: String

  override def invokeBlock[A](request: Request[A], block: (ExtTargetRequest[A]) => Future[Result]): Future[Result] = {
    val tgOptFut = MExtTarget.getById(tgId)
    val pwOpt = PersonWrapper.getFromRequest(request)
    tgOptFut flatMap {
      // Запрошенная цель существует. Нужно проверить права на её узел.
      case Some(tg) =>
        val adnNodeOptFut = IsAdnNodeAdmin.isAdnNodeAdmin(tg.adnId, pwOpt)
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
            IsAdnNodeAdmin.onUnauth(request, pwOpt)
        }

      case None =>
        LOGGER.info(s"User $pwOpt tried to access to ExtTarget[$tgId], but id does not exist.")
        tgNotFound(request, pwOpt)
    }
  }

  /** Что делать и что возвращать юзеру, если цель не найдена? */
  def tgNotFound(request: Request[_], pwOpt: PwOpt_t): Future[Result] = {
    val res = Results.NotFound("Target does not exist: " + tgId)
    Future successful res
  }
}


/** Экземпляр реквеста, касающегося таргета. */
case class ExtTargetRequest[A](
  extTarget   : MExtTarget,
  pwOpt       : PwOpt_t,
  adnNode     : MAdnNode,
  sioReqMd    : SioReqMd,
  request     : Request[A]
)
  extends AbstractRequestForAdnNode(request)
{
  override def isMyNode = true
}


/** Дефолтовая реализация [[CanAccessExtTargetBase]] с поддержкой проления сессии. */
case class CanAccessExtTarget(tgId: String)
  extends CanAccessExtTargetBase
  with ExpireSession[ExtTargetRequest]

