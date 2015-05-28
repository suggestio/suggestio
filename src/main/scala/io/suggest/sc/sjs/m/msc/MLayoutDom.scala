package io.suggest.sc.sjs.m.msc

import io.suggest.sc.sjs.m.mdom.GetDivById
import io.suggest.sc.ScConstants.Layout._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.05.15 11:53
 * Description:
 */
object MLayoutDom extends GetDivById {

  def rootDiv() = getDivById(ROOT_ID)

  def layoutDiv() = getDivById(LAYOUT_ID)

}
