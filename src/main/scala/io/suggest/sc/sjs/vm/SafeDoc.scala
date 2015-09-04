package io.suggest.sc.sjs.vm

import io.suggest.sc.sjs.vm.util.EraseBg
import io.suggest.sjs.common.vm.{IVm, VmT}
import io.suggest.sjs.common.vm.doc.DocumentVm
import io.suggest.sjs.common.vm.overflow.OverflowT
import io.suggest.sjs.common.vm.wnd.WindowVm
import org.scalajs.dom.Node
import org.scalajs.dom.raw.HTMLBodyElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.05.15 10:30
 * Description: Статическая модель для безопасного доступа к некоторым полям модели DOM.
 */

// TODO Нужно капитально отрефакторить все эти vm'ки.
object SafeDoc
  extends DocumentVm()


object SafeWnd
  extends WindowVm()


object SafeBody extends VmT with OverflowT with EraseBg {

  override type T = HTMLBodyElement

  override def _underlying = SafeDoc.body

  def append(vm: IVm { type T <: Node } ): Unit = {
    _underlying.appendChild( vm._underlying )
  }
}