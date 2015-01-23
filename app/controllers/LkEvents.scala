package controllers

import models._
import models.event.MEvent
import play.twirl.api.Html
import util.PlayMacroLogsImpl
import util.acl.{HasNodeEventAccess, IsAdnNodeAdmin}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
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
    val eventsFut = MEvent.findByOwner(adnId, limit = limit, offset = offset)
    implicit val ctx = implicitly[Context]
    // Нужно отрендерить каждое событие с помощью соотв.шаблона. Для этого нужно собрать аргументы для каждого события.
    val evtsRndrFut = eventsFut.flatMap { mevents =>
      // Пакетно отфетчить рекламные карточки, создав карту оных.
      val allAdIds = mevents
        .iterator
        .flatMap { _.argsInfo.adIdOpt }
        .toSet
      val madsMapFut = MAd.multiGet(allAdIds)
        .map { _.iterator
          .flatMap { mad => mad.id.map { _ -> mad } }
          .toMap
        }

      // Пакетно отфетчить все необходимые ноды, исключая текущий узел.
      val allNodeIds = mevents
        .iterator
        .flatMap { _.argsInfo.adnIdOpt }
        .filter { _ != adnId }    // Текущую ноду фетчить не надо -- она уже в request лежит.
        .toSet
      val nodesMapFut = MAdnNodeCache.multiGet(allNodeIds)
        .map { allNodes =>
          // Добавить текущую ноду в финальную карту.
          (allNodes.iterator ++ Iterator(request.adnNode))
            .flatMap { adnNode => adnNode.id.map { _ -> adnNode } }
            .toMap
        }

      // TODO Пакетно отфетчить все необходимые MExtTarget

      // Когда все карты будут готовы, надо будет запустить рендер отфетченных событий в HTML.
      madsMapFut.flatMap { madsMap =>
        nodesMapFut.flatMap { nodesMap =>
          // Параллельный рендер всех событий
          Future.traverse(mevents.zipWithIndex) { case (mevent, i) =>
            Future {
              val rArgs = event.RenderArgs(
                mevent      = mevent,
                adnNodeOpt  = mevent.argsInfo.adnIdOpt.flatMap(nodesMap.get),
                madOpt      = mevent.argsInfo.adIdOpt.flatMap(madsMap.get)
              )
              mevent.etype.render(rArgs)(ctx) -> i
            }
          } map {
            // Восстанавливаем исходный порядок после параллельного рендера.
            _.sortBy(_._2).map(_._1)
          }
        }
      }
    }
    // Рендерим конечный результат: страница или инлайн
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
          NotFound("Already deleted?")
      }
  }

}
