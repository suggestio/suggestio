package util.billing.cron

import io.suggest.mbill2.m.item.MItem
import io.suggest.mbill2.m.item.status.MItemStatuses
import io.suggest.mbill2.m.item.typ.MItemType
import models.adv.build.MCtxOuter
import slick.dbio.Effect.Read
import slick.sql.SqlAction
import util.adv.build.IAdvBuilderUtilDi
import util.adv.geo.tag.IGeoTagsUtilDi

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.03.18 18:31
  * Description: Абстрактный код системы активации размещений.
  */
abstract class ActivateAdvs
  extends AdvsUpdate
  with IAdvBuilderUtilDi
  with IGeoTagsUtilDi
{

  import mCommonDi._
  import slick.profile.api._

  /** Фьючерс внешнего контекста для adv-билдера. */
  override def builderCtxOuterFut: Future[MCtxOuter] = {
    val sql = mItems.query
      .filter(_itemsSql)
    advBuilderUtil.prepareInstallNew(sql)
  }


  override def findItemsForAdId(adId: String, itypes: List[MItemType]): SqlAction[Iterable[MItem], NoStream, Read] = {
    // TODO Искать все item'ы: и начавшиеся оффлайн, и уже в онлайне которые.
    // Нужно искать те же item'ы, что и в findAdIds, т.е. только оффлайновые и созревшие. Только в рамках карточки.
    mItems.query
      .filter { i =>
        (i.nodeId === adId) &&
        (i.iTypeStr inSet itypes.map(_.value)) && (
          _itemsSql(i) || i.statusStr === MItemStatuses.Online.value
        )
      }
      .result
  }

  def sqlInstallOnlyForItems(mitems: Iterable[MItem]): Iterable[MItem] = {
    // Т.к. findItemsForAdId() возвращает все item'ы, а бывает, что на обработку интересуют только Offline-item'ы, без Online.
    // И тут можно профильтровать.
    mitems
  }

  override def tryUpdateAd(tuData0: TryUpdateBuilder, mitems: Iterable[MItem]): Future[TryUpdateBuilder] = {
    // Инкрементальный install всех необходимых item'ов на карточку.
    val b2 = advBuilderFactory
      .builder(
        acc0Fut = Future.successful(tuData0.acc),
        now     = now
      )
      .installSql(
        // Разделить item'ы на уже онлайновые и ещё пока оффлайновые.
        sqlInstallOnlyForItems(mitems)
      )
      .clearNode()
      .installNode(mitems)

    for {
      acc2 <- b2.accFut
    } yield {
      TryUpdateBuilder(acc2)
    }
  }


  override def run(): Future[Int] = {
    val runFut = super.run()
    // Необходимо произвести изменения, связанные с общими данными.
    for (_ <- runFut) {
      advBuilderUtil.afterInstallNew(_builderCtxOuterFut)
    }
    // Вернуть всё-таки исходный фьючерс, т.к. в ребилд тегов может идти какое-то время.
    runFut
  }

  override def purgeItemStatus = MItemStatuses.Refused

}

