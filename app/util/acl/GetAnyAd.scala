package util.acl

import controllers.SioController
import models.MNode
import models.req.SioReqMd
import play.api.mvc._
import util.acl.PersonWrapper.PwOpt_t

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.10.15 17:32
 * Description: ActionBuild для запроса действия над любой карточкой без проверки прав.
 */
trait GetAnyAd
  extends SioController
{

  import mCommonDi._

  /** Комбинация из MaybeAuth и читалки adId из [[models.MNode]]. */
  trait GetAnyAdAbstract
    extends ActionBuilder[RequestWithAd]
  {

    /** id запрашиваемой карточки. */
    def adId: String

    override def invokeBlock[A](request: Request[A], block: (RequestWithAd[A]) => Future[Result]): Future[Result] = {
      val madFut = MNode.getById(adId)
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
      val res = NotFound("Ad not found: " + adId)
      Future successful res
    }
  }

  /**
   * Публичный доступ к указанной рекламной карточке.
   * @param adId id рекламной карточки.
   */
  case class GetAnyAd(override val adId: String)
    extends GetAnyAdAbstract
    with ExpireSession[RequestWithAd]

}


/** Абстрактный реквест в сторону какой-то рекламной карточки. */
abstract class AbstractRequestWithAd[A](request: Request[A]) extends AbstractRequestWithPwOpt(request) {
  def mad: MNode
}

/** Экземпляр реквеста, содержащего рекламную запрашиваемую карточку. */
case class RequestWithAd[A](
  mad       : MNode,
  request   : Request[A],
  pwOpt     : PwOpt_t,
  sioReqMd  : SioReqMd
)
  extends AbstractRequestWithAd(request)

