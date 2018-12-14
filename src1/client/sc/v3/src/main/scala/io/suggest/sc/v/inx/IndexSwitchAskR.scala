package io.suggest.sc.v.inx

import chandu0101.scalajs.react.components.materialui.{Mui, MuiButton, MuiButtonProps, MuiButtonSizes, MuiButtonVariants, MuiColorTypes, MuiSnackBar, MuiSnackBarAnchorOrigin, MuiSnackBarContent, MuiSnackBarContentClasses, MuiSnackBarContentProps, MuiSnackBarProps, MuiSvgIconProps}
import diode.react.ModelProxy
import io.suggest.css.CssR
import io.suggest.i18n.MsgCodes
import io.suggest.msg.Messages
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.sc.m.inx.{ApproveIndexSwitch, CancelIndexSwitch, MInxSwitchAskS}
import io.suggest.sc.styl.ScCssStatic
import io.suggest.sc.v.hdr.LogoR
import io.suggest.sc.v.search.NodesFoundR
import japgolly.scalajs.react.{BackendScope, Callback, ReactEvent, ScalaComponent}
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.11.18 18:43
  * Description: wrap-React-компонент всплывающего вопроса о переключении выдачи в новую локацию.
  */
class IndexSwitchAskR(
                       val logoR    : LogoR,
                       nodesFoundR  : NodesFoundR,
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
      val scCss = ScCssStatic.Notifies

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
          val _message: VdomNode = <.div(
            ^.`class` := scCss.content.htmlClass,
            Messages( MsgCodes.`Location.changed` ),

            <.br,

            // Логотип узла:
            propsOpt.whenDefined { props =>
              props.searchCss.fold [VdomElement] {
                val i = props.nodesResp.results.head.props
                propsOptProxy.wrap { _ =>
                  val logoProps = logoR.PropsVal(
                    logoOpt     = i.logoOpt,
                    nodeNameOpt = i.name,
                    styled      = false,
                  )
                  Some( logoProps ): logoR.Props_t
                }( logoR.apply )
              } { searchCss =>
                <.div(
                  propsOptProxy.wrap(_ => searchCss)( CssR.apply ),
                  propsOptProxy.wrap { _ =>
                    NodesFoundR.PropsVal(
                      req             = searchCss.args.req,
                      hasMore         = false,
                      selectedIds     = Set.empty,
                      searchCss       = searchCss,
                    )
                  }( nodesFoundR.apply )
                )

              }
            },

          )

          val btnIconProps = new MuiSvgIconProps {
            override val className = scCss.smallBtnSvgIcon.htmlClass
          }

          // Содержание правой части:
          val _action = <.span(
            {
              // Кнопка подтверждения перехода в узел:
              val msgCode = MsgCodes.`Go.into`
              MuiButton.component.withKey( msgCode )(
                new MuiButtonProps {
                  override val onClick = _onApproveJsCbF
                  override val variant = MuiButtonVariants.text
                  override val size = MuiButtonSizes.small
                  override val color = MuiColorTypes.inherit
                }
              )(
                Mui.SvgIcons.CheckCircle(btnIconProps)(),
                Messages( msgCode )
              )
            },

            <.br,

            {
              // Кнопка сокрытия уведомления:
              val msgCode = MsgCodes.`Cancel`
              // Сделать ToolTip с банальной подсказкой? Надо?
              MuiButton.component.withKey(msgCode)(
                new MuiButtonProps {
                  override val onClick = _onCloseJsCbF
                  override val variant = MuiButtonVariants.text
                  override val size = MuiButtonSizes.small
                  override val color = MuiColorTypes.inherit
                }
              )(
                Mui.SvgIcons.CancelOutlined(btnIconProps)(),
                Messages( msgCode )
              )
            },
          )

          val cssClasses = new MuiSnackBarContentClasses {
            // Чтобы кнопки выравнивались вертикально, а не горизонтально
            override val action = scCss.snackActionCont.htmlClass
          }

          // Объединяем всё:
          new MuiSnackBarContentProps {
            override val message = _message.rawNode
            override val action = _action.rawNode
            override val classes = cssClasses
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
