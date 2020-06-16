package io.suggest.sc.v.hdr

import com.materialui.{MuiColorTypes, MuiLinearProgress, MuiLinearProgressClasses, MuiLinearProgressProps}
import diode.react.ModelProxy
import io.suggest.css.Css
import io.suggest.react.ReactCommonUtil
import io.suggest.sc.m.MScReactCtx
import io.suggest.sc.v.styl.ScCssStatic
import japgolly.scalajs.react.{BackendScope, React, ScalaComponent}
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.11.18 15:37
  * Description: Прогресс-бар в заголовке для индикации подгрузки данных с сервера.
  */
class HdrProgressR(
                    scReactCtxP     : React.Context[MScReactCtx],
                  ) {

  type Props_t = Some[Boolean]
  type Props = ModelProxy[Props_t]

  class Backend($: BackendScope[Props, Unit]) {

    def render(propsSomeProxy: Props): VdomElement = {
      scReactCtxP.consume { scReactCtx =>
        ReactCommonUtil.maybeEl( propsSomeProxy.value.value ) {
          MuiLinearProgress {
            val cssClasses = new MuiLinearProgressClasses {
              override val root = Css.flat(
                ScCssStatic.Header.progress.htmlClass,
                scReactCtx.scCss.fgColorBg.htmlClass,
              )
            }
            new MuiLinearProgressProps {
              override val classes = cssClasses
              override val color = MuiColorTypes.primary
            }
          }
        }
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

  def apply(propsSomeProxy: Props) = component( propsSomeProxy )

}
