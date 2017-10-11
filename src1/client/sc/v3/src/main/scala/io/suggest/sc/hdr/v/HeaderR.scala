package io.suggest.sc.hdr.v

import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.color.MColorData
import io.suggest.sc.hdr.m.{MHeaderState, MHeaderStates}
import io.suggest.sc.index.MSc3IndexResp
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.sc.styl.GetScCssF
import io.suggest.spa.OptFastEq

import scalacss.ScalaCssReact._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.07.17 17:49
  * Description: Компонент заголовка выдачи.
  */

class HeaderR(
               menuBtnR                   : MenuBtnR,
               searchBtnR                 : SearchBtnR,
               getScCssF                  : GetScCssF,
               protected[this] val logoR  : LogoR
             ) {


  /** Модель пропертисов для рендера компонента заголовка.
    *
    * @param hdrState Состояние заголовка.
    * @param node Данные по текущему узлу, в контексте которого работаем, если есть.
    */
  case class PropsVal(
                       hdrState   : MHeaderState,
                       node       : MSc3IndexResp
                     )

  implicit object HeaderPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.hdrState eq b.hdrState) &&
        (a.node eq b.node)
    }
  }

  type Props = ModelProxy[Option[PropsVal]]


  /** Коннекшены для props'ов кнопок. */
  protected case class State(
                              plainGridC      : ReactConnectProxy[Option[MColorData]],
                              menuC           : ReactConnectProxy[Option[MColorData]],
                              searchC         : ReactConnectProxy[Option[MColorData]],
                              logoPropsOptC   : ReactConnectProxy[Option[logoR.PropsVal]]
                            )


  /** Рендерер. */
  protected class Backend($: BackendScope[Props, State]) {

    def render(propsProxy: Props, s: State): VdomElement = {
      propsProxy().whenDefinedEl { _ =>
        val scCss = getScCssF()
        <.div(
          scCss.Header.header,

          // Кнопки заголовка в зависимости от состояния.
          // Кнопки при нахождении в обычной выдаче без посторонних вещей.
          s.plainGridC { fgColorDataOptProxy =>
            <.span(
              menuBtnR( fgColorDataOptProxy ),
              searchBtnR( fgColorDataOptProxy )
            )
          },

          // Логотип посередине заголовка.
          s.logoPropsOptC { logoR.apply }
        )
      }
    }

  }


  val component = ScalaComponent.builder[Props]("Header")
    .initialStateFromProps { propsProxy =>
      def __fgColorDataOptProxy(hStates: MHeaderState*) = {
        propsProxy.connect { props =>
          props
            .map(_.node)
            .filter { _ => props.map(_.hdrState).exists(hStates.contains) }
            .flatMap(_.colors.fg)
        }( OptFastEq.Plain )
      }
      val HS = MHeaderStates
      State(
        plainGridC  = __fgColorDataOptProxy( HS.PlainGrid ),
        menuC       = __fgColorDataOptProxy( HS.Menu ),
        searchC     = __fgColorDataOptProxy( HS.Search ),
        logoPropsOptC    = propsProxy.connect { propsOpt =>
          for (props <- propsOpt) yield {
            logoR.PropsVal(
              logoOpt     = props.node.logoOpt,
              nodeNameOpt = props.node.name
            )
          }
        }( OptFastEq.Wrapped( logoR.LogoPropsValFastEq ) )
      )
    }
    .renderBackend[Backend]
    .build

  def apply(props: Props) = component( props )

}
