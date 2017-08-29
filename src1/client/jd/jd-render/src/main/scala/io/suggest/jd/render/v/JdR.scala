package io.suggest.jd.render.v

import diode.react.ModelProxy
import io.suggest.jd.render.m.MJdArgs
import japgolly.scalajs.react.{BackendScope, ScalaComponent}
import japgolly.scalajs.react.vdom.VdomElement

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.08.17 15:21
  * Description: Компонент для рендера одного документа-блока для карточки.
  */
class JdR(
           jdRendererF: ModelProxy[MJdArgs] => JdRendererR
         ) {

  type Props = ModelProxy[MJdArgs]

  class Backend($: BackendScope[Props, Unit]) {

    def render(propsProxy: Props): VdomElement = {
      // Собрать и запустить рендерер:
      jdRendererF( propsProxy )
        .renderDocument()
    }

  }


  val component = ScalaComponent.builder[Props]("Jd")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(blkRenderDataProxy: Props) = component( blkRenderDataProxy )

}

