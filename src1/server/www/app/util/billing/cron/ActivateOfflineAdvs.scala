package util.billing.cron

import io.suggest.es.model.EsModel
import javax.inject.Inject
import io.suggest.mbill2.m.item.{MItem, MItems}
import io.suggest.mbill2.m.item.status.MItemStatuses
import io.suggest.model.n2.node.MNodes
import io.suggest.streams.StreamsUtil
import models.mproj.ICommonDi
import util.adv.build.{AdvBuilderFactory, AdvBuilderUtil}
import util.adv.geo.tag.GeoTagsUtil

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.02.16 14:18
  * Description: Система активации item'ов, стоящих в очереди на размещение.
  *
  * Используется reinstall-методика, т.е. карточка сбрасывается и эджи компилятся и сохраняются заново.
  */

class ActivateOfflineAdvs @Inject() (
                                      override val esModel            : EsModel,
                                      override val advBuilderUtil     : AdvBuilderUtil,
                                      override val geoTagsUtil        : GeoTagsUtil,
                                      override val mNodes             : MNodes,
                                      override val mItems             : MItems,
                                      override val advBuilderFactory  : AdvBuilderFactory,
                                      override val streamsUtil        : StreamsUtil,
                                      override val mCommonDi          : ICommonDi
                                    )
  extends ActivateAdvs
{

  import mCommonDi._
  import slick.profile.api._

  override def sqlInstallOnlyForItems(mitems: Iterable[MItem]): Iterable[MItem] = {
    super
      .sqlInstallOnlyForItems(mitems)
      .filter(_.status == MItemStatuses.Offline)
  }

  /** Ищем только карточки, у которых есть offline ads с dateStart < now. */
  override def _itemsSql(i: mItems.MItemsTable): Rep[Option[Boolean]] = {
    (i.statusStr === MItemStatuses.Offline.value) &&
      (i.dateStartOpt <= now)
  }

}

/** guice factory для быстрой сборки экземпляров [[ActivateOfflineAdvs]]. */
trait ActivateOfflineAdvsFactory {
  def create(): ActivateOfflineAdvs
}

