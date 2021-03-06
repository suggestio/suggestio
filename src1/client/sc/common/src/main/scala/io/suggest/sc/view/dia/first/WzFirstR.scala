package io.suggest.sc.view.dia.first

import com.materialui.{Mui, MuiAlertTitle, MuiButton, MuiButtonProps, MuiButtonSizes, MuiButtonVariants, MuiColorTypes, MuiLinearProgress, MuiLinearProgressClasses, MuiLinearProgressProps, MuiProgressVariants, MuiSnackBarContent, MuiSnackBarContentProps}
import diode.data.Pot
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.html.HtmlConstants
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.lk.r.plat.PlatformComponents
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.sc.model.MScRoot
import io.suggest.sc.model.dia.first.{MWzFirstS, MWzFrames, MWzPhases, YesNoWz}
import io.suggest.sc.view.snack.ISnackComp
import io.suggest.sc.view.styl.ScCssStatic
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.01.19 12:37
  * Description: wrap-компонент для первого запуска система.
  */
class WzFirstR(
                platformComponents    : PlatformComponents,
                crCtxP                : React.Context[MCommonReactCtx],
              )
  extends ISnackComp
{

  type Props_t = MScRoot
  type Props = ModelProxy[Props_t]


  case class State(
                    wz1PotC         : ReactConnectProxy[Pot[MWzFirstS]],
                  )


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


    def render(p: Props, s: State): VdomElement = {
      crCtxP.consume { crCtx =>
        s.wz1PotC { propsPotProxy =>
          val pot = propsPotProxy.value
          // TODO pot.renderFailed()

          pot.toOption.whenDefinedEl { props =>
            // Snackbar message:
            // Строка заголовка окна диалога. Чтобы диалог не прыгал, рендерим заголовок всегда.
            val (iconComp, title) = props.phase match {
              case MWzPhases.GeoLocPerm =>
                platformComponents.muiLocationIconComp -> crCtx.messages( MsgCodes.`Geolocation` )
              case MWzPhases.BlueToothPerm =>
                Mui.SvgIcons.BluetoothSearching -> MsgCodes.`Bluetooth`
              case MWzPhases.NotificationPerm =>
                Mui.SvgIcons.Notifications -> crCtx.messages( MsgCodes.`Notifications` )
              case _ =>
                Mui.SvgIcons.DoneAll -> MsgCodes.`Suggest.io`
            }

            val messageContent = React.Fragment(
              MuiAlertTitle(
                new MuiAlertTitle.Props {
                  override val classes = new MuiAlertTitle.Classes {
                    override val root = ScCssStatic.justifyBetween.htmlClass
                  }
                }
              )(
                title,
              ),

              // Крутилка ожидания:
              ReactCommonUtil.maybeNode( props.frame ==* MWzFrames.InProgress ) {
                React.Fragment(
                  MuiLinearProgress {
                    val css = new MuiLinearProgressClasses {
                      override val root = ScCssStatic.Snacks.progress.htmlClass
                    }
                    new MuiLinearProgressProps {
                      override val classes = css
                      override val variant = MuiProgressVariants.indeterminate
                      override val color = MuiColorTypes.primary
                    }
                  },
                  <.br
                )
              },

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
                  (props.phase match {
                    case MWzPhases.Starting =>
                      Left( "" )
                    case MWzPhases.GeoLocPerm =>
                      Right( MsgCodes.`GPS` )
                    case MWzPhases.BlueToothPerm =>
                      Right( MsgCodes.`Bluetooth` )
                    case MWzPhases.NotificationPerm =>
                      Right( crCtx.messages(MsgCodes.`Notifications`) )
                  }).fold(
                    identity[String],
                    compName =>
                      crCtx.messages( MsgCodes.`You.can.enable.0.later.on.left.panel`, compName )
                  )

                // Прогресс-ожидание. Собрать крутилку.
                case MWzFrames.InProgress =>
                  crCtx.messages( MsgCodes.`Requesting.permission` )
              }
            )

            // Action buttons on the snack bottom:
            val actionButtons: Seq[VdomNode] = props.frame match {

              // Для запроса пермишшена - две кнопки.
              case MWzFrames.AskPerm =>
                // Кнопка "Позже"
                val laterBtn: VdomNode = MuiButton {
                  new MuiButtonProps {
                    override val variant = MuiButtonVariants.text
                    override val onClick = _laterClickCbF
                    override val size    = MuiButtonSizes.large
                  }
                } (
                  crCtx.messages( MsgCodes.`Later` )
                )
                // Кнопка "Разрешить"
                val allowBtn: VdomNode = MuiButton {
                  new MuiButtonProps {
                    override val variant = MuiButtonVariants.contained
                    override val onClick = _allowClickCbF
                    override val size    = MuiButtonSizes.large
                    override val startIcon = iconComp()().rawNode
                  }
                } (
                  crCtx.messages( MsgCodes.`Continue` ),
                )

                laterBtn :: (HtmlConstants.NBSP_STR: VdomNode) :: allowBtn :: Nil

              // Для информационного диалога - одна кнопка.
              case MWzFrames.Info =>
                val okBtn: VdomNode = MuiButton {
                  new MuiButtonProps {
                    override val color   = MuiColorTypes.primary
                    override val variant = MuiButtonVariants.text
                    override val onClick = _laterClickCbF
                    override val size    = MuiButtonSizes.large
                  }
                } (
                  crCtx.messages( MsgCodes.`Next` ),
                )
                okBtn :: Nil

              // Для режима ожидания - нужна кнопка отмены.
              case MWzFrames.InProgress =>
                val cancelBtn: VdomNode = MuiButton {
                  new MuiButtonProps {
                    override val color   = MuiColorTypes.primary
                    override val variant = MuiButtonVariants.text
                    override val onClick = _laterClickCbF
                    override val size    = MuiButtonSizes.large
                    override val startIcon = iconComp()().rawNode
                  }
                } (
                  crCtx.messages( MsgCodes.`Cancel` )
                )
                cancelBtn :: Nil
            }
            val actionContent = React.Fragment( actionButtons: _* )

            MuiSnackBarContent {
              new MuiSnackBarContentProps {
                override val message = messageContent.rawNode
                override val action = actionContent.rawNode
              }
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
        wz1PotC = propsProxy.connect( _.dialogs.first.view ),
      )
    }
    .renderBackend[Backend]
    .build

}
