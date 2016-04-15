package util.billing.cron

import com.google.inject.Inject
import io.suggest.mbill2.m.item.status.MItemStatuses
import io.suggest.mbill2.m.item.typ.MItemType
import io.suggest.mbill2.m.item.{MItem, MItems}
import io.suggest.model.n2.node.MNodes
import models.adv.build.MCtxOuter
import models.mproj.ICommonDi
import org.joda.time.Interval
import slick.dbio.Effect.Read
import slick.profile.SqlAction
import util.adv.build.{AdvBuilderFactory, AdvBuilderUtil}

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.02.16 14:17
  * Description: Обновлялка adv sls, которая снимает уровни отображения с имеющейся рекламы,
  * которая должна уйти из выдачи по истечению срока размещения.
  */

class DisableExpiredAdvs @Inject() (
  advBuilderUtil                  : AdvBuilderUtil,
  override val mNodes             : MNodes,
  override val mCommonDi          : ICommonDi,
  override val advBuilderFactory  : AdvBuilderFactory,
  override val mItems             : MItems
)
  extends AdvsUpdate
{

  import LOGGER._
  import mCommonDi._
  import slick.driver.api._

  private def _expiredItemsSql(i: mItems.MItemsTable) = {
    (i.statusStr === MItemStatuses.Online.strId) &&
      (i.dateEndOpt <= now)
  }

  override def findAdIds(max: Int): StreamingDBIO[Traversable[String], String] = {
    mItems.query
      .filter(_expiredItemsSql)
      .map(_.adId)
      .distinct
      // Избегаем скачка слишком резкой нагрузки, ограничивая кол-во обрабатываемых карточек.
      .take(max)
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


  override def builderCtxOuterFut: Future[MCtxOuter] = {
    advBuilderUtil.prepareUnInstall {
      mItems.query
        .filter(_expiredItemsSql)
    }
  }


  private def _isExpired(ivl: Interval): Boolean = {
    // используем !isAfter вместо isBefore, т.к. при тестировании как-то удалось попасть пальцем в небо.
    !ivl.getEnd.isAfter(now)
  }

  override def findItemsForAdId(adId: String, itypes: List[MItemType]): SqlAction[Iterable[MItem], NoStream, Read] = {
    // Собираем ВСЕ online-item'ы: и истекшие, и ещё нет.
    // Все нужны для ребилда самой карточки. Истекшие нужны для проведения истчения в биллинге.
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


  override def run(): Future[Int] = {
    val runFut = super.run()
    // Необходимо отребилдить теги, затронутые изменениями в item'ах.
    for (_ <- runFut) {
      advBuilderUtil.afterUnInstall(_builderCtxOuterFut)
    }
    // Вернуть исходный фьючерс, т.к. ребилд может длиться долго и закончится крэшем.
    runFut
  }
}


/** Интерфейс Guice-factory, возвращающая инстансы [[DisableExpiredAdvs]]. */
trait DisableExpiredAdvsFactory {
  def create(): DisableExpiredAdvs
}

