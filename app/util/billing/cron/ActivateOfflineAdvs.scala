package util.billing.cron

import com.google.inject.Inject
import io.suggest.mbill2.m.item.status.MItemStatuses
import io.suggest.mbill2.m.item.typ.MItemType
import io.suggest.mbill2.m.item.{MItem, MItems}
import models.adv.build.TryUpdateBuilder
import models.mproj.ICommonDi
import slick.dbio.Effect.Read
import slick.profile.SqlAction
import util.adv.build.AdvBuilderFactory

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.02.16 14:18
  * Description: Система активации item'ов, стоящих в очереди на размещение.
  *
  * Используется инкрементальный метод, т.е. на карточку просто до-устанавливаются без очистки и прочего.
  */

class ActivateOfflineAdvs @Inject() (
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

  private def _findItems = {
    mItems.query
      .filter { i =>
        (i.statusStr === MItemStatuses.Offline.strId) &&
          (i.dateStartOpt <= now)
      }
  }

  override def findAdIds: StreamingDBIO[Traversable[String], String] = {
    // Ищем только карточки, у которых есть offline ads с dateStart < now
    _findItems
      .map(_.adId)
      .distinct
      .result
  }


  override def findItemsForAdId(adId: String, itypes: List[MItemType]): SqlAction[Iterable[MItem], NoStream, Read] = {
    // Нужно искать те же item'ы, что и в findAdIds, т.е. только оффлайновые и созревшие. Только в рамках карточки.
    _findItems
      .filter { i =>
        // TODO Opt не ясно, догадается ли оптимизатор постгреса искать по adId, если adId только во втором filter объявлен.
        (i.adId === adId) &&
          (i.iTypeStr inSet itypes.map(_.strId))
      }
      .result
  }


  override def tryUpdateAd(tuData0: TryUpdateBuilder, mitems: Iterable[MItem]): Future[TryUpdateBuilder] = {
    // Инкрементальный install всех необходимых item'ов на карточку.
    val acc00Fut = Future.successful(tuData0.acc)
    val b00 = advBuilderFactory
      .builder(acc00Fut, now)
      .prepareInstallNew(mitems)
    val b2 = mitems.foldLeft(b00)(_.install(_))
    for {
      acc2 <- b2.accFut
    } yield {
      TryUpdateBuilder(acc2)
    }
  }

}

/** guice factory для быстрой сборки экземпляров [[ActivateOfflineAdvs]]. */
trait ActivateOfflineAdvsFactory {
  def create(): ActivateOfflineAdvs
}

