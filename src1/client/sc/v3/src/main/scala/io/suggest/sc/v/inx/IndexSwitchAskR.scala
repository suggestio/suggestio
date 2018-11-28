package io.suggest.sc.v.inx

import chandu0101.scalajs.react.components.materialui.{Mui, MuiButton, MuiButtonProps, MuiButtonSizes, MuiIconButton, MuiIconButtonProps, MuiSnackBar, MuiSnackBarAnchorOrigin, MuiSnackBarContent, MuiSnackBarContentProps, MuiSnackBarProps}
import diode.react.ModelProxy
import io.suggest.common.html.HtmlConstants
import io.suggest.i18n.MsgCodes
import io.suggest.msg.Messages
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.sc.m.inx.{ApproveIndexSwitch, CancelIndexSwitch, MInxSwitchAskS}
import io.suggest.sc.v.hdr.LogoR
import japgolly.scalajs.react.{BackendScope, Callback, ReactEvent, ScalaComponent}
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.11.18 18:43
  * Description: wrap-React-компонент всплывающего вопроса о переключении выдачи в новую локацию.
  */
class IndexSwitchAskR(
                       val logoR: LogoR
                     ) {

  import io.suggest.spa.OptFastEq.Wrapped
  import logoR.LogoPropsValFastEq

  type Props_t = Option[MInxSwitchAskS]
  type Props = ModelProxy[Props_t]


  class Backend($: BackendScope[Props, Unit]) {

    /** Закрытие плашки без аппрува. */
    private def _onClose(e: ReactEvent): Callback =
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, CancelIndexSwitch )
    private lazy val _onCloseJsCbF = ReactCommonUtil.cbFun1ToJsCb( _onClose )


    private def _onApprove(e: ReactEvent): Callback =
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, ApproveIndexSwitch )
    private lazy val _onApproveJsCbF = ReactCommonUtil.cbFun1ToJsCb( _onApprove )


    def render(propsOptProxy: Props): VdomElement = {
      // Чтобы диалог выплывал снизу, надо чтобы контейнер компонента был заранее (всегда) отрендеренным в DOM.
      val propsOpt = propsOptProxy.value

      MuiSnackBar {
        val _anchorOrigin = new MuiSnackBarAnchorOrigin {
          override val vertical   = MuiSnackBarAnchorOrigin.bottom
          override val horizontal = MuiSnackBarAnchorOrigin.center
        }
        new MuiSnackBarProps {
          override val open         = propsOpt.nonEmpty
          override val anchorOrigin = _anchorOrigin
          override val onClose      = _onCloseJsCbF
        }
      } (

        // Содержимое плашки - приглашение на смену узла.
        MuiSnackBarContent {
          // Содержимое левой части сообщения:
          val _message: VdomNode = <.span(
            Messages( MsgCodes.`Location.changed` ),

            <.br,
            // Логотип узла:
            propsOpt.whenDefined { props =>
              val i = props.nextIndex
              propsOptProxy.wrap { _ =>
                val logoProps = logoR.PropsVal(
                  logoOpt     = i.logoOpt,
                  nodeNameOpt = i.name
                )
                Some( logoProps ): logoR.Props_t
              }( logoR.apply )
            },
          )

          // Содержание правой части:
          val _action = VdomArray(
            {
              // Кнопка подтверждения перехода в узел:
              val msgCode = MsgCodes.`Go.into`
              MuiButton.component.withKey( msgCode )(
                new MuiButtonProps {
                  override val onClick = _onApproveJsCbF
                  override val size = MuiButtonSizes.small
                }
              )(
                Messages( msgCode ),
                HtmlConstants.ELLIPSIS,
              )
            },

            {
              // Кнопка сокрытия уведомления:
              val msgCode = MsgCodes.`Close`
              // Сделать ToolTip с банальной подсказкой? Надо?
              MuiIconButton.component.withKey(msgCode)(
                new MuiIconButtonProps {
                  override val onClick = _onCloseJsCbF
                }
              )(
                Mui.SvgIcons.Close()()
              )
            },

          )

          // Объединяем всё:
          new MuiSnackBarContentProps {
            override val message = _message.rawNode
            override val action = _action.rawNode
          }
        }

      )

    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build


  def apply(propsOptProxy: Props) = component( propsOptProxy )

}
