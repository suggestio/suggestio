package io.suggest.sjs.dt.period.vm

import io.suggest.sjs.common.fsm.{SjsFsm, InitLayoutFsmChange}
import io.suggest.sjs.common.vm.find.FindElT
import io.suggest.sjs.jqdtpick._
import org.scalajs.dom.raw.HTMLInputElement

import org.scalajs.jquery.jQuery

import scala.scalajs.js.Date

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

  def initDtPicker(fsm: SjsFsm, args: Options): Unit = {
    args.onSelectDate = {(ct: Date, input: JqDtPicker) =>
      // TODO null этот нужно убрать отсюда, придумав иной метод передачи сообщения без event.
      fsm !! _changeSignalModel(null)
    }
    jQuery(_underlying)
      .datetimepicker(args)
  }

}
