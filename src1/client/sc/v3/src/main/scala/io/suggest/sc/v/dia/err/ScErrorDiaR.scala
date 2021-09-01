package io.suggest.sc.v.dia.err

import com.materialui.{Mui, MuiFab, MuiFabProps, MuiFabVariants, MuiIconButton, MuiIconButtonClasses, MuiIconButtonProps, MuiLinearProgress, MuiLinearProgressProps, MuiProgressVariants, MuiSnackBarContent, MuiSnackBarContentProps, MuiToolTip, MuiToolTipProps, MuiTypoGraphy, MuiTypoGraphyProps, MuiTypoGraphyVariants}
import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.empty.OptionUtil
import io.suggest.css.Css
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.react.r.CatchR
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.sc.m.dia.err.MScErrorDia
import io.suggest.sc.m.{CloseError, RetryError}
import io.suggest.sc.v.snack.ISnackComp
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
  extends ISnackComp
{ that =>

  type Props_t = MScErrorDia
  type Props = ModelProxy[Option[Props_t]]

  case class State(
                    messageCodeOptC     : ReactConnectProxy[Option[String]],
                    hintOptC            : ReactConnectProxy[Option[String]],
                    retryPendingOptC    : ReactConnectProxy[Option[Boolean]],
                  )


  class Backend($: BackendScope[Props, State]) {

    private val onClickCbF = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      ReactDiodeUtil.dispatchOnProxyScopeCB($, RetryError)
    }

    private val _onCloseCbF = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      ReactDiodeUtil.dispatchOnProxyScopeCB($, CloseError)
    }


    def render(p: Props, s: State): VdomElement = {
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
              //override val color    = MuiColorTypes.default  // mui-5.0: button.color.default := primary
              override val classes  = cssClasses
              override val centerRipple = true
            }
          } (
            // Без стиля root = C.smallBtnSvgIcon, т.к. это слишком деформирует неотключаемую кругловатую "тень" кнопки.
            Mui.SvgIcons.CancelOutlined()(),
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
        val retryIcon = Mui.SvgIcons.Cached()()
        val retryText = crCtxProv.message( MsgCodes.`Try.again` )

        MuiToolTip(
          new MuiToolTipProps {
            override val title = retryText.rawNode
          }
        )(
          // div нужен, т.к. mui-tooltip дёргает в useIsFocusVisible.js:91, рушится на внезапный node.ownerDocument = undefined.
          <.div(
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
            },
          ),
        )
      }

      CatchR.component( p.zoom(_ => that.getClass.getSimpleName) )(
        MuiSnackBarContent {
          new MuiSnackBarContentProps {
            override val action  = _message.rawNode
            override val message = _retryBtn.rawNode
          }
        }
      )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(

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
