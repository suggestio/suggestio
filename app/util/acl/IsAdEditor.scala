package util.acl

import play.api.mvc.{Result, Request, ActionBuilder}
import models._
import util.acl.PersonWrapper.PwOpt_t
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client
import com.fasterxml.jackson.annotation.JsonIgnore

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
  private def maybeAllowed[A](pwOpt: PwOpt_t, mad: MAd, request: Request[A], srmFut: Future[SioReqMd]): Future[Option[RequestWithAd[A]]] = {
    if (PersonWrapper isSuperuser pwOpt) {
      srmFut map { srm =>
        Some(RequestWithAd(mad, request, pwOpt, srm)())
      }
    } else {
      pwOpt match {
        case Some(pw) =>
          for {
            adnNodeOpt <- MAdnNodeCache.getByIdCached(mad.producerId)
            srm <- srmFut
          } yield {
            adnNodeOpt flatMap { adnNode =>
              if (adnNode.personIds contains pw.personId) {
                Some(RequestWithAd(mad, request, pwOpt, srm)(adnNodeOpt))
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
case class IsAdEditor(adId: String) extends ActionBuilder[RequestWithAd] {
  protected def invokeBlock[A](request: Request[A], block: (RequestWithAd[A]) => Future[Result]): Future[Result] = {
    val pwOpt = PersonWrapper.getFromRequest(request)
    val srmFut = SioReqMd.fromPwOpt(pwOpt)
    MAd.getById(adId) flatMap {
      case Some(mad) =>
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
 * @param producerOpt Закешированные данные владельцу карточки.
 * @tparam A Параметр типа реквеста.
 */
case class RequestWithAd[A](
  mad: MAd,
  request: Request[A],
  pwOpt: PwOpt_t,
  sioReqMd: SioReqMd)
  (producerOpt: Option[MAdnNode] = None)
  extends AbstractRequestWithPwOpt(request) {

  /** Для доступа к изготовителю рекламы надо использовать этот фьючерс, а не producerOpt, который может быть
    * неожиданно пустым. */
  @JsonIgnore
  lazy val producerOptFut: Future[Option[MAdnNode]] = {
    if (producerOpt.isDefined) {
      Future successful producerOpt
    } else {
      MAdnNodeCache.getByIdCached(mad.producerId) 
    }
  }

  @JsonIgnore
  def producerId = mad.producerId

}

