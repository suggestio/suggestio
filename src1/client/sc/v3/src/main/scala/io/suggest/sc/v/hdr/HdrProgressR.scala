package io.suggest.sc.v.hdr

import chandu0101.scalajs.react.components.materialui.{MuiColorTypes, MuiLinearProgress, MuiLinearProgressClasses, MuiLinearProgressProps}
import diode.react.ModelProxy
import io.suggest.react.ReactCommonUtil
import io.suggest.sc.styl.GetScCssF
import japgolly.scalajs.react.{BackendScope, ScalaComponent}
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.11.18 15:37
  * Description: Прогресс-бар в заголовке для индикации подгрузки данных с сервера.
  */
class HdrProgressR(
                    getScCssF: GetScCssF
                  ) {

  type Props_t = Some[Boolean]
  type Props = ModelProxy[Props_t]

  class Backend($: BackendScope[Props, Unit]) {

    def render(propsSomeProxy: Props): VdomElement = {
      ReactCommonUtil.maybeEl( propsSomeProxy.value.value ) {
        val hdrCss = getScCssF().Header
        MuiLinearProgress {
          val cssClasses = new MuiLinearProgressClasses {
            override val root = hdrCss.progress.htmlClass
          }
          new MuiLinearProgressProps {
            override val classes = cssClasses
            override val color = MuiColorTypes.primary
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