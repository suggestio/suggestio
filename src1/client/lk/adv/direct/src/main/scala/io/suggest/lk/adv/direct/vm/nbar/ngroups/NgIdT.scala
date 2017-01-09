package io.suggest.lk.adv.direct.vm.nbar.ngroups

import io.suggest.adv.direct.AdvDirectFormConstants
import io.suggest.sjs.common.vm.attr.AttrVmT

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.12.15 18:05
 * Description: Быстрый доступ к id категории групп нод.
 */
trait NgIdT extends AttrVmT {

  def ngId = getAttribute( AdvDirectFormConstants.ATTR_CAT_ID )

}
