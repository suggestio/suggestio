package io.suggest.ad.edit.v.edit.strip

import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProps, ReactConnectProxy}
import io.suggest.ad.blk.{IBlockSize, IBlockSizes}
import io.suggest.ad.edit.m.BlockSizeBtnClick
import io.suggest.ad.edit.v.LkAdEditCss
import io.suggest.common.{MHand, MHands}
import io.suggest.msg.Messages
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
                       labelMsgCode     : String,
                       contCss          : StyleA,
                       model            : IBlockSizes[_ <: IBlockSize],
                       current          : IBlockSize
                     )
  implicit object PlusMinusControlsPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.labelMsgCode eq b.labelMsgCode) &&
        (a.contCss eq b.contCss) &&
        (a.model eq b.model) &&
        (a.current eq b.current)
    }
  }


  type Props_t = Option[PropsVal]
  type Props = ModelProxy[Props_t]

  case class State(
                    leftEnabledC : ReactConnectProxy[Some[Boolean]],
                    rightEnabledC: ReactConnectProxy[Some[Boolean]]
                  )

  /** Бэкэнд рендера. */
  class Backend($: BackendScope[Props, State]) {

    /** Реакция на клик по одной из кнопок увеличения/уменьшения размера. */
    private def onBtnClick(mhand: MHand): Callback = {
      // Надо бы применить dispatchOnProxyScopeCB(), но тут зависимость от props.value...
      $.props >>= { p =>
        p.dispatchCB( BlockSizeBtnClick(p.value.get.model, mhand) )
      }
    }


    /** Рендеринг компонента. */
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
            Messages( props.labelMsgCode )
          ),

          <.div(
            whCss.btnsContainer,

            MHands.values.toVdomArray { mhand =>
              val (handCssClass, isEnabledSomeC) = _mhand2css( mhand )

              isEnabledSomeC.withKey(mhand.value) { isEnabledSomeProxy =>
                // TODO Выключать кнопку при неактивности
                val isEnabled = isEnabledSomeProxy.value.value
                <.div(
                  whCss.btn,
                  handCssClass,
                  (^.onClick --> onBtnClick(mhand))
                    .when(isEnabled)
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
          props.current.value > props.model.min.value
        },
        rightEnabledC = __enabledC { props =>
          props.current.value < props.model.max.value
        }
      )
    }
    .renderBackend[Backend]
    .build


  def _apply(propsOptProxy: Props) = component( propsOptProxy )
  val apply: ReactConnectProps[Props_t] = _apply

}
