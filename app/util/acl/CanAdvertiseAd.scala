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
  private def maybeAllowed[A](pwOpt: PwOpt_t, mad: MAd, request: Request[A]): Future[Option[RequestWithAd[A]]] = {
    if (PersonWrapper isSuperuser pwOpt) {
      MAdnNodeCache.getByIdCached(mad.producerId) flatMap { adnNodeOpt =>
        if (adnNodeOpt exists isAdvertiserNode) {
          val adnNode = adnNodeOpt.get
          SioReqMd.fromPwOptAdn(pwOpt, adnNode.id.get) map { srm =>
            Some(RequestWithAd(mad, request, pwOpt, srm, adnNode))
          }
        } else {
          Future successful None
        }
      }
    } else {
      pwOpt match {
        case Some(pw) =>
          MAdnNodeCache.getByIdCached(mad.producerId).flatMap { adnNodeOpt =>
            adnNodeOpt
              .filter {
                adnNode => adnNode.personIds.contains(pw.personId)  &&  isAdvertiserNode(adnNode)
              }
              .fold
                { Future successful Option.empty[RequestWithAd[A]] }
                {adnNode =>
                  SioReqMd.fromPwOptAdn(pwOpt, adnNode.id.get) map { srm =>
                    Some(RequestWithAd(mad, request, pwOpt, srm, adnNode))
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
  override def invokeBlock[A](request: Request[A], block: (RequestWithAd[A]) => Future[Result]): Future[Result] = {
    val pwOpt = PersonWrapper.getFromRequest(request)
    MAd.getById(adId) flatMap {
      case Some(mad) =>
        CanAdvertiseAd.maybeAllowed(pwOpt, mad, request) flatMap {
          case Some(req1) => block(req1)
          case None       => onUnauth(request)
        }

      case None => onUnauth(request)
    }
  }
}
