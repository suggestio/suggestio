package io.suggest.bill.cart.v.order

import com.materialui.{Mui, MuiAvatar, MuiButton, MuiButtonProps, MuiButtonVariants, MuiCard, MuiCardContent, MuiCardHeader, MuiColorTypes, MuiDialog, MuiDialogActions, MuiDialogActionsProps, MuiDialogContent, MuiDialogMaxWidths, MuiDialogProps, MuiDialogTitle, MuiGrid, MuiLinearProgress, MuiLinearProgressProps, MuiProgressVariants, MuiSx}
import diode.data.Pot
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.bill.cart.m.{UnHoldOrderDialogOpen, UnHoldOrderRequest}
import io.suggest.common.empty.OptionUtil
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.react.ReactCommonUtil.Implicits.VdomElOptionExt
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.sjs.common.empty.JsOptionUtil
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.ueq.UnivEqUtil._
import io.suggest.ueq.JsUnivEqUtil._

import scala.scalajs.js

/** Confirmation dialog component for unholding (cancelling not-yet-paid) order. */
class UnholdOrderDiaR(
                       crCtxP      : React.Context[MCommonReactCtx],
                     ) {

  type Props_t = Pot[Boolean]
  type Props = ModelProxy[Props_t]


  case class State(
                    isDiaOpenedSomeC        : ReactConnectProxy[Some[Boolean]],
                    isRequestPendingSomeC   : ReactConnectProxy[Some[Boolean]],
                    errorOptC               : ReactConnectProxy[Option[Throwable]],
                  )


  class Backend( $: BackendScope[Props, State] ) {

    private lazy val _onDialogClose = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, UnHoldOrderDialogOpen( isOpen = false ) )
    }

    private lazy val _onUnholdConfirmClick = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, UnHoldOrderRequest() )
    }

    def render(s: State): VdomElement = {
      s.isDiaOpenedSomeC { isDiaOpenedSomeProxy =>
        crCtxP.consume { crCtx =>
          MuiDialog(
            new MuiDialogProps {
              override val open = isDiaOpenedSomeProxy.value.value
              override val onClose = _onDialogClose
              override val fullWidth = true
              override val maxWidth = MuiDialogMaxWidths.xs
            }
          )(
            MuiDialogTitle()(
              crCtx.messages( MsgCodes.`Cancel.order...` ),
            ),

            MuiDialogContent()(
              crCtx.messages( MsgCodes.`Are.you.sure` ),

              // Render error, if error.
              s.errorOptC { errorOptProxy =>
                errorOptProxy.value.whenDefinedEl { ex =>
                  MuiCard()(
                    MuiCardHeader(
                      new MuiCardHeader.Props {
                        override val avatar = {
                          MuiAvatar()(
                            Mui.SvgIcons.Error()(),
                          ).raw
                        }
                        override val title = crCtx.messages( MsgCodes.`Something.gone.wrong` ).rawNode
                      }
                    ),
                    MuiCardContent()(
                      ex.getClass.getSimpleName,
                      HtmlConstants.COLON,
                      HtmlConstants.SPACE,
                      ex.getMessage,
                    ),
                  )
                }
              },
            ),

            MuiDialogActions(
              new MuiDialogActionsProps {
                override val sx = new MuiSx {
                  override val justifyContent = MuiGrid.JustifyContent.`space-between`
                }
              }
            )(

              s.isRequestPendingSomeC { isRequestPendingSomeProxy =>
                val isPending = isRequestPendingSomeProxy.value.value

                React.Fragment(
                  // Confirmation button:
                  MuiButton(
                    new MuiButtonProps {
                      override val startIcon = Mui.SvgIcons.RemoveShoppingCart()().raw
                      override val onClick = _onUnholdConfirmClick
                      override val color = MuiColorTypes.error
                      override val variant = MuiButtonVariants.contained
                      override val disabled = isPending
                    }
                  )(
                    crCtx.messages( MsgCodes.`Yes` ),
                  ),

                  // Pending progress bar:
                  MuiLinearProgress.component(
                    new MuiLinearProgressProps {
                      override val variant =
                        if (isPending) MuiProgressVariants.indeterminate
                        else MuiProgressVariants.determinate
                      override val value = JsOptionUtil.maybeDefined( !isPending )( 0 )
                      override val sx = new MuiSx {
                        override val visibility = js.defined {
                          if (isPending) Css.Display.VISIBLE
                          else Css.Display.HIDDEN
                        }
                        override val flexGrow = js.defined( 1 )
                      }
                    }
                  ),

                  // Close dialog button:
                  MuiButton(
                    new MuiButtonProps {
                      override val variant = MuiButtonVariants.text
                      override val onClick = _onDialogClose
                      override val disabled = isPending
                    }
                  )(
                    crCtx.messages( MsgCodes.`Close` ),
                  ),
                )
              }

            ),
          )
        }
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]
    .initialStateFromProps { propsProxy =>
      State(

        isDiaOpenedSomeC = propsProxy.connect { props =>
          val isDiaVisible = props !===* Pot.empty
          OptionUtil.SomeBool( isDiaVisible )
        },

        isRequestPendingSomeC = propsProxy.connect { props =>
          OptionUtil.SomeBool( props.isPending )
        },

        errorOptC = propsProxy.connect { props =>
          props.exceptionOption
        },

      )
    }
    .renderBackend[Backend]
    .build

}
