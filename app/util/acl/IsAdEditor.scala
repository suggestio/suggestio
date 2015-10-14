package util.acl

import controllers.{IDb, SioController}
import io.suggest.di.IEsClient
import models.req.SioReqMd
import play.api.mvc._
import models._
import util.acl.PersonWrapper.PwOpt_t
import util.async.AsyncUtil
import util.xplay.SioHttpErrorHandler
import scala.concurrent.Future
import util.{PlayMacroLogsDyn, PlayMacroLogsI}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.03.14 16:39
 * Description: Проверка прав на управление рекламной карточкой.
 */

trait AdEditBaseCtl extends SioController with IEsClient {

  /** Кое какая утиль для action builder'ов, редактирующих карточку. */
  trait AdEditBase extends PlayMacroLogsI {
    /** id рекламной карточки, которую клиент хочет поредактировать. */
    def adId: String

    def forbidden[A](msg: String, request: Request[A]): Result = {
      Forbidden(s"Forbidden access for ad[$adId]: $msg")
    }

    def forbiddenFut[A](msg: String, request: Request[A]): Future[Result] = {
      Future successful forbidden(msg, request)
    }

    def adNotFound(request: RequestHeader): Future[Result] = {
      LOGGER.trace(s"invokeBlock(): Ad not found: $adId")
      SioHttpErrorHandler.http404Fut(request)
    }
  }

}


/** Аддон для контроллеров, занимающихся редактированием рекламных карточек. */
trait CanEditAd extends AdEditBaseCtl with IDb {

  /** Редактировать карточку может только владелец магазина. */
  trait CanEditAdBase extends ActionBuilder[RequestWithAdAndProducer] with AdEditBase {

    /** Асинхронно обратится к реализации модели MAdvStatic за инфой по наличию текущих размещений. */
    def hasAdvUntilNow(adId: String, model: MAdvStatic): Future[Boolean] = {
      Future {
        db.withConnection { implicit c =>
          model.hasAdvUntilNow(adId)
        }
      }(AsyncUtil.jdbcExecutionContext)
    }

    /** Асинхронно параллельно обратится к [[models.MAdvOk]] и [[models.MAdvReq]] моделям инфой о наличии текущих размещений. */
    def hasAdv(adId: String): Future[Boolean] = {
      val hasAdvReqFut = hasAdvUntilNow(adId, MAdvReq)
      for {
        hasAdvOk  <- hasAdvUntilNow(adId, MAdvOk)
        hasAdvReq <- hasAdvReqFut
      } yield {
        hasAdvOk || hasAdvReq
      }
    }

    override def invokeBlock[A](request: Request[A], block: (RequestWithAdAndProducer[A]) => Future[Result]): Future[Result] = {
      val pwOpt = PersonWrapper.getFromRequest(request)
      pwOpt match {
        case Some(pw) =>
          val madOptFut = MAd.getById(adId)
          val srmFut = SioReqMd.fromPwOpt(pwOpt)
          val hasAdvFut = hasAdv(adId)
          madOptFut flatMap {
            case Some(mad) =>
              val adnNodeOpt = MAdnNodeCache.getById(mad.producerId)
              val isSuperuser = PersonWrapper.isSuperuser(pwOpt)
              hasAdvFut flatMap { hasAdv =>
                if (hasAdv && !isSuperuser) {
                  // Если объява уже где-то опубликована, то значит редактировать её нельзя.
                  forbiddenFut("Ad is advertised somewhere. Cannot edit during advertising.", request)
                } else {
                  if (isSuperuser) {
                    MAdnNodeCache.getById(mad.producerId).flatMap { adnNodeOpt =>
                      srmFut flatMap { srm =>
                        val req1 = RequestWithAdAndProducer(mad, request, pwOpt, srm, adnNodeOpt.get)
                        block(req1)
                      }
                    }
                  } else {
                    adnNodeOpt flatMap { adnNodeOpt =>
                      adnNodeOpt
                        .filter { adnNode => IsAdnNodeAdmin.isAdnNodeAdminCheck(adnNode, pwOpt) }
                        .fold {
                          LOGGER.debug(s"isEditAllowed(${mad.id.get}, $pwOpt): Not a producer[${mad.producerId}] admin.")
                          forbiddenFut("No node admin rights", request)
                        } { adnNode =>
                          srmFut flatMap { srm =>
                            val req1 = RequestWithAdAndProducer(mad, request, pwOpt, srm, adnNode)
                            block(req1)
                          }
                        }
                    }
                  } // else
                }
              }

            case None =>
              adNotFound(request)
          }

        // Анонимусу нельзя запрашивать редактирование карточки при любых обстоятельства.
        case None =>
          IsAuth.onUnauth(request)
      }
    }
  }

