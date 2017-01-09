package io.suggest.sjs.jqdtpick

import scala.scalajs.js
import scala.scalajs.js.UndefOr

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 01.02.16 14:19
 * Description: Init options for xdsoft jquery dateTimePicker.
 * @see [[http://xdsoft.net/jqplugins/datetimepicker]]
 */
@js.native
trait Options extends js.Object {

  var datepicker: Boolean = js.native

  var timepicker: Boolean = js.native

  var allowTimes: js.Array[String] = js.native

  var format: String = js.native

  var inline: Boolean = js.native

  var lang: String = js.native

  var startDate: String = js.native

  var minDate: String = js.native

  var maxDate: String = js.native

  var formatDate: String = js.native

  var formatTime: String = js.native

  var mask: Boolean = js.native

  var weekends: js.Array[String] = js.native

  var lazyInit: Boolean = js.native

  var value: String = js.native

  var step: Int = js.native

  var closeOnDateSelect: Boolean = js.native

  var closeOnWithoutClick: Boolean = js.native

  var validateOnBlur: Boolean = js.native

  var weeks: Boolean = js.native

  var theme: String = js.native

  var defaultDate: String = js.native

  var defaultTime: UndefOr[String] = js.native

  var opened: Boolean = js.native

  var yearOffset: Int = js.native

  var todayButton: Boolean = js.native

  var defaultSelect: Boolean = js.native

  var allowBlank: Boolean = js.native

  var timepickerScrollbar: Boolean = js.native


  var onSelectDate: Callback_t = js.native

  var onSelectTime: Callback_t = js.native

  var onChangeMonth: Callback_t = js.native

  var onChangeYear: Callback_t = js.native

  var onChangeDateTime: Callback_t = js.native

  var onShow: Callback_t = js.native

  var onClose: Callback_t = js.native

  var onGenerate: Callback_t = js.native


  var withoutCopyright: Boolean = js.native

  var inverseButton: Boolean = js.native

  var scrollMonth: Boolean = js.native

  var scrollTime: Boolean = js.native

  var scrollInput: Boolean = js.native

  var hours12: Boolean = js.native

  var yearStart: Int = js.native
  var yearEnd: Int = js.native

  var roundTime: String = js.native

  /** @see [[Constants.Days]] */
  var dayOfWeekStart: Int = js.native

  var className: String = js.native

  var id: String = js.native

  var style: String = js.native

}
