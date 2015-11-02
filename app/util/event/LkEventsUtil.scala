package util.event

import com.google.inject.Inject
import io.suggest.model.es.{EsModelT, EsModelStaticT}
import io.suggest.playx.ICurrentApp
import models.adv.{MAdvStaticT, MAdvI}
import models.event.search.MEventsSearchArgs
import models.event._
import models._
import org.elasticsearch.client.Client
import org.joda.time.DateTime
import play.api.Application
import play.api.db.DB
import play.twirl.api.Html
import util.PlayMacroLogsDyn
import util.async.AsyncUtil

import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.01.15 10:25
 * Description: Утиль для контроллера LkEvents, который изрядно разжирел в первые дни разработки.
 */
class LkEventsUtil @Inject() (
  implicit val ec                 : ExecutionContext,
  implicit val esClient           : Client,
  override implicit val current   : Application
)
  extends PlayMacroLogsDyn
  with ICurrentApp
{

  /**
   * Если узел -- ресивер без геолокации, то надо отрендерить плашку на эту тему.
   * Этот метод вызывается контроллером при рендере начала списка событий.
   * @param mnode Узел.
   * @param ctx Контекст рендера.
   * @return Фьючерс, если есть чего рендерить.
   */
  def getGeoWelcome(mnode: MNode)(implicit ctx: Context): Future[Option[(Html, DateTime)]] = {
    val res = mnode.geo.shapes.size match {
      // Нет гео-шейпов у этого ресивера. Нужно отрендерить сообщение об этой проблеме. TODO Отсеивать просто-точки из подсчёта?
      case 0 =>
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
      case _ => None
    }
    Future successful res
  }

  /**
   * MultiGet-чтение одной из adv-моделей в карту (id -> [[MAdvI]]+).
   * Из модели извлечь только необходимые id'шники.
   * @param mevents Коллекция событий.
   * @param model Модель [[MAdvStatic]].
   * @param getIdF Функция для извлечения id из коллекции.
   * @tparam T1 Тип экземпляров модели, с которой работаем.
   * @return Фьючерс с результатами.
   */
  def readAdvModel[T1 <: MAdvI](mevents: Iterable[IEvent], model: MAdvStaticT {type T = T1})
                               (getIdF: IEvent => TraversableOnce[Int]): Future[Map[Int, T1]] = {
    val allAdvIdsIter = mevents
      .iterator
      .flatMap(getIdF)
    if (allAdvIdsIter.nonEmpty) {
      // Есть размещения, связанные с исходной коллекцией событий. Прочитать их из БД.
      val allAdvIds = allAdvIdsIter.toSet.toSeq
      val resFut = Future {
        DB.withConnection { implicit c =>
          model.multigetByIds(allAdvIds)
        }
      }(AsyncUtil.jdbcExecutionContext)
      // Завернуть результаты в карту:
      resFut.map {
        _.iterator
          .map { adv => adv.id.get -> adv }
          .toMap
      }
    } else {
      // Нечего искать.
      Future successful Map.empty
    }
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
    MEvent.updateAll(queryOpt = searchArgs.toEsQueryOpt) { mevent0 =>
      val res = mevent0.copy(
        isUnseen = false,
        ttlDays = Some(MEvent.TTL_DAYS_SEEN)
      )
      Future successful res
    }
  }

}
