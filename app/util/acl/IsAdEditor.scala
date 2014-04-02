package util.acl

import play.api.mvc.{Result, Request, ActionBuilder}
import models._
import util.acl.PersonWrapper.PwOpt_t
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.03.14 16:39
 * Description: Проверка прав на управление рекламной карточкой.
 */

object IsAdEditor {

  /**
   * Определить, можно ли пропускать реквест на исполнение экшена.
   * @param pwOpt Данные о текущем юзере.
   * @param mad Рекламная карточка.
   * @param request Реквест.
   * @tparam A Параметр типа реквеста.
   * @return None если нельзя. Some([[RequestWithAd]]) если можно исполнять реквест.
   */
  private def maybeAllowed[A](pwOpt: PwOpt_t, mad: MMartAd, request: Request[A], srmFut: Future[SioReqMd]): Future[Option[RequestWithAd[A]]] = {
    if (PersonWrapper isSuperuser pwOpt) {
      srmFut map { srm =>
        Some(RequestWithAd(mad, request, pwOpt, srm))
      }
    } else {
      pwOpt match {
        case Some(pw) =>
          mad.producerId match {
            // Это реклама магазина. Редактировать может владелец магазина.
            case Some(shopId) =>
              for {
                mshopOpt <- MShop.getById(shopId)
                srm <- srmFut
              } yield {
                mshopOpt flatMap { mshop =>
                  if (mshop.personIds contains pw.personId) {
                    Some(RequestWithAd(mad, request, pwOpt, srm, mshopOpt = mshopOpt))
                  } else {
                    None
                  }
                }
              }

            // Это реклама ТЦ. Редактировать может только владелец ТЦ.
            case None =>
              for {
                mmartOpt <- MMart.getById(mad.receiverIds)
                srm <- srmFut
              } yield {
                mmartOpt flatMap { mmart =>
                  if (mmart.personIds contains pw.personId) {
                    Some(RequestWithAd(mad, request, pwOpt, srm, mmartOpt=mmartOpt))
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
  protected def invokeBlock[A](request: Request[A], block: (RequestWithAd[A]) => Future[Result]): Future[Result] = {
    val pwOpt = PersonWrapper.getFromRequest(request)
    val srmFut = SioReqMd.fromPwOpt(pwOpt)
    MMartAd.getById(adId) flatMap {
      case Some(mad) =>
        // TODO Нужно проверять права доступа как-то: для ТЦ и для магазина
        IsAdEditor.maybeAllowed(pwOpt, mad, request, srmFut) flatMap {
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
 * @param mshopOpt Закешированные данные по магазину, если было чтение.
 * @param mmartOpt Закешированные данные по ТЦ, если было чтение.
 * @tparam A Параметр типа реквеста.
 */
case class RequestWithAd[A](
  mad: MMartAd,
  request: Request[A],
  pwOpt: PwOpt_t,
  sioReqMd: SioReqMd,
  private val mshopOpt: Option[MShop] = None,
  private val mmartOpt: Option[MMart] = None
) extends AbstractRequestWithPwOpt(request) {

  lazy val mshopOptFut: Future[Option[MShop]] = {
    if (mshopOpt.isDefined) {
      Future successful mshopOpt
    } else {
      mad.producerId match {
        case Some(shopId) => MShop.getById(shopId)
        case None         => Future successful None
      }
    }
  }

  lazy val mmartOptFut: Future[Option[MMart]] = {
    if (mmartOpt.isDefined)
      Future successful mmartOpt
    else
      MMart.getById(mad.receiverIds)
  }

}

