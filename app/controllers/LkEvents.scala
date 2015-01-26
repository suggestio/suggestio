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
import util.event.EventTypes
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
        MAdnNodeGeo.countByNode(adnId).map {
          // Нет гео-шейпов у этого ресивера. Нужно отрендерить сообщение об этой проблеме. TODO Отсеивать просто-точки из подсчёта?
          case 0 =>
            val etype = EventTypes.NodeGeoWelcome
            // Дата создания события формируется на основе даты создания узла.
            // Нужно также, чтобы это событие не было первым в списке событий, связанных с созданием узла.
            val dt = request.adnNode.meta.dateCreated.plusSeconds(10)
            val mevt = MEventTmp(
              etype       = etype,
              ownerId     = adnId,
              argsInfo    = ArgsInfo(adnIdOpt = Some(adnId)),
              isCloseable = false,
              isUnseen    = true,
              id          = Some(adnId),
              dateCreated = dt
            )
            val rargs = event.RenderArgs(
              mevent        = mevt,
              withContainer = true,
              adnNodeOpt    = Some(request.adnNode)
            )
            val html = etype.render(rargs)(ctx)
            Some(html -> dt)

          // Есть геошейпы для узла. Ничего рендерить не надо.
          case _ => None
        }
      } else {
        Future successful None
      }
    }

    // Нужно отрендерить каждое хранимое событие с помощью соотв.шаблона. Для этого нужно собрать аргументы для каждого события.
    val evtsRndrFut = eventsFut.flatMap { mevents =>
      // Пакетно отфетчить рекламные карточки в виде карты.
      val madsMapFut = {
        val allAdIds = mevents
          .iterator
          .flatMap { _.argsInfo.adIdOpt }
          .toSet
        MAd.multiGetMap(allAdIds)
      }

      // Пакетно отфетчить все необходимые MExtTarget в виде карты.
      val advExtTgsMapFut = {
        val allTgsFut = mevents
          .iterator
          .flatMap { _.argsInfo.advExtTgIdOpt }
          .toSet
        MExtTarget.multiGetMap(allTgsFut)
      }

      // Пакетно отфетчить все необходимые ноды, включая текущий узел в финальную карту.
      // Используется кеш, поэтому это будет быстрее и должно запускаться в последнюю очередь.
      val nodesMapFut = {
        val allNodeIds = mevents
          .iterator
          .flatMap { _.argsInfo.adnIdOpt }
          .filter { _ != adnId }    // Текущую ноду фетчить не надо -- она уже в request лежит.
          .toSet
        MAdnNodeCache.multiGetMap(allNodeIds, List(request.adnNode))
      }

      // Асинхронно собираем карту размещений из всех adv-моделей.
      val advsMapFut: Future[Map[Int, MAdvI]] = {
        val allAdvIdsIter = mevents
          .iterator
          .flatMap { _.argsInfo.advIdOpt }
        if (allAdvIdsIter.nonEmpty) {
          // Есть размещения, связанные с исходной коллекцией событий.
          val allAdvIds = allAdvIdsIter.toSet.toSeq
          Future.traverse(Seq(MAdvReq, MAdvOk, MAdvRefuse)) { model =>
            Future {
              DB.withConnection { implicit c =>
                MAdvReq.multigetByIds(allAdvIds)
              }
            }(AsyncUtil.jdbcExecutionContext)

          } map {
            // Объединить все коллекции в одну карту. Ускоряем весь процесс через iterator'ы:
            _.iterator
             .flatMap { _.iterator }
             .map { adv => adv.id.get -> adv }
             .toMap
          }
        } else {
          // Нечего искать.
          Future successful Map.empty
        }
      }

      for {
        // Когда все карты будут готовы, надо будет запустить рендер отфетченных событий в HTML.
        madsMap       <- madsMapFut
        nodesMap      <- nodesMapFut
        advExtTgsMap  <- advExtTgsMapFut
        advsMap       <- advsMapFut
        // Параллельный рендер всех событий
        events   <-  Future.traverse(mevents) { case mevent =>
          Future {
            // Запускаем рендер одного нотификейшена
            val ai = mevent.argsInfo
            val rArgs = event.RenderArgs(
              mevent      = mevent,
              adnNodeOpt  = ai.adnIdOpt.flatMap(nodesMap.get),
              advExtTgOpt = ai.advExtTgIdOpt.flatMap(advExtTgsMap.get),
              madOpt      = ai.adIdOpt.flatMap(madsMap.get),
              advOpt      = ai.advIdOpt.flatMap(advsMap.get)
            )
            mevent.etype.render(rArgs)(ctx) -> mevent.dateCreated
          }
        }
        // Нужно закинуть в кучу ещё уведомление об отсутствующей геолокации
        geoWelcomeOpt  <- geoWelcomeFut
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
      mevents
        .iterator
        .filter(_.isUnseen)
        .map { mevt =>
          MEvent
            .tryUpdate(mevt) { _.copy(isUnseen = false) }
            .onFailure { case ex => error("Failed to mark event as 'seen': " + mevt, ex) }
        }
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