  sealed abstract class CanEditAd
    extends CanEditAdBase
    with ExpireSession[RequestWithAdAndProducer]
    with PlayMacroLogsDyn

  /** Запрос формы редактирования карточки должен сопровождаться выставлением CSRF-токена. */
  case class CanEditAdGet(override val adId: String)
    extends CanEditAd
    with CsrfGet[RequestWithAdAndProducer]

  /** Сабмит формы редактирования рекламной карточки должен начинаться с проверки CSRF-токена. */
  case class CanEditAdPost(override val adId: String)
    extends CanEditAd
    with CsrfPost[RequestWithAdAndProducer]

}


/** Абстрактный реквест в сторону какой-то рекламной карточки на тему воздействия со стороны продьюсера. */
abstract class AbstractRequestWithAdAndProducer[A](request: Request[A])
  extends AbstractRequestWithAd(request)
{
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
case class RequestWithAdAndProducer[A](
  mad       : MAd,
  request   : Request[A],
  pwOpt     : PwOpt_t,
  sioReqMd  : SioReqMd,
  producer  : MAdnNode
)
  extends AbstractRequestWithAdAndProducer(request)
{
  def producerId = mad.producerId
}



/** Статический логгер для класса запиливаем тут. object всё равно создаётся при компиляции case class'а, поэтому
  * оверхеда тут нет. */
trait CanUpdateSls extends AdEditBaseCtl with OnUnauthNodeCtl {

  /** Проверка прав на возможность обновления уровней отображения рекламной карточки. */
  trait CanUpdateSlsBase extends ActionBuilder[RequestWithAdAndProducer] with AdEditBase with OnUnauthNode {

    override def invokeBlock[A](request: Request[A], block: (RequestWithAdAndProducer[A]) => Future[Result]): Future[Result] = {
      val madOptFut = MAd.getById(adId)
      val pwOpt = PersonWrapper.getFromRequest(request)
      pwOpt match {
        // Юзер залогинен. Продолжаем...
        case Some(pw) =>
          madOptFut flatMap {
            // Найдена запрошенная рекламная карточка
            case Some(mad) =>
              // Модер может запретить бесплатное размещение карточки. Если стоит черная метка, то на этом можно закончить.
              val isMdrProhibited = mad.moderation.freeAdv.exists { !_.isAllowed }
              if (isMdrProhibited) {
                // Админы s.io когда-то запретили бесплатно размещать эту карточку. Пока бан не снять, карточку публиковать бесплатно нельзя.
                LOGGER.debug("invokeBlock(): cannot update sls for false-moderated ad " + adId + " mdrResult = " + mad.moderation.freeAdv)
                forbiddenFut("false-moderated ad", request)
              } else {
                MAdnNodeCache.getById(mad.producerId) flatMap { producerOpt =>
                  val isNodeAdmin = producerOpt.exists {
                    producer =>
                      IsAdnNodeAdmin.isAdnNodeAdminCheck(producer, pwOpt)
                  }
                  if (isNodeAdmin) {
                    // Юзер является админом. Всё ок.
                    val req1 = RequestWithAdAndProducer(mad, request, pwOpt, SioReqMd(), producerOpt.get)
                    block(req1)
                  } else {
                    // Юзер не является админом, либо (маловероятно) producer-узел был удалён (и нельзя проверить права).
                    LOGGER.debug(s"invokeBlock(): No node-admin rights for update sls for ad=$adId producer=${producerOpt.flatMap(_.id)}")
                    forbiddenFut("No edit rights", request)
                  }
                }
              }

            // Рекламная карточка не найдена.
            case None =>
              adNotFound(request)
          }

        // С анонимусами разговор короткий.
        case None =>
          LOGGER.trace("invokeBlock(): Anonymous access prohibited to " + adId)
          onUnauthNode(request, pwOpt)
      }
    }
  }

  /** Реализация [[CanUpdateSlsBase]] с истечением времени сессии. */
  case class CanUpdateSls(adId: String)
    extends CanUpdateSlsBase
    with ExpireSession[RequestWithAdAndProducer]
    with PlayMacroLogsDyn

}
