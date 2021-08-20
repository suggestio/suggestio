package util.billing.cron

import io.suggest.mbill2.m.item.MItem
import io.suggest.mbill2.m.item.status.MItemStatuses
import play.api.inject.Injector

import javax.inject.Inject

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.02.16 14:18
  * Description: Система активации item'ов, стоящих в очереди на размещение.
  *
  * Используется reinstall-методика, т.е. карточка сбрасывается и эджи компилятся и сохраняются заново.
  */
final class ActivateOfflineAdvs @Inject() (
                                            override val injector: Injector,
                                          )
  extends ActivateAdvs
{

  import slickHolder.slick
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

