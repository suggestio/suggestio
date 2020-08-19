package io.suggest.lk.nodes.form.r.tree

import com.materialui.{MuiCircularProgress, MuiCircularProgressProps, MuiColorTypes, MuiListItem, MuiListItemProps, MuiListItemSecondaryAction, MuiListItemText, MuiListItemTextProps, MuiProgressVariants, MuiSwitchProps, MuiTypoGraphy, MuiTypoGraphyProps}
import diode.react.ModelProxy
import io.suggest.common.html.HtmlConstants
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.lk.nodes.form.m.{MNodeEnabledUpdateState, NodeIsEnabledChanged}
import io.suggest.lk.r.plat.PlatformComponents
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import ReactCommonUtil.Implicits._
import io.suggest.ueq.UnivEqUtil._
import io.suggest.ueq.JsUnivEqUtil._
import diode.data.Pot
import io.suggest.sjs.common.empty.JsOptionUtil
import io.suggest.spa.FastEqUtil
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.08.2020 22:18
  * Description: Компонент флага isEnabled.
  */
final class NodeEnabledR(
                          platformComponents   : PlatformComponents,
                          crCtxP               : React.Context[MCommonReactCtx],
                        ) {

  case class PropsVal(
                       isEnabledUpd             : Option[MNodeEnabledUpdateState],
                       isEnabled                : Boolean,
                       request                  : Pot[_],
                       canChangeAvailability    : Option[Boolean],
                     )
  implicit val PropsValFastEq = FastEqUtil[PropsVal] { (a, b) =>
    (a.isEnabledUpd ===* b.isEnabledUpd) &&
    (a.isEnabled ==* b.isEnabled) &&
    (a.request ===* b.request) &&
    (a.canChangeAvailability ===* b.canChangeAvailability)
  }

  type Props_t = PropsVal
  type Props = ModelProxy[Props_t]

  // Компонент рендерится внутри списка (дерева), поэтому в состояние - последний снимок props.

  class Backend($: BackendScope[Props, Props_t]) {

    /** Реакция на изменение значения флага активности узла. */
    private val _onNodeEnabledClickCbF = ReactCommonUtil.cbFun1ToJsCb { e: ReactEventFromInput =>
      val isChecked = e.target.checked
      ReactDiodeUtil.dispatchOnProxyScopeCBf($) { props: Props =>
        NodeIsEnabledChanged( isEnabled = isChecked )
      }
    }

    def render(s: Props_t): VdomElement = {
      val isChecked = s.isEnabledUpd
        .fold( s.isEnabled )( _.newIsEnabled )
      val isDisabled = s.isEnabledUpd
        .exists(_.request.isPending)

      s.canChangeAvailability.whenDefinedEl { canChangeAvail =>
        MuiListItem(
          new MuiListItemProps {
            override val onChange = JsOptionUtil.maybeDefined( canChangeAvail )( _onNodeEnabledClickCbF )
            override val disabled = isDisabled
            override val button   = canChangeAvail
          }
        )(

          MuiListItemText(
            new MuiListItemTextProps {

              override val primary =
                crCtxP.message( MsgCodes.`Is.enabled` ).rawNode

              override val secondary = React.Fragment(
                // Текстом написать, включёно или нет.
                crCtxP.message( MsgCodes.yesNo(isChecked) ),
                // Отрендерить ошибку запроса.
                s.isEnabledUpd
                  .flatMap(_.request.exceptionOption)
                  .whenDefinedNode { ex =>
                    React.Fragment(
                      <.br,
                      MuiTypoGraphy(
                        new MuiTypoGraphyProps {
                          override val color = MuiColorTypes.error
                        }
                      )(
                        crCtxP.message( MsgCodes.`Error` ),
                        HtmlConstants.COLON, HtmlConstants.SPACE,
                        ex.toString,
                      ),
                    )
                  },
              ).rawNode

            }
          )(),

          // Отрендерить крутилку текущего запроса:
          ReactCommonUtil.maybeNode( isDisabled ) {
            MuiCircularProgress(
              new MuiCircularProgressProps {
                override val variant = MuiProgressVariants.indeterminate
              }
            )
          },

          MuiListItemSecondaryAction()(
            platformComponents.muiSwitch {
              new MuiSwitchProps {
                override val checked = isChecked
                override val disabled = isDisabled
              }
            },
          ),

        )
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps( ReactDiodeUtil.modelProxyValueF )
    .renderBackend[Backend]
    .configure( ReactDiodeUtil.statePropsValShouldComponentUpdate( PropsValFastEq ) )
    .build

}
