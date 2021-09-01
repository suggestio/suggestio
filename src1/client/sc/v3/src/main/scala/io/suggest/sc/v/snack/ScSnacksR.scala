package io.suggest.sc.v.snack

import com.materialui.{MuiAnchorOrigin, MuiSnackBar, MuiSnackBarProps}
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.react.ReactDiodeUtil.Implicits._
import io.suggest.react.r.CatchR
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.sc.m.{CloseError, MScRoot}
import io.suggest.sc.m.inx.{IndexSwitchNodeClick, MInxSwitch}
import io.suggest.sc.v.dia.err.ScErrorDiaR
import io.suggest.sc.v.dia.first.WzFirstR
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
                       wzFirstR                : WzFirstR,
                     ) {

  type Props_t = MScRoot
  type Props = ModelProxy[Props_t]


  /** @param currSnackOrNullC Текущий маркер-класс для обозначения отображаемого snack'а, либо null. */
  case class State(
                    currSnackOrNullC      : ReactConnectProxy[ISnackComp],
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
      val _anchorOrigin = new MuiAnchorOrigin {
        override val vertical   = MuiAnchorOrigin.bottom
        override val horizontal = MuiAnchorOrigin.center
      }

      CatchR.component( p.resetZoom( classOf[ScSnacksR].getSimpleName ) )(
        s.currSnackOrNullC { currSnackOrNullProxy =>
          val currSnackOrNull = currSnackOrNullProxy.value
          val child: VdomNode = if (currSnackOrNull eq null) {
            EmptyVdom

          } else if (currSnackOrNull ===* offlineSnackR) {
            // Offline network message.
            p.wrap( _.dev.onLine )( offlineSnackR.component.apply )

          } else if (currSnackOrNull ===* indexSwitchAskR) {
            // There are two or more nodes here: render index swith stuff.
            p.wrap( _.index.state.switch )( indexSwitchAskR.component.apply )( implicitly, MInxSwitch.MInxSwitchFeq )

          } else if (currSnackOrNull ===* wzFirstR) {
            wzFirstR.component( p )

          } else {
            // Showing error message
            // TODO Надо хоть что-то рендерить? Может как-то обойтись без ненужного?
            p.wrap(_.dialogs.error)( scErrorDiaR.component.apply )(implicitly, OptFastEq.Plain)
          }

          MuiSnackBar {
            new MuiSnackBarProps {
              override val open         = currSnackOrNull ne null
              override val anchorOrigin = _anchorOrigin
              override val onClose      = _onCloseJsCbF
            }
          } (
            // TODO mui5 - При Grow идёт запись в node.style. Поэтому тут div.
            <.div(
              child
            )
          )
        }
      )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(

        currSnackOrNullC = propsProxy.connect { p =>
          if (!p.dev.onLine.isOnline)
            offlineSnackR
          else if (p.dialogs.first.isVisible)
            wzFirstR
          else if (p.index.state.switch.ask.nonEmpty)
            indexSwitchAskR
          else if (p.dialogs.error.nonEmpty)
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
trait ISnackComp
object ISnackComp {
  @inline implicit def univEq: UnivEq[ISnackComp] = UnivEq.force
}