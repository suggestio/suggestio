package controllers

import com.github.nscala_time.time.OrderingImplicits._
import models._
import models.adv.MExtTarget
import models.event.{EventsSearchArgs, ArgsInfo, MEventTmp, MEvent}
import org.joda.time.DateTime
import play.api.db.DB
import play.api.i18n.Messages
import play.twirl.api.Html
import util.PlayMacroLogsImpl
import util.acl.{HasNodeEventAccess, IsAdnNodeAdmin}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.async.AsyncUtil
import util.event.{LkEventsUtil, EventTypes}
import util.event.SiowebNotifier.Implicts.sn
import util.SiowebEsUtil.client
import play.api.Play.{current, configuration}
import views.html.lk.event._

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.01.15 11:08
 * Description: Контроллер раздела уведомлений в личном кабинете.
 * Контроллер поддерживает отображение уведомлений, удаление оных и прочие действия.
 */
object LkEvents extends SioControllerImpl with PlayMacroLogsImpl {

  import LOGGER._

  private val LIMIT_MAX  = configuration.getInt("lk.events.nodeIndex.limit.max") getOrElse 10
  private val OFFSET_MAX = configuration.getInt("lk.events.nodeIndex.offset.max") getOrElse 300

  /**
   * Рендер страницы текущих нотификаций.
   * Рендер происходит в несколько шагов с использованием сильного распараллеливания.
   * @param adnId id узла.
   * @param offset0 Сдвиг результатов. Нужен для постраничного вывода.
   * @param inline Инлайновый рендер ответа, вместо страницы? Для ajax-вызовов.
   * @return 200 OK + страница со списком уведомлений.
   */
  def nodeIndex(adnId: String, limit0: Int, offset0: Int, inline: Boolean) = IsAdnNodeAdmin(adnId).async { implicit request =>
    val limit = Math.min(LIMIT_MAX, limit0)
    val offset = Math.min(OFFSET_MAX, offset0)
    // Запустить фетчинг событий из хранилища.
    // withVsn нужен из-за того, что у нас используется tryUpdate() для выставления isUnseen-флага.
    val eventsSearch = new EventsSearchArgs(
      ownerId       = Some(adnId),
      returnVersion = Some(true),
      maxResults    = limit,
      offset        = offset
    )
    val eventsFut = MEvent.dynSearch(eventsSearch)

    // implicit чтобы компилятор сразу показал те места, где этот ctx забыли явно передать.
    implicit val ctx = implicitly[Context]

    // Если начало списка, и узел -- ресивер, то нужно проверить, есть ли у него геошейпы. Если нет, то собрать ещё одно событие...
    val geoWelcomeFut: Future[Option[(Html, DateTime)]] = {
      if (offset == 0  &&  request.adnNode.adn.isReceiver) {
        LkEventsUtil.getGeoWelcome(request.adnNode)(ctx)
      } else {
        Future successful None
      }
    }

    // Нужно отрендерить каждое хранимое событие с помощью соотв.шаблона. Для этого нужно собрать аргументы для каждого события.
    val evtsRndrFut = eventsFut.flatMap { mevents =>
      // В фоне пакетно отфетчить рекламные карточки и ext-таргеты в виде карт:
      val madsMapFut        = LkEventsUtil.readEsModel(mevents, MAd)(_.argsInfo.adIdOpt)
      val advExtTgsMapFut   = LkEventsUtil.readEsModel(mevents, MExtTarget)(_.argsInfo.advExtTgIds)

      // Параллельно собираем карты размещений из всех adv-моделей.
      val advsReqMapFut     = LkEventsUtil.readAdvModel(mevents, MAdvReq)(_.argsInfo.advReqIdOpt)
      val advsOkMapFut      = LkEventsUtil.readAdvModel(mevents, MAdvOk)(_.argsInfo.advOkIdOpt)
      val advsRefuseMapFut  = LkEventsUtil.readAdvModel(mevents, MAdvRefuse)(_.argsInfo.advRefuseIdOpt)

      // В фоне пакетно отфетчить все необходимые ноды через кеш узлов, но текущий узел прямо закинуть в финальную карту.
      // Используется кеш, поэтому это будет быстрее и должно запускаться в последнюю очередь.
      val nodesMapFut = {
        val allNodeIds = mevents
          .iterator
          .flatMap { _.argsInfo.adnIdOpt }
          .filter { _ != adnId }    // Текущую ноду фетчить не надо -- она уже в request лежит.
          .toSet
        MAdnNodeCache.multiGetMap(allNodeIds, List(request.adnNode))
      }

      for {
        // Когда все карты будут готовы, надо будет запустить рендер отфетченных событий в HTML.
        madsMap       <- madsMapFut
        nodesMap      <- nodesMapFut
        advExtTgsMap  <- advExtTgsMapFut
        advsReqMap    <- advsReqMapFut
        advsOkMap     <- advsOkMapFut
        advsRefuseMap <- advsRefuseMapFut
        // Параллельный рендер всех событий
        events        <- Future.traverse(mevents) { case mevent =>
          Future {
            // Запускаем рендер одного нотификейшена
            val ai = mevent.argsInfo
            val rArgs = event.RenderArgs(
              mevent        = mevent,
              adnNodeOpt    = ai.adnIdOpt.flatMap(nodesMap.get),
              advExtTgs   = ai.advExtTgIds.flatMap(advExtTgsMap.get),
              madOpt        = ai.adIdOpt.flatMap(madsMap.get),
              advReqOpt     = ai.advReqIdOpt.flatMap(advsReqMap.get),
              advOkOpt      = ai.advOkIdOpt.flatMap(advsOkMap.get),
              advRefuseOpt  = ai.advRefuseIdOpt.flatMap(advsRefuseMap.get)
            )
            mevent.etype.render(rArgs)(ctx) -> mevent.dateCreated
          }
        }
        // Нужно закинуть в кучу ещё уведомление об отсутствующей геолокации
        geoWelcomeOpt <- geoWelcomeFut
      } yield {
        // Восстанавливаем порядок по дате после параллельного рендера.
        // TODO Opt тут можно оптимизировать объединение и сортировку коллекций.
        (geoWelcomeOpt.toSeq ++ events)
          .sortBy(_._2)
          .map(_._1)
      }
    }

    // Автоматически помечать все непрочитанные сообщения как прочитанные:
    eventsFut.onSuccess { case mevents =>
      LkEventsUtil.markUnseenAsSeen(mevents)
    }

    // Рендерим конечный результат: страница или же инлайн
    evtsRndrFut.map { evtsRndr =>
      val render: Html = if (inline)
        _eventsListTpl(evtsRndr)(ctx)
      else
        nodeIndexTpl(evtsRndr, request.adnNode)(ctx)
      Ok(render)
    }
  }



  /**
   * Юзер нажал на кнопку выпиливания события.
   * @param eventId id события.
   * @return 2хх если всё ок. Иначе 4xx.
   */
  def nodeEventDelete(eventId: String) = HasNodeEventAccess(eventId, srmFull = false, onlyCloseable = true).async {
    implicit request =>
      request.mevent.delete.map {
        case true =>
          NoContent
        case false =>
          NotFound(Messages("e.event.not.found"))
      }
  }

}
