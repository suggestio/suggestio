package util.acl

import io.suggest.di.{IExecutionContext, IEsClient}
import models.req.SioReqMd
import play.api.mvc._
import models._
import util.di.INodeCache
import scala.concurrent.Future
import util.PlayMacroLogsDyn

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.10.15 19:39
 * Description: Аддон для контроллера для поддержки проверки прав
 * на редактирование в рамках узла ad show levels.
 */

trait CanUpdateSls
  extends AdEditBaseCtl
  with OnUnauthNodeCtl
  with INodeCache
  with IEsClient
  with IExecutionContext
{

  /** Проверка прав на возможность обновления уровней отображения рекламной карточки. */
  trait CanUpdateSlsBase
    extends ActionBuilder[RequestWithAdAndProducer]
    with AdEditBase
    with OnUnauthNode
  {

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
                mNodeCache.getById(mad.producerId) flatMap { producerOpt =>
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
