package io.suggest.ad.edit.v.edit.strip

import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.ad.blk.{BlockHeights, BlockMeta, BlockWidths}
import io.suggest.ad.edit.v.LkAdEditCss
import io.suggest.i18n.MsgCodes
import io.suggest.jd.tags.Strip
import japgolly.scalajs.react.{BackendScope, ScalaComponent}
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.sjs.common.spa.OptFastEq.Wrapped

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.08.17 18:49
  * Description: React-компонент редактирования strip-а.
  */
class StripEditR(
                  val plusMinusControlsR    : PlusMinusControlsR,
                  css                       : LkAdEditCss
                ) {

  import plusMinusControlsR.PlusMinusControlsPropsValFastEq

  /** Алиас сложного типа для пропертисов. */
  type Props = ModelProxy[Option[Strip]]

  protected case class State(
                              heightPropsOptC   : ReactConnectProxy[Option[plusMinusControlsR.PropsVal]],
                              widthPropsOptC    : ReactConnectProxy[Option[plusMinusControlsR.PropsVal]]
                            )

  class Backend($: BackendScope[Props, State]) {

    def render(p: Props, s: State): VdomElement = {
      p().whenDefinedEl { _ =>
        <.div(

          // Кнопки управление шириной и высотой блока.
          s.heightPropsOptC { plusMinusControlsR.apply },

          s.widthPropsOptC { plusMinusControlsR.apply }

          // TODO Загрузка фоновой картинки и выбора цвета фона.

        )
      }
    }

  }


  val component = ScalaComponent.builder[Props]("StripEdit")
    .initialStateFromProps { propsOptProxy =>

      // Фунция с дедублицированным кодом сборки коннекшена до пропертисов plus-minus control'ов.
      def __mkWhPmProps(f: BlockMeta => plusMinusControlsR.PropsVal) = {
        propsOptProxy.connect { propsOpt =>
          propsOpt
            .flatMap(_.bm)
            .map(f)
        }
      }

      // Сборка состояния компонента со всеми data-коннекшенами:
      State(
        heightPropsOptC = __mkWhPmProps { bm =>
          plusMinusControlsR.PropsVal(
            labelMsgCode  = MsgCodes.`Height`,
            contCss       = css.WhControls.contHeight,
            model         = BlockHeights,
            current       = bm.h
          )
        },
        widthPropsOptC = __mkWhPmProps { bm =>
          plusMinusControlsR.PropsVal(
            labelMsgCode  = MsgCodes.`Width`,
            contCss       = css.WhControls.contWidth,
            model         = BlockWidths,
            current       = bm.w
          )
        }
      )
    }
    .renderBackend[Backend]
    .build


  def apply(stripOptProxy: Props) = component( stripOptProxy )

}
