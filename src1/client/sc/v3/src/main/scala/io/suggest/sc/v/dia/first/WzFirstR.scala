package io.suggest.sc.v.dia.first

import com.materialui.{Mui, MuiButton, MuiButtonProps, MuiButtonVariants, MuiColorTypes, MuiDialog, MuiDialogActions, MuiDialogActionsClasses, MuiDialogActionsProps, MuiDialogContent, MuiDialogContentText, MuiDialogProps, MuiDialogTitle, MuiDialogTitleClasses, MuiDialogTitleProps, MuiLinearProgress, MuiSvgIconProps}
import diode.UseValueEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.html.HtmlConstants
import io.suggest.css.{Css, CssR}
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.sc.m.MScRoot
import io.suggest.sc.m.dia.YesNoWz
import io.suggest.sc.m.dia.first.{MWzFirstS, MWzFrames, MWzPhases}
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.01.19 12:37
  * Description: wrap-компонент для первого запуска система.
  */
class WzFirstR(
                crCtxProv     : React.Context[MCommonReactCtx],
              ) {

  type Props_t = MScRoot
  type Props = ModelProxy[Props_t]


  case class State(
                    diaPropsC       : ReactConnectProxy[MWz1Props],
                    wz1OptC         : ReactConnectProxy[Option[MWzFirstS]],
                    cssOptC         : ReactConnectProxy[Option[WzFirstCss]],
                  )

  case class MWz1Props(
                        fullScreen  : Boolean,
                        visible     : Boolean,
                      )
    extends UseValueEq


  class Backend($: BackendScope[Props, State]) {

    private def _yesNoCbF(yesNo: Boolean) = {
      ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
        ReactDiodeUtil.dispatchOnProxyScopeCB($, YesNoWz(yesNo))
      }
    }

    /** Callback клика по кнопке разрешения. */
    private val _allowClickCbF = _yesNoCbF(true)

    /** Callback нажатия на кнопку отказа. */
    private val _laterClickCbF = _yesNoCbF(false)


    def render(s: State): VdomElement = {
      lazy val diaBody = crCtxProv.consume { crCtx =>
        React.Fragment(
          s.cssOptC( _.value.whenDefinedEl( CssR.component.apply ) ),

          s.wz1OptC { propsOptProxy =>
            propsOptProxy.value.whenDefinedEl { props =>
              React.Fragment(
                // Строка заголовка окна диалога. Чтобы диалог не прыгал, рендерим заголовок всегда.
                {
                  val (icon, title) = props.phase match {
                    case MWzPhases.GeoLocPerm =>
                      Mui.SvgIcons.MyLocation -> crCtx.messages(MsgCodes.`Geolocation`)
                    case MWzPhases.BlueToothPerm =>
                      Mui.SvgIcons.BluetoothSearching -> MsgCodes.`Bluetooth`
                    case MWzPhases.NotificationPerm =>
                      Mui.SvgIcons.Notifications -> crCtx.messages( MsgCodes.`Notifications` )
                    case _ =>
                      Mui.SvgIcons.DoneAll -> MsgCodes.`Suggest.io`
                  }

                  MuiDialogTitle {
                    val titleCss = new MuiDialogTitleClasses {
                      override val root = props.css.header.htmlClass
                    }
                    new MuiDialogTitleProps {
                      override val classes = titleCss
                    }
                  } (
                    title,
                    icon(
                      new MuiSvgIconProps {
                        override val className = Css.Floatt.RIGHT
                      }
                    )(),
                  )
                },

                // Плашка с вопросом геолокации.
                MuiDialogContent()(

                  // Крутилка ожидания:
                  ReactCommonUtil.maybeNode( props.frame ==* MWzFrames.InProgress ) {
                    MuiLinearProgress()
                  },

                  MuiDialogContentText()(

                    // Сборка текста вопроса:
                    props.frame match {
                      case MWzFrames.AskPerm =>
                        val msgCodeOpt: Option[String] = props.phase match {
                          case MWzPhases.GeoLocPerm =>
                            Some( MsgCodes.`0.uses.geoloc.to.find.ads` )
                          case MWzPhases.BlueToothPerm =>
                            Some( MsgCodes.`0.uses.bt.to.find.ads.indoor` )
                          case MWzPhases.NotificationPerm =>
                            Some( MsgCodes.`Notify.about.offers.nearby` )
                          case _ =>
                            None
                        }
                        msgCodeOpt.whenDefinedNode { msgCode =>
                          crCtx.messages( msgCode, MsgCodes.`Suggest.io` )
                        }

                      case MWzFrames.Info =>
                        (
                          props.phase match {
                            case MWzPhases.Starting =>
                              Left( "" )
                            case MWzPhases.GeoLocPerm =>
                              Right( MsgCodes.`GPS` )
                            case MWzPhases.BlueToothPerm =>
                              Right( MsgCodes.`Bluetooth` )
                            case MWzPhases.NotificationPerm =>
                              Right( crCtx.messages(MsgCodes.`Notifications`) )
                            case MWzPhases.Finish =>
                              val msg = crCtx.messages( MsgCodes.`Settings.done.0.ready.for.using`, MsgCodes.`Suggest.io` )
                              Left( msg )
                          }
                        ).fold(
                          identity[String],
                          compName =>
                            crCtx.messages( MsgCodes.`You.can.enable.0.later.on.left.panel`, compName )
                        )

                      // Прогресс-ожидание. Собрать крутилку.
                      case MWzFrames.InProgress =>
                        crCtx.messages( MsgCodes.`Please.wait` )
                    }

                  ),
                ),

                // Кнопки управления диалогом:
                {
                  val btns: Seq[VdomNode] = props.frame match {

                    // Для запроса пермишшена - две кнопки.
                    case MWzFrames.AskPerm =>
                      // Кнопка "Позже"
                      val laterBtn: VdomNode = MuiButton {
                        new MuiButtonProps {
                          override val color   = MuiColorTypes.secondary
                          override val variant = MuiButtonVariants.text
                          override val onClick = _laterClickCbF
                        }
                      } (
                        crCtx.messages( MsgCodes.`Later` )
                      )
                      // Кнопка "Разрешить"
                      val allowBtn: VdomNode = MuiButton {
                        new MuiButtonProps {
                          override val color   = MuiColorTypes.primary
                          override val variant = MuiButtonVariants.contained
                          override val onClick = _allowClickCbF
                        }
                      } (
                        crCtx.messages( MsgCodes.`Allow.0`, HtmlConstants.SPACE ),
                      )

                      laterBtn :: allowBtn :: Nil

                    // Для информационного диалога - одна кнопка.
                    case MWzFrames.Info =>
                      val okBtn: VdomNode = MuiButton {
                        new MuiButtonProps {
                          override val color   = MuiColorTypes.primary
                          override val variant = MuiButtonVariants.contained
                          override val onClick = _laterClickCbF
                        }
                      } (
                        crCtx.messages(
                          if (props.phase ==* MWzPhases.Finish) MsgCodes.`_to.Finish`
                          else MsgCodes.`Next`
                        )
                      )
                      okBtn :: Nil

                    // Для режима ожидания - нужна кнопка отмены.
                    case MWzFrames.InProgress =>
                      val cancelBtn: VdomNode = MuiButton {
                        new MuiButtonProps {
                          override val color   = MuiColorTypes.secondary
                          override val variant = MuiButtonVariants.text
                          override val onClick = _laterClickCbF
                        }
                      } (
                        crCtx.messages( MsgCodes.`Cancel` )
                      )
                      cancelBtn :: Nil

                  }

                  val diaActionsCss = new MuiDialogActionsClasses {
                    override val root = props.css.footer.htmlClass
                  }
                  val diaActionsProps = new MuiDialogActionsProps {
                    override val classes = diaActionsCss
                  }

                  MuiDialogActions( diaActionsProps )( btns: _* )
                }
              )
            }
          }
        )
      }

      s.diaPropsC { diaPropsProxy =>
        val diaProps = diaPropsProxy.value
        // Общие пропертисы для любых собранных диалогов:
        MuiDialog {
          new MuiDialogProps {
            override val open = diaProps.visible
            override val fullScreen = diaProps.fullScreen
            override val onClose = _laterClickCbF
          }
        } (
          diaBody
        )
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(
        diaPropsC = propsProxy.connect { props =>
          MWz1Props(
            fullScreen  = props.dev.screen.info.isDialogWndFullScreen,
            visible     = props.dialogs.first.isVisible,
          )
        },
        wz1OptC = propsProxy.connect( _.dialogs.first.view ),
        cssOptC = propsProxy.connect( _.dialogs.first.cssOpt ),
      )
    }
    .renderBackend[Backend]
    .build

}
