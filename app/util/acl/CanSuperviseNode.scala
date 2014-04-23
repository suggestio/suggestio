package util.acl

import play.api.mvc.{Result, Request, ActionBuilder}
import scala.concurrent.Future
import IsAdnNodeAdmin.onUnauth
import models._
import util.acl.PersonWrapper.PwOpt_t
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.04.14 16:53
 * Description: Супервайзинг узлов, на которое имеет право родительский супервизор.
 */

object CanSuperviseNode {
  /** Для проверки прав из шаблонов.
    * @param slaveNode Подчинённая нода.
    * @param supNode Узел-супервизор
    * @return true, если есть права на супервайзинг.
    */
  def canSuperviseNode(slaveNode: MAdnNode, supNode: MAdnNode)(implicit ctx: util.Context): Boolean = {
    supNode.id.isDefined &&
      slaveNode.adn.supId == supNode.id &&
      (PersonWrapper.isSuperuser(ctx.pwOpt) ||
        (supNode.personIds.contains(ctx.pwOpt.get.personId) && supNode.adn.isSupervisor)
      )
  }
}

case class CanSuperviseNode(adnId: String) extends ActionBuilder[RequestForNodeSupervision] {
  protected def invokeBlock[A](request: Request[A], block: (RequestForNodeSupervision[A]) => Future[Result]): Future[Result] = {
    MAdnNodeCache.getByIdCached(adnId) flatMap {
      case Some(mslave) if mslave.adn.supId.isDefined =>
        val pwOpt = PersonWrapper.getFromRequest(request)
        val supId = mslave.adn.supId.get
        val srmFut = SioReqMd.fromPwOptAdn(pwOpt, supId)
        IsAdnNodeAdmin.isAdnNodeAdmin(supId, pwOpt) flatMap {
          case Some(msup) if msup.adn.isSupervisor || PersonWrapper.isSuperuser(pwOpt) =>
            srmFut flatMap { srm =>
              val req1 = RequestForNodeSupervision(slaveNode = mslave, supNode = msup, request, pwOpt, srm)
              block(req1)
            }

          case _ => onUnauth(request)
        }

      // Не возвращаем 404 для защиты от возможных (бессмысленных) сканов.
      // None означает что или магазина нет, или ТЦ у магазина не указан (удалённый магазин, интернет-магазин).
      case None => onUnauth(request)
    }
  }
}


// Реквесты
abstract class AbstractRequestForNodeSupervision[A](request: Request[A]) extends AbstractRequestWithPwOpt(request) {
  def supNode: MAdnNode
  def slaveNode: MAdnNode
}

case class RequestForNodeSupervision[A](slaveNode: MAdnNode, supNode: MAdnNode, request: Request[A], pwOpt: PwOpt_t, sioReqMd: SioReqMd)
  extends AbstractRequestForNodeSupervision(request)


/** Просматривать прямые под-узлы может просматривать тот, кто записан в supId. */
case class CanViewAsSlaveNode(adnId: String) extends ActionBuilder[RequestForNodeSupervision] {
  override protected def invokeBlock[A](request: Request[A], block: (RequestForNodeSupervision[A]) => Future[Result]): Future[Result] = {
    MAdnNodeCache.getByIdCached(adnId) flatMap {
      case Some(mslave) if mslave.adn.supId.isDefined =>
        val pwOpt = PersonWrapper.getFromRequest(request)
        val supId = mslave.adn.supId.get
        val srmFut = SioReqMd.fromPwOptAdn(pwOpt, supId)
        IsAdnNodeAdmin.isAdnNodeAdmin(supId, pwOpt) flatMap {
          case Some(msup) =>
            srmFut flatMap { srm =>
              val req1 = RequestForNodeSupervision(slaveNode = mslave, supNode = msup, request, pwOpt, srm)
              block(req1)
            }

          case _ => onUnauth(request)
        }

      // Не возвращаем 404 для защиты от возможных (бессмысленных) сканов.
      // None означает что или магазина нет, или ТЦ у магазина не указан (удалённый магазин, интернет-магазин).
      case None => onUnauth(request)
    }
  }
}
