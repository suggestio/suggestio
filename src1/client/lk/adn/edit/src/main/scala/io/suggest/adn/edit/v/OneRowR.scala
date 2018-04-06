package io.suggest.adn.edit.v

import diode.FastEq
import diode.react.ReactConnectProxy
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.lk.r.PropTableR
import io.suggest.msg.Messages
import japgolly.scalajs.react.{Callback, ReactEventFromInput, ScalaComponent}
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.ueq.UnivEqUtil._
import japgolly.scalajs.react.vdom.TagOf
import japgolly.univeq._
import org.scalajs.dom.html

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.04.18 12:39
  * Description: Один ряд для инпута с (обычно) текстовым полем.
  */
class OneRowR {

  type Props = PropsVal

  case class ValueVal(
                       value: String,
                       error: Option[String] = None
                     )
  implicit case object OneRowRValueValFastEq extends FastEq[ValueVal] {
    override def eqv(a: ValueVal, b: ValueVal): Boolean = {
      (a.value ===* b.value) &&
        (a.error ===* b.error)
    }
  }

  case class PropsVal(
                       nameCode     : String,
                       onChangeF    : ReactEventFromInput => Callback,
                       conn         : ReactConnectProxy[ValueVal],
                       isTextArea   : Boolean = false,
                       isRequired   : Boolean = false
                     )
  implicit object OneRowRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.nameCode ===* b.nameCode) &&
        (a.onChangeF eq b.onChangeF) &&
        (a.conn eq b.conn) &&
        (a.isTextArea ==* b.isTextArea) &&
        (a.isRequired ==* b.isRequired)
    }
  }

  val component = ScalaComponent.builder[Props]("OneRow")
    .stateless
    .render_P { props =>
      val inputId = props.nameCode
      PropTableR.Outer(
        PropTableR.Row(
          <.label(
            ^.`for` := inputId,
            Messages( props.nameCode ),

            if (props.isRequired) {
              <.span(
                ^.`class` := Css.Input.REQUIRED_ICON,
                HtmlConstants.SPACE,
                HtmlConstants.ASTERISK
              )
            } else {
              EmptyVdom
            }
          )
        )(
          props.conn { valueValProxy =>
            val valueVal = valueValProxy.value
            val tag: TagOf[html.Element] = if (props.isTextArea) {
              <.textarea
            } else {
              <.input(
                ^.`type` := HtmlConstants.Input.text
              )
            }
            <.div(
              ^.classSet1(
                Css.Input.INPUT,
                Css.Input.ERROR -> valueVal.error.nonEmpty
              ),
              tag(
                ^.id        := inputId,
                ^.value     := valueVal.value,
                valueVal.error.whenDefined { errMsgCode =>
                  ^.title   := Messages( errMsgCode )
                },
                ^.onChange ==> props.onChangeF
              )
            )
          }
        )
      )
    }
    .shouldComponentUpdatePure { $ =>
      $.currentProps != $.nextProps
    }
    .build

  def apply(propsVal: PropsVal) = component(propsVal)

}
