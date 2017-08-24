package io.suggest.jd.render.v

import scalacss.ScalaCssReact._
import diode.react.ModelProxy
import io.suggest.jd.render.m.{MJdBlockRa, MJdCommonRa}
import io.suggest.model.n2.edge.MPredicates
import japgolly.scalajs.react.{BackendScope, ScalaComponent}
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.08.17 15:21
  * Description: Компонент для рендера одного документа-блока для карточки.
  */
class JdBlockR(
                blockCssF: () => JdCss
              ) {

  type Props = ModelProxy[MJdBlockRa]

  class Backend($: BackendScope[Props, Unit]) {


    def render(propsProxy: Props): VdomElement = {
      val bCss = blockCssF()
      val props = propsProxy()

      new JdRendererR(bCss, props.common)
        .render(props.templateBlockStrip)
    }

  }


  val component = ScalaComponent.builder[Props]("JdBlk")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(blkRenderDataProxy: Props) = component( blkRenderDataProxy )

}

