package io.suggest.lk.nodes.form.r.tree

import com.materialui.{Mui, MuiColorTypes, MuiList, MuiListItem, MuiListItemIcon, MuiListItemProps, MuiListItemSecondaryAction, MuiListItemSecondaryActionProps, MuiListItemText, MuiListItemTextProps, MuiSvgIcon, MuiSvgIconProps, MuiSwitch, MuiSwitchProps, MuiToolTip, MuiToolTipProps, MuiTypoGraphy, MuiTypoGraphyClasses, MuiTypoGraphyColors, MuiTypoGraphyProps, MuiTypoGraphyVariants}
import diode.react.ModelProxy
import io.suggest.lk.nodes.form.m.{MNodeStateRender, MTreeRoles, ModifyNode}
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import ReactCommonUtil.Implicits._
import io.suggest.common.empty.OptionUtil
import io.suggest.common.empty.OptionUtil.BoolOptOps
import io.suggest.common.html.HtmlConstants
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.lk.nodes.form.r.LkNodesFormCss
import io.suggest.lk.nodes.{MLknOpKeys, MLknOpValue}
import io.suggest.n2.node.MNodeTypes
import io.suggest.netif.NetworkingUtil
import io.suggest.radio.MRadioSignalTypes
import io.suggest.react.ReactDiodeUtil.Implicits._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.spa.FastEqUtil
import io.suggest.ueq.UnivEqUtil._
import io.suggest.scalaz.ScalazUtil.Implicits._
import io.suggest.scalaz.ZTreeUtil.zTreeUnivEq
import japgolly.univeq._
import scalaz.{EphemeralStream, Tree}

import scala.scalajs.js
import scalajs.js.JSConverters._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.08.2020 14:56
  * Description: Компонент переключения в заголовоке узла.
  * Используется для быстрого неточного управления размещением карточки на узле.
  */
