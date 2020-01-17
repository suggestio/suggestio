package io.suggest.n2.edge.edit.v.inputs.act

import com.materialui.{Mui, MuiDialog, MuiDialogActions, MuiDialogContent, MuiDialogProps, MuiDialogTitle, MuiFab, MuiFabProps, MuiFabVariants, MuiIconButton, MuiIconButtonProps, MuiLinearProgress, MuiLinearProgressProps, MuiProgressVariants}
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.n2.edge.edit.m.{DeleteCancel, DeleteEdge, MDeleteDiaS}
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import ReactCommonUtil.Implicits._
import diode.data.Pot
import io.suggest.common.empty.OptionUtil
import io.suggest.common.html.HtmlConstants
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.01.2020 18:22
  * Description: Диалог подтверждения удаления эджа.
  */
class DeleteDiaR(
                  crCtxProv: React.Context[MCommonReactCtx],
                ) {

  type Props_t = MDeleteDiaS
  type Props = ModelProxy[Props_t]

  case class State(
                    isOpenedSomeC       : ReactConnectProxy[Some[Boolean]],
                    isPendingSomeC      : ReactConnectProxy[Some[Boolean]],
                    errorPotC           : ReactConnectProxy[Pot[None.type]],
                  )

  class Backend($: BackendScope[Props, State]) {

    private val _onCancelClick = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, DeleteCancel )
    }

    private val _onDeleteConfirmClick = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, DeleteEdge(true) )
    }

    def render(s: State): VdomElement = {
      crCtxProv.consume { crCtx =>
        // Заголовок
        val _diaTitle = MuiDialogTitle()(
          MuiIconButton(
            new MuiIconButtonProps {
              override val onClick = _onCancelClick
            }
          )(
            Mui.SvgIcons.Close()(),
          ),
          crCtx.messages( MsgCodes.`Deletion` ),
        )

        // Содержимое диалога
        val _diaContent = MuiDialogContent()(
          crCtx.messages( MsgCodes.`Are.you.sure` ),

          s.isPendingSomeC { isPendingSomeProxy =>
            ReactCommonUtil.maybeEl( isPendingSomeProxy.value.value ) {
              MuiLinearProgress(
                new MuiLinearProgressProps {
                  override val variant = MuiProgressVariants.indeterminate
                }
              )
            }
          },

          // Рендер ошибки
          s.errorPotC { errorPotProxy =>
            errorPotProxy.value.exceptionOption.whenDefinedEl { ex =>
              <.div(
                crCtx.messages( MsgCodes.`Error` ),
                HtmlConstants.SPACE,
                ex.getClass.getSimpleName,
                HtmlConstants.COLON,
                HtmlConstants.NBSP,
                ex.getMessage,
              ): VdomElement
            }
          },
        )

        // Кнопки диалога
        val _diaActions = {
          val _cancelBtnChs = List[VdomNode](
            Mui.SvgIcons.Cancel()(),
            crCtx.messages( MsgCodes.`Cancel` ),
          )

          val _confirmDeleteChs = List[VdomNode](
            Mui.SvgIcons.Delete()(),
            crCtx.messages( MsgCodes.`Delete` ),
          )

          MuiDialogActions()(
            s.isPendingSomeC { isPendingSomeProxy =>
              val _isDisabled = isPendingSomeProxy.value.value
              React.Fragment(
                // Кнопка отмена:
                MuiFab(
                  new MuiFabProps {
                    override val variant = MuiFabVariants.extended
                    override val onClick = _onCancelClick
                    override val disabled = _isDisabled
                  }
                )(_cancelBtnChs: _*),

                // Кнопка подтверждения
                MuiFab(
                  new MuiFabProps {
                    override val variant = MuiFabVariants.extended
                    override val onClick = _onDeleteConfirmClick
                    override val disabled = _isDisabled
                  }
                )(_confirmDeleteChs: _*),
              )
            }
          )
        }

        // Диалог
        s.isOpenedSomeC { isOpenedSomeProxy =>
          MuiDialog(
            new MuiDialogProps {
              override val open = isOpenedSomeProxy.value.value
            }
          )(
            _diaTitle,
            _diaContent,
            _diaActions,
          )
        }

      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(
        isOpenedSomeC = propsProxy.connect { props =>
          OptionUtil.SomeBool( props.opened )
        },
        isPendingSomeC = propsProxy.connect { props =>
          OptionUtil.SomeBool( props.deleteReq.isPending )
        },
        errorPotC = propsProxy.connect { props =>
          if (props.deleteReq.isFailed)
            props.deleteReq
          else
            Pot.empty
        },
      )
    }
    .renderBackend[Backend]
    .build

}
