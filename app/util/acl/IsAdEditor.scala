package util.acl

import play.api.mvc.{SimpleResult, Request, ActionBuilder}
import models._
import util.acl.PersonWrapper.PwOpt_t
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.03.14 16:39
 * Description: Управление рекламной карточкой.
 */

object IsAdEditor {
  private def maybeAllowed[A](pwOpt: PwOpt_t, mad: MMartAd, request: Request[A]): Future[Option[RequestWithAd[A]]] = {
    if (PersonWrapper isSuperuser pwOpt) {
      Future successful Some(RequestWithAd(mad, request, pwOpt))
    } else {
      pwOpt match {
        case Some(pw) =>
          mad.shopId match {
            case Some(shopId) =>
              // Это реклама магазина. Редактировать может владелец магазина.
              MShop.getById(shopId) map { mshopOpt =>
                mshopOpt flatMap { mshop =>
                  if (mshop.personIds contains pw.personId) {
                    Some(RequestWithAd(mad, request, pwOpt, mshopOpt = mshopOpt))
                  } else {
                    None
                  }
                }
              }

            case None =>
              // Это реклама ТЦ. Редактировать может только владелец ТЦ.
              MMart.getById(mad.martId) map { mmartOpt =>
                mmartOpt flatMap { mmart =>
                  if (mmart.personIds contains pw.personId) {
                    Some(RequestWithAd(mad, request, pwOpt, mmartOpt=mmartOpt))
                  } else {
                    None
                  }
                }
              }
          }

        case None => Future successful None
      }
    }
  }
}


/** Редактировать карточку может только владелец магазина. */
case class IsAdEditor(adId: String) extends ActionBuilder[RequestWithAd] {
  protected def invokeBlock[A](request: Request[A], block: (RequestWithAd[A]) => Future[SimpleResult]): Future[SimpleResult] = {
    val pwOpt = PersonWrapper.getFromRequest(request)
    MMartAd.getById(adId) flatMap {
      case Some(mad) =>
        // TODO Нужно проверять права доступа как-то: для ТЦ и для магазина
        IsAdEditor.maybeAllowed(pwOpt, mad, request) flatMap {
          case Some(req1) => block(req1)
          case None       => IsAuth onUnauth request
        }

      case None => IsAuth onUnauth request
    }
  }
}


/**
 * Запрос, содержащий данные по рекламе и тому, к чему она относится.
 * В запросе кешируются значения MShop/MMart, если они были прочитаны во время проверки прав.
 * @param mad Рекламная карточка.
 * @param request Реквест
 * @param pwOpt Данные по юзеру.
 * @param mshopOpt Данные по магазину, если есть.
 * @param mmartOpt Данные по ТЦ, если есть.
 * @tparam A Параметр типа реквеста.
 */
case class RequestWithAd[A](
  mad: MMartAd,
  request: Request[A],
  pwOpt: PwOpt_t,
  private val mshopOpt: Option[MShop] = None,
  private val mmartOpt: Option[MMart] = None
) extends AbstractRequestWithPwOpt(request) {

  lazy val mshopOptFut: Future[Option[MShop]] = {
    if (mshopOpt.isDefined) {
      Future successful mshopOpt
    } else {
      mad.shopId match {
        case Some(shopId) => MShop.getById(shopId)
        case None         => Future successful None
      }
    }
  }

  lazy val mmartOptFut: Future[Option[MMart]] = {
    if (mmartOpt.isDefined)
      Future successful mmartOpt
    else
      MMart.getById(mad.martId)
  }

}

