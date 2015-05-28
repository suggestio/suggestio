package io.suggest.sc.sjs.m.msearch

import io.suggest.sc.sjs.m.mdom.GetDivById
import io.suggest.sc.ScConstants.Search._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.05.15 11:48
 * Description: Доступ к DOM-элементам панели поиска.
 */
object MSearchDom extends GetDivById {

  def rootDiv() = getDivById(ROOT_DIV_ID)

}
