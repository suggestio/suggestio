package io.suggest.sjs.common.vm.of

import scala.scalajs.js

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.01.16 13:22
 * Description: Утиль для of-окружения.
 */
object OfUtil {

  def isInstance(v: js.Any): Boolean = {
    v != null && !js.isUndefined(v)
  }

}
