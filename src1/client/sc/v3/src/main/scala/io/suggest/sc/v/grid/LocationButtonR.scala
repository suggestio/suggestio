package io.suggest.sc.v.grid

import com.materialui.{MuiCircularProgress, MuiCircularProgressProps, MuiColorTypes, MuiFab, MuiFabProps, MuiFade, MuiFadeProps, MuiProgressVariants, MuiToolTip, MuiToolTipPlacements, MuiToolTipProps}
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.empty.OptionUtil
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.lk.r.plat.PlatformComponents
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.sc.index.MScIndexArgs
import io.suggest.sc.m.{GeoLocTimerStart, MScRoot}
import io.suggest.sc.m.inx.MScSwitchCtx
import io.suggest.sc.v.styl.ScCssStatic
import scalacss.ScalaCssReact._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

final class LocationButtonR(
                             platformComponents     : PlatformComponents,
                             crCtxP                 : React.Context[MCommonReactCtx],
                           ) {

  type Props_t = MScRoot
  type Props = ModelProxy[Props_t]

  case class State(
                    isPendingOptC       : ReactConnectProxy[Option[Boolean]],
                    isVisibleSomeC      : ReactConnectProxy[Some[Boolean]],
                  )

  class Backend($: BackendScope[Props, State]) {

    private val _onLocationButtonClick = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, GeoLocTimerStart(
        switchCtx = MScSwitchCtx(
          indexQsArgs = MScIndexArgs(
            retUserLoc = false,
          ),
          demandLocTest = true,
        ),
        allowImmediate = false,
        animateLocBtn = true,
      ))
    }

    def render(s: State): VdomElement = {
      val iconVdom = platformComponents.muiLocationIconComp()()

      val content = <.span(
        ScCssStatic.Grid.locationButton,

        MuiToolTip(
          new MuiToolTipProps {
            override val placement = MuiToolTipPlacements.Left
            override val title = crCtxP.message( MsgCodes.`Current.location` ).rawNode
          }
        )(
          <.span(
            s.isPendingOptC { isPendingOptProxy =>
              val isPendingOpt = isPendingOptProxy.value
              MuiFab(
                new MuiFabProps {
                  override val onClick = _onLocationButtonClick
                  override val disabled = isPendingOpt.nonEmpty
                  override val color = MuiColorTypes.primary
                }
              )(
                if (isPendingOpt contains[Boolean] true) {
                  MuiCircularProgress(
                    new MuiCircularProgressProps {
                      override val variant = MuiProgressVariants.indeterminate
                    }
                  )
                } else {
                  iconVdom
                },
              )
            }
          ),
        ),
      )

      s.isVisibleSomeC { isVisibleSomeProxy =>
        MuiFade(
          new MuiFadeProps {
            override val appear = false
            override val in = isVisibleSomeProxy.value.value
          }
        )(
          content,
        )
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]
    .initialStateFromProps { propsProxy =>
      State(

        isPendingOptC = propsProxy.connect { props =>
          val glTimerPot = props.internals.info.geoLockTimer

          if (glTimerPot.exists(_.reason.animateLocBtn) && glTimerPot.isPending) {
            // Display rotating progress:
            OptionUtil.SomeBool.someTrue

          } else if ( glTimerPot.isPending || props.index.resp.isPending || props.index.state.switch.ask.nonEmpty ) {
            // Only disable button, without circular-progress
            OptionUtil.SomeBool.someFalse

          } else {
            None
          }
        },

        isVisibleSomeC = propsProxy.connect { props =>
          val r = props.index.state.switch.ask.isEmpty &&
            props.dialogs.error.isEmpty &&
            !props.dialogs.first.isVisible
          OptionUtil.SomeBool( r )
        },

      )
    }
    .renderBackend[Backend]
    .build

}
