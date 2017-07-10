package io.suggest.sc.hdr.v

import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.model.n2.node.meta.colors.MColorData
import io.suggest.sc.ScCss
import io.suggest.sc.hdr.m.{MHeaderState, MHeaderStates}
import io.suggest.sc.m.MScNodeInfo
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.sjs.common.spa.OptFastEq.Plain

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


  /** Коннекшены для props'ов кнопок. */
  protected case class State(
                              plainGridC : ReactConnectProxy[Option[MColorData]],
                              menuC      : ReactConnectProxy[Option[MColorData]],
                              searchC    : ReactConnectProxy[Option[MColorData]]
                            )


  /** Рендерер. */
  protected class Backend($: BackendScope[Props, State]) {

    def render(s: State): VdomElement = {
      <.div(
        ScCss.Header.header,

        // #smRootProducerHeaderButtons
        // Кнопки в зависимости от состояния.
        // Кнопки при нахождении в обычной выдаче без посторонних вещей.
        s.plainGridC { fgColorDataOptProxy =>
          <.span(
            MenuBtnR( fgColorDataOptProxy ),
            SearchBtnR( fgColorDataOptProxy )
          )
        },

        // #smHdrNodeLogo
        <.span(
          // TODO Текстовый логотип.
          // TODO Логотип картинкой.
        )

      )
    }

  }


  val component = ScalaComponent.builder[Props]("Header")
    .initialStateFromProps { propsProxy =>
      def __fgColorDataOptProxy(hStates: MHeaderState*) = {
        propsProxy.connect { props =>
          props.node
            .filter { _ => hStates.contains( props.hdrState ) }
            .flatMap(_.colors.fg)
        }
      }
      val HS = MHeaderStates
      State(
        plainGridC  = __fgColorDataOptProxy( HS.PlainGrid ),
        menuC       = __fgColorDataOptProxy( HS.Menu ),
        searchC     = __fgColorDataOptProxy( HS.Search )
      )
    }
    .renderBackend[Backend]
    .build

  def apply(props: Props) = component( props )

}
