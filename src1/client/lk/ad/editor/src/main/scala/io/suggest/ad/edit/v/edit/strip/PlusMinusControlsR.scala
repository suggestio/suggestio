package io.suggest.ad.edit.v.edit.strip

import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.ad.blk.IBlockSizes
import io.suggest.ad.edit.v.LkAdEditCss
import io.suggest.common.{MHand, MHands}
import io.suggest.css.Css
import japgolly.scalajs.react.{BackendScope, Callback, ScalaComponent}
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.react.ReactCommonUtil.Implicits._

import scalacss.ScalaCssReact._
import scalacss.internal.StyleA

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.08.17 19:04
  * Description: Кнопки +/- для управления шириной или высотой рекламного блока.
  *
  * Компонент следует использовать через .wrap() вместо connect().
  */
class PlusMinusControlsR(
                          css: LkAdEditCss
                        ) {

  /** Контейнер настроек для работы этого компонента. */
  case class PropsVal(
                       label     : String,
                       contCss   : StyleA,
                       model     : IBlockSizes,
                       min       : Int,
                       current   : Int,
                       max       : Int
                     )

  type Props = ModelProxy[Option[PropsVal]]

  case class State(
                    leftEnabledC : ReactConnectProxy[Some[Boolean]],
                    rightEnabledC: ReactConnectProxy[Some[Boolean]]
                  )

  /** Бэкэнд рендера. */
  class Backend($: BackendScope[Props, State]) {

    def onBtnClick(mhand: MHand): Callback = {
      ???
    }

    def render(p: Props, s: State): VdomElement = {
      p().whenDefinedEl { props =>
        val whCss = css.WhControls

        /** Конвертация направления в css-класс. */
        def _mhand2css(mhand: MHand) = {
          mhand match {
            case MHands.Left  => (whCss.decrease, s.leftEnabledC)
            case MHands.Right => (whCss.increase, s.rightEnabledC)
          }
        }

        <.div(
          css.editorFieldContainer,
          props.contCss,

          <.label(
            whCss.label,
            props.label
          ),

          <.div(
            whCss.btnsContainer,

            MHands.values.toVdomArray { mhand =>
              val (handCssClass, isEnabledSomeC) = _mhand2css( mhand )
              isEnabledSomeC { isEnabledSomeProxy =>
                val isEnabled = isEnabledSomeProxy.value.value
                <.div(
                  whCss.btn,
                  handCssClass,
                  ^.classSet(
                    Css.Display.INVISIBLE -> !isEnabled
                  ),
                  if (isEnabled) {
                    ^.onClick --> onBtnClick(mhand)
                  } else {
                    EmptyVdom
                  }
                )
              }
            }
          )
        )
      }
    }

  }


  val component = ScalaComponent.builder[Props]("PmCtls")
    .initialStateFromProps { propsProxy =>
      def __enabledC(isEnabledF: PropsVal => Boolean): ReactConnectProxy[Some[Boolean]] = {
        propsProxy.connect { propsOpt =>
          Some( propsOpt.exists( isEnabledF ) )
        }
      }
      State(
        leftEnabledC = __enabledC { props =>
          props.current > props.min
        },
        rightEnabledC = __enabledC { props =>
          props.max < props.current
        }
      )
    }
    .renderBackend[Backend]
    .build


  def apply(propsOptProxy: Props) = component( propsOptProxy )

}
