package io.suggest.lk.nodes.form.r.pop

import com.materialui.{Mui, MuiAlert, MuiAlertTitle, MuiButton, MuiButtonProps, MuiButtonSizes, MuiCircularProgress, MuiCircularProgressProps, MuiColorTypes, MuiDialog, MuiDialogActions, MuiDialogClasses, MuiDialogContent, MuiDialogMaxWidths, MuiDialogProps, MuiProgressVariants, MuiTypoGraphy, MuiTypoGraphyColors, MuiTypoGraphyProps, MuiTypoGraphyVariants}
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.empty.OptionUtil
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.lk.nodes.form.m.{MNfcDiaS, MNfcOperations, NfcDialog, NfcWrite, NodesDiConf}
import io.suggest.lk.r.plat.{PlatformComponents, PlatformCssStatic}
import io.suggest.react.ReactCommonUtil.Implicits.VdomElOptionExt
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.spa.OptFastEq
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.ueq.JsUnivEqUtil._

import scala.scalajs.js.UndefOr

/** NFC Dialog react-component. */
class NfcDialogR(
                  diConfig              : NodesDiConf,
                  platformCssStatic     : () => PlatformCssStatic,
                  platformComponents    : PlatformComponents,
                  crCtxP                : React.Context[MCommonReactCtx],
                ) {

  type Props_t = Option[MNfcDiaS]
  type Props = ModelProxy[Props_t]


  case class State(
                    dialogOpenedSome          : ReactConnectProxy[Some[Boolean]],
                    writeDisabledOpt          : ReactConnectProxy[Option[Boolean]],
                    canCancelOperationOpt     : ReactConnectProxy[Option[Boolean]],
                    operationPendingSome      : ReactConnectProxy[Some[Boolean]],
                    operationErrorOpt         : ReactConnectProxy[Option[Throwable]],
                  )


  class Backend($: BackendScope[Props, State]) {

    private val _onDialogCloseClick = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, NfcDialog( isOpen = false ) )
    }

    private val _onWriteBtnClick = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, NfcWrite( Some(MNfcOperations.WriteShowcase) ) )
    }

    /** User cancelling/stopping current NFC operation. */
    private val _onCancelOperationClick = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, NfcWrite.cancel )
    }

    private val _onMakeReadOnlyClick = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, NfcWrite( Some(MNfcOperations.MakeReadOnly) ) )
    }


    def render(s: State): VdomElement = {
      val platCss = platformCssStatic()

      // Dialog content children:
      val diaChs = List[VdomNode](

        // Dialog title.
        platformComponents.diaTitle(Nil)(
          platformComponents.diaTitleText(
            MsgCodes.`NFC`,
          ),
        ),


        // Write tag button:
        MuiDialogContent()(

          // Pending operation.
          {
            // Cancelling current operation button.
            val cancelBtn = s.canCancelOperationOpt { canCancelOperationOptProxy =>
              canCancelOperationOptProxy.value.whenDefinedEl { canCancelOp =>
                MuiButton(
                  new MuiButtonProps {
                    override val startIcon  = Mui.SvgIcons.Cancel()().rawNode
                    override val onClick    = _onCancelOperationClick
                    override val disabled   = !canCancelOp
                    override val fullWidth  = true
                    override val size       = MuiButtonSizes.large
                  }
                )(
                  crCtxP.message( MsgCodes.`Cancel` ),
                )
              }
            }

            s.operationPendingSome { operationPendingSomeProxy =>
              ReactCommonUtil.maybeEl( operationPendingSomeProxy.value.value ) {
                MuiAlert(
                  new MuiAlert.Props {
                    override val icon = {
                      MuiCircularProgress(
                        new MuiCircularProgressProps {
                          override val variant = MuiProgressVariants.indeterminate
                        }
                      )
                        .rawNode
                    }
                    override val severity = MuiAlert.Severity.INFO
                  }
                )(
                  // Title needed, because circular progress increases height.
                  MuiAlertTitle()(
                    crCtxP.message( MsgCodes.`Bring.nfc.tag.to.device` ),
                  ),
                  cancelBtn
                )
              }
            }
          },

          // NFC Operation error:
          s.operationErrorOpt { operationErrorOptProxy =>
            operationErrorOptProxy.value.whenDefinedEl { ex =>
              MuiAlert(
                new MuiAlert.Props {
                  override val severity = MuiAlert.Severity.ERROR
                }
              )(
                MuiAlertTitle()(
                  crCtxP.message( MsgCodes.`Error` ),
                ),
                Option( ex.getMessage )
                  .filter( _.nonEmpty )
                  .getOrElse[String]( ex.getClass.getSimpleName ),
              )
            }
          },

          // Writing instructions
          MuiTypoGraphy(
            new MuiTypoGraphyProps {
              override val variant = MuiTypoGraphyVariants.body1
            }
          )(
            crCtxP.consume { crCtx =>
              crCtx.messages(
                MsgCodes.`You.can.flash.nfc.tag.with.link.to.0`,
                crCtx.messages(
                  diConfig
                    .contextAdId()
                    .fold( MsgCodes.`_nfc._to.current.node` )( _ => MsgCodes.`_nfc._to.current.ad` )
                ),
              )
            }
          ),

          // Write showcase button
          {
            val ico = Mui.SvgIcons.Memory()()
            s.writeDisabledOpt { writeDisabledSomeProxy =>
              writeDisabledSomeProxy.value.whenDefinedEl { isWriteDisabled =>
                MuiButton(
                  new MuiButtonProps {
                    override val startIcon  = ico.rawNode
                    override val onClick    = _onWriteBtnClick
                    override val disabled   = isWriteDisabled
                    override val color      = MuiColorTypes.primary
                    override val fullWidth  = true
                  }
                )(
                  crCtxP.message( MsgCodes.`Write.nfc.tag` ),
                )
              }
            }
          },

          // Make read-only operation, if API available:
          ReactCommonUtil.maybeNode( diConfig.nfcApi.exists(_.canMakeReadOnly) ) {
            val ico = Mui.SvgIcons.Lock()()
            s.writeDisabledOpt { writeDisabledSomeProxy =>
              writeDisabledSomeProxy.value.whenDefinedEl { isWriteDisabled =>
                <.div(
                  MuiTypoGraphy(
                    new MuiTypoGraphyProps {
                      override val variant = MuiTypoGraphyVariants.body1
                      val mt = 3
                    }
                  )(
                    crCtxP.message( MsgCodes.`Also.you.can.make.nfc.tag.non.writable` ),
                  ),

                  MuiTypoGraphy(
                    new MuiTypoGraphyProps {
                      override val color = MuiTypoGraphyColors.error
                      override val variant = MuiTypoGraphyVariants.caption
                    }
                  )(
                    crCtxP.message( MsgCodes.`This.action.cannot.be.undone` ),
                  ),

                  MuiButton(
                    new MuiButtonProps {
                      override val startIcon  = ico.rawNode
                      override val onClick    = _onMakeReadOnlyClick
                      override val disabled   = isWriteDisabled
                      override val color      = MuiColorTypes.secondary
                      override val fullWidth  = true
                    }
                  )(
                    crCtxP.message( MsgCodes.`Make.nfc.tag.read.only` ),
                  ),
                )
              }
            }
          },



        ),


        // Dialog controls.
        MuiDialogActions(
          platformComponents.diaActionsProps()(platCss)
        )(
          // Кнопка "Закрыть"
          MuiButton(
            new MuiButtonProps {
              override val onClick = _onDialogCloseClick
              override val size = MuiButtonSizes.large
            }
          )(
            crCtxP.message( MsgCodes.`Close` ),
          ),
        ),

      )

      val dialogCss = new MuiDialogClasses {
        override val paper = platCss.Dialogs.paper.htmlClass
      }
      s.dialogOpenedSome { dialogOpenedProxy =>
        MuiDialog(
          new MuiDialogProps {
            override val open = dialogOpenedProxy.value.value
            override val classes = dialogCss
            override val onClose = _onDialogCloseClick
            override val maxWidth = MuiDialogMaxWidths.xs
          }
        )( diaChs: _* )
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(

        dialogOpenedSome = propsProxy.connect { m =>
          OptionUtil.SomeBool(m.isDefined)
        },

        writeDisabledOpt = propsProxy.connect { m =>
          m.fold( OptionUtil.SomeBool.someTrue ) { s =>
            OptionUtil.SomeBool( s.writing.isPending )
          }
        },

        canCancelOperationOpt = propsProxy.connect { m =>
          m.flatMap(
            _.cancelF.flatMap(
              _ => OptionUtil.SomeBool.someTrue))
        },

        operationPendingSome = propsProxy.connect { m =>
          val isPending = m.exists(_.writing.isPending)
          OptionUtil.SomeBool( isPending )
        },

        operationErrorOpt = propsProxy.connect { m =>
          m.flatMap(_.writing.exceptionOption)
        }( OptFastEq.Plain ),

      )
    }
    .renderBackend[Backend]
    .build

}
