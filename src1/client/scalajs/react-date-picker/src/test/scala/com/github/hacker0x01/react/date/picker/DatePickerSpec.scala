package com.github.hacker0x01.react.date.picker

import com.momentjs.Moment
import japgolly.scalajs.react.React
import minitest._

import scala.scalajs.js
import scala.scalajs.js.UndefOr

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.01.17 12:21
  * Description: Tests for [[DatePicker]] ond [[DatePickerR]].
  */
object DatePickerSpec extends SimpleTestSuite {

  test("DatePicker is a class") {
    val v = new DatePicker
    assert( v != null )
  }

  test("DatePicker should be instantable") {
    val fac = React.createFactory( js.constructorOf[DatePicker].asInstanceOf[DatePicker] )
    assert( fac != null )
  }

  test("DatePickerR should make a component") {
    val comp = DatePickerR(
      new DatePickerPropsR {
        override val selected: UndefOr[Date_t] = Moment()
      }
    )()
    comp.toString
    assert( comp != null )
  }

}
