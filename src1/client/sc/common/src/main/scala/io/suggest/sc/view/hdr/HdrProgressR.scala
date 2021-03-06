package io.suggest.sc.view.hdr

import com.materialui.{MuiColorTypes, MuiLinearProgress, MuiLinearProgressClasses, MuiLinearProgressProps}
import diode.react.ModelProxy
import io.suggest.css.Css
import io.suggest.react.ReactCommonUtil
import io.suggest.sc.view.styl.{ScCss, ScCssStatic}
import japgolly.scalajs.react.{BackendScope, React, ScalaComponent}
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.11.18 15:37
  * Description: Прогресс-бар в заголовке для индикации подгрузки данных с сервера.
  */
class HdrProgressR(
                    scCssP     : React.Context[ScCss],
                  ) {

  type Props_t = Some[Boolean]
  type Props = ModelProxy[Props_t]

  class Backend($: BackendScope[Props, Unit]) {

    def render(propsSomeProxy: Props): VdomElement = {
      ReactCommonUtil.maybeEl( propsSomeProxy.value.value ) {
        scCssP.consume { scCss =>
          MuiLinearProgress {
            val cssClasses = new MuiLinearProgressClasses {
              override val root = Css.flat(
                ScCssStatic.Header.progress.htmlClass,
                scCss.fgColorBg.htmlClass,
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
