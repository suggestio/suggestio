package io.suggest.sc.sjs.vm.res

import io.suggest.sc.ScConstants.Rsc._
import io.suggest.sc.sjs.vm.layout.LayContentVm
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.05.15 15:59
 * Description: Контейнер для common-ресурсов. Стили выдачи всякие.
 */
object CommonRes extends ResStaticT {

  override type T = CommonRes
  override def DOM_ID = COMMON_ID

  override protected def _insertDiv(lay: LayContentVm, div: HTMLDivElement): Unit = {
    lay.insertFirst(div)
  }
}


case class CommonRes(
  override val _underlying: HTMLDivElement
) extends ResT
