package io.suggest.sc.sjs.v.res

import io.suggest.sc.ScConstants.Rsc._
import io.suggest.sc.sjs.m.mdom.MResContainers

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.05.15 15:59
 * Description: Контейнер для common-ресурсов. Стили выдачи всякие.
 */
object CommonRes extends ResourceContainerT {

  override def id = COMMON_ID

  override def findContainer() = MResContainers.commonResDiv()

}
