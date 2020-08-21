package io.suggest.lk.nodes.form.r.pop

import com.materialui.{MuiButton, MuiButtonProps, MuiButtonSizes, MuiButtonVariants, MuiDialog, MuiDialogActions, MuiDialogClasses, MuiDialogContent, MuiDialogMaxWidths, MuiDialogProps, MuiTextField, MuiTextFieldProps}
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.empty.OptionUtil
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.lk.nodes.form.m.{MEditNodeState, NodeEditCancelClick, NodeEditNameChange, NodeEditOkClick}
import io.suggest.lk.r.plat.{PlatformComponents, PlatformCssStatic}
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.spa.{FastEqUtil, OptFastEq}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.ueq.UnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.08.2020 8:43
  * Description: wrap-компонент диалога редактирования названия узла.
  */
class NameEditDiaR(
                    platformCssStatic     : () => PlatformCssStatic,
                    platformComponents    : PlatformComponents,
                    crCtxP                : React.Context[MCommonReactCtx],
                  ) {

  case class PropsVal(
                       nameOrig     : String,
                       state        : MEditNodeState,
                     )
  implicit lazy val nameEditPvFeq = FastEqUtil[PropsVal] { (a, b) =>
    (a.nameOrig ===* b.nameOrig) &&
    (a.state ===* b.state)
  }

  type Props_t = Option[PropsVal]
  type Props = ModelProxy[Props_t]

  case class State(
                    isVisibleSomeC            : ReactConnectProxy[Some[Boolean]],
                    propsOptC                 : ReactConnectProxy[Option[MEditNodeState]],
                    okBtnEnabledSomeC         : ReactConnectProxy[Some[Boolean]],
                    nameOrigC                 : ReactConnectProxy[Option[String]],
                  )

  class Backend($: BackendScope[Props, State]) {

    /** Callback нажатия по кнопке сохранения отредактированного узла. */
    private lazy val _onOkClick = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, NodeEditOkClick )
    }

    /** Callback нажатия по кнопке отмены редактирования узла. */
    private lazy val _onCancelClick = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, NodeEditCancelClick )
    }

    private lazy val _onInputChanged = ReactCommonUtil.cbFun1ToJsCb { e: ReactEventFromInput =>
      val name2 = e.target.value
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, NodeEditNameChange( name2 ) )
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
              override val onClose = _onCancelClick
              override val maxWidth = MuiDialogMaxWidths.lg
            }
          )(

            // input-поле.
            MuiDialogContent()(
              {
                val _inputLabel = crCtx.messages( MsgCodes.`Name` ): VdomNode
                val _inputHelperText = s.nameOrigC { nameOrigOptProxy =>
                  val nameOrig = nameOrigOptProxy.value getOrElse ""
                  React.Fragment(
                    crCtx.messages( MsgCodes.`Type.new.name.for.beacon.0`, nameOrig )
                  )
                }

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
                      override val required       = true
                      override val helperText     = _inputHelperText.rawNode
                      override val onChange       = _onInputChanged
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
                    override val onClick = _onOkClick
                    override val variant = MuiButtonVariants.text
                    override val size = MuiButtonSizes.large
                  }
                )(
                  crCtx.messages( MsgCodes.`Save` ),
                )
              },

              // Кнопка отмены.
              MuiButton(
                new MuiButtonProps {
                  override val onClick = _onCancelClick
                  override val variant = MuiButtonVariants.text
                  override val size = MuiButtonSizes.large
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

        propsOptC = propsProxy.connect(_.map(_.state))( OptFastEq.Plain ),

        okBtnEnabledSomeC = propsProxy.connect { m =>
          val isEnabled = m.exists { pv =>
            val s = pv.state
            s.nameValid && !s.saving.isPending
          }
          OptionUtil.SomeBool( isEnabled )
        },

        nameOrigC = propsProxy.connect( _.map(_.nameOrig) )( OptFastEq.Plain ),

      )
    }
    .renderBackend[Backend]
    .build

}
