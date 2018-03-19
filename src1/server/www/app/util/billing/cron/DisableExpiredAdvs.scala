package util.billing.cron

import javax.inject.Inject
import io.suggest.mbill2.m.item.status.MItemStatuses
import io.suggest.mbill2.m.item.typ.MItemType
import io.suggest.mbill2.m.item.{MItem, MItems}
import io.suggest.model.n2.node.MNodes
import models.adv.build.MCtxOuter
import models.mproj.ICommonDi
import org.threeten.extra.Interval
import slick.dbio.Effect.Read
import slick.sql.SqlAction
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
  import slick.profile.api._


  override def _itemsSql(i: mItems.MItemsTable): Rep[Option[Boolean]] = {
    (i.statusStr === MItemStatuses.Online.value) &&
      (i.dateEndOpt <= now)
  }


  override def hasItemsForProcessing(mitems: Iterable[MItem]): Boolean = {
    super.hasItemsForProcessing(mitems) && {
      // Если есть хотя бы один item с истекшей dateEnd, то можно продолжать обработку.
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
        .filter(_itemsSql)
    }
  }


  private def _isExpired(ivl: Interval): Boolean = {
    // используем !isAfter вместо isBefore, т.к. при тестировании как-то удалось попасть пальцем в небо.
    !ivl.getEnd
      .isAfter( nowInstant )
  }

  val nowInstant = now.toInstant

  override def findItemsForAdId(adId: String, itypes: List[MItemType]): SqlAction[Iterable[MItem], NoStream, Read] = {
    // Собираем ВСЕ online-item'ы: и истекшие, и ещё нет.
    // Все нужны для ребилда самой карточки. Истекшие нужны для проведения истчения в биллинге.
    mItems.query
      .filter { i =>
        // Ищем item'ы для картоки в online-состоянии.
        (i.nodeId === adId) &&
          (i.statusStr === MItemStatuses.Online.value) &&
          (i.iTypeStr inSet itypes.map(_.value))
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
      .clearNode()
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

  override def purgeItemStatus = MItemStatuses.Finished

}


/** Интерфейс Guice-factory, возвращающая инстансы [[DisableExpiredAdvs]]. */
trait DisableExpiredAdvsFactory {
  def create(): DisableExpiredAdvs
}

