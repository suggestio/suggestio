package io.suggest.sc.v.snack

import com.materialui.{MuiAnchorOrigin, MuiSnackBar, MuiSnackBarProps}
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.sc.m.{CloseError, MScRoot}
import io.suggest.sc.m.inx.{IndexSwitchNodeClick, MInxSwitch}
import io.suggest.sc.v.dia.err.ScErrorDiaR
import io.suggest.sc.v.inx.IndexSwitchAskR
import io.suggest.spa.{DAction, OptFastEq}
import io.suggest.ueq.UnivEqUtil._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.06.2020 20:07
  * Description: Единый snackbar с различными snack-компонентами внутри.
  */
final class ScSnacksR(
                       indexSwitchAskR         : IndexSwitchAskR,
                       scErrorDiaR             : ScErrorDiaR,
                       offlineSnackR           : OfflineSnackR,
                     ) {

  type Props_t = MScRoot
  type Props = ModelProxy[Props_t]


  /** @param currSnackOrNullC Текущий маркер-класс для обозначения отображаемого snack'а, либо null. */
  case class State(
                    currSnackOrNullC      : ReactConnectProxy[SnackComp],
                  )


  class Backend( $: BackendScope[Props, State] ) {

    /** Закрытие плашки без аппрува. */
    private lazy val _onCloseJsCbF = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      // TODO Определить текущие открытые snack'и, и выслать туда сигналы?
      $.props >>= { propsProxy: Props =>
        val p = propsProxy.value

        var actions = List.empty[DAction]
        if (p.index.state.switch.ask.nonEmpty)
          actions ::= IndexSwitchNodeClick()
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
      // MuiSnackBar отображает в отдельном слое ровно одну плашку.

      // Всплывающая плашка для смены узла:
      lazy val inxSwitch: VdomElement = p.wrap( _.index.state.switch )( indexSwitchAskR.component.apply )( implicitly, MInxSwitch.MInxSwitchFeq )

      lazy val offline: VdomElement = p.wrap( _.dev.onLine )( offlineSnackR.component.apply )

      // Плашка ошибки выдачи. Используем AnyRefEq (OptFeq.Plain) для ускорения: ошибки редки в общем потоке.
      lazy val scErr: VdomElement = p.wrap(_.dialogs.error)( scErrorDiaR.component.apply )(implicitly, OptFastEq.Plain)

      val _anchorOrigin = new MuiAnchorOrigin {
        override val vertical   = MuiAnchorOrigin.bottom
        override val horizontal = MuiAnchorOrigin.center
      }
      s.currSnackOrNullC { currSnackOrNullProxy =>
        val currSnackOrNull = currSnackOrNullProxy.value
        val child = if (currSnackOrNull ===* offlineSnackR) {
          offline
        } else if (currSnackOrNull ===* indexSwitchAskR) {
          inxSwitch
        } else {
          // TODO Надо хоть что-то рендерить? Может как-то обойтись без ненужного?
          scErr
        }

        MuiSnackBar {
          new MuiSnackBarProps {
            override val open         = currSnackOrNull ne null
            override val anchorOrigin = _anchorOrigin
            override val onClose      = _onCloseJsCbF
          }
        } ( child )
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(

        currSnackOrNullC = propsProxy.connect { p =>
          if (!p.dev.onLine.isOnline)
            offlineSnackR
          else if (p.index.state.switch.ask.nonEmpty)
            indexSwitchAskR
          else  if (p.dialogs.error.nonEmpty)
            scErrorDiaR
          else
            null
        },

      )
    }
    .renderBackend[Backend]
    .build

}


/** Marker-trait для классов, содержащий компонент snackbar-content.
  * Это нужно, чтобы классы использовать в качестве идентификаторов, не плодя доп.сущностей. */
trait SnackComp
object SnackComp {
  @inline implicit def univEq: UnivEq[SnackComp] = UnivEq.force
}