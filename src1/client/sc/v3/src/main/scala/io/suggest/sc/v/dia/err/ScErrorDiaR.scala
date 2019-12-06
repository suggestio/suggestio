package io.suggest.sc.v.dia.err

import com.materialui.{Mui, MuiDialog, MuiDialogActions, MuiDialogContent, MuiDialogContentText, MuiDialogProps, MuiDialogTitle, MuiFab, MuiFabProps, MuiFabVariants, MuiIconButton, MuiIconButtonClasses, MuiIconButtonProps, MuiLinearProgress, MuiLinearProgressProps, MuiProgressVariants, MuiSvgIconProps, MuiToolTip, MuiToolTipProps, MuiTypoGraphy, MuiTypoGraphyProps, MuiTypoGraphyVariants}
import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.empty.OptionUtil
import io.suggest.css.Css
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.sc.m.dia.err.MScErrorDia
import io.suggest.sc.m.{CloseError, RetryError}
import io.suggest.sc.styl.ScCssStatic
import io.suggest.spa.OptFastEq
import io.suggest.ueq.UnivEqUtil._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.12.2019 12:06
  * Description: Диалог ошибки выдачи.
  */
class ScErrorDiaR(
                   commonReactCtxProv     : React.Context[MCommonReactCtx],
                 ) {

  type Props_t = MScErrorDia
  type Props = ModelProxy[Option[Props_t]]

  case class State(
                    isVisibleSomeC      : ReactConnectProxy[Some[Boolean]],
                    messageCodeOptC     : ReactConnectProxy[Option[String]],
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

      val closeMsg = commonReactCtxProv.consume { crCtx =>
        crCtx.messages( MsgCodes.`Close` )
      }

      // Дочерние элементы диалога:
      val dialogChildren = List[VdomElement](

        // Заголовок диалога:
        MuiDialogTitle()(
          // ErrorOutline | Healing | Rowing | Bathtub
          //Mui.SvgIcons.Healing()(),

          commonReactCtxProv.consume { crCtx =>
            crCtx.messages( MsgCodes.`Something.gone.wrong` )
          },

          // Кнопка закрытия диалога ошибки - справа.
          MuiToolTip(
            new MuiToolTipProps {
              override val title = closeMsg.rawElement
            }
          )(
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
        ),

        // Содержимое диалога:
        MuiDialogContent()(

          // Текст ошибки:
          MuiDialogContentText()(
            MuiTypoGraphy(
              new MuiTypoGraphyProps {
                override val variant = MuiTypoGraphyVariants.h3
              }
            )(
              commonReactCtxProv.consume { crCtx =>
                s.messageCodeOptC {
                  _.value.whenDefinedEl { messageCode =>
                    <.span(
                      crCtx.messages( messageCode ),
                    )
                  }
                }
              }
            )
          ),


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
                      else MuiProgressVariants.static
                    }
                  }
                ),
              )
            }
          }

        ),


        // Кнопка "Повторить" запрос или что-то такое.
        MuiDialogActions()(

          // Кнопка "Повторить", когда возможно.
          {
            val onClickCbF = ReactCommonUtil.cbFun1ToJsCb( _onRetryClick )
            val retryIcon = Mui.SvgIcons.Cached()()
            val retryText = commonReactCtxProv.consume { crCtx =>
              crCtx.messages( MsgCodes.`Try.again` )
            }
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
                  retryText,
                )
              }
            }
          },

          // Кнопка сокрытия сообщения об ошибке.
          MuiFab {
            new MuiFabProps {
              override val variant = MuiFabVariants.extended
              override val onClick = _onCloseCbF
            }
          } (
            closeMsg
          ),

        ),

      )

      // Весь диалог:
      s.isVisibleSomeC { isVisibleSomeProxy =>
        MuiDialog(
          new MuiDialogProps {
            override val open     = isVisibleSomeProxy.value.value
            override val onClose  = _onCloseCbF
          }
        )( dialogChildren: _* )
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

      )
    }
    .renderBackend[Backend]
    .build

  def apply(propsOptProxy: Props) = component(propsOptProxy)

}
