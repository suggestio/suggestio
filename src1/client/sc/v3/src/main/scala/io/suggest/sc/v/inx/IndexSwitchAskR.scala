package io.suggest.sc.v.inx

import com.materialui.{Mui, MuiButton, MuiButtonClasses, MuiButtonProps, MuiButtonSizes, MuiButtonVariants, MuiColorTypes, MuiSnackBar, MuiSnackBarAnchorOrigin, MuiSnackBarContent, MuiSnackBarContentClasses, MuiSnackBarContentProps, MuiSnackBarProps, MuiSvgIconProps}
import diode.react.ModelProxy
import io.suggest.css.CssR
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.sc.m.inx.{CancelIndexSwitch, MInxSwitchAskS}
import io.suggest.sc.styl.ScCssStatic
import io.suggest.sc.v.search.{NodesFoundR, SearchCss}
import japgolly.scalajs.react.{BackendScope, Callback, React, ReactEvent, ScalaComponent}
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.11.18 18:43
  * Description: wrap-React-компонент всплывающего вопроса о переключении выдачи в новую локацию.
  */
class IndexSwitchAskR(
                       nodesFoundR          : NodesFoundR,
                       commonReactCtxProv   : React.Context[MCommonReactCtx],
                     ) {

  type Props_t = Option[MInxSwitchAskS]
  type Props = ModelProxy[Props_t]

  class Backend($: BackendScope[Props, Unit]) {

    /** Закрытие плашки без аппрува. */
    private def _onClose(e: ReactEvent): Callback =
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, CancelIndexSwitch )
    private lazy val _onCloseJsCbF = ReactCommonUtil.cbFun1ToJsCb( _onClose )


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
          val btnIconProps = new MuiSvgIconProps {
            override val className = scCss.smallBtnSvgIcon.htmlClass
          }

          // Содержимое левой части сообщения:
          val _message: VdomNode = <.div(
            ^.`class` := scCss.content.htmlClass,

            commonReactCtxProv.consume { crCtx =>
              crCtx.messages( MsgCodes.`Location.changed` )
            },

            // Кнопка сокрытия уведомления:
            MuiButton.component {
              val cssClasses = new MuiButtonClasses {
                override val root = scCss.cancel.htmlClass
              }
              new MuiButtonProps {
                override val onClick = _onCloseJsCbF
                override val variant = MuiButtonVariants.text
                override val size = MuiButtonSizes.small
                override val color = MuiColorTypes.inherit
                override val classes = cssClasses
              }
            } (
              Mui.SvgIcons.CancelOutlined(btnIconProps)(),
              commonReactCtxProv.consume { crCtx =>
                crCtx.messages( MsgCodes.`Cancel` )
              }
            ),

            <.br,

            // Логотип узла:
            propsOpt.whenDefined { props =>
              <.div(
                propsOptProxy.wrap(_ => props.searchCss)( CssR.apply )(implicitly, SearchCss.SearchCssFastEq),
                propsOptProxy.wrap { _ =>
                  NodesFoundR.PropsVal(
                    req             = props.searchCss.args.req,
                    hasMore         = false,
                    selectedIds     = Set.empty,
                    searchCss       = props.searchCss,
                  )
                }( nodesFoundR.apply )(implicitly, NodesFoundR.NodesFoundRPropsValFastEq)
              )
            },
          )

          val cssClasses = new MuiSnackBarContentClasses {
            // Чтобы кнопки выравнивались вертикально, а не горизонтально
            override val action = scCss.snackActionCont.htmlClass
            override val message = scCss.snackMsg.htmlClass
          }

          // Объединяем всё:
          new MuiSnackBarContentProps {
            override val message = _message.rawNode
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
