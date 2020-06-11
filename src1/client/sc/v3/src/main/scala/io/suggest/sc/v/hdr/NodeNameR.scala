package io.suggest.sc.v.hdr

import diode.FastEq
import io.suggest.react.ReactCommonUtil
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.sc.m.MScReactCtx
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, React, ScalaComponent}
import scalacss.ScalaCssReact._
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.07.17 18:13
  * Description: Вывод названия узла в рамочке.
  */
class NodeNameR(
                 scReactCtxP     : React.Context[MScReactCtx],
               ) {

  case class PropsVal(
                       nodeName   : String,
                       styled     : Boolean,
                     )
  implicit object NodeNameRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.nodeName ===* b.nodeName) &&
      (a.styled ==* b.styled)
    }
  }

  type Props = PropsVal


  class Backend($: BackendScope[Props, Unit]) {

    def render(props: Props): VdomElement = {
      // Отрендерить название текущего узла.
      scReactCtxP.consume { scReactCtx =>
        val styles = scReactCtx.scCss.Header.Logo.Txt
        val dotsStyles = styles.Dots

        val dot0 = <.span(
          dotsStyles.dot,
          ReactCommonUtil.maybe(props.styled)(dotsStyles.dotColor),
        )

        <.span(
          styles.logo,
          ReactCommonUtil.maybe( props.styled )(styles.colored),
          props.nodeName,
          dot0( dotsStyles.left ),
          dot0( dotsStyles.right ),
        )
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

}
