package io.suggest.sc.sjs.vm

import io.suggest.sc.sjs.vm.util.domvm.EraseBg
import io.suggest.sjs.common.view.safe.SafeElT
import io.suggest.sjs.common.view.safe.doc.SafeDocument
import io.suggest.sjs.common.view.safe.overflow.OvfHiddenT
import io.suggest.sjs.common.view.safe.wnd.SafeWindow
import org.scalajs.dom.raw.HTMLBodyElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.05.15 10:30
 * Description: Статическая модель для безопасного доступа к некоторым полям модели DOM.
 */
object SafeDoc
  extends SafeDocument()


object SafeWnd
  extends SafeWindow()


object SafeBody extends SafeElT with OvfHiddenT with EraseBg {

  override type T = HTMLBodyElement

  override def _underlying = SafeDoc.body

}