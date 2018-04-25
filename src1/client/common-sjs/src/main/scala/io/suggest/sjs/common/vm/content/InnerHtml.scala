package io.suggest.sjs.common.vm.content

import io.suggest.sjs.common.vm.IVm
import org.scalajs.dom.Element

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.08.15 18:23
 * Description: Быстрый доступ к частоиспользуемым вещам, связанным с innerHtml.
 */

trait SetInnerHtml extends IVm {

  override type T <: Element

  def setContent(html: String): Unit = {
    _underlying.innerHTML = html
  }
}
