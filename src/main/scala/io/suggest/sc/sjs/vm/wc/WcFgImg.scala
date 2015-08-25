package io.suggest.sc.sjs.vm.wc

import io.suggest.adv.ext.model.im.Size2di
import io.suggest.sc.sjs.vm.util.domvm.FindElT
import io.suggest.sc.sjs.vm.util.domvm.get.GetDivById
import io.suggest.sc.ScConstants.Welcome.FG_IMG_ID
import io.suggest.sjs.common.view.safe.SafeElT
import io.suggest.sjs.common.view.safe.css.DataWh
import org.scalajs.dom.raw.HTMLImageElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.08.15 12:05
 * Description: VM'ка для взаимодействия с картинкой переднего плана карточки приветствия.
 */
object WcFgImg extends FindElT with GetDivById {
  override def DOM_ID = FG_IMG_ID
  override type Dom_t = HTMLImageElement
  override type T = WcFgImg
}


trait WcFgImgT extends SafeElT with WcImgUtil with DataWh {

  override type T = HTMLImageElement

  def adjust(): Size2di = {
    val iwh = getDataWh.get
    val newWidth  = iwh.width  / 2
    val newHeight = iwh.height / 2
    val newSz = Size2di(newWidth, height = newHeight)
    val marginTopPx = - (newHeight + 50) / 2
    setImageWhMargin(newSz, marginTopPx)
    newSz
  }
}


case class WcFgImg(
  override val _underlying: HTMLImageElement
)
  extends WcFgImgT
{
  override lazy val getDataWh = super.getDataWh
}
