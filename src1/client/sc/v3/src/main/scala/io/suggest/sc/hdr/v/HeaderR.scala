package io.suggest.sc.hdr.v

import diode.react.ModelProxy
import io.suggest.model.n2.node.meta.colors.MColors
import io.suggest.sc.ScCss
import io.suggest.sc.hdr.m.MHeaderState
import io.suggest.sc.m.MScNodeInfo
import japgolly.scalajs.react.{BackendScope, ScalaComponent}
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._

import scalacss.ScalaCssReact._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.07.17 17:49
  * Description: Компонент заголовка выдачи.
  */
object HeaderR {

  /** Модель пропертисов для рендера компонента заголовка.
    *
    * @param hdrState Состояние заголовка.
    * @param node Данные по текущему узлу, в контексте которого работаем, если есть.
    */
  case class PropsVal(
                       hdrState   : MHeaderState,
                       node       : Option[MScNodeInfo]
                     )

  type Props = ModelProxy[PropsVal]


  class Backend($: BackendScope[Props, Unit]) {

    def render(p: Props): VdomElement = {
      <.div(
        ScCss.Header.header,

        // #smRootProducerHeaderButtons
        <.span(
          // TODO Кнопки в зависимости от состояния.
        ),

        // #smHdrNodeLogo
        <.span(
          // TODO Текстовый логотип.
          // TODO Логотип картинкой.
        )

      )
    }

  }


  val component = ScalaComponent.builder[Props]("Header")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(props: Props) = component( props )

}
