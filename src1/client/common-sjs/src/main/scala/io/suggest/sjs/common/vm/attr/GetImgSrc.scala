package io.suggest.sjs.common.vm.attr

import io.suggest.sjs.common.vm.IVm
import org.scalajs.dom.raw.HTMLImageElement

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.11.15 19:30
  * Description: Поддержка опционального доступа к значению img.src.
  */
trait GetImgSrc extends IVm {

  override type T <: HTMLImageElement

  def srcOpt: Option[String] = {
    val src0 = _underlying.src
    if (src0 != null && src0.nonEmpty) {
      Some(src0)
    } else {
      None
    }
  }

}
