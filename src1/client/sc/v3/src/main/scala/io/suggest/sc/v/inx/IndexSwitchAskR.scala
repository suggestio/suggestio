package io.suggest.sc.v.inx

import com.materialui.{Mui, MuiAnchorOrigin, MuiButton, MuiButtonClasses, MuiButtonProps, MuiButtonSizes, MuiButtonVariants, MuiColorTypes, MuiSnackBar, MuiSnackBarContent, MuiSnackBarContentClasses, MuiSnackBarContentProps, MuiSnackBarProps, MuiSvgIconProps}
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.empty.OptionUtil
import io.suggest.css.CssR
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import ReactCommonUtil.Implicits._
import ReactDiodeUtil.Implicits._
import io.suggest.sc.m.MScReactCtx
import io.suggest.sc.m.inx.{CancelIndexSwitch, MInxSwitch}
import io.suggest.sc.m.search.{MNodesFoundRowProps, MNodesFoundS}
import io.suggest.sc.v.search.SearchCss
import io.suggest.sc.v.search.found.{NfListR, NfRowR}
import io.suggest.sc.v.styl.ScCssStatic
import io.suggest.spa.OptFastEq
import japgolly.scalajs.react.{BackendScope, React, ReactEvent, ScalaComponent}
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.11.18 18:43
  * Description: wrap-компонент всплывающего вопроса о переключении выдачи в новую локацию.
  */
class IndexSwitchAskR(
                       nfListR          : NfListR,
                       crCtxProv        : React.Context[MCommonReactCtx],
                       scReactCtxProv   : React.Context[MScReactCtx],
                     ) {

  type Props_t = MInxSwitch
  type Props = ModelProxy[Props_t]

  case class State(
                    isOpenedSomeC             : ReactConnectProxy[Some[Boolean]],
                    nodesFoundPropsC          : ReactConnectProxy[Seq[MNodesFoundRowProps]],
                    searchCssOptC             : ReactConnectProxy[Option[SearchCss]],
                    nodesFoundSOptC           : ReactConnectProxy[Option[MNodesFoundS]],
                  )

  class Backend($: BackendScope[Props, State]) {

    /** Закрытие плашки без аппрува. */
    private lazy val _onCloseJsCbF = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, CancelIndexSwitch )
    }


    def render(s: State): VdomElement = {
      // Чтобы диалог выплывал снизу, надо чтобы контейнер компонента был заранее (всегда) отрендеренным в DOM.
      val notsCss = ScCssStatic.Notifies

      // Содержимое плашки - приглашение на смену узла.
      val snackBarContent = MuiSnackBarContent {
        val btnIconProps = new MuiSvgIconProps {
          override val className = notsCss.smallBtnSvgIcon.htmlClass
        }

        // Содержимое левой части сообщения:
        val _message = crCtxProv.consume { crCtx =>
          <.div(
            ^.`class` := notsCss.content.htmlClass,

            crCtx.messages( MsgCodes.`Location.changed` ),

            // Кнопка сокрытия уведомления:
            MuiButton.component {
              val cssClasses = new MuiButtonClasses {
                override val root = notsCss.cancel.htmlClass
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
              crCtx.messages( MsgCodes.`Cancel` ),
            ),

            <.br,

            // Список найденных узлов:
            <.div(
              s.searchCssOptC {
                _.value.whenDefinedEl { CssR.component.apply }
              },

              scReactCtxProv.consume { scReactCtx =>
                // Список найденных узлов:
                val nfRowR = NfRowR( crCtx, scReactCtx )
                val nodesFoundRows = s.nodesFoundPropsC( nfRowR.rows )
                s.nodesFoundSOptC { nodesFoundSOptProxy =>
                  nodesFoundSOptProxy.value.whenDefinedEl { nodesFoundS =>
                    val p2 = nodesFoundSOptProxy.resetZoom( nodesFoundS )
                    nfListR.component( p2 )( nodesFoundRows )
                  }
                }
              },
            ),
          )
        }

        val cssClasses = new MuiSnackBarContentClasses {
          // Чтобы кнопки выравнивались вертикально, а не горизонтально
          override val action = notsCss.snackActionCont.htmlClass
          override val message = notsCss.snackMsg.htmlClass
        }

        // Объединяем всё:
        new MuiSnackBarContentProps {
          override val message = _message.rawNode
          override val classes = cssClasses
        }
      }

      s.isOpenedSomeC { isOpenedSomeProxy =>
        MuiSnackBar {
          val _anchorOrigin = new MuiAnchorOrigin {
            override val vertical   = MuiAnchorOrigin.bottom
            override val horizontal = MuiAnchorOrigin.center
          }
          new MuiSnackBarProps {
            override val open         = isOpenedSomeProxy.value.value
            override val anchorOrigin = _anchorOrigin
            override val onClose      = _onCloseJsCbF
          }
        } ( snackBarContent )
      }

    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(

        isOpenedSomeC = propsProxy.connect { propsOpt =>
          OptionUtil.SomeBool( propsOpt.ask.nonEmpty )
        },

        nodesFoundPropsC = propsProxy.connect( _.ask.fold[Seq[MNodesFoundRowProps]](Nil)(_.nodesFoundProps) ),

        searchCssOptC = propsProxy.connect(_.searchCssOpt)( OptFastEq.Wrapped(SearchCss.SearchCssFastEq) ),

        nodesFoundSOptC = propsProxy.connect( _.nodesFoundOpt )( OptFastEq.Wrapped(MNodesFoundS.MNodesFoundSFastEq) ),

      )
    }
    .renderBackend[Backend]
    .build

}
