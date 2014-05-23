package util.acl

import play.api.mvc.{Result, Request, ActionBuilder}
import models._
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

object CanAdvertiseAd {

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
   * @return None если нельзя. Some([[RequestWithAd]]) если можно исполнять реквест.
   */
  private def maybeAllowed[A](pwOpt: PwOpt_t, mad: MAd, request: Request[A], srmFut: Future[SioReqMd]): Future[Option[RequestWithAd[A]]] = {
    if (PersonWrapper isSuperuser pwOpt) {
      for {
        adnNodeOpt <- MAdnNodeCache.getByIdCached(mad.producerId)
        srm <- srmFut
      } yield {
        if (adnNodeOpt exists isAdvertiserNode) {
          Some(RequestWithAd(mad, request, pwOpt, srm, adnNodeOpt.get))
        } else {
          None
        }
      }
    } else {
      pwOpt match {
        case Some(pw) =>
          for {
            adnNodeOpt <- MAdnNodeCache.getByIdCached(mad.producerId)
            srm <- srmFut
          } yield {
            adnNodeOpt flatMap { adnNode =>
              if (adnNode.personIds.contains(pw.personId)  &&  isAdvertiserNode(adnNode)) {
                Some(RequestWithAd(mad, request, pwOpt, srm, adnNode))
              } else {
                None
              }
            }
          }

        case None => Future successful None
      }
    }
  }

}


/** Редактировать карточку может только владелец магазина. */
case class CanAdvertiseAd(adId: String) extends ActionBuilder[RequestWithAd] {
  protected def invokeBlock[A](request: Request[A], block: (RequestWithAd[A]) => Future[Result]): Future[Result] = {
    val pwOpt = PersonWrapper.getFromRequest(request)
    val srmFut = SioReqMd.fromPwOpt(pwOpt)
    MAd.getById(adId) flatMap {
      case Some(mad) =>
        CanAdvertiseAd.maybeAllowed(pwOpt, mad, request, srmFut) flatMap {
          case Some(req1) => block(req1)
          case None       => onUnauth(request)
        }

      case None => onUnauth(request)
    }
  }
}
