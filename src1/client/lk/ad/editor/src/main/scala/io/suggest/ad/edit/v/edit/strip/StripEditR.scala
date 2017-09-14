package io.suggest.ad.edit.v.edit.strip

import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.ad.blk.{BlockHeights, BlockMeta, BlockWidths}
import io.suggest.ad.edit.m.edit.{MColorPick, MColorsState}
import io.suggest.ad.edit.m.edit.strip.MStripEdS
import io.suggest.ad.edit.v.LkAdEditCss
import io.suggest.ad.edit.v.edit.ColorPickR
import io.suggest.i18n.MsgCodes
import io.suggest.jd.tags.Strip
import japgolly.scalajs.react.{BackendScope, ScalaComponent}
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._

import scalacss.ScalaCssReact._
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.sjs.common.i18n.Messages
import io.suggest.sjs.common.spa.OptFastEq.Wrapped

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.08.17 18:49
  * Description: React-компонент редактирования strip-а.
  */
class StripEditR(
                  val plusMinusControlsR    : PlusMinusControlsR,
                  val colorPickR            : ColorPickR,
                  css                       : LkAdEditCss,
                  deleteStripBtnR           : DeleteStripBtnR
                ) {

  import plusMinusControlsR.PlusMinusControlsPropsValFastEq
  import MStripEdS.MEditStripSFastEq
  import MColorPick.MColorPickFastEq

  case class PropsVal(
                       strip        : Strip,
                       edS          : MStripEdS,
                       colorsState  : MColorsState
                     )

  implicit object StripEditRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.strip eq b.strip) &&
        (a.edS eq b.edS) &&
        (a.colorsState eq b.colorsState)
    }
  }

  /** Алиас сложного типа для пропертисов. */
  type Props = ModelProxy[Option[PropsVal]]

  protected case class State(
                              heightPropsOptC   : ReactConnectProxy[Option[plusMinusControlsR.PropsVal]],
                              widthPropsOptC    : ReactConnectProxy[Option[plusMinusControlsR.PropsVal]],
                              stripEdSOptC      : ReactConnectProxy[Option[MStripEdS]],
                              bgColorPropsOptC  : ReactConnectProxy[Option[MColorPick]]
                            )

  class Backend($: BackendScope[Props, State]) {

    def render(p: Props, s: State): VdomElement = {
      p().whenDefinedEl { _ =>
        <.div(

          // Кнопки управление шириной и высотой блока.
          <.div(
            css.WhControls.outer,
            s.heightPropsOptC { plusMinusControlsR.apply },
            s.widthPropsOptC { plusMinusControlsR.apply }
          ),

          // Выбор цвета фона. Допускается прозрачный фон.
          s.bgColorPropsOptC { colorOptProxy =>
            colorPickR(colorOptProxy)(
              // TODO Opt Рендер необязателен из-за Option, но список children не ленив. Можно ли это исправить, кроме как передавая суть children внутри props?
              Messages( MsgCodes.`Bg.color` )
            )
          },

          <.br,

          // Кнопка удаления текущего блока.
          s.stripEdSOptC { deleteStripBtnR.apply }

          // TODO Загрузка фоновой картинки

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
            .flatMap(_.strip.bm)
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
        },

        stripEdSOptC = propsOptProxy.connect { propsOpt =>
          propsOpt.map(_.edS)
        },

        bgColorPropsOptC = propsOptProxy.connect { propsOpt =>
          for (props <- propsOpt) yield {
            MColorPick(
              colorOpt    = props.strip.bgColor,
              colorsState = props.colorsState,
              pickS       = props.edS.bgColorPick
            )
          }
        }

      )
    }
    .renderBackend[Backend]
    .build


  def apply(stripOptProxy: Props) = component( stripOptProxy )

}
