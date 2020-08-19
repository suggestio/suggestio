package io.suggest.lk.nodes.form.r.pop

import com.materialui.{MuiButton, MuiButtonProps, MuiButtonVariants, MuiDialog, MuiDialogActions, MuiDialogClasses, MuiDialogContent, MuiDialogProps, MuiTextField, MuiTextFieldProps}
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.empty.OptionUtil
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.lk.nodes.form.m.{MEditNodeState, NodeEditCancelClick, NodeEditOkClick}
import io.suggest.lk.r.plat.{PlatformComponents, PlatformCssStatic}
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.08.2020 8:43
  * Description: Компонент диалога редактирования названия узла.
  */
class NameEditDiaR(
                    platformCssStatic     : () => PlatformCssStatic,
                    platformComponents    : PlatformComponents,
                    crCtxP                : React.Context[MCommonReactCtx],
                  ) {

  type Props_t = Option[MEditNodeState]
  type Props = ModelProxy[Props_t]

  case class State(
                    isVisibleSomeC            : ReactConnectProxy[Some[Boolean]],
                    propsOptC                 : ReactConnectProxy[Option[MEditNodeState]],
                    okBtnEnabledSomeC         : ReactConnectProxy[Some[Boolean]],
                  )

  class Backend($: BackendScope[Props, State]) {

    /** Callback нажатия по кнопке сохранения отредактированного узла. */
    private lazy val onNodeEditOkClick = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, NodeEditOkClick )
    }

    /** Callback нажатия по кнопке отмены редактирования узла. */
    private lazy val onNodeEditCancelClick = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, NodeEditCancelClick )
    }


    def render(s: State): VdomElement = {
      crCtxP.consume { crCtx =>
        // Пихаем connection'ы внутрь функции текущего connection, т.к. isOpened меняется только с открытием/сокрытием всего диалога.
        s.isVisibleSomeC { isVisibleSomeProxy =>
          val isOpened = isVisibleSomeProxy.value.value
          val platCss = platformCssStatic()
          val diaCss = new MuiDialogClasses {
            override val paper = platCss.Dialogs.paper.htmlClass
          }

          MuiDialog(
            new MuiDialogProps {
              override val open = isOpened
              override val classes = diaCss
            }
          )(

            // input-поле.
            MuiDialogContent()(
              {
                val _inputLabel = crCtx.messages( MsgCodes.`Name` ): VdomNode
                val _inputHelperText = crCtx.messages( MsgCodes.`Type.new.name.for.beacon.0` ): VdomNode

                s.propsOptC { propsOptProxy =>
                  val propsOpt = propsOptProxy.value
                  val _name = propsOpt.name
                  MuiTextField(
                    new MuiTextFieldProps {
                      override val disabled       = propsOpt.isPending
                      override val value          = _name
                      override val error          = _name.isEmpty || propsOpt.exceptionOption.nonEmpty
                      override val label          = _inputLabel.rawNode
                      override val autoFocus      = true
                      override val fullWidth      = true
                      override val required       = _name.isEmpty
                      override val helperText     = _inputHelperText.rawNode
                    }
                  )()
                }
              }
            ),


            // Кнопки дальнейшего управления диалогом:
            MuiDialogActions(
              platformComponents.diaActionsProps()(platCss)
            )(
              // Кнопка сохранения
              s.okBtnEnabledSomeC { okBtnEnabledSomeProxy =>
                MuiButton(
                  new MuiButtonProps {
                    override val disabled = !okBtnEnabledSomeProxy.value.value
                    override val onClick = onNodeEditOkClick
                    override val variant = MuiButtonVariants.text
                  }
                )(
                  crCtx.messages( MsgCodes.`Save` ),
                )
              },

              // Кнопка отмены.
              MuiButton(
                new MuiButtonProps {
                  override val onClick = onNodeEditCancelClick
                  override val variant = MuiButtonVariants.text
                }
              )(
                crCtx.messages( MsgCodes.`Cancel` ),
              ),
            ),

          )
        }
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(

        isVisibleSomeC = propsProxy.connect { props =>
          OptionUtil.SomeBool( props.nonEmpty )
        },

        propsOptC = propsProxy.connect(identity),

        okBtnEnabledSomeC = propsProxy.connect { m =>
          OptionUtil.SomeBool( m.nameValid && !m.isPending )
        },

      )
    }
    .renderBackend[Backend]
    .build

}
