package io.suggest.sc.v.hdr

import diode.react.ModelProxy
import io.suggest.react.ReactCommonUtil
import io.suggest.sc.m.MScReactCtx
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import scalacss.ScalaCssReact._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.07.17 17:49
  * Description: Компонент заголовка выдачи.
  */

class HeaderR(
               scReactCtxP     : React.Context[MScReactCtx],
             ) {

  type Props_t = Some[Boolean]
  type Props = ModelProxy[Props_t]


  /** Рендерер. */
  protected class Backend($: BackendScope[Props, Unit]) {

    def render(propsProxy: Props, children: PropsChildren): VdomElement = {
      ReactCommonUtil.maybeEl(propsProxy.value.value) {
        scReactCtxP.consume { scReactCtx =>
          <.div(
            scReactCtx.scCss.Header.header,

            children
          )
        }
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackendWithChildren[Backend]
    .build

  def apply(props: Props)(children: VdomNode*) = component( props )(children: _*)

}
