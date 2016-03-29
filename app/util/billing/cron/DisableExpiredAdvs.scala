package util.billing.cron

import com.google.inject.Inject
import io.suggest.mbill2.m.item.status.MItemStatuses
import io.suggest.mbill2.m.item.typ.MItemType
import io.suggest.mbill2.m.item.{MItem, MItems}
import models.adv.build.{Acc, TryUpdateBuilder}
import models.mproj.ICommonDi
import org.joda.time.Interval
import slick.dbio.Effect.Read
import slick.profile.SqlAction
import util.adv.build.AdvBuilderFactory

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.02.16 14:17
  * Description: Обновлялка adv sls, которая снимает уровни отображения с имеющейся рекламы,
  * которая должна уйти из выдачи по истечению срока размещения.
  */

class DisableExpiredAdvs @Inject() (
  override val mCommonDi          : ICommonDi,
  override val advBuilderFactory  : AdvBuilderFactory,
  override val mItems             : MItems
)
  extends AdvsUpdate
{

  import LOGGER._
  import mCommonDi._
  import slick.driver.api._


  override def findAdIds: StreamingDBIO[Traversable[String], String] = {
    mItems.query
      .filter { i =>
        (i.statusStr === MItemStatuses.Online.strId) &&
          (i.dateEndOpt <= now)
      }
      .map(_.adId)
      .distinct
      // Избегаем скачка слишком резкой нагрузки, ограничивая кол-во обрабатываемых карточек.
      .take(MAX_ADS_PER_RUN)
      .result
  }


  override def hasItemsForProcessing(mitems: Iterable[MItem]): Boolean = {
    super.hasItemsForProcessing(mitems) && {
      // Если есть хотя бы один item с истекшей dateEnd, то можно продолжать обработку.
      // TODO Есть ложные срабатывания. Возможно, есть проблема с десериализацией прямо в joda.interval.
      val res = mitems
        .flatMap(_.dtIntervalOpt)
        .exists(_isExpired)
      trace(s"hasItemsForProcessing(${mitems.size}): $res")
      res
    }
  }


  private def _isExpired(ivl: Interval): Boolean = {
    // используем !isAfter вместо isBefore, т.к. при тестировании как-то удалось попасть пальцем в небо.
    !ivl.getEnd.isAfter(now)
  }

  override def findItemsForAdId(adId: String, itypes: List[MItemType]): SqlAction[Iterable[MItem], NoStream, Read] = {
    // Собираем все item'ы: и истекшие, и ещё нет. Это нужно для ребилда карточки без каких-то item'ов.
    mItems.query
      .filter { i =>
        // Ищем item'ы для картоки в online-состоянии.
        (i.adId === adId) &&
          (i.statusStr === MItemStatuses.Online.strId) &&
          (i.iTypeStr inSet itypes.map(_.strId))
      }
      .result
  }


  override def tryUpdateAd(tuData0: TryUpdateBuilder, mitems: Iterable[MItem]): Future[TryUpdateBuilder] = {
    // mitems содержит одновременно разные по сути item'ы: истекшие и ещё нет.
    // На основе истекших надо собрать SQL для обновления биллинга.
    // На основе остальных (не истекших) -- пересобрать размещения в карточке и сохранить карточку.
    // Поэтому нужно два билдера-аккамулятора.
    val acc00Fut = Future.successful(tuData0.acc)

    // Проходим билдерами по mitems, вызывая ту или иную логику в зависимости от того, истёк item или же нет.
    val (expired, rest) = mitems.partition { i =>
      i.dtIntervalOpt.exists(_isExpired)
    }

    val b2 = advBuilderFactory
      .builder(acc00Fut, now)
      // Подготовить SQL для деинсталляции
      .unInstallSql(expired)
      // Для пересборки размещений карточки нужно сначала очистить текущие размещения карточки:
      .clearAd()
      .installNode(rest)

    // Объеденить два аккамулятора в финальный акк, возвращаемый наверх.
    for {
      acc2 <- b2.accFut
    } yield {
      TryUpdateBuilder(acc2)
    }
  }

}


/** Интерфейс Guice-factory, возвращающая инстансы [[DisableExpiredAdvs]]. */
trait DisableExpiredAdvsFactory {
  def create(): DisableExpiredAdvs
}

