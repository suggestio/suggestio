package io.suggest.lk.r

import com.materialui.{Mui, MuiIconButton, MuiIconButtonProps}
import diode.react.ModelProxy
import io.suggest.lk.m.TouchDevSet
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import japgolly.scalajs.react.vdom.html_<^.^
import japgolly.scalajs.react.vdom.{TagMod, VdomElement}
import japgolly.scalajs.react.{BackendScope, ReactEvent, ScalaComponent}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.09.2019 12:30
  * Description: wrap-компонент переключения touch/mouse-режима.
  */
class TouchSwitchR {

  type Props_t = Some[Boolean]
  type Props = ModelProxy[Props_t]


  /** Общий код сборки touch-события. */
  def _touchDevSet(isTouchDevProxy: Props) = {
    val isTouchDev = isTouchDevProxy.value.value
    TouchDevSet( !isTouchDev )
  }


  /** TagMod для автоперехвата событий touch и не-touch девайсов. */
  def autoSwitch(isTouchDevProxy: Props): TagMod = {
    val cb = isTouchDevProxy.dispatchCB( _touchDevSet(isTouchDevProxy) )

    if ( isTouchDevProxy.value.value )
      ^.onDragStart --> cb
    else
      // TODO Не работает определение таскания мышкой на touch-девайсе...
      ^.onTouchStart --> cb
  }


  class Backend($: BackendScope[Props, Unit]) {

    private val _onTouchSwitchClickJsCb = {
      ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
        ReactDiodeUtil.dispatchOnProxyScopeCBf($)( _touchDevSet )
      }
    }


    def render(propsProxy: Props): VdomElement = {
      MuiIconButton(
        new MuiIconButtonProps {
          override val onClick = _onTouchSwitchClickJsCb
        }
      )(
        {
          val isTouchDev = propsProxy.value.value
          val iconComp =
            if (isTouchDev) Mui.SvgIcons.TouchApp
            else Mui.SvgIcons.Mouse
          iconComp()()
        }
      )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .renderBackend[Backend]
    .build

  def apply(isTouchDevSomeProxy: Props) = component(isTouchDevSomeProxy)

}
