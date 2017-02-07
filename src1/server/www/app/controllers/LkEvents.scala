package controllers

import java.time.OffsetDateTime

import com.google.inject.Inject
import io.suggest.model.n2.node.MNodes
import io.suggest.util.logs.MacroLogsImpl
import models._
import models.adv.MExtTargets
import models.event.MEvents
import models.event.search.MEventsSearchArgs
import models.mproj.ICommonDi
import play.api.i18n.Messages
import play.twirl.api.Html
import util.acl.{CanAccessEvent, IsAdnNodeAdmin}
import util.event.LkEventsUtil
import util.lk.LkAdUtil
import views.html.lk.event._

import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.01.15 11:08
 * Description: Контроллер раздела уведомлений в личном кабинете.
 * Контроллер поддерживает отображение уведомлений, удаление оных и прочие действия.
 */
class LkEvents @Inject() (
  lkEventsUtil                    : LkEventsUtil,
  lkAdUtil                        : LkAdUtil,
  mNodes                          : MNodes,
  mExtTargets                     : MExtTargets,
  isAdnNodeAdmin                  : IsAdnNodeAdmin,
  canAccessEvent                  : CanAccessEvent,
  mEvents                         : MEvents,
  override val mCommonDi          : ICommonDi
)
  extends SioControllerImpl
  with MacroLogsImpl
{

  import LOGGER._
  import mCommonDi._

  private val LIMIT_MAX  = configuration.getInt("lk.events.nodeIndex.limit.max").getOrElse(10)
  private val OFFSET_MAX = configuration.getInt("lk.events.nodeIndex.offset.max").getOrElse(300)

  /**
   * Рендер страницы текущих нотификаций.
   * Рендер происходит в несколько шагов с использованием сильного распараллеливания.
    *
    * @param adnId id узла.
   * @param offset0 Сдвиг результатов. Нужен для постраничного вывода.
   * @param inline Инлайновый рендер ответа, вместо страницы? Для ajax-вызовов.
   * @return 200 OK + страница со списком уведомлений.
   */
  def nodeIndex(adnId: String, limit0: Int, offset0: Int, inline: Boolean) = isAdnNodeAdmin.Get(adnId, U.Lk).async { implicit request =>
    val limit = Math.min(LIMIT_MAX, limit0)
    val offset = Math.min(OFFSET_MAX, offset0)
    // Запустить фетчинг событий из хранилища.
    // withVsn нужен из-за того, что у нас используется tryUpdate() для выставления isUnseen-флага.
    val eventsSearch = MEventsSearchArgs(
      ownerId       = Some(adnId),
      returnVersion = Some(true),
      limit         = limit,
      offset        = offset
    )
    val eventsFut = mEvents.dynSearch(eventsSearch)

    val ctxFut = request.user.lkCtxDataFut.map { implicit ctxData =>
      getContext2
    }

    // Если начало списка, и узел -- ресивер, то нужно проверить, есть ли у него геошейпы. Если нет, то собрать ещё одно событие...
    val geoWelcomeFut: Future[Option[(Html, OffsetDateTime)]] = ctxFut.flatMap { implicit ctx =>
      val mnode = request.mnode
      if (offset == 0  &&  mnode.extras.adn.exists(_.isReceiver)) {
        lkEventsUtil.getGeoWelcome(mnode)(ctx)
      } else {
        Future.successful( None )
      }
    }

    // Нужно отрендерить каждое хранимое событие с помощью соотв.шаблона. Для этого нужно собрать аргументы для каждого события.
    val evtsRndrFut = for {
      mevents   <- eventsFut
      ctx       <- ctxFut
      evtsRndr  <- {
        // В фоне пакетно отфетчить рекламные карточки и ext-таргеты в виде карт:
        val madsMapFut = lkEventsUtil.readEsModel(mevents, mNodes)(_.argsInfo.adIdOpt)
        val advExtTgsMapFut = lkEventsUtil.readEsModel(mevents, mExtTargets)(_.argsInfo.advExtTgIds)

        // Если передается карточка, то следует сразу передать и block RenderArgs для отображения превьюшки.
        val brArgsMapFut = madsMapFut.flatMap { madsMap =>
          val dsOpt = ctx.deviceScreenOpt
          val ressFut = Future.traverse(madsMap) {
            case (madId, mad) =>
              lkAdUtil.tiledAdBrArgs(mad, dsOpt)
                .map {
                  madId -> _
                }
          }
          ressFut.map {
            _.toMap
          }
        }

        // В фоне пакетно отфетчить все необходимые ноды через кеш узлов, но текущий узел прямо закинуть в финальную карту.
        // Используется кеш, поэтому это будет быстрее и должно запускаться в последнюю очередь.
        val nodesMapFut = {
          val allNodeIds = mevents
            .iterator
            .flatMap {
              _.argsInfo.adnIdOpt
            }
            .filter {
              _ != adnId
            } // Текущую ноду фетчить не надо -- она уже в request лежит.
            .toSet
          mNodesCache.multiGetMap(allNodeIds, List(request.mnode))
        }

        for {
          // Когда все карты будут готовы, надо будет запустить рендер отфетченных событий в HTML.
          madsMap       <- madsMapFut
          nodesMap      <- nodesMapFut
          advExtTgsMap  <- advExtTgsMapFut
          brArgsMap     <- brArgsMapFut
          // Параллельный рендер всех событий
          events <- Future.traverse(mevents) { mevent =>
            Future {
              // Запускаем рендер одного нотификейшена
              val ai = mevent.argsInfo
              val rArgs = event.RenderArgs(
                mevent = mevent,
                adnNodeOpt = ai.adnIdOpt.flatMap(nodesMap.get),
                advExtTgs = ai.advExtTgIds.flatMap(advExtTgsMap.get),
                madOpt = ai.adIdOpt.flatMap(madsMap.get),
                brArgs = ai.adIdOpt.flatMap(brArgsMap.get)
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
    } yield {
      evtsRndr
    }

    // Автоматически помечать все сообщения как прочитанные при первом заходе на страницу событий:
    if (offset == 0 && !inline) {
      evtsRndrFut.onSuccess { case _ =>
        lkEventsUtil.markAllSeenForNode(adnId) onComplete {
          case Success(count) =>
            if (count > 0)
              trace(s"nodeIndex($adnId): $count events marked as read.")
          case Failure(ex) =>
            warn(s"nodeIndex($adnId): Failed to mark node events as seen", ex)
        }
      }
    }

    // Рендерим конечный результат: страница или же инлайн
    for {
      evtsRndr  <- evtsRndrFut
      ctx       <- ctxFut
    } yield {
      val render: Html = if (inline)
        _eventsListTpl(evtsRndr)(ctx)
      else
        nodeIndexTpl(evtsRndr, request.mnode)(ctx)
      Ok(render)
    }
  }



  /**
    * Юзер нажал на кнопку выпиливания события.
    *
    * @param eventId id события.
    * @return 2хх если всё ок. Иначе 4xx.
    */
  def nodeEventDelete(eventId: String) = canAccessEvent.Post(eventId, onlyCloseable = true).async { implicit request =>
    for {
      isDeleted <- mEvents.deleteById(eventId)
    } yield {
      if (isDeleted)
        NoContent
      else
        NotFound(Messages("e.event.not.found"))
    }
  }

}
