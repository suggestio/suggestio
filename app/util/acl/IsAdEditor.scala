package util.acl

import play.api.mvc.{Result, Request, ActionBuilder}
import models._
import util.acl.PersonWrapper.PwOpt_t
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client
import com.fasterxml.jackson.annotation.JsonIgnore
import IsAdnNodeAdmin.onUnauth
import play.api.Play.current
import play.api.db.DB
import util.PlayMacroLogsImpl

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.03.14 16:39
 * Description: Проверка прав на управление рекламной карточкой.
 */

object IsAdEditor extends PlayMacroLogsImpl {

  import LOGGER._

  /**
   * Определить, можно ли пропускать реквест на исполнение экшена.
   * @param pwOpt Данные о текущем юзере.
   * @param mad Рекламная карточка.
   * @param request Реквест.
   * @tparam A Параметр типа реквеста.
   * @return None если нельзя. Some([[RequestWithAd]]) если можно исполнять реквест.
   */
  def maybeAllowed[A](pwOpt: PwOpt_t, mad: MAd, request: Request[A], srmFut: Future[SioReqMd]): Future[Option[RequestWithAd[A]]] = {
    val hasAdv = DB.withConnection { implicit c =>
      MAdvOk.hasAdvUntilNow(mad.id.get)  ||  MAdvReq.hasAdvUntilNow(mad.id.get)
    }
    trace(s"maybeAllowed(${mad.id.get}): ad has advs = $hasAdv")
    if (hasAdv) {
      // Если объява уже где-то опубликована, то значит редактировать её нельзя.
      Future successful None
    } else {
      if (PersonWrapper isSuperuser pwOpt) {
        for {
          adnNodeOpt <- MAdnNodeCache.getByIdCached(mad.producerId)
          srm <- srmFut
        } yield {
          Some(RequestWithAd(mad, request, pwOpt, srm, adnNodeOpt.get))
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

}


/** Редактировать карточку может только владелец магазина. */
trait IsAdEditorBase extends ActionBuilder[RequestWithAd] {
  def adId: String
  protected def invokeBlock[A](request: Request[A], block: (RequestWithAd[A]) => Future[Result]): Future[Result] = {
    val pwOpt = PersonWrapper.getFromRequest(request)
    val srmFut = SioReqMd.fromPwOpt(pwOpt)
    MAd.getById(adId) flatMap {
      case Some(mad) =>
        IsAdEditor.maybeAllowed(pwOpt, mad, request, srmFut) flatMap {
          case Some(req1) => block(req1)
          case None       => onUnauth(request)
        }

      case None => onUnauth(request)
    }
  }
}
case class IsAdEditor(adId: String)
  extends IsAdEditorBase
  with ExpireSession[RequestWithAd]


/**
 * Запрос, содержащий данные по рекламе и тому, к чему она относится.
 * В запросе кешируются значения MShop/MMart, если они были прочитаны во время проверки прав.
 * @param mad Рекламная карточка.
 * @param request Реквест
 * @param pwOpt Данные по юзеру.
 * @tparam A Параметр типа реквеста.
 */
case class RequestWithAd[A](
  mad: MAd,
  request: Request[A],
  pwOpt: PwOpt_t,
  sioReqMd: SioReqMd,
  producer: MAdnNode
) extends AbstractRequestWithPwOpt(request) {

  @JsonIgnore
  def producerId = mad.producerId

}

