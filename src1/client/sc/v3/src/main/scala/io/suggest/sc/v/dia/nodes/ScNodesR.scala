package io.suggest.sc.v.dia.nodes

import com.materialui.MuiTextField.Variant
import com.materialui.{MuiButton, MuiButtonProps, MuiButtonSizes, MuiDialog, MuiDialogActions, MuiDialogClasses, MuiDialogContent, MuiDialogContentProps, MuiDialogProps, MuiIconButton, MuiIconButtonProps, MuiMenuItem, MuiMenuItemProps, MuiModalCloseReason, MuiSelectProps, MuiTextField, MuiTextFieldProps}
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.empty.OptionUtil
import io.suggest.css.Css
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
import japgolly.univeq._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import scalacss.ScalaCssReact._

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

  private def _isDiaFullScreen(mroot: MScRoot): Boolean =
    mroot.dev.screen.info.isDialogWndFullScreen( minWidth = 450 )

  class Backend($: BackendScope[Props, State]) {

    private lazy val _onCloseBtnClick = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, ScNodesShowHide(visible = false) )
    }

    private lazy val _onModeChange = ReactCommonUtil.cbFun1ToJsCb { e: ReactEventFromInput =>
      val nextMode = MLkNodesModes.withValue( e.target.value )
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, ScNodesModeChanged(nextMode) )
    }

    def render(propsProxy: Props, s: State): VdomElement = {
      crCtxP.consume { crCtx =>
        s.circuitOptC { circuitOptProxy =>
          val circuitOpt = circuitOptProxy.value
          val platCss = platfromCss()

          val isNativeSelect = propsProxy.value.dev.platform.isCordova
          val modeSelectProps = new MuiSelectProps {
            override val native = isNativeSelect
            override val variant = MuiTextField.Variants.standard
          }

          val modeMsg = crCtx.messages( MsgCodes.`Mode` )
          val nodesManagementMsg = crCtx.messages( MsgCodes.`Nodes.management` )

          // Сборка фунцкии, которая собирает один элемент селекта.
          val __mkSelectOption = if (isNativeSelect) {
            (_value: MLkNodesMode, _title: String) =>
              <.option(
                ^.value := _value.value,
                _title,
              ): VdomElement
          } else {
            (_value: MLkNodesMode, _title: String) =>
              MuiMenuItem.component.apply(
                new MuiMenuItemProps {
                  override val value = _value.value
                }
              )(_title): VdomElement
          }

          val diaChildren = List[VdomNode](

            // Заголовок
            platformComponents.diaTitle(Nil)(
              // Стрелочка "назад".
              <.div(
                platCss.Dialogs.backBtn,
                MuiIconButton {
                  new MuiIconButtonProps {
                    override val onClick = _onCloseBtnClick
                  }
                } (
                  platformComponents.arrowBack()(),
                )
              ),

              // Текстовый заголовок: статически или в виде селекта режима, когда доступно более одного режима формы.
              platformComponents.diaTitleText(
                s.haveFocusedAdAdminSomeC { haveFocusedAdAdminSomeProxy =>
                  val haveFocusedAdmin = haveFocusedAdAdminSomeProxy.value.value
                  if (!haveFocusedAdmin) {
                    // Нет сфокусированной карточки: просто рендерить обычный заголовок.
                    React.Fragment(
                      nodesManagementMsg
                    )

                  } else {
                    // Рендерим селект, где можно выбрать или управление узлами, или карточку.
                    // Пункт селекта обычного режима работы (управление узлами)
                    val nodesManageItem = __mkSelectOption( MLkNodesModes.NodesManage, nodesManagementMsg )

                    // Пункт селекта для управления размещением карточки в узлах:
                    // TODO Отрендерить внутри миниатюру главного блока текущей карточки, если ненативный select.
                    val advInNodesItem = __mkSelectOption(
                      MLkNodesModes.AdvInNodes,
                      crCtx.messages( MsgCodes.`Ad.adv.manage` )
                    )

                    // Непосредственно селект.
                    s.nodesModeC { nodesModeProxy =>
                      MuiTextField(
                        new MuiTextFieldProps {
                          override val select = true
                          override val label = modeMsg.rawNode
                          override val value = nodesModeProxy.value.value
                          override val fullWidth = true
                          override val onChange = _onModeChange
                          override val SelectProps = modeSelectProps
                        }
                      )(
                        nodesManageItem,
                        advInNodesItem,
                      )
                    }
                  }
                },
              ),
            ),

            // Содержимое диалога.
            MuiDialogContent(
              new MuiDialogContentProps {
                override val dividers = true
              }
            )(
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
                  override val onClick = _onCloseBtnClick
                  override val fullWidth = true
                }
              )(
                crCtx.messages( MsgCodes.`Close` )
              )
            ),

          )

          // Вложенный коннекшен допускается, т.к. обновление внешнего связано только с полным монтированием-демонтированием диалога.
          scCssP.consume { scCss =>
            s.isDiaFullScreenSomeC { isDiaFullScreenSomeProxy =>
              val _isFullScreen = isDiaFullScreenSomeProxy.value.value
              val _rootCssU = JsOptionUtil.maybeDefined( _isFullScreen )(
                Css.flat(
                  scCss.Header.header.htmlClass,
                  scCss.Dialogs.unsafeBottom.htmlClass,
                )
              )
              MuiDialog {
                val diaCss = new MuiDialogClasses {
                  override val paper = platCss.Dialogs.paper.htmlClass
                  // Если есть unsafe-зоны на экране, то нужен отступ сверху в fullscreen-режиме:
                  override val root = _rootCssU
                }
                new MuiDialogProps {
                  override val open = circuitOpt.nonEmpty
                  override val classes = diaCss
                  override val onClose = _onCloseBtnClick
                  override val fullScreen = _isFullScreen
                  override val fullWidth = true
                }
              } (
                diaChildren: _*
              )
            }
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
          OptionUtil.SomeBool( _isDiaFullScreen(mroot) )
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
