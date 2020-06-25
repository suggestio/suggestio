package io.suggest.sc.v.snack

import com.materialui.{MuiAnchorOrigin, MuiSnackBar, MuiSnackBarProps}
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.empty.OptionUtil
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.sc.m.{CloseError, MScRoot}
import io.suggest.sc.m.inx.{CancelIndexSwitch, MInxSwitch}
import io.suggest.sc.v.dia.err.ScErrorDiaR
import io.suggest.sc.v.inx.IndexSwitchAskR
import io.suggest.spa.{DAction, OptFastEq}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.06.2020 20:07
  * Description: Единый snackbar с различными snack-компонентами внутри.
  */
final class ScSnacksR(
                       indexSwitchAskR         : IndexSwitchAskR,
                       scErrorDiaR             : ScErrorDiaR,
                     ) {

  type Props_t = MScRoot
  type Props = ModelProxy[Props_t]


  case class State(
                    isOpenedSomeC: ReactConnectProxy[Some[Boolean]],
                  )


  class Backend( $: BackendScope[Props, State] ) {

    /** Закрытие плашки без аппрува. */
    private lazy val _onCloseJsCbF = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      // TODO Определить текущие открытые snack'и, и выслать туда сигналы?
      $.props >>= { propsProxy: Props =>
        val p = propsProxy.value

        var actions = List.empty[DAction]
        if (p.index.state.switch.ask.nonEmpty)
          actions ::= CancelIndexSwitch
        if (p.dialogs.error.nonEmpty)
          actions ::= CloseError

        actions
          .iterator
          .map { a =>
            ReactDiodeUtil.dispatchOnProxyScopeCB( $, a )
          }
          .reduceOption(_ >> _)
          .getOrElse( Callback.empty )
      }
    }


    def render(p: Props, s: State): VdomElement = {
      val children = List[VdomElement](

        // Всплывающая плашка для смены узла:
        p.wrap( _.index.state.switch )( indexSwitchAskR.component.apply )( implicitly, MInxSwitch.MInxSwitchFeq ),

        // Плашка ошибки выдачи. Используем AnyRefEq (OptFeq.Plain) для ускорения: ошибки редки в общем потоке.
        p.wrap(_.dialogs.error)( scErrorDiaR.component.apply )(implicitly, OptFastEq.Plain),

      )

      val _anchorOrigin = new MuiAnchorOrigin {
        override val vertical   = MuiAnchorOrigin.bottom
        override val horizontal = MuiAnchorOrigin.center
      }
      s.isOpenedSomeC { isOpenedSomeProxy =>
        MuiSnackBar {
          new MuiSnackBarProps {
            override val open         = isOpenedSomeProxy.value.value
            override val anchorOrigin = _anchorOrigin
            override val onClose      = _onCloseJsCbF
          }
        } ( children: _* )
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(

        isOpenedSomeC = propsProxy.connect { p =>
          val isOpen =
            p.index.state.switch.ask.nonEmpty ||
            p.dialogs.error.nonEmpty
          OptionUtil.SomeBool( isOpen )
        },

      )
    }
    .renderBackend[Backend]
    .build

}
