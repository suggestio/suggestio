package io.suggest.sc.v.dia.nodes

import com.materialui.{MuiButton, MuiButtonProps, MuiButtonSizes, MuiDialog, MuiDialogActions, MuiDialogClasses, MuiDialogContent, MuiDialogProps, MuiMenuItem, MuiMenuItemProps, MuiTextField, MuiTextFieldProps}
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.empty.OptionUtil
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.lk.nodes.form.LkNodesFormCircuit
import io.suggest.lk.nodes.form.m.{MLkNodesMode, MLkNodesModes}
import io.suggest.lk.nodes.form.r.LkNodesFormR
import io.suggest.lk.r.plat.{PlatformComponents, PlatformCssStatic}
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.sc.m.{MScRoot, ScNodesModeChanged, ScNodesShowHide}
import io.suggest.sc.v.styl.ScCss
import io.suggest.sjs.common.empty.JsOptionUtil
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.scalajs.js.UndefOr

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.08.2020 14:41
  * Description: Контейнер для формы редактора узлов.
  */
class ScNodesR(
                lkNodesFormR        : LkNodesFormR,
                platformComponents  : PlatformComponents,
                platfromCss         : () => PlatformCssStatic,
                crCtxP              : React.Context[MCommonReactCtx],
                scCssP              : React.Context[ScCss],
              ) {

  type Props_t = MScRoot
  type Props = ModelProxy[Props_t]

  case class State(
                    circuitOptC                   : ReactConnectProxy[Option[LkNodesFormCircuit]],
                    isDiaFullScreenSomeC          : ReactConnectProxy[Some[Boolean]],
                    haveFocusedAdAdminSomeC       : ReactConnectProxy[Some[Boolean]],
                    nodesModeC                    : ReactConnectProxy[MLkNodesMode],
                  )

  class Backend($: BackendScope[Props, State]) {

    private lazy val _onCloseClick = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, ScNodesShowHide(visible = false) )
    }

    private lazy val _onModeChange = ReactCommonUtil.cbFun1ToJsCb { e: ReactEventFromInput =>
      val nextMode = MLkNodesModes.withValue( e.target.value )
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, ScNodesModeChanged(nextMode) )
    }

    def render(s: State): VdomElement = {
      s.circuitOptC { circuitOptProxy =>
        val platCss = platfromCss()

        val circuitOpt = circuitOptProxy.value

        val diaChildren = List[VdomNode](

          // Заголовок
          platformComponents.diaTitle(Nil)(
            {
              val nodesManagementMsg = crCtxP.message( MsgCodes.`Nodes.management` )
              s.haveFocusedAdAdminSomeC { haveFocusedAdAdminSomeProxy =>
                val haveFocusedAdmin = haveFocusedAdAdminSomeProxy.value.value
                if (!haveFocusedAdmin) {
                  // Нет сфокусированной карточки: просто рендерить обычный заголовок.
                  nodesManagementMsg

                } else {
                  // Рендерим селект, где можно выбрать или управление узлами, или карточку.
                  // Пункт селекта обычного режима работы (управление узлами)
                  val nodesManageItem = MuiMenuItem(
                    new MuiMenuItemProps {
                      override val value = MLkNodesModes.NodesManage.value
                    }
                  )(
                    nodesManagementMsg,
                  )

                  // Пункт селекта для управления размещением карточки в узлах:
                  val advInNodesItem = MuiMenuItem(
                    new MuiMenuItemProps {
                      override val value = MLkNodesModes.AdvInNodes.value
                    }
                  )(
                    crCtxP.message( MsgCodes.`Current.ad.adv.management` ),
                    // TODO Отрендерить тут миниатюру главного блока текущей карточки.
                  )

                  // Непосредственно селект.
                  s.nodesModeC { nodesModeProxy =>
                    MuiTextField(
                      new MuiTextFieldProps {
                        override val select = true
                        override val label = crCtxP.message( MsgCodes.`Mode` ).rawNode
                        override val value = nodesModeProxy.value.value
                        override val fullWidth = true
                        override val onChange = _onModeChange
                      }
                    )(
                      nodesManageItem,
                      advInNodesItem,
                    )
                  }
                }
              }
            },
          ),

          // Содержимое диалога.
          MuiDialogContent()(
            circuitOpt.whenDefinedEl { circuit =>
              circuit.wrap(identity(_))( lkNodesFormR.component.apply )
            },
          ),

          // Кнопки внизу.
          MuiDialogActions(
            platformComponents.diaActionsProps()(platCss)
          )(
            MuiButton(
              new MuiButtonProps {
                override val size = MuiButtonSizes.large
                override val onClick = _onCloseClick
              }
            )(
              crCtxP.message( MsgCodes.`Close` )
            )
          ),
        )

        // Вложенный коннекшен допускается, т.к. обновление внешнего связано только с полным монтированием-демонтированием диалога.
        scCssP.consume { scCss =>
          s.isDiaFullScreenSomeC { isDiaFullScreenSomeProxy =>
            val _isFullScreen = isDiaFullScreenSomeProxy.value.value
            val _rootCssU = JsOptionUtil.maybeDefined( _isFullScreen )( scCss.Header.header.htmlClass )
            MuiDialog {
              val diaCss = new MuiDialogClasses {
                override val paper = platCss.Dialogs.paper.htmlClass
                // Если есть unsafe-зоны на экране, то нужен отступ сверху в fullscreen-режиме:
                override val root = _rootCssU
              }
              new MuiDialogProps {
                override val open = circuitOpt.nonEmpty
                override val classes = diaCss
                override val onClose = _onCloseClick
                override val fullScreen = _isFullScreen
              }
            } (
              diaChildren: _*
            )
          }
        }
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(

        circuitOptC = propsProxy.connect( _.dialogs.nodes.circuit ),

        isDiaFullScreenSomeC = propsProxy.connect { mroot =>
          OptionUtil.SomeBool( mroot.dev.screen.info.isDialogWndFullScreen(450) )
        },

        haveFocusedAdAdminSomeC = propsProxy.connect { mroot =>
          val focAdIdOpt = mroot.dialogs.nodes.focusedAdId
          OptionUtil.SomeBool( focAdIdOpt.nonEmpty )
        },

        nodesModeC = propsProxy.connect( _.dialogs.nodes.mode ),

      )
    }
    .renderBackend[Backend]
    .build

}
