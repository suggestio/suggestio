package io.suggest.sc.v.hdr

import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.color.MColorData
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.sc.index.MSc3IndexResp
import io.suggest.sc.m.hdr.{MHeaderState, MHeaderStates}
import io.suggest.sc.styl.GetScCssF
import io.suggest.spa.OptFastEq
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.ueq.UnivEqUtil._
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
      (a.hdrState ===* b.hdrState) &&
      (a.node ===* b.node)
    }
  }

  type Props_t = Option[PropsVal]
  type Props = ModelProxy[Props_t]


  /** Коннекшены для props'ов кнопок. */
  protected case class State(
                              hdrGridBtnOptC          : ReactConnectProxy[Option[MColorData]],
                              hdrLogoOptC             : ReactConnectProxy[Option[logoR.PropsVal]],
                            )


  /** Рендерер. */
  protected class Backend($: BackendScope[Props, State]) {

    def render(propsProxy: Props, s: State): VdomElement = {

      // Кнопки заголовка в зависимости от состояния.
      // Кнопки при нахождении в обычной выдаче без посторонних вещей.
      val plainGridBtns = s.hdrGridBtnOptC { fgColorOptProxy =>
        <.span(
          menuBtnR( fgColorOptProxy ),
          searchBtnR( fgColorOptProxy ),
        )
      }

      // Логотип посередине заголовка.
      val logo = s.hdrLogoOptC { logoR.apply }

      propsProxy().whenDefinedEl { _ =>
        val scCss = getScCssF()
        <.div(
          scCss.Header.header,
          plainGridBtns,
          logo,
        )
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsOptProxy =>

      def __fgColorDataOptProxy(hStates: MHeaderState*): ReactConnectProxy[Option[MColorData]] = {
        propsOptProxy.connect { propsOpt =>
          for {
            props <- propsOpt
            if hStates contains props.hdrState
            fgColor <- props.node.colors.fg
          } yield {
            fgColor
          }
        }( OptFastEq.Plain )
      }

      State(
        hdrGridBtnOptC = __fgColorDataOptProxy( MHeaderStates.PlainGrid ),
        hdrLogoOptC = propsOptProxy.connect { propsOpt =>
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
