package util.billing.cron

import com.google.inject.Inject
import io.suggest.mbill2.m.item.status.MItemStatuses
import io.suggest.mbill2.m.item.typ.MItemType
import io.suggest.mbill2.m.item.{MItem, MItems}
import io.suggest.model.n2.node.MNodes
import models.adv.build.MCtxOuter
import models.mproj.ICommonDi
import slick.dbio.Effect.Read
import slick.profile.SqlAction
import util.adv.build.{AdvBuilderFactory, AdvBuilderUtil}
import util.adv.geo.tag.GeoTagsUtil

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.02.16 14:18
  * Description: Система активации item'ов, стоящих в очереди на размещение.
  *
  * Используется reinstall-метод, т.е. карточка сбрасывается и эджи компилятся и сохраняются заново.
  */

class ActivateOfflineAdvs @Inject() (
  advBuilderUtil                  : AdvBuilderUtil,
  geoTagsUtil                     : GeoTagsUtil,
  override val mNodes             : MNodes,
  override val mItems             : MItems,
  override val advBuilderFactory  : AdvBuilderFactory,
  override val mCommonDi          : ICommonDi
)
  extends AdvsUpdate
{

  import mCommonDi._
  import slick.driver.api._


  /** Окно обработки можно увеличить, т.к. тут инкрементальный апдейт и мало mitem'ов запрашивается. */
  override def MAX_ADS_PER_RUN = 20

  private def _offlineItemsSql(i: mItems.MItemsTable) = {
    (i.statusStr === MItemStatuses.Offline.strId) &&
      (i.dateStartOpt <= now)
  }

  override def findAdIds(max: Int): StreamingDBIO[Traversable[String], String] = {
    // Ищем только карточки, у которых есть offline ads с dateStart < now
    mItems.query
      .filter(_offlineItemsSql)
      .map(_.nodeId)
      .distinct
      .take(max)
      .result
  }


  /** Фьючерс внешнего контекста для adv-билдера. */
  override def builderCtxOuterFut: Future[MCtxOuter] = {
    val sql = mItems.query
      .filter(_offlineItemsSql)
    advBuilderUtil.prepareInstallNew(sql)
  }


  override def findItemsForAdId(adId: String, itypes: List[MItemType]): SqlAction[Iterable[MItem], NoStream, Read] = {
    // TODO Искать все item'ы: и начавшиеся оффлайн, и уже в онлайне которые.
    // Нужно искать те же item'ы, что и в findAdIds, т.е. только оффлайновые и созревшие. Только в рамках карточки.
    mItems.query
      .filter { i =>
        (i.nodeId === adId) &&
        (i.iTypeStr inSet itypes.map(_.strId)) && (
          _offlineItemsSql(i) || i.statusStr === MItemStatuses.Online.strId
        )
      }
      .result
  }


  override def tryUpdateAd(tuData0: TryUpdateBuilder, mitems: Iterable[MItem]): Future[TryUpdateBuilder] = {
    // Разделить item'ы на уже онлайновые и ещё пока оффлайновые.
    val offline = mitems.filter(_.status == MItemStatuses.Offline)

    // Инкрементальный install всех необходимых item'ов на карточку.
    val acc0Fut = Future.successful(tuData0.acc)
    val b2 = advBuilderFactory
      .builder(acc0Fut, now)
      .installSql(offline)
      .clearAd()
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

}

/** guice factory для быстрой сборки экземпляров [[ActivateOfflineAdvs]]. */
trait ActivateOfflineAdvsFactory {
  def create(): ActivateOfflineAdvs
}

