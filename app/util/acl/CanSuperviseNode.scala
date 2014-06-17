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

trait CanSuperviseNodeBase extends ActionBuilder[RequestForSlave] {
  def adnId: String
  protected def invokeBlock[A](request: Request[A], block: (RequestForSlave[A]) => Future[Result]): Future[Result] = {
    MAdnNodeCache.getByIdCached(adnId) flatMap {
      case Some(mslave) if mslave.adn.supId.isDefined =>
        val pwOpt = PersonWrapper.getFromRequest(request)
        val supId = mslave.adn.supId.get
        val srmFut = SioReqMd.fromPwOptAdn(pwOpt, supId)
        IsAdnNodeAdmin.isAdnNodeAdmin(supId, pwOpt) flatMap {
          case Some(msup) if msup.adn.isSupervisor || PersonWrapper.isSuperuser(pwOpt) =>
            srmFut flatMap { srm =>
              val req1 = RequestForSlave(slaveNode = mslave, supNode = msup, request, pwOpt, srm)
              block(req1)
            }

          case _ => onUnauth(request)
        }

      // Не возвращаем 404 для защиты от возможных (бессмысленных) сканов.
      // None означает что или магазина нет, или ТЦ у магазина не указан (удалённый магазин, интернет-магазин).
      case _ => onUnauth(request)
    }
  }
}

case class CanSuperviseNode(adnId: String)
  extends CanSuperviseNodeBase
  with ExpireSession[RequestForSlave]



// Реквесты
abstract class AbstractRequestForSlave[A](request: Request[A]) extends AbstractRequestWithPwOpt(request) {
  def supNode: MAdnNode
  def slaveNode: MAdnNode
}

case class RequestForSlave[A](slaveNode: MAdnNode, supNode: MAdnNode, request: Request[A], pwOpt: PwOpt_t, sioReqMd: SioReqMd)
  extends AbstractRequestForSlave(request)


/** Просматривать прямые под-узлы может просматривать тот, кто записан в supId. */
trait CanViewSlaveBase extends ActionBuilder[RequestForSlave] {
  def adnId: String
  override protected def invokeBlock[A](request: Request[A], block: (RequestForSlave[A]) => Future[Result]): Future[Result] = {
    val pwOpt = PersonWrapper.getFromRequest(request)
    MAdnNodeCache.getByIdCached(adnId) flatMap {
      case Some(mslave) if mslave.adn.supId.isDefined || PersonWrapper.isSuperuser(pwOpt) =>
        val supId = mslave.adn.supId.get
        val srmFut = SioReqMd.fromPwOptAdn(pwOpt, supId)
        IsAdnNodeAdmin.isAdnNodeAdmin(supId, pwOpt) flatMap {
          case Some(msup) =>
            srmFut flatMap { srm =>
              val req1 = RequestForSlave(slaveNode = mslave, supNode = msup, request, pwOpt, srm)
              block(req1)
            }

          case _ => onUnauth(request)
        }

      // Не возвращаем 404 для защиты от возможных (бессмысленных) сканов.
      // None означает что или магазина нет, или ТЦ у магазина не указан (удалённый магазин, интернет-магазин).
      case _ => onUnauth(request)
    }
  }
}
case class CanViewSlave(adnId: String)
  extends CanViewSlaveBase
  with ExpireSession[RequestForSlave]



/** Просматривать рекламу с прямых под-узлов может просматривать тот, кто записан в supId. */
trait CanViewSlaveAdBase extends ActionBuilder[RequestForSlaveAd] {
  def adId: String
  override protected def invokeBlock[A](request: Request[A], block: (RequestForSlaveAd[A]) => Future[Result]): Future[Result] = {
    MAd.getById(adId) flatMap {
      case Some(mad) =>
        val adnId = mad.producerId
        val pwOpt = PersonWrapper.getFromRequest(request)
        MAdnNodeCache.getByIdCached(adnId) flatMap {
          case Some(mslave) if mslave.adn.supId.isDefined || PersonWrapper.isSuperuser(pwOpt) =>
            val supId = mslave.adn.supId.get
            val srmFut = SioReqMd.fromPwOptAdn(pwOpt, supId)
            IsAdnNodeAdmin.isAdnNodeAdmin(supId, pwOpt) flatMap {
              case Some(msup) =>
                srmFut flatMap { srm =>
                  val req1 = RequestForSlaveAd(mad = mad, slaveNode = mslave, supNode = msup, request, pwOpt, srm)
                  block(req1)
                }

              case _ => onUnauth(request)
            }

          // Не возвращаем 404 для защиты от возможных (бессмысленных) сканов.
          // None означает что или магазина нет, или ТЦ у магазина не указан (удалённый магазин, интернет-магазин).
          case _ => onUnauth(request)
        }

      case None => onUnauth(request)
    }
  }
}
case class CanViewSlaveAd(adId: String)
  extends CanViewSlaveAdBase
  with ExpireSession[RequestForSlaveAd]


case class RequestForSlaveAd[A](mad: MAd, slaveNode: MAdnNode, supNode: MAdnNode, request: Request[A], pwOpt: PwOpt_t, sioReqMd: SioReqMd)
  extends AbstractRequestForSlave(request)


/** Можно ли влиять на рекламную карточку подчинённого узла? Да, если узел подчинён и если юзер -- админу узла-супервизора. */
trait CanSuperviseSlaveAdBase extends ActionBuilder[RequestForSlaveAd] {
  def adId: String
  protected def invokeBlock[A](request: Request[A], block: (RequestForSlaveAd[A]) => Future[Result]): Future[Result] = {
    val pwOpt = PersonWrapper.getFromRequest(request)
    // Для экшенов модерации обычно (пока что) не требуется bill-контекста, поэтому делаем srm по-простому.
    val srmFut = SioReqMd.fromPwOpt(pwOpt)
    MAd.getById(adId) flatMap {
      case Some(mad) =>
        val slaveNodeOptFut = MAdnNodeCache.getByIdCached(mad.producerId)
        // TODO Наверное надо проверять права супервайзера этого узла над подчинённым узлов.
        Future.traverse(mad.receivers.valuesIterator) { adRcvr =>
          IsAdnNodeAdmin.isAdnNodeAdmin(adRcvr.receiverId, pwOpt)
        } flatMap { results =>
          results.find(_.isDefined).flatten match {
            // isSuperuser проверяется тут, чтобы легче выявлять ошибки, даже будучи админом.
            case Some(supNode) if supNode.adn.isSupervisor || PersonWrapper.isSuperuser(pwOpt) =>
              slaveNodeOptFut.flatMap { slaveNodeOpt =>
                srmFut.flatMap { srm =>
                  val req1 = RequestForSlaveAd(
                    mad = mad,
                    slaveNode = slaveNodeOpt.get,
                    supNode = supNode,
                    request = request,
                    pwOpt = pwOpt,
                    sioReqMd = srm
                  )
                  block(req1)
                }
              }
            case _ => onUnauth(request)
          }
        }

      case _ => onUnauth(request)
    }
  }
}

case class CanSuperviseSlaveAd(adId: String)
  extends CanSuperviseSlaveAdBase
  with ExpireSession[RequestForSlaveAd]

