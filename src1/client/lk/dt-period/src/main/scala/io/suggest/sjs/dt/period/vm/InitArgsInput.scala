package io.suggest.sjs.dt.period.vm

import io.suggest.dt.interval.DatesIntervalConstants
import io.suggest.sjs.common.vm.attr.JsonStringInputValueT
import io.suggest.sjs.common.vm.find.FindElT
import io.suggest.sjs.common.vm.rm.SelfRemoveT
import io.suggest.sjs.jqdtpick.Options
import org.scalajs.dom.raw.HTMLInputElement

import scala.scalajs.js

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.01.16 11:48
 * Description: Аргументы инициализации datetimepicker рендерятся в шаблоне в виде JSON.
 * Это необходимо, т.к. есть связь с локалью и прочими server-side вещами.
 */
object InitArgsInput extends FindElT {
  override type T       = InitArgsInput
  override type Dom_t   = HTMLInputElement
  override def DOM_ID   = DatesIntervalConstants.INIT_ARGS_INPUT_ID
}


import InitArgsInput.Dom_t


trait InitArgsInputT extends JsonStringInputValueT with SelfRemoveT {

  override type JsonVal_t = Options
  override type T = Dom_t

}


case class InitArgsInput(override val _underlying: Dom_t)
  extends InitArgsInputT
