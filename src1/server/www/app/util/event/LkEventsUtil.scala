package util.event

import com.google.inject.Inject
import io.suggest.es.model.{EsModelStaticT, EsModelT}
import models.event._
import models.event.search.MEventsSearchArgs

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.01.15 10:25
 * Description: Утиль для контроллера LkEvents, который изрядно разжирел в первые дни разработки.
 */
class LkEventsUtil @Inject() (
  mEvents   : MEvents
) {


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