final class NodeHeaderR(
                         distanceValueR       : DistanceValueR,
                         treeStuffR           : TreeStuffR,
                         crCtxP               : React.Context[MCommonReactCtx],
                         lkNodesFormCss       : React.Context[LkNodesFormCss],
                       ) {

  case class PropsVal(
                       render         : MNodeStateRender,
                       isAdv          : Boolean,
                       asList         : Boolean,
                       chs            : EphemeralStream[Tree[String]] = null,
                       onClick        : Option[js.Function1[ReactEvent, Unit]] = None,
                     )
  implicit lazy val nodeHeaderPvFeq = FastEqUtil[PropsVal] { (a, b) =>
    MNodeStateRender.NodeStateRenderFeq.eqv( a.render, b.render ) &&
    (a.isAdv ==* b.isAdv) &&
    (a.chs ===* b.chs) &&
    (a.asList ==* b.asList) &&
    (a.onClick eq b.onClick)
  }


  type Props_t = PropsVal
  type Props = ModelProxy[Props_t]


  class Backend($: BackendScope[Props, Props_t]) {

    private lazy val _stopPropagationCbF = ReactCommonUtil.cbFun1ToJsCb { e: ReactEvent =>
      e.stopPropagationCB
    }

    /** Callback изменения галочки управления размещением текущей карточки на указанном узле. */
    private lazy val _onAdvOnNodeChangedCbF = ReactCommonUtil.cbFun1ToJsCb { e: ReactEventFromInput =>
      val isChecked = e.target.checked
      ReactDiodeUtil.dispatchOnProxyScopeCBf($) { props: Props =>
        ModifyNode(
          nodePath = Some( props.value.render.nodePath ),
          key = MLknOpKeys.AdvEnabled,
          value = MLknOpValue(
            bool = OptionUtil.SomeBool( isChecked ),
          ),
        )
      } >> e.stopPropagationCB
    }

    private def _BLUETOOTH_ICON = Mui.SvgIcons.BluetoothAudio

    def render(propsProxy: Props, s: Props_t): VdomElement = {
      val st = s.render.state
      val advPot = st.optionBoolPot( MLknOpKeys.AdvEnabled )
      val infoOpt = st.infoPot.toOption


      // Produce wifi/bluetooth icon from radio-signal:
      def __radioToIconOpt: Option[MuiSvgIcon] = {
        for (beacon <- st.beacon) yield {
          val signal = beacon.data.signal.signal
          signal.typ match {
            case MRadioSignalTypes.WiFi =>
              // Convert signal rssi to one of Wifi-icons:
              if (!beacon.isVisible) {
                Mui.SvgIcons.SignalWifi0Bar
              } else signal.rssi.fold[MuiSvgIcon]( Mui.SvgIcons.Wifi ) { rssi =>
                if (rssi > -50) Mui.SvgIcons.SignalWifi4Bar
                else if (rssi > -60) Mui.SvgIcons.SignalWifi3Bar
                else if (rssi > -70) Mui.SvgIcons.SignalWifi2Bar
                else if (rssi > -80) Mui.SvgIcons.SignalWifi1Bar
                else Mui.SvgIcons.SignalWifi0Bar
              }

            case MRadioSignalTypes.BluetoothEddyStone =>
              _BLUETOOTH_ICON
          }
        }
      }

      // TODO Пока делаем однострочный список, хотя лучше задействовать что-то иное (тулбар?).
      val listItemRendered = MuiListItem(
        new MuiListItemProps {
          override val onClick = s.onClick.orUndefined
        }
      )(

        // Иконка-значок типа узла:
        infoOpt
          .flatMap(_.ntype)
          .fold [VdomNode] {
            (if (st.role ==* MTreeRoles.BeaconSignal) {
              __radioToIconOpt
            } else if (st.role ==* MTreeRoles.BeaconsDetected) {
              // Render radio beacons/wifi group icon:
              Some( Mui.SvgIcons.SettingsInputAntenna )
            } else
              None
            )
              .whenDefinedNode { iconComponent =>
                MuiListItemIcon()(
                  iconComponent()(),
                )
              }
          } { ntype =>
            // Convert node-type to svg-icon:
            MuiListItemIcon()(
              (ntype match {
                case MNodeTypes.WifiAP                        => __radioToIconOpt getOrElse Mui.SvgIcons.Wifi
                case MNodeTypes.BleBeacon                     => _BLUETOOTH_ICON
                case MNodeTypes.AdnNode                       => Mui.SvgIcons.HomeWorkOutlined
                case MNodeTypes.Person                        => Mui.SvgIcons.PersonOutlined
                case MNodeTypes.Ad                            => Mui.SvgIcons.DashboardOutlined
                case MNodeTypes.Tag                           => Mui.SvgIcons.LocalOfferOutlined
                case er if er ==>> MNodeTypes.ExternalRsc     => Mui.SvgIcons.ShareOutlined
                case MNodeTypes.Media.Image                   => Mui.SvgIcons.ImageOutlined
                case f if f ==>> MNodeTypes.Media             => Mui.SvgIcons.InsertDriveFileOutlined
                case _                                        => Mui.SvgIcons.BuildOutlined
              })()(),
            )
          },

        // Название узла:
        MuiListItemText {
          val radioNameCustomOpt = st.beacon
            .flatMap(_.data.signal.signal.customName)

          val _textPrimary = (for {
            info <- infoOpt
            name <- info.name
            if name.nonEmpty
          } yield {
            name
          })
            // Use Wi-Fi as unknown node name:
            .orElse( radioNameCustomOpt )
            .orUndefined

          var _textSecondaryNode: Option[VdomNode] = (for {
            info <- infoOpt
            ntype <- info.ntype
            if _textPrimary.nonEmpty
          } yield {
            crCtxP.message( ntype.singular )
          })
            .orElse[VdomNode](
              for {
                radio <- st.beacon
                signal = radio.data.signal.signal
                uid <- signal.factoryUid
              } yield {
                signal.typ match {
                  case MRadioSignalTypes.WiFi =>
                    // Render MAC-address in human-readable format:
                    NetworkingUtil.unminifyMacAddress( uid )

                  case MRadioSignalTypes.BluetoothEddyStone =>
                    // Render Beacon UUID as-is
                    uid
                }
              }
            )

          // Prepend Wi-Fi SSID Name in secondary string
          for ( radioName <- radioNameCustomOpt if !_textPrimary.contains(radioName) ) {
            _textSecondaryNode = _textSecondaryNode
              .map[VdomNode] { suffix =>
                React.Fragment( radioName, " | ", suffix )
              }
              .orElse {
                radioNameCustomOpt
                  .map(m => m: VdomNode)
              }
          }

          val _textSecondary = _textSecondaryNode
            .map(_.rawNode)
            .orUndefined

          new MuiListItemTextProps {
            override val primary = _textPrimary
            override val secondary = _textSecondary
          }
        }(),

        // Если не-adv режим, то отрендерить в заголовке расстояние до маячка.
        (for {
          beacon <- st.beacon
          if !s.isAdv
        } yield {
          val chs = lkNodesFormCss.consume { lkNodesCss =>
            MuiTypoGraphy {
              val css = new MuiTypoGraphyClasses {
                override val root = lkNodesCss.Node.flexLine.htmlClass
              }
              new MuiTypoGraphyProps {
                override val variant = MuiTypoGraphyVariants.caption
                override val color = MuiTypoGraphyColors.textSecondary
                override val noWrap = true
                override val classes = css
              }
            } (
              ReactCommonUtil.maybeNode( beacon.isVisible && beacon.data.lastDistanceCm.exists(_ <= 15) )(
                Mui.SvgIcons.NearMeOutlined()()
              ),
              distanceValueR.component( propsProxy.resetZoom(beacon) ),
            )
          }
          MuiListItemSecondaryAction()( chs )
        })
          .whenDefinedNode,

        // Ошибка запроса:
        advPot.exceptionOption.whenDefinedNode { ex =>
          MuiToolTip {
            val ttContent = <.span(
              ex.getMessage,
              HtmlConstants.SPACE,
              ex.getClass.getSimpleName,
              // Рендерить stacktrace в development mode.
              ReactCommonUtil.maybe( scalajs.LinkingInfo.developmentMode ) {
                <.div(
                  <.br,
                  ex.getStackTrace
                    .zipWithIndex
                    .toVdomArray { case (stackEl, i) =>
                      <.span(
                        ^.key := i,
                        stackEl.toString,
                        <.br
                      )
                    },
                )
              },
            )
            new MuiToolTipProps {
              override val title = ttContent.rawElement
            }
          } (
            Mui.SvgIcons.Error(
              new MuiSvgIconProps {
                override val color = MuiColorTypes.error
              }
            )(),
          )
        },

        // Линия прогресса.
        ReactCommonUtil.maybeNode( advPot.isPending || st.infoPot.isPending ) {
          treeStuffR.LineProgress()
        },

        // Если размещение рекламной карточки, то отрендерить свитчер.
        ReactCommonUtil.maybeNode(
          s.isAdv &&
            infoOpt
              .flatMap(_.ntype)
              .exists(_.showScLink)
        ) {
          MuiListItemSecondaryAction(
            new MuiListItemSecondaryActionProps {
              override val onClick = _stopPropagationCbF
            }
          )(
            MuiSwitch(
              new MuiSwitchProps {
                override val checked = {
                  advPot
                    .toOption
                    .getOrElseFalse
                }
                override val disabled       = advPot.isPending
                override val onChange       = _onAdvOnNodeChangedCbF
              }
            ),
          )
        },

        // Заголовок виртуальной группы маячков.
        ReactCommonUtil.maybeNode( st.role ==* MTreeRoles.BeaconsDetected ) {
          React.Fragment(
            MuiListItemText()(
              crCtxP.message( MsgCodes.`Radio.beacons` ),
            ),
            ReactCommonUtil.maybeNode( s.chs != null )(
              MuiListItemSecondaryAction()(
                MuiTypoGraphy(
                  new MuiTypoGraphyProps {
                    override val variant = MuiTypoGraphyVariants.caption
                    override val color = MuiTypoGraphyColors.textSecondary
                  }
                )(
                  s.chs.length,
                )
              ),
            ),
          )
        },

      ): VdomElement

      if (s.asList)
        MuiList()(
          listItemRendered
        )
      else
        listItemRendered
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps( ReactDiodeUtil.modelProxyValueF )
    .renderBackend[Backend]
    .configure( ReactDiodeUtil.statePropsValShouldComponentUpdate( nodeHeaderPvFeq ) )
    .build

}
