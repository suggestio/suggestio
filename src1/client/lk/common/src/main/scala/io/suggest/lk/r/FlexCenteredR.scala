package io.suggest.lk.r

import com.materialui.{MuiBox, MuiSx}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import scala.scalajs.js

object FlexCenteredR {

  val component = ScalaComponent
    .builder[Boolean]
    .stateless
    .render_PC { (withOuterDiv, propsChildren) =>
      // For centering inside flex toolbar, use this as left/right placeholders.
      val placeHolder = MuiBox.component(
        new MuiBox.Props {
          override val sx = new MuiSx {
            override val flexGrow = 1: js.Any
          }
        }
      )()

      val chs = List[VdomNode](
        placeHolder,
        propsChildren,
        placeHolder
      )

      if (withOuterDiv) {
        MuiBox(
          new MuiBox.Props {
            override val sx = new MuiSx {
              override val display = "flex": js.Any
            }
          }
        )( chs: _* )

      } else {
        React.Fragment( chs: _* )
      }
    }
    .build

}
