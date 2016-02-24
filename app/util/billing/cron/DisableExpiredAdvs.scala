package util.billing.cron

import com.google.inject.Inject
import io.suggest.mbill2.m.item.status.MItemStatuses
import io.suggest.mbill2.m.item.{MItem, MItems}
import models.mproj.ICommonDi
import util.adv.build.{AdvBuilderFactory, IAdvBuilder}

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

  override def findItemsForProcessing = mItems.findCurrentForStatus( MItemStatuses.Online )

  override protected def _buildAction(b: IAdvBuilder, mitem: MItem): IAdvBuilder = {
    b.uninstall(mitem)
  }

}


trait DisableExpiredAdvsFactory {
  def create(): DisableExpiredAdvs
}

