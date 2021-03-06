package com.github.hacker0x01.react.date.picker

import org.scalajs.dom.Element
import japgolly.scalajs.react.{JsComponent, ReactEvent, Children}

import scala.scalajs.js
import scala.scalajs.js.{UndefOr, |}
import scala.scalajs.js.annotation.JSImport

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.01.17 11:19
  * Description: react DatePicker component support for scala.js.
  */
object DatePickerR {

  val component = JsComponent[DatePickerPropsR, Children.None, Null]( DatePickerJs )

  def apply(props: DatePickerPropsR) = component( props )

}


/** Native component facade. */
@JSImport("react-datepicker", JSImport.Default)
@js.native
protected object DatePickerJs extends js.Object


/** Component properties. */
trait DatePickerPropsR extends js.Object {

  val autoComplete: UndefOr[String] = js.undefined

  val autoFocus: UndefOr[Boolean] = js.undefined

  val className: UndefOr[String] = js.undefined

  val customInput: UndefOr[Element] = js.undefined

  val dateFormat: UndefOr[String | js.Array[js.Any]] = js.undefined

  /** defaultValue: 'MMMM YYYY' */
  val dateFormatCalendar: UndefOr[String] = js.undefined

  val disabled: UndefOr[Boolean] = js.undefined

  /** @see [[DropDownModes]] for possible values. */
  val dropdownMode: UndefOr[String] = js.undefined

  val endDate: UndefOr[Date_t] = js.undefined

  val excludeDates: UndefOr[js.Array[Date_t]] = js.undefined

  val filterDate: UndefOr[js.Function1[Date_t, Boolean]] = js.undefined

  val fixedHeight: UndefOr[Boolean] = js.undefined

  val highlightDates: UndefOr[js.Array[Date_t]] = js.undefined

  val id: UndefOr[String] = js.undefined

  val includeDates: UndefOr[js.Array[Date_t]] = js.undefined

  val inline: UndefOr[Boolean] = js.undefined

  val isClearable: UndefOr[Boolean] = js.undefined

  val locale: UndefOr[String] = js.undefined

  val maxDate: UndefOr[Date_t] = js.undefined

  val minDate: UndefOr[Date_t] = js.undefined

  val monthsShown: UndefOr[Int] = js.undefined

  val name: UndefOr[String] = js.undefined

  val onBlur: UndefOr[js.Function1[ReactEvent, Unit]] = js.undefined

  val onChange: UndefOr[js.Function2[Date_t, ReactEvent, Unit]] = js.undefined

  val onFocus: UndefOr[js.Function1[ReactEvent, Unit]] = js.undefined

  val onMonthChange: UndefOr[js.Function1[Date_t, Unit]] = js.undefined

  val openToDate: UndefOr[js.Object] = js.undefined

  val peekNextMonth: UndefOr[Boolean] = js.undefined

  val placeholderText: UndefOr[String] = js.undefined

  val popperPlacement: UndefOr[String] = js.undefined

  val popoverAttachment: UndefOr[String] = js.undefined

  val popoverTargetAttachment: UndefOr[String] = js.undefined

  val popoverTargetOffset: UndefOr[String] = js.undefined

  val readOnly: UndefOr[Boolean] = js.undefined

  val renderCalendarTo: UndefOr[js.Any] = js.undefined

  val required: UndefOr[Boolean] = js.undefined

  val scrollableYearDropdown: UndefOr[Boolean] = js.undefined

  val selected: UndefOr[Date_t] = js.undefined

  val selectsEnd: UndefOr[Boolean] = js.undefined

  val selectsStart: UndefOr[Boolean] = js.undefined

  val showMonthDropdown: UndefOr[Boolean] = js.undefined

  val showWeekNumbers: UndefOr[Boolean] = js.undefined

  val showYearDropdown: UndefOr[Boolean] = js.undefined

  val forceShowMonthNavigation: UndefOr[Boolean] = js.undefined

  val startDate: UndefOr[Date_t] = js.undefined

  val tabIndex: UndefOr[Int] = js.undefined

  val tetherConstraints: UndefOr[js.Array[js.Any]] = js.undefined

  val title: UndefOr[String] = js.undefined

  val todayButton: UndefOr[String] = js.undefined

  val utcOffset: UndefOr[Double] = js.undefined

}
