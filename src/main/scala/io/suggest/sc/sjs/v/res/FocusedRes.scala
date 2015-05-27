package io.suggest.sc.sjs.v.res

import io.suggest.sc.ScConstants.Rsc._
import io.suggest.sc.sjs.m.mdom.MResContainers

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.05.15 16:09
 * Description: Контейнер для focused-ресурсов.
 */
object FocusedRes extends ResourceContainerT {

  override def id = FOCUSED_ID

  override def findContainer() = MResContainers.focusedResDiv()

}
