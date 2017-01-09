package io.suggest.sjs.jqdtpick

import org.scalajs.jquery.JQuery

import scala.language.implicitConversions
import scala.scalajs.js

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.01.16 10:26
 * Description: Поддержка xdsoft dateTimePicker для scala.js.
 */
@js.native
sealed trait JqDtPicker extends JQuery {

  /** Initialize datepicker. */
  def datetimepicker(options: Options = js.native): JqDtPicker = js.native

}
