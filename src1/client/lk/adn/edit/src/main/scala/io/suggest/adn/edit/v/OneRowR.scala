package io.suggest.adn.edit.v

import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.lk.r.PropTableR
import io.suggest.msg.Messages
import japgolly.scalajs.react.ScalaComponent
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.04.18 12:39
  * Description: Один ряд для инпута с (обычно) текстовым полем.
  */
class OneRowR {

  type Props = PropsVal

  case class PropsVal(
                       nameCode   : String,
                       inputId    : String,
                       isRequired : Boolean
                     )

  val component = ScalaComponent.builder[Props]("OneRow")
    .stateless
    .render_PC { (props, children) =>
      PropTableR.Outer(
        PropTableR.Row(
          <.label(
            ^.`for` := props.inputId,
            Messages( props.nameCode ),

            if (props.isRequired) {
              <.span(
                ^.`class` := Css.Input.REQUIRED_ICON,
                HtmlConstants.SPACE,
                "*"
              )
            } else {
              EmptyVdom
            }
          )
        )(
          <.div(
            ^.`class` := Css.Input.INPUT,
            children
          )
        )
      )
    }
    .shouldComponentUpdatePure { $ =>
      $.currentProps != $.nextProps
    }
    .build

  def apply(propsVal: PropsVal)(children: VdomNode*) = component(propsVal)(children: _*)

}
