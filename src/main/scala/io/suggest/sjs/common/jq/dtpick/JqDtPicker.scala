package io.suggest.sjs.common.jq.dtpick

import org.scalajs.jquery.JQuery

import scala.scalajs.js
import scala.language.implicitConversions

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.01.16 10:26
 * Description: Поддержка xdsoft dateTimePicker для scala.js.
 */
@js.native
sealed trait JqDtPicker extends JQuery {

  /** Инициализация выборки даты. */
  def datetimepicker(args: js.Object = js.native): JqDtPicker = js.native

}


object JqDtPicker {

  /** Прозрачное приведение из инстанса jQuery. */
  implicit def fromJq(jQuery: JQuery): JqDtPicker = {
    jQuery.asInstanceOf[JqDtPicker]
  }

}
