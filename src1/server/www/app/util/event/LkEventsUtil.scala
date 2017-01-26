package util.event

import java.time.OffsetDateTime

import com.google.inject.Inject
import io.suggest.model.es.{EsModelStaticT, EsModelT}
import models._
import models.event._
import models.event.search.MEventsSearchArgs
import models.mctx.Context
import models.mproj.ICommonDi
import play.twirl.api.Html

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.01.15 10:25
 * Description: Утиль для контроллера LkEvents, который изрядно разжирел в первые дни разработки.
 */
class LkEventsUtil @Inject() (
  mEvents   : MEvents,
  mCommonDi : ICommonDi
) {

  import mCommonDi._

  /**
   * Если узел -- ресивер без геолокации, то надо отрендерить плашку на эту тему.
   * Этот метод вызывается контроллером при рендере начала списка событий.
   * @param mnode Узел.
   * @param ctx Контекст рендера.
   * @return Фьючерс, если есть чего рендерить.
   */
  def getGeoWelcome(mnode: MNode)(implicit ctx: Context): Future[Option[(Html, OffsetDateTime)]] = {
    val nodeHasGss = mnode.edges
      .withPredicateIter( MPredicates.NodeLocation )
      .flatMap(_.info.geoShapes)
      .nonEmpty
    val res = if (!nodeHasGss) {
      // Нет гео-шейпов у этого ресивера. Нужно отрендерить сообщение об этой проблеме. TODO Отсеивать просто-точки из подсчёта?
      val etype = MEventTypes.NodeGeoWelcome
      // Дата создания события формируется на основе даты создания узла.
      // Нужно также, чтобы это событие не было первым в списке событий, связанных с созданием узла.
      val dt = mnode.meta.basic
        .dateCreated
        .plusSeconds(10)
      val nodeId = mnode.id.get
      val mevt = MEventTmp(
        etype       = etype,
        ownerId     = nodeId,
        argsInfo    = ArgsInfo(adnIdOpt = Some(nodeId)),
        isCloseable = false,
        isUnseen    = true,
        id          = Some(nodeId),
        dateCreated = dt
      )
      val rargs = event.RenderArgs(
        mevent        = mevt,
        withContainer = true,
        adnNodeOpt    = Some(mnode)
      )
      val html = etype.render(rargs)(ctx)
      Some(html -> dt)

      // Есть геошейпы для узла. Ничего рендерить не надо.
    } else {
      None
    }
    Future successful res
  }


  /**
   * MultiGet-чтение одной из ES-моделей.
   * @param mevents Список событий.
   * @param model Статическая модель
   * @param getIdF Извлекатель id из события.
   * @tparam T1 Тип экземпляров модели.
   * @return Фьючерс с картой вида (id:String -> EsModelT+).
   */
  def readEsModel[T1 <: EsModelT](mevents: Iterable[IEvent], model: EsModelStaticT {type T = T1})
                                 (getIdF: IEvent => TraversableOnce[String]): Future[Map[String, T1]] = {
    val allIds = mevents
      .iterator
      .flatMap(getIdF)
      .toSet
    model.multiGetMap(allIds)
  }


  /**
   * Для указанного узла отметить все сообщения как прочитанные.
   * Т.к. событий может быть очень много, используем search scroll + put.
   * @param adnId id узла.
   * @return Фьючерс с кол-вом обновлённых элементов.
   */
  def markAllSeenForNode(adnId: String): Future[Int] = {
    val searchArgs = MEventsSearchArgs(
      ownerId = Some(adnId),
      onlyUnseen = true
    )
    val scroller = mEvents.startScroll(
      queryOpt = searchArgs.toEsQueryOpt
    )
    mEvents.updateAll(scroller) { mevent0 =>
      val res = mevent0.copy(
        isUnseen = false,
        ttlDays = Some(MEvent.TTL_DAYS_SEEN)
      )
      Future.successful(res)
    }
  }

}
