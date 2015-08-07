package io.suggest.sc.sjs.vm.util.height3

import io.suggest.sjs.common.view.safe.ISafe
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.08.15 17:01
 * Description:
 */
trait IRootEl extends ISafe {

  override type T <: HTMLDivElement
  protected type Wrapper_t <: IWrapperEl

  def wrapper: Option[Wrapper_t]

}

trait IWrapperEl extends ISafe {

}


