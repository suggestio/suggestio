package io.suggest.sc.sjs.vm.wc

import io.suggest.adv.ext.model.im.ISize2di
import io.suggest.common.css.CssSzImplicits
import io.suggest.sc.ScConstants.Welcome.FG_INFO_DIV_ID
import io.suggest.sjs.common.vm.VmT
import io.suggest.sjs.common.vm.find.FindDiv
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.08.15 12:07
 * Description: VM'ка для взаимодействия с надписью переднего плана.
 */
object WcFgInfo extends FindDiv {
  override def DOM_ID = FG_INFO_DIV_ID
  override type T = WcFgInfo
}


trait WcFgInfoT extends VmT with CssSzImplicits {

  override type T = HTMLDivElement

  def adjust(fgImgSz: ISize2di): Unit = {
    _underlying.style.marginTop = (fgImgSz.height / 2).px
  }

}


case class WcFgInfo(
  override val _underlying: HTMLDivElement
)
  extends WcFgInfoT
