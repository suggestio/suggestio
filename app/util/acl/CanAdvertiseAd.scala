package util.acl

import play.api.mvc._
import models._
import util.PlayMacroLogsImpl
import util.acl.PersonWrapper.PwOpt_t
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client
import IsAdnNodeAdmin.onUnauth

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.05.14 18:24
 * Description: Проверка прав на размещение рекламной карточки.
 */

object CanAdvertiseAd extends PlayMacroLogsImpl {

  import LOGGER._

  /** Является ли указанный узел рекламодателем? */
  def isAdvertiserNode(adnNode: MAdnNode): Boolean = {
    adnNode.adn.isEnabled  &&  adnNode.adn.isProducer
  }

  /**
   * Определить, можно ли пропускать реквест на исполнение экшена.
   * @param pwOpt Данные о текущем юзере.
   * @param mad Рекламная карточка.
   * @param request Реквест.
   * @tparam A Параметр типа реквеста.
   * @return None если нельзя. Some([[RequestWithAdAndProducer]]) если можно исполнять реквест.
   */
  def maybeAllowed[A](pwOpt: PwOpt_t, mad: MAd, request: Request[A]): Future[Option[RequestWithAdAndProducer[A]]] = {
    if (PersonWrapper isSuperuser pwOpt) {
      MAdnNodeCache.getById(mad.producerId) flatMap { adnNodeOpt =>
        if (adnNodeOpt exists isAdvertiserNode) {
          val adnNode = adnNodeOpt.get
          SioReqMd.fromPwOptAdn(pwOpt, adnNode.id.get) map { srm =>
            Some(RequestWithAdAndProducer(mad, request, pwOpt, srm, adnNode))
          }
        } else {
          debug(s"maybeAllowed($pwOpt, ${mad.id.get}): superuser, but ad producer node ${mad.producerId} is not allowed to advertise.")
          Future successful None
        }
      }
    } else {
      pwOpt match {
        case Some(pw) =>
          MAdnNodeCache.getById(mad.producerId).flatMap { adnNodeOpt =>
            adnNodeOpt
              .filter { adnNode =>
                adnNode.personIds.contains(pw.personId)  &&  isAdvertiserNode(adnNode)
              }
              .fold
                {
                  debug(s"maybeAllowed($pwOpt, ${mad.id.get}): User is not node ${mad.producerId} admin or node is not a producer.")
                  Future successful Option.empty[RequestWithAdAndProducer[A]]
                }
                {adnNode =>
                  SioReqMd.fromPwOptAdn(pwOpt, adnNode.id.get) map { srm =>
                    Some(RequestWithAdAndProducer(mad, request, pwOpt, srm, adnNode))
                  }
                }
          }

        case None =>
          trace(s"maybeAllowed(${mad.id.get}): anonymous access prohibited")
          Future successful None
      }
    }
  }

}


/** Редактировать карточку может только владелец магазина. */
trait CanAdvertiseAdBase extends ActionBuilder[RequestWithAdAndProducer] {
  import CanAdvertiseAd.LOGGER._
  def adId: String
  def invokeBlock[A](request: Request[A], block: (RequestWithAdAndProducer[A]) => Future[Result]): Future[Result] = {
    val pwOpt = PersonWrapper.getFromRequest(request)
    MAd.getById(adId) flatMap {
      case Some(mad) =>
        CanAdvertiseAd.maybeAllowed(pwOpt, mad, request) flatMap {
          case Some(req1) =>
            block(req1)
          case None =>
            debug(s"invokeBlock(): maybeAllowed($pwOpt, mad=${mad.id.get}) -> false.")
            onUnauth(request, pwOpt)
        }

      case None =>
        debug("invokeBlock(): MAd not found: " + adId)
        onUnauth(request, pwOpt)
    }
  }
}

sealed trait CanAdvertiseAdBase2
  extends CanAdvertiseAdBase
  with ExpireSession[RequestWithAdAndProducer]

/** Запрос какой-то формы размещения рекламной карточки. */
case class CanAdvertiseAdGet(adId: String)
  extends CanAdvertiseAdBase2
  with CsrfGet[RequestWithAdAndProducer]

/** Сабмит какой-то формы размещения рекламной карточки. */
case class CanAdvertiseAdPost(adId: String)
  extends CanAdvertiseAdBase2
  with CsrfPost[RequestWithAdAndProducer]

