package io.suggest.sc.sjs.vm.layout

import io.suggest.sc.ScConstants.Layout
import io.suggest.sc.sjs.m.magent.IMScreen
import io.suggest.sc.sjs.vm.util.domvm.FindDiv
import io.suggest.sc.sjs.vm.util.domvm.create.{CreateDiv, CreateVm}
import io.suggest.sjs.common.view.safe.SafeElT
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.06.15 9:58
 * Description: Модель доступа к div sioMartLayout.
 */
object LayContentVm extends FindDiv with CreateDiv with CreateVm {

  override type T = LayContentVm
  override protected def DOM_ID = Layout.LAYOUT_ID

}


/** Абстрактная реализация модели. */
trait LayContentVmT extends SafeElT {

  override type T = HTMLDivElement

  /** Выставить css-класс отображения для layoutDiv. */
  def setWndClass(scrWh: IMScreen): Unit = {
    setWndClass(scrWh.width)
  }
  def setWndClass(w: Int): Unit = {
    val cssClassOrNull: String = {
      if (w <= 660) {
        "sm-w-400"
      } else if (w <= 800) {
        "sm-w-800"
      } else if (w <= 980) {
        "sm-w-980"
      } else {
        null
      }
    }
    if (cssClassOrNull != null) {
      _underlying.className = cssClassOrNull
    }
  }

  def setIndexHtml(html: String): Unit = {
    _underlying.innerHTML = html
  }

}


case class LayContentVm(
  override val _underlying: HTMLDivElement
) extends LayContentVmT
