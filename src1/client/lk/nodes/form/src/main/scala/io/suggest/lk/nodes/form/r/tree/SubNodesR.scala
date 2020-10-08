package io.suggest.lk.nodes.form.r.tree

import com.materialui.{Mui, MuiIconButton, MuiIconButtonProps, MuiListItem, MuiListItemSecondaryAction, MuiListItemText, MuiListItemTextProps, MuiToolTip, MuiToolTipProps}
import diode.react.ModelProxy
import io.suggest.common.html.HtmlConstants
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.lk.nodes.MLknOpKeys
import io.suggest.lk.nodes.form.m.{CreateNodeClick, MNodeState, MNodeStateRender}
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.scalaz.NodePath_t
import io.suggest.spa.FastEqUtil
import io.suggest.ueq.UnivEqUtil._
import io.suggest.scalaz.ScalazUtil.Implicits._
import io.suggest.scalaz.ZTreeUtil.zTreeUnivEq
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.univeq._
import scalaz.{EphemeralStream, Tree}

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
                       rawNodePathRev: NodePath_t,
                       subNodes: EphemeralStream[Tree[String]],
                       nodesMap: Map[String, MNodeState],
                     )
  implicit val pvFeq = FastEqUtil[PropsVal] { (a, b) =>
    (a.rawNodePathRev ===* b.rawNodePathRev) &&
    (b.subNodes ===* b.subNodes)
  }

  type Props_t = PropsVal
  type Props = ModelProxy[Props_t]

  private lazy val _createMsg = crCtxP.message( MsgCodes.`Create` )

  class Backend($: BackendScope[Props, Props_t]) {

    private lazy val _onCreateNodeClickCbF = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      ReactDiodeUtil.dispatchOnProxyScopeCBf($) { p: Props =>
        CreateNodeClick(
          parentPath = Some( MNodeStateRender.unRawNodePathRev( p.value.rawNodePathRev ) ),
        )
      }
    }

    def render(s: Props_t): VdomElement = {
      MuiListItem()(

        crCtxP.consume { crCtx =>
          // Текст
          MuiListItemText {
            val chCount = s.subNodes.length
            val subNodesInfo = ReactCommonUtil.maybeNode( chCount > 0 ) {
              <.span(
                // Вывести общее кол-во под-узлов.
                crCtx.messages( MsgCodes.`N.nodes`, chCount),

                // Вывести кол-во выключенных под-узлов, если такие есть.
                {
                  // Считаем disabled-узлы:
                  val chCountDisabled = (for {
                    treeIdTree <- s.subNodes.iterator
                    treeId = treeIdTree.rootLabel
                    mns  <- s.nodesMap.get( treeId )
                    info <- mns.infoPot.toOption
                    if info.options
                      .get(MLknOpKeys.NodeEnabled)
                      .flatMap(_.bool) contains[Boolean] false
                  } yield {
                    1
                  })
                    .sum

                  ReactCommonUtil.maybeEl(chCountDisabled > 0) {
                    <.span(
                      HtmlConstants.COMMA,
                      HtmlConstants.NBSP_STR,
                      crCtx.messages( MsgCodes.`N.disabled`, chCountDisabled ),
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
          }()
        },

        MuiListItemSecondaryAction()(
          MuiToolTip(
            new MuiToolTipProps {
              override val title = React.Fragment(
                _createMsg,
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


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps( ReactDiodeUtil.modelProxyValueF )
    .renderBackend[Backend]
    .configure( ReactDiodeUtil.statePropsValShouldComponentUpdate( FastEqUtil.AnyRefFastEq ) )
    .build

}
