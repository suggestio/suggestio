package io.suggest.sc.v.dia.err

import com.materialui.{Mui, MuiAnchorOrigin, MuiFab, MuiFabProps, MuiFabVariants, MuiIconButton, MuiIconButtonClasses, MuiIconButtonProps, MuiLinearProgress, MuiLinearProgressProps, MuiProgressVariants, MuiSnackBar, MuiSnackBarContent, MuiSnackBarContentProps, MuiSnackBarProps, MuiSvgIconProps, MuiToolTip, MuiToolTipProps, MuiTypoGraphy, MuiTypoGraphyProps, MuiTypoGraphyVariants}
import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.empty.OptionUtil
import io.suggest.css.Css
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.sc.m.dia.err.MScErrorDia
import io.suggest.sc.m.{CloseError, RetryError}
import io.suggest.sc.v.snack.SnackComp
import io.suggest.sc.v.styl.ScCssStatic
import io.suggest.spa.OptFastEq
import io.suggest.ueq.UnivEqUtil._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.scalajs.js


/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.12.2019 12:06
  * Description: wrap-компонент диалога ошибки выдачи.
  */
class ScErrorDiaR(
                   crCtxProv     : React.Context[MCommonReactCtx],
                 )
  extends SnackComp
{

  type Props_t = MScErrorDia
  type Props = ModelProxy[Option[Props_t]]

  case class State(
                    isVisibleSomeC      : ReactConnectProxy[Some[Boolean]],
                    messageCodeOptC     : ReactConnectProxy[Option[String]],
                    hintOptC            : ReactConnectProxy[Option[String]],
                    retryPendingOptC    : ReactConnectProxy[Option[Boolean]],
                  )


  class Backend($: BackendScope[Props, State]) {

    private def _onRetryClick(e: ReactEvent): Callback =
      ReactDiodeUtil.dispatchOnProxyScopeCB($, RetryError)

    private def _onCloseClick(e: ReactEvent): Callback =
      ReactDiodeUtil.dispatchOnProxyScopeCB($, CloseError)


    def render(s: State): VdomElement = {
      val _onCloseCbF = ReactCommonUtil.cbFun1ToJsCb( _onCloseClick )
      val C = ScCssStatic.Notifies

      val _message = <.div(
        // Кнопка закрытия диалога ошибки - справа.
        MuiToolTip {
          val closeMsg = crCtxProv.message( MsgCodes.`Close` )

          new MuiToolTipProps {
            override val title = closeMsg.rawElement
          }
        } (
          MuiIconButton.component {
            val cssClasses = new MuiIconButtonClasses {
              override val root = Css.flat( C.cancel.htmlClass, C.cancelTopRight.htmlClass )
            }
            new MuiIconButtonProps {
              override val onClick  = _onCloseCbF
              //override val color    = MuiColorTypes.inherit
              override val classes  = cssClasses
            }
          } (
            Mui.SvgIcons.CancelOutlined(
              // TODO Надо это? Скопипасчено из IndexSwitchAskR:
              new MuiSvgIconProps {
                override val className = C.smallBtnSvgIcon.htmlClass
              }
            )(),
          )
        ),

        // Текст ошибки:
        crCtxProv.consume { crCtx =>
          s.messageCodeOptC {
            _.value.whenDefinedEl { messageCode =>
              MuiTypoGraphy(
                new MuiTypoGraphyProps {
                  override val variant = MuiTypoGraphyVariants.body1
                }
              )(
                crCtx.messages( messageCode ),
              )
            }
          }
        },

        s.hintOptC { hintOptProxy =>
          hintOptProxy.value.whenDefinedEl { hintText =>
            MuiTypoGraphy(
              new MuiTypoGraphyProps {
                override val variant = MuiTypoGraphyVariants.body2
              }
            )(
              hintText
            )
          }
        },

        // Горизонтальный прогресс-бар для pending-запросов.
        s.retryPendingOptC {
          _.value.whenDefinedEl { isPending =>
            <.div(
              if (isPending) ^.visibility.visible
              else ^.visibility.hidden,

              MuiLinearProgress(
                new MuiLinearProgressProps {
                  override val variant = {
                    if (isPending) MuiProgressVariants.indeterminate
                    else MuiProgressVariants.determinate
                  }
                  override val value = if (isPending) js.undefined else js.defined(0)
                }
              ),
            )
          }
        },

      )


      // Экшены snack-контента.
      val _retryBtn = {
        // Кнопка "Повторить", когда возможно.
        val onClickCbF = ReactCommonUtil.cbFun1ToJsCb( _onRetryClick )
        val retryIcon = Mui.SvgIcons.Cached()()
        val retryText = crCtxProv.message( MsgCodes.`Try.again` )

        MuiToolTip(
          new MuiToolTipProps {
            override val title = retryText.rawNode
          }
        )(
          s.retryPendingOptC {
            _.value.whenDefinedEl { isPending =>
              MuiFab {
                new MuiFabProps {
                  override val variant = MuiFabVariants.extended
                  override val onClick = onClickCbF
                  override val disabled = isPending
                }
              } (
                retryIcon,
              )
            }
          }
        )
      }

      val snackContent = MuiSnackBarContent {
        new MuiSnackBarContentProps {
          override val action  = _message.rawNode
          override val message = _retryBtn.rawNode
        }
      }

      // Рендерить снизу посередине.
      val _anchorOrigin = new MuiAnchorOrigin {
        override val vertical   = MuiAnchorOrigin.bottom
        override val horizontal = MuiAnchorOrigin.center
      }
      s.isVisibleSomeC { isVisibleSomeProxy =>
        MuiSnackBar {
          new MuiSnackBarProps {
            override val open         = isVisibleSomeProxy.value.value
            override val anchorOrigin = _anchorOrigin
            override val onClose      = _onCloseCbF
          }
        } (
          snackContent
        )
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(

        isVisibleSomeC = propsProxy.connect { props =>
          OptionUtil.SomeBool( props.nonEmpty )
        }( FastEq.AnyRefEq ),

        messageCodeOptC = propsProxy.connect( _.map(_.messageCode) )( OptFastEq.Plain ),

        retryPendingOptC = propsProxy.connect {
          _.flatMap { m =>
            OptionUtil.SomeBool( m.potIsPending )
          }
        }( FastEq.AnyRefEq ),

        hintOptC = propsProxy.connect( _.flatMap(_.hint) ),

      )
    }
    .renderBackend[Backend]
    .build

}
