package io.suggest.sc.sjs.vm.wc

import io.suggest.common.geom.d2.Size2di
import io.suggest.dev.MScreen
import io.suggest.sc.ScConstants.Welcome.BG_IMG_ID
import io.suggest.sjs.common.vm.VmT
import io.suggest.sjs.common.vm.attr.DataWh
import io.suggest.sjs.common.vm.find.FindElT
import org.scalajs.dom.raw.HTMLImageElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.08.15 11:59
 * Description: VM'ка для взаимодействия с фоновой картинкой карточки приветствия.
 */
object WcBgImg extends FindElT {
  override def DOM_ID = BG_IMG_ID
  override type Dom_t = HTMLImageElement
  override type T = WcBgImg
}


trait WcBgImgT extends VmT with WcImgUtil with DataWh {

  override type T = HTMLImageElement

  /** Подогнать картинку под экран текущего устройства. */
  def adjust(screen: MScreen): Unit = {
    val iwh = getDataWh.get
    val newWh: Size2di = {
      if (iwh.width.toDouble / iwh.height.toDouble < screen.width.toDouble / screen.height.toDouble) {
        val w = screen.width
        val h = w * iwh.height / iwh.width
        Size2di(w, height = h)
      } else {
        val h = screen.height
        val w = h * iwh.width / iwh.height
        Size2di(w, height = h)
      }
    }
    val marginTopPx = - newWh.height / 2
    setImageWhMargin(newWh, marginTopPx)
  }

}


case class WcBgImg(
  override val _underlying: HTMLImageElement
)
  extends WcBgImgT
