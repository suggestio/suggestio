package io.suggest.sjs.common.view.safe.scroll

import io.suggest.sjs.common.view.safe.ISafe
import org.scalajs.dom.Window

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.06.15 16:53
 * Description: Перемотка окна наверх.
 */
trait ScrollTop extends ISafe {

  override type T <: Window

  def scrollTop(): Unit = {
    _underlying.scrollTo(0, 0)
  }

}
