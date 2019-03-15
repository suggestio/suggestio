package io.suggest.sc.v.dia.first

import chandu0101.scalajs.react.components.materialui.{Mui, MuiButton, MuiButtonProps, MuiButtonVariants, MuiColorTypes, MuiDialog, MuiDialogActions, MuiDialogContent, MuiDialogContentText, MuiDialogProps, MuiDialogTitle, MuiLinearProgress, MuiSvgIconProps}
import diode.FastEq
import diode.react.ModelProxy
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.sc.m.dia.YesNoWz
import io.suggest.sc.m.dia.first.{MWzFirstS, MWzFrames, MWzPhases}
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, React, ReactEvent, ScalaComponent}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.01.19 12:37
  * Description: React-компонент для первого запуска система.
  */
class WzFirstR(
                commonReactCtxProv     : React.Context[MCommonReactCtx],
              ) {

  case class PropsVal(
                       first      : MWzFirstS,
                       fullScreen : Boolean,
                     )
  implicit object WzFirstRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.first ===* b.first) &&
      (a.fullScreen ==* b.fullScreen)
    }
  }


  type Props_t = Option[PropsVal]
  type Props = ModelProxy[Props_t]


  class Backend($: BackendScope[Props, Unit]) {

    private def _yesNoCbF(yesNo: Boolean) = {
      ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
        ReactDiodeUtil.dispatchOnProxyScopeCB($, YesNoWz(yesNo))
      }
    }

    /** Callback клика по кнопке разрешения. */
    private val _allowClickCbF = _yesNoCbF(true)

    /** Callback нажатия на кнопку отказа. */
    private val _laterClickCbF = _yesNoCbF(false)


    def render(propsOptProxy: Props): VdomElement = {
      // TODO Надо бы, чтобы компонент диалога был отрендерен всегда и один на всех. Сейчас - слетает анимация сокрытия из-за деинициализации.

      propsOptProxy.value.whenDefinedEl { props =>
        // Общие пропертисы для любых собранных диалогов:
        commonReactCtxProv.consume { crCtx =>
          MuiDialog {
            new MuiDialogProps {
              override val open = props.first.visible
              override val fullScreen = props.fullScreen
              override val onClose = _laterClickCbF
            }
          } (

            // Строка заголовка окна диалога. Чтобы диалог не прыгал, рендерим заголовок всегда.
            {
              val (icon, title) = props.first.phase match {
                case MWzPhases.GeoLocPerm =>
                  Mui.SvgIcons.MyLocation -> crCtx.messages(MsgCodes.`Geolocation`)
                case MWzPhases.BlueToothPerm =>
                  Mui.SvgIcons.BluetoothSearching -> MsgCodes.`Bluetooth`
                case _ =>
                  Mui.SvgIcons.DoneAll -> MsgCodes.`Suggest.io`
              }
              MuiDialogTitle()(
                title,
                icon(
                  new MuiSvgIconProps {
                    override val className = Css.Floatt.RIGHT
                  }
                )()
              )
            },

            // Плашка с вопросом геолокации.
            MuiDialogContent()(

              // Крутилка ожидания:
              ReactCommonUtil.maybeNode( props.first.frame ==* MWzFrames.InProgress ) {
                MuiLinearProgress()
              },

              MuiDialogContentText()(

                // Сборка текста вопроса:
                props.first.frame match {
                  case MWzFrames.AskPerm =>
                    val msgCodeOpt = props.first.phase match {
                      case MWzPhases.GeoLocPerm =>
                        Some( MsgCodes.`0.uses.geoloc.to.find.ads` )
                      case MWzPhases.BlueToothPerm =>
                        Some( MsgCodes.`0.uses.bt.to.find.ads.indoor` )
                      case _ =>
                        None
                    }
                    msgCodeOpt.whenDefinedNode { msgCode =>
                      crCtx.messages( msgCode, MsgCodes.`Suggest.io` )
                    }

                  case MWzFrames.Info =>
                    (
                      props.first.phase match {
                        case MWzPhases.Starting =>
                          Left( "" )
                        case MWzPhases.GeoLocPerm =>
                          Right( MsgCodes.`GPS` )
                        case MWzPhases.BlueToothPerm =>
                          Right( MsgCodes.`Bluetooth` )
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
              val btns: Seq[VdomNode] = props.first.frame match {

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
                      if (props.first.phase ==* MWzPhases.Finish) MsgCodes.`_to.Finish`
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
              MuiDialogActions()( btns: _* )
            }

          )
        }
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

  def apply(propsOptProps: Props) = component( propsOptProps )

}
