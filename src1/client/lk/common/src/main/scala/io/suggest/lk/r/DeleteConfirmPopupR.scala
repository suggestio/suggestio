package io.suggest.lk.r

import com.materialui.{MuiButton, MuiButtonProps, MuiButtonSizes, MuiDialog, MuiDialogActions, MuiDialogClasses, MuiDialogContent, MuiDialogMaxWidths, MuiDialogProps, MuiLinearProgress, MuiLinearProgressProps, MuiProgressVariants, MuiTypoGraphy, MuiTypoGraphyColors, MuiTypoGraphyProps}
import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.empty.OptionUtil
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.lk.m.{DeleteConfirmPopupCancel, DeleteConfirmPopupOk, MDeleteConfirmPopupS}
import io.suggest.lk.r.plat.{PlatformComponents, PlatformCssStatic}
import io.suggest.react.ReactCommonUtil
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.sjs.common.empty.JsOptionUtil
import io.suggest.spa.OptFastEq
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.scalajs.js.UndefOr

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.03.17 18:01
  * Description: Компонент формы удаления узла, рендерится в попапе.
  */
class DeleteConfirmPopupR(
                           platformCss          : () => PlatformCssStatic,
                           platformComponents   : PlatformComponents,
                           crCtxP               : React.Context[MCommonReactCtx],
                         ) {

  type Props = ModelProxy[Option[MDeleteConfirmPopupS]]


  case class State(
                    diaOpenedSomeC        : ReactConnectProxy[Some[Boolean]],
                    isPendingSomeC        : ReactConnectProxy[Some[Boolean]],
                    exceptionOptC         : ReactConnectProxy[Option[Throwable]],
                  )

  class Backend($: BackendScope[Props, State]) {

    /** Callback клика по кнопке ПОДТВЕРЖДЕНИЯ удаления узла. */
    private val onOkClick = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      dispatchOnProxyScopeCB($, DeleteConfirmPopupOk)
    }

    /** Callback нажатия кнопки ОТМЕНЫ удаления узла. */
    private val onCancelClick = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      dispatchOnProxyScopeCB($, DeleteConfirmPopupCancel)
    }


    def render(s: State): VdomElement = {
      val platCss = platformCss()
      val diaCss = new MuiDialogClasses {
        override val paper = platCss.Dialogs.paper.htmlClass
      }

      val diaChs = List[VdomElement](
        // Заголовок.
        platformComponents.diaTitle( Nil )(
          crCtxP.message( MsgCodes.`Are.you.sure` )
        ),

        // Прогресс-бар
        MuiDialogContent()(
          s.isPendingSomeC { isPendingSomeProxy =>
            val isPending = isPendingSomeProxy.value.value
            <.span(
              if (isPending) ^.visibility.visible
              else ^.visibility.hidden,

              MuiLinearProgress(
                new MuiLinearProgressProps {
                  override val variant = if (isPending) MuiProgressVariants.indeterminate else MuiProgressVariants.determinate
                  override val value = JsOptionUtil.maybeDefined( !isPending )(0)
                }
              ),
            )
          },

          s.exceptionOptC { exceptionOpt =>
            exceptionOpt.value.whenDefinedEl { ex =>
              MuiTypoGraphy(
                new MuiTypoGraphyProps {
                  override val color = MuiTypoGraphyColors.error
                }
              )(
                ex.getMessage,
              )
            }
          },

        ),

        // Кнопки
        MuiDialogActions {
          platformComponents.diaActionsProps()( platCss )
        }(
          // Кнопка подтверждения
          {
            val yesMsg = crCtxP.message( MsgCodes.`Yes.delete.it` )
            s.isPendingSomeC { isPendingSomeProxy =>
              val isPending = isPendingSomeProxy.value.value
              MuiButton(
                new MuiButtonProps {
                  override val onClick  = onOkClick
                  override val disabled = isPending
                  override val size = MuiButtonSizes.large
                }
              )( yesMsg )
            }
          },

          // Кнопка отмены.
          MuiButton(
            new MuiButtonProps {
              override val onClick = onCancelClick
              override val size = MuiButtonSizes.large
            }
          )(
            crCtxP.message( MsgCodes.`Cancel` )
          ),
        ),
      )

      s.diaOpenedSomeC { diaOpenedSomeProxy =>
        MuiDialog {
          new MuiDialogProps {
            override val open = diaOpenedSomeProxy.value.value
            override val classes = diaCss
            override val onClose = onCancelClick
            override val maxWidth = MuiDialogMaxWidths.sm
            override val fullWidth = true
          }
        }( diaChs: _* )
      }
    }

  }


  val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .initialStateFromProps { propsProxy =>
      State(

        diaOpenedSomeC = propsProxy.connect { propsOpt =>
          OptionUtil.SomeBool( propsOpt.isDefined )
        },

        isPendingSomeC = propsProxy.connect { propsOpt =>
          val isPending = propsOpt.exists(_.request.isPending)
          OptionUtil.SomeBool( isPending )
        },

        exceptionOptC = propsProxy.connect { propsOpt =>
          propsOpt.flatMap(_.request.exceptionOption)
        }( OptFastEq.Wrapped(FastEq.AnyRefEq) ),

      )
    }
    .renderBackend[Backend]
    .build

}
