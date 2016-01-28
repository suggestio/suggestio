package io.suggest.lk.dt.interval.vm

import io.suggest.sjs.common.vm.find.FindElT
import org.scalajs.dom.raw.HTMLInputElement

import scala.scalajs.js
import org.scalajs.jquery.jQuery
import io.suggest.sjs.common.jq.dtpick.JqDtPicker._

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

  def valueOpt = Option(_underlying.value)

  def initDtPicker(args: js.Object): Unit = {
    jQuery(_underlying)
      .datetimepicker(args)
  }

}
