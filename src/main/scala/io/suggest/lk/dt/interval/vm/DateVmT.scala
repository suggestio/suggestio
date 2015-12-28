package io.suggest.lk.dt.interval.vm

import io.suggest.sjs.common.vm.find.FindElT
import org.scalajs.dom.raw.HTMLInputElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.12.15 17:54
 * Description: Инпуты дат в случае задания произвольного периода размещения.
 */
trait DateStaticVmT extends FindElT {

  override type Dom_t = HTMLInputElement

}


trait DateVmT extends InitLayoutFsmChange {

  override type T = HTMLInputElement

}
