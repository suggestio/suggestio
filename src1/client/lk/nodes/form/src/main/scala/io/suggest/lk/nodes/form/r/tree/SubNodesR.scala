package io.suggest.lk.nodes.form.r.tree

import com.materialui.{Mui, MuiIconButton, MuiIconButtonProps, MuiListItem, MuiListItemSecondaryAction, MuiListItemText, MuiListItemTextProps, MuiToolTip, MuiToolTipProps}
import diode.{FastEq, UseValueEq}
import diode.react.ModelProxy
import io.suggest.common.html.HtmlConstants
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.lk.nodes.form.m.CreateNodeClick
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.spa.FastEqUtil
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.08.2020 22:15
  * Description: Компонент ряда краткого описания под-узлов.
  */
final class SubNodesR(
                       crCtxP               : React.Context[MCommonReactCtx],
                     ) {

  case class PropsVal(
                       chCount            : Int,
                       chCountEnabled     : Int,
                     )
    extends UseValueEq  // Не влияет, и feq внизу задан явно.

  @inline implicit def pvUnivEq: UnivEq[PropsVal] = UnivEq.derive


  type Props_t = PropsVal
  type Props = ModelProxy[Props_t]


  class Backend($: BackendScope[Props, Props_t]) {

    private lazy val _onCreateNodeClickCbF = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, CreateNodeClick )
    }

    def render(s: Props_t): VdomElement = {
      crCtxP.consume { crCtx =>
        MuiListItem()(

          // Текст
          MuiListItemText {
            val subNodesInfo = ReactCommonUtil.maybeNode( s.chCount > 0 ) {
              <.span(
                // Вывести общее кол-во под-узлов.
                crCtxP.message( MsgCodes.`N.nodes`, s.chCount),

                // Вывести кол-во выключенных под-узлов, если такие есть.
                {
                  val countDisabled = s.chCount - s.chCountEnabled
                  ReactCommonUtil.maybeEl(countDisabled > 0) {
                    <.span(
                      HtmlConstants.COMMA,
                      HtmlConstants.NBSP_STR,
                      crCtxP.message( MsgCodes.`N.disabled`, countDisabled ),
                    )
                  }
                },

                // Рендерим поддержку добавления нового под-узла:
                HtmlConstants.COMMA,
                HtmlConstants.SPACE
              )
            }
            new MuiListItemTextProps {
              override val primary = crCtx.messages( MsgCodes.`Subnodes` )
              override val secondary = subNodesInfo.rawNode
            }
          }(),

          MuiListItemSecondaryAction()(
            MuiToolTip(
              new MuiToolTipProps {
                override val title = React.Fragment(
                  crCtx.messages( MsgCodes.`Create` ),
                  HtmlConstants.ELLIPSIS,
                ).rawNode
              }
            )(
              MuiIconButton(
                new MuiIconButtonProps {
                  override val onClick = _onCreateNodeClickCbF
                }
              )(
                Mui.SvgIcons.Add()(),
              ),
            ),
          ),

        )
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps( ReactDiodeUtil.modelProxyValueF )
    .renderBackend[Backend]
    .configure( ReactDiodeUtil.statePropsValShouldComponentUpdate( FastEqUtil.AnyValueEq ) )
    .build

}
