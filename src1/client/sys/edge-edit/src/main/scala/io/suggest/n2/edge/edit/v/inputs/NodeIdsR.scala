package io.suggest.n2.edge.edit.v.inputs

import com.materialui.{Mui, MuiFormControl, MuiFormControlClasses, MuiFormControlProps, MuiFormLabel, MuiIconButton, MuiIconButtonProps, MuiInput, MuiInputLabel, MuiInputProps, MuiToolTip, MuiToolTipProps}
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.empty.OptionUtil
import io.suggest.common.html.HtmlConstants
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.n2.edge.edit.m.{NodeIdAdd, NodeIdChange}
import io.suggest.n2.edge.edit.v.EdgeEditCss
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.01.2020 8:52
  * Description: Редактор списка id узлов (или иных ключей эджа).
  */
class NodeIdsR(
                crCtxProv: React.Context[MCommonReactCtx],
              ) {

  type Props_t = Seq[String]
  type Props = ModelProxy[Props_t]

  case class State(
                    showAddBtnC     : ReactConnectProxy[Some[Boolean]],
                    nodeIdsC        : ReactConnectProxy[Seq[String]],
                  )


  class Backend($: BackendScope[Props, State]) {

    private def _onNodeIdChange(i: Int)(e: ReactEventFromInput): Callback = {
      val value2 = e.target.value
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, NodeIdChange(i, value2) )
    }

    private val _onAddClickCbF = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, NodeIdAdd )
    }

    def render(s: State): VdomElement = {
      val addSvg = Mui.SvgIcons.Add()()

      val addMsg = crCtxProv.consume { crCtx =>
        crCtx.messages( MsgCodes.`Add` )
      }

      MuiFormControl {
        val css = new MuiFormControlClasses {
          override val root = EdgeEditCss.input.htmlClass
        }
        new MuiFormControlProps {
          override val classes = css
        }
      } (

        MuiFormLabel()(
          crCtxProv.consume { crCtx =>
            crCtx.messages( MsgCodes.`Node.ids.or.keys` )
          },
        ),

        // Список id узлов.
        s.nodeIdsC { nodeIdsProxy =>
          <.span(
            // Список input'ов
            (for {
              (nodeId, i) <- nodeIdsProxy
                .value
                .iterator
                .zipWithIndex
            } yield {
              <.span(
                ^.key := i,
                MuiInput {
                  val _onChangeF = ReactCommonUtil.cbFun1ToJsCb( _onNodeIdChange(i) )
                  new MuiInputProps {
                    override val value = nodeId
                    override val onChange = _onChangeF
                  }
                },
                HtmlConstants.COMMA, HtmlConstants.NBSP_STR,
              )
            })
              .toVdomArray,
          )
        },

        // Кнопка добавки узла в список узлов:
        s.showAddBtnC { showAddBtnSomeProxy =>
          val showAddBtn = showAddBtnSomeProxy.value.value
          <.span(
            if (showAddBtn) ^.visibility.visible
            else ^.visibility.hidden,

            MuiToolTip(
              new MuiToolTipProps {
                override val title = addMsg.rawNode
              }
            )(
              MuiIconButton(
                new MuiIconButtonProps {
                  override val disabled = !showAddBtn
                  override val onClick = _onAddClickCbF
                }
              )(
                addSvg,
              ),
            ),
          )
        },

      )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { nodeIdsProxy =>
      State(
        showAddBtnC = nodeIdsProxy.connect { nodeIds =>
          OptionUtil.SomeBool( nodeIds.isEmpty || !(nodeIds contains "") )
        },
        nodeIdsC = nodeIdsProxy.connect( identity(_) ),
      )
    }
    .renderBackend[Backend]
    .build

}
