package util.event

import io.suggest.model.{EsModelStaticT, EsModelT}
import io.suggest.ym.model.MAdnNodeGeo
import models.event.{MEvent, IEvent, ArgsInfo, MEventTmp}
import models._
import org.joda.time.DateTime
import play.api.db.DB
import play.twirl.api.Html
import util.PlayMacroLogsDyn
import util.SiowebEsUtil.client
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.async.AsyncUtil
import play.api.Play.current
import util.event.SiowebNotifier.Implicts.sn

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.01.15 10:25
 * Description: Утиль для контроллера LkEvents, который изрядно разжирел в первые дни разработки.
 */
object LkEventsUtil extends PlayMacroLogsDyn {

  /**
   * Если узел -- ресивер без геолокации, то надо отрендерить плашку на эту тему.
   * Этот метод вызывается контроллером при рендере начала списка событий.
   * @param adnNode Узел.
   * @param ctx Контекст рендера.
   * @return Фьючерс, если есть чего рендерить.
   */
  def getGeoWelcome(adnNode: MAdnNode)(implicit ctx: Context): Future[Option[(Html, DateTime)]] = {
    val adnId = adnNode.id.get
    MAdnNodeGeo.countByNode(adnId).map {
      // Нет гео-шейпов у этого ресивера. Нужно отрендерить сообщение об этой проблеме. TODO Отсеивать просто-точки из подсчёта?
      case 0 =>
        val etype = EventTypes.NodeGeoWelcome
        // Дата создания события формируется на основе даты создания узла.
        // Нужно также, чтобы это событие не было первым в списке событий, связанных с созданием узла.
        val dt = adnNode.meta.dateCreated.plusSeconds(10)
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
          adnNodeOpt    = Some(adnNode)
        )
        val html = etype.render(rargs)(ctx)
        Some(html -> dt)

      // Есть геошейпы для узла. Ничего рендерить не надо.
      case _ => None
    }
  }

  /**
   * MultiGet-чтение одной из adv-моделей в карту (id -> [[models.MAdvI]]+).
   * Из модели извлечь только необходимые id'шники.
   * @param mevents Коллекция событий.
   * @param model Модель [[models.MAdvStatic]].
   * @param getIdF Функция для извлечения id из коллекции.
   * @tparam T1 Тип экземпляров модели, с которой работаем.
   * @return Фьючерс с результатами.
   */
  def readAdvModel[T1 <: MAdvI](mevents: Iterable[IEvent], model: MAdvStatic {type T = T1})
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
   * Пометить все непрочитанные сообщения как прочитанные.
   * @param mevents События, экземпляры [[models.event.MEvent]].
   * @return Фьючерс для синхронизации.
   */
  def markUnseenAsSeen(mevents: Iterable[MEvent]): Future[_] = {
    val iter = mevents
      .iterator
      .filter(_.isUnseen)
    if (iter.nonEmpty) {
      Future.traverse(iter) { mevt =>
        val fut = MEvent.tryUpdate(mevt) {
          _.copy(isUnseen = false)
        }
        fut.onFailure { case ex => LOGGER.error("Failed to mark event as 'seen': " + mevt, ex)}
        fut
      }
    } else {
      Future successful Nil
    }
  }

}
