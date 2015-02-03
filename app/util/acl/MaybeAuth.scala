package util.acl

import models.MAd
import play.api.mvc._
import util.acl.PersonWrapper.PwOpt_t
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.10.13 15:10
 * Description: ActionBuilder для определения залогиненности юзера.
 */
trait MaybeAuthAbstract extends ActionBuilder[AbstractRequestWithPwOpt] {

  /**
   * Вызывается генератор экшена в билдере.
   * @param request Реквест.
   * @param block Суть действий в виде функции, возвращающей фьючерс.
   * @tparam A Подтип реквеста.
   * @return Фьючерс, описывающий результат.
   */
  override def invokeBlock[A](request: Request[A], block: (AbstractRequestWithPwOpt[A]) => Future[Result]): Future[Result] = {
    val pwOpt = PersonWrapper.getFromRequest(request)
    val srmFut = SioReqMd.fromPwOpt(pwOpt)
    srmFut flatMap { srm =>
      block(RequestWithPwOpt(pwOpt, request, srm))
    }
  }

}

/** Сборка данных по текущей сессии юзера в реквест. */
object MaybeAuth
  extends MaybeAuthAbstract
  with ExpireSession[AbstractRequestWithPwOpt]
  with CookieCleanup[AbstractRequestWithPwOpt]




/** Абстрактный реквест в сторону какой-то рекламной карточки. */
abstract class AbstractRequestWithAd[A](request: Request[A]) extends AbstractRequestWithPwOpt(request) {
  def mad: MAd
}

/** Экземпляр реквеста, содержащего рекламную запрашиваемую карточку. */
case class RequestWithAd[A](
  mad: MAd,
  request: Request[A],
  pwOpt: PwOpt_t,
  sioReqMd: SioReqMd
) extends AbstractRequestWithAd(request)



/** Комбинация из MaybeAuth и читалки adId из модели [[models.MAd]]. */
trait GetAnyAdAbstract extends ActionBuilder[RequestWithAd] {
  def adId: String

  override def invokeBlock[A](request: Request[A], block: (RequestWithAd[A]) => Future[Result]): Future[Result] = {
    val madFut = MAd.getById(adId)
    val pwOpt = PersonWrapper.getFromRequest(request)
    val srmFut = SioReqMd.fromPwOpt(pwOpt)
    madFut flatMap {
      case Some(mad) =>
        srmFut flatMap { srm =>
          val req1 = RequestWithAd(mad, request, pwOpt, srm)
          block(req1)
        }
      case None =>
        adNotFound(pwOpt, request)
    }
  }

  /** Что возвращать, если карточка не найдена. */
  def adNotFound(pwOpt: PwOpt_t, request: Request[_]): Future[Result] = {
    val res = Results.NotFound("Ad not found: " + adId)
    Future successful res
  }
}

/**
 * Публичный доступ к указанной рекламной карточке.
 * @param adId id рекламной карточки.
 */
case class GetAnyAd(adId: String)
  extends GetAnyAdAbstract
  with ExpireSession[RequestWithAd]

