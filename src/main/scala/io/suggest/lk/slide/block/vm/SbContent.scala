package io.suggest.lk.slide.block.vm

import io.suggest.common.slide.block.SbConstants
import io.suggest.sjs.common.view.VUtil
import io.suggest.sjs.common.vm.VmT
import io.suggest.sjs.common.vm.style.ShowHideDisplayT
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.12.15 16:56
 * Description: Тело slide-блока.
 */

object SbContent {

  type Dom_t = HTMLDivElement

  /**
   * Найти ближайший slide block content div к указанному элементу.
   * @param vm Текущая view-model.
   * @return Ближайший [[SbContent]] наверх по дереву, если есть.
   */
  def of(vm: VmT): Option[SbContent] = {
    for (vmCnt <- VUtil.hasCssClass(vm, SbConstants.CONTENT_CLASS)) yield {
      val vmCnt2 = vmCnt._underlying.asInstanceOf[Dom_t]
      apply(vmCnt2)
    }
  }

}


import io.suggest.lk.slide.block.vm.SbContent.Dom_t


trait SbContentT extends ShowHideDisplayT {
  override type T = Dom_t
}


case class SbContent(override val _underlying: Dom_t)
  extends SbContentT
