package io.suggest.sc.sjs.m.msc

import io.suggest.sc.ScConstants.Layout._
import io.suggest.sc.sjs.vm.util.domvm.get.GetDivById

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.05.15 11:53
 * Description:
 */
@deprecated("Use vm.layout.* models instead", "23.jun.2015")
object MLayoutDom extends GetDivById {

  def rootDiv() = getDivById(ROOT_ID)

  def layoutDiv() = getDivById(LAYOUT_ID)

}
