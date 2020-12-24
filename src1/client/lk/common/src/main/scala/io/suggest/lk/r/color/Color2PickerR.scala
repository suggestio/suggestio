package io.suggest.lk.r.color

import com.github.mikbry.materialui.color.{Color, ColorPicker}
import diode.react.ModelProxy
import io.suggest.color.MColorData
import io.suggest.lk.m.color.MColor2PickerCtx
import io.suggest.react.ReactCommonUtil
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.scalajs.js.JSConverters._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.12.2020 12:22
  * Description: Второе поколение компонента color-picker'ов на базе Mui-color.
  * Теперь это wrap-компонент.
  * Для чтения используется ModelProxy, для записи по разным направлениям используется функция onChange.
  *
  * Компонент Mui-color используется в uncontolled-режиме, что позволяет необходимости следить за состоянием picker'а.
  * Для передачи палитры предлагаемых цветов используется React.Context.
  */
final class Color2PickerR(
                           colorPickerCtxP: React.Context[MColor2PickerCtx],
                         ) {

  case class PropsVal(
                       colorProxy     : ModelProxy[MColorData],
                       onChange       : MColorData => Callback,
                       onOpenClose    : Option[(Boolean) => Callback],
                     )

  type Props = PropsVal


  class Backend( $: BackendScope[Props, Unit] ) {

    private val _onColorChanged = ReactCommonUtil.cbFun1ToJsCb { color: Color =>
      val hexColor = color.hex
      $.props >>= { props: Props =>
        props.onChange( MColorData( MColorData.stripDiez(hexColor) )  )
      }
    }

    private lazy val _onOpenClose = ReactCommonUtil.cbFun1ToJsCb { isOpen: Boolean =>
      $.props >>= { props: Props =>
        props.onOpenClose.get( isOpen )
      }
    }

    def render(p: Props): VdomElement = {
      // Функция реакции на открытие.
      val _onOpenF = p.onOpenClose
        .map(_ => _onOpenClose)
        .orUndefined
      // Тут без коннекшена, т.к. иначе colorPicker почему-то тупит, т.к. до него не доходит значение сверху

      colorPickerCtxP.consume { colorPickCtx =>
        // Дёргаем контекст внутри коннекшена, чтобы избежать пере-монтирования colorPicker'а при изменении палитры.
        val mcd = p.colorProxy.value
        ColorPicker.component(
          new ColorPicker.Props {
            override val value          = mcd.hexCode
            override val palette        = colorPickCtx.toMuiColorPalette
            override val onChange       = _onColorChanged
            override val onOpen         = _onOpenF
            override val hideTextfield  = true
            override val disableAlpha   = true
          }
        )
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

}
