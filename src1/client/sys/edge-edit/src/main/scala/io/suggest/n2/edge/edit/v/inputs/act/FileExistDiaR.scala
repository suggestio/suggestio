package io.suggest.n2.edge.edit.v.inputs.act

import com.materialui.{MuiButton, MuiButtonProps, MuiDialog, MuiDialogActions, MuiDialogContent, MuiDialogContentText, MuiDialogProps}
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.empty.OptionUtil
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.n2.edge.edit.m.{FileExistAppendToNodeIds, FileExistCancel, FileExistReplaceNodeIds}
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.scalajs.js
import scala.scalajs.js.UndefOr

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.01.2020 17:18
  * Description: Диалог с вопросом по поводу уже существующего файла.
  * Юзеру предоставляется выбор, что делать с ситуацией дальше.
  */
class FileExistDiaR(
                     nodeLinkR: NodeLinkR,
                     crCtxProv: React.Context[MCommonReactCtx],
                   ) {

  type Props_t = Option[String]
  type Props = ModelProxy[Props_t]

  case class State(
                    isShownSomeC    : ReactConnectProxy[Some[Boolean]],
                    nodeIdC         : ReactConnectProxy[String],
                  )

  class Backend( $: BackendScope[Props, State] ) {

    private lazy val _replaceNodeIdsClick = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, FileExistReplaceNodeIds )
    }

    private lazy val _appendToNodeIdsClick = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, FileExistAppendToNodeIds )
    }

    private lazy val _cancelClick = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, FileExistCancel )
    }


    def render(s: State): VdomElement = {
      lazy val diaContent = crCtxProv.consume { crCtx =>
        React.Fragment(

          MuiDialogContent()(
            s.nodeIdC { nodeIdProxy =>
              val nodeId = nodeIdProxy.value
              MuiDialogContentText()(
                crCtx.messages( MsgCodes.`File.already.exist.on.node.0`, nodeId ),
                nodeLinkR.component( nodeId ),
              )
            },
          ),

          MuiDialogActions()(
            // Кнопки
            MuiButton(
              new MuiButtonProps {
                override val onClick = _replaceNodeIdsClick
              }
            )(
              crCtx.messages( MsgCodes.`Replace.node.ids` )
            ),

            MuiButton(
              new MuiButtonProps {
                override val onClick = _appendToNodeIdsClick
              }
            )(
              crCtx.messages( MsgCodes.`Append.to.node.ids` )
            ),

            MuiButton(
              new MuiButtonProps {
                override val onClick = _cancelClick
              }
            )(
              crCtx.messages( MsgCodes.`Cancel` )
            ),
          ),

        )
      }

      s.isShownSomeC { isShownSomeProxy =>
        MuiDialog(
          new MuiDialogProps {
            override val open = isShownSomeProxy.value.value
            override val onClose = _cancelClick
          }
        )( diaContent )
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { props =>
      val emptyStr = ""
      State(
        isShownSomeC = props.connect { nodeIdOpt =>
          OptionUtil.SomeBool( nodeIdOpt.nonEmpty )
        },
        nodeIdC = props.connect( _ getOrElse emptyStr ),
      )
    }
    .renderBackend[Backend]
    .build

}
