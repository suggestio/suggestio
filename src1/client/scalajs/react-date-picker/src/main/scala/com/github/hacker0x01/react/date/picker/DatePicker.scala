package com.github.hacker0x01.react.date.picker

import io.suggest.react.JsWrapperR
import org.scalajs.dom.raw.HTMLDivElement

import scala.scalajs.js
import scala.scalajs.js.{Dynamic, UndefOr}
import scala.scalajs.js.annotation.ScalaJSDefined

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.01.17 11:19
  * Description:
  */
case class DatePicker(props: DatePickerPropsR) extends JsWrapperR[DatePickerPropsR, HTMLDivElement] {
  override protected def _rawComponent: Dynamic = ???
}


@ScalaJSDefined
trait DatePickerPropsR extends js.Object {

  val autoComplete: UndefOr[String] = js.undefined

}
