package util.billing.cron

import io.suggest.es.model.EsModel
import javax.inject.Inject
import io.suggest.mbill2.m.item.{MItem, MItems}
import io.suggest.mbill2.m.item.status.MItemStatuses
import io.suggest.n2.node.MNodes
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
final class ActivateOfflineAdvs @Inject() (
                                            override val mCommonDi          : ICommonDi
                                          )
  extends ActivateAdvs
{

  import mCommonDi.current.injector
  override lazy val esModel = injector.instanceOf[EsModel]
  override lazy val advBuilderUtil = injector.instanceOf[AdvBuilderUtil]
  override lazy val geoTagsUtil = injector.instanceOf[GeoTagsUtil]
  override lazy val mNodes = injector.instanceOf[MNodes]
  override lazy val mItems = injector.instanceOf[MItems]
  override lazy val advBuilderFactory = injector.instanceOf[AdvBuilderFactory]
  override lazy val streamsUtil = injector.instanceOf[StreamsUtil]


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

