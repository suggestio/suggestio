package util.acl

import play.api.mvc._
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

  def forbidden[A](adId: String, msg: String, request: Request[A]): Result = {
    Results.Forbidden(s"Forbidden access for ad[$adId]: $msg")
  }
  def forbiddenFut[A](adId: String, msg: String, request: Request[A]): Future[Result] = {
    Future successful forbidden(adId, msg, request)
  }

  def adNotFound(adId: String, request: RequestHeader): Future[Result] = {
    trace(s"invokeBlock(): Ad not found: $adId")
    controllers.Application.http404Fut(request)
  }
}

import IsAdEditor._

/** Редактировать карточку может только владелец магазина. */
trait CanEditAdBase extends ActionBuilder[RequestWithAd] {
  import LOGGER._

  def adId: String
  protected def invokeBlock[A](request: Request[A], block: (RequestWithAd[A]) => Future[Result]): Future[Result] = {
    val pwOpt = PersonWrapper.getFromRequest(request)
    val srmFut = SioReqMd.fromPwOpt(pwOpt)
    MAd.getById(adId) flatMap {
      case Some(mad) =>
        val hasAdv = DB.withConnection { implicit c =>
          MAdvOk.hasAdvUntilNow(mad.id.get)  ||  MAdvReq.hasAdvUntilNow(mad.id.get)
        }
        if (hasAdv) {
          // Если объява уже где-то опубликована, то значит редактировать её нельзя.
          forbiddenFut(adId, "Ad is advertised somewhere. Cannot edit during advertising.", request)
        } else {
          if (PersonWrapper isSuperuser pwOpt) {
            MAdnNodeCache.getByIdCached(mad.producerId).flatMap { adnNodeOpt =>
              srmFut flatMap { srm =>
                val req1 = RequestWithAd(mad, request, pwOpt, srm, adnNodeOpt.get)
                block(req1)
              }
            }
          } else {
            pwOpt match {
              case Some(pw) =>
                MAdnNodeCache.getByIdCached(mad.producerId) flatMap { adnNodeOpt =>
                  adnNodeOpt
                    .filter { adnNode => IsAdnNodeAdmin.isAdnNodeAdminCheck(adnNode, pwOpt) }
                    .fold {
                      debug(s"isEditAllowed(${mad.id.get}, $pwOpt): Not a producer[${mad.producerId}] admin.")
                      forbiddenFut(adId, "No node admin rights", request)
                    } { adnNode =>
                      srmFut flatMap { srm =>
                        val req1 = RequestWithAd(mad, request, pwOpt, srm, adnNode)
                        block(req1)
                      }
                    }
                }

              case None =>
                debug(s"isEditAllowed(${mad.id.get}, $pwOpt): Anonymous access prohibited.")
                onUnauth(request)
            }
          }
        }

      case None =>
        adNotFound(adId, request)
    }
  }
}
case class CanEditAd(adId: String)
  extends CanEditAdBase
  with ExpireSession[RequestWithAd]


/** Абстрактный реквест в сторону какой-то рекламной карточки на тему воздействия со стороны продьюсера. */
abstract class AbstractRequestWithAdFromProducer[A](request: Request[A]) extends AbstractRequestWithPwOpt(request) {
  def mad: MAd
  def producer: MAdnNode
}


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
) extends AbstractRequestWithAdFromProducer(request) {
  @JsonIgnore
  def producerId = mad.producerId
}



/** Статический логгер для класса запиливаем тут. object всё равно создаётся при компиляции case class'а, поэтому
  * оверхеда тут нет. */
object CanUpdateSls extends PlayMacroLogsImpl

/** Проверка прав на возможность обновления уровней отображения рекламной карточки. */
trait CanUpdateSlsBase extends ActionBuilder[RequestWithAd] {
  import CanUpdateSls.LOGGER._

  def adId: String

  override def invokeBlock[A](request: Request[A], block: (RequestWithAd[A]) => Future[Result]): Future[Result] = {
    val pwOpt = PersonWrapper getFromRequest request
    pwOpt match {
      // Юзер залогинен. Продолжаем...
      case Some(pw) =>
        MAd.getById(adId) flatMap {
          // Найдена запрошенная рекламная карточка
          case Some(mad) =>
            // Модер может запретить бесплатное размещение карточки. Если стоит черная метка, то на этом можно закончить.
            val isMdrProhibited = mad.moderation.freeAdv.exists { !_.isAllowed }
            if (isMdrProhibited) {
              // Админы s.io когда-то запретили бесплатно размещать эту карточку. Пока бан не снять, карточку публиковать бесплатно нельзя.
              debug("invokeBlock(): cannot update sls for false-moderated ad " + adId + " mdrResult = " + mad.moderation.freeAdv)
              forbiddenFut(adId, "false-moderated ad", request)
            } else {
              MAdnNodeCache.getByIdCached(mad.producerId) flatMap { producerOpt =>
                val isNodeAdmin = producerOpt.exists {
                  producer  =>  IsAdnNodeAdmin.isAdnNodeAdminCheck(producer, pwOpt)
                }
                if (isNodeAdmin) {
                  // Юзер является админом. Всё ок.
                  val req1 = RequestWithAd(mad, request, pwOpt, SioReqMd(), producerOpt.get)
                  block(req1)
                } else {
                  // Юзер не является админом, либо (маловероятно) producer-узел был удалён (и нельзя проверить права).
                  debug(s"invokeBlock(): No node-admin rights for update sls for ad=$adId producer=${producerOpt.flatMap(_.id)}")
                  forbiddenFut(adId, "No edit rights", request)
                }
              }
            }

          // Рекламная карточка не найдена.
          case None =>
            adNotFound(adId, request)
        }

      // С анонимусами разговор короткий.
      case None =>
        trace("invokeBlock(): Anonymous access prohibited to " + adId)
        onUnauth(request)
    }
  }
}

/** Реализация [[CanUpdateSlsBase]] с истечением времени сессии. */
case class CanUpdateSls(adId: String)
  extends CanUpdateSlsBase
  with ExpireSession[RequestWithAd]

