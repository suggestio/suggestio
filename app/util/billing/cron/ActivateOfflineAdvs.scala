package util.billing.cron

import com.google.inject.Inject
import io.suggest.mbill2.m.item.status.MItemStatuses
import io.suggest.mbill2.m.item.{MItem, MItems}
import models.mproj.ICommonDi
import util.adv.build.{AdvBuilderFactory, IAdvBuilder}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.02.16 14:18
  * Description: Обновлялка adv sls, добавляющая уровни отображаения к существующей рекламе,
  * которая должна бы выйти в свет.
  */

class ActivateOfflineAdvs @Inject() (
  override val mItems             : MItems,
  override val advBuilderFactory  : AdvBuilderFactory,
  override val mCommonDi          : ICommonDi
)
  extends AdvsUpdate
{

  /** Экшен поточнго поиск id item'ов для обработки, сгруппированы по adId. */
  override def findItemsForProcessing = mItems.findCurrentForStatus( MItemStatuses.Offline )

  override protected def _buildAction(b: IAdvBuilder, mitem: MItem): IAdvBuilder = {
    b.install(mitem)
  }

}


/** guice factory для быстрой сборки экземпляров [[ActivateOfflineAdvs]]. */
trait ActivateOfflineAdvsFactory {
  def create(): ActivateOfflineAdvs
}
