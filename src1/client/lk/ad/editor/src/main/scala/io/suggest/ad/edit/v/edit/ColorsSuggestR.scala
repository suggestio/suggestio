package io.suggest.ad.edit.v.edit

import diode.FastEq
import diode.react.ModelProxy
import io.suggest.ad.edit.m.ColorChanged
import io.suggest.color.MColorData
import io.suggest.css.Css
import io.suggest.react.ReactCommonUtil
import japgolly.scalajs.react.{BackendScope, Callback, ScalaComponent}
import io.suggest.ueq.UnivEqUtil._
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.sjs.common.i18n.Messages
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.10.17 18:57
  * Description: Рекомендуемые цвета (фона).
  * Подборка цветов получается на основе гистограммы.
  */
class ColorsSuggestR {

  /** Модель данных для рендера.
    *
    * @param titleMsgCode Код заголовка из MsgCodes.
    * @param colors Цвета.
    */
  case class PropsVal(
                       titleMsgCode   : String,
                       colors         : Seq[MColorData]
                     )
  /** Поддержка FastEq для [[PropsVal]]. */
  implicit object ColorsSuggestPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.titleMsgCode ===* b.titleMsgCode) &&
        (a.colors ===* b.colors)
    }
  }


  type Props = ModelProxy[Option[PropsVal]]


  class Backend($: BackendScope[Props, Unit]) {

    private def _onColorClick(mcd: MColorData): Callback = {
      dispatchOnProxyScopeCB($, ColorChanged(mcd, isCompleted = true, forceTransform = true))
    }

    def render(p: Props): VdomElement = {
      p.value.whenDefinedEl { props =>
        <.div(
          ^.`class` := Css.Overflow.HIDDEN,
          // Чтобы не было DocBodyClick, который сбрасывает поворот картинки.
          ^.onClick ==> ReactCommonUtil.stopPropagationCB,

          // Заголовок.
          <.h1(
            Messages( props.titleMsgCode )
          ),

          // Список цветов.
          <.div(
            ^.`class` := Css.ColorsBlock.LIST,

            props.colors.toVdomArray { mcd =>
              <.div(
                ^.`class`           := Css.ColorsBlock.COLOR_BLOCK,
                ^.backgroundColor   := mcd.hexCode,
                ^.key               := mcd.code,
                ^.onClick          --> _onColorClick(mcd)
              )
            }
          )
        )
      }
    }

  }


  val component = ScalaComponent.builder[Props]("ColorSuggest")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(propsProxy: Props) = component(propsProxy)

}
