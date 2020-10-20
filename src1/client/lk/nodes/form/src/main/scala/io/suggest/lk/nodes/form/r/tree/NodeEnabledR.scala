package io.suggest.lk.nodes.form.r.tree

import com.materialui.{MuiColorTypes, MuiListItem, MuiListItemProps, MuiListItemSecondaryAction, MuiListItemText, MuiListItemTextProps, MuiSwitchProps, MuiTypoGraphy, MuiTypoGraphyProps}
import diode.react.ModelProxy
import io.suggest.common.html.HtmlConstants
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.lk.nodes.form.m.ModifyNode
import io.suggest.lk.r.plat.PlatformComponents
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import ReactCommonUtil.Implicits._
import io.suggest.ueq.UnivEqUtil._
import io.suggest.ueq.JsUnivEqUtil._
import diode.data.Pot
import io.suggest.common.empty.OptionUtil
import io.suggest.lk.nodes.form.r.LkNodesFormCss
import io.suggest.lk.nodes.{MLknOpKeys, MLknOpValue}
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
                          treeStuffR           : TreeStuffR,
                          platformComponents   : PlatformComponents,
                          crCtxP               : React.Context[MCommonReactCtx],
                          lkNodesFormCssP      : React.Context[LkNodesFormCss],
                        ) {

  case class PropsVal(
                       isEnabled                : Pot[Boolean],
                       canChangeAvailability    : Option[Boolean],
                     )
  implicit val PropsValFastEq = FastEqUtil[PropsVal] { (a, b) =>
    (a.isEnabled ==* b.isEnabled) &&
    (a.canChangeAvailability ===* b.canChangeAvailability)
  }

  type Props_t = PropsVal
  type Props = ModelProxy[Props_t]

  // Компонент рендерится внутри списка (дерева), поэтому в состояние - последний снимок props.

  class Backend($: BackendScope[Props, Props_t]) {

    /** Реакция на изменение значения флага активности узла. */
    private val _onNodeEnabledClickCbF = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      ($.state: CallbackTo[Props_t]) >>= { s =>
        val wasChecked = s.isEnabled.get
        val action = ModifyNode(
          key = MLknOpKeys.NodeEnabled,
          value = MLknOpValue(
            bool = OptionUtil.SomeBool( !wasChecked ),
          )
        )
        ReactDiodeUtil.dispatchOnProxyScopeCB( $, action )
      }
    }

    def render(s: Props_t): VdomElement = {
      val isChecked = s.isEnabled contains[Boolean] true
      val isDisabled = s.isEnabled.isPending

      s.canChangeAvailability.whenDefinedEl { canChangeAvail =>
        MuiListItem(
          new MuiListItemProps {
            override val button = true
            override val onClick = JsOptionUtil.maybeDefined( canChangeAvail )( _onNodeEnabledClickCbF )
          }
        )(

          MuiListItemText {
            val _secondaryText = React.Fragment(
              // Текстом написать, включёно или нет.
              crCtxP.message( MsgCodes.yesNo(isChecked) ),
              // Отрендерить ошибку запроса.
              s.isEnabled
                .exceptionOption
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
            )
            new MuiListItemTextProps {
              override val primary = crCtxP.message( MsgCodes.`Is.enabled` ).rawNode
              override val secondary = _secondaryText.rawNode
            }
          }(),


          {
            val chs = List[VdomNode](
              // Отрендерить крутилку текущего запроса:
              ReactCommonUtil.maybeNode( isDisabled ) {
                treeStuffR.LineProgress()
              },

              platformComponents.muiSwitch {
                new MuiSwitchProps {
                  override val checked = isChecked
                  override val disabled = isDisabled
                  // switch не пробрасывает событие переключения наверх - нужен отдельных listener.
                  override val onClick = JsOptionUtil.maybeDefined( canChangeAvail )( _onNodeEnabledClickCbF )
                }
              },
            )
            lkNodesFormCssP.consume { lknCss =>
              MuiListItemSecondaryAction( lknCss.Node.sceActProgressProps )( chs: _* )
            }
          },

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
