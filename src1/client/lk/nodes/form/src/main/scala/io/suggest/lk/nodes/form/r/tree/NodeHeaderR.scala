package io.suggest.lk.nodes.form.r.tree

import com.materialui.{Mui, MuiColorTypes, MuiList, MuiListItem, MuiListItemIcon, MuiListItemSecondaryAction, MuiListItemText, MuiListItemTextProps, MuiSvgIconProps, MuiSwitch, MuiSwitchProps, MuiToolTip, MuiToolTipProps, MuiTypoGraphy, MuiTypoGraphyColors, MuiTypoGraphyProps, MuiTypoGraphyVariants}
import diode.react.ModelProxy
import io.suggest.lk.nodes.form.m.{AdvOnNodeChanged, MNodeState, MNodeStateRender, MTreeRoles}
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import ReactCommonUtil.Implicits._
import io.suggest.common.empty.OptionUtil.BoolOptOps
import io.suggest.common.html.HtmlConstants
import io.suggest.i18n.MCommonReactCtx
import io.suggest.n2.node.MNodeTypes
import io.suggest.react.ReactDiodeUtil.Implicits._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.spa.FastEqUtil
import io.suggest.ueq.UnivEqUtil._
import io.suggest.scalaz.ScalazUtil.Implicits._
import io.suggest.scalaz.ZTreeUtil.zTreeUnivEq
import japgolly.univeq._
import scalaz.{EphemeralStream, Tree}

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
                       ) {

  case class PropsVal(
                       render         : MNodeStateRender,
                       isAdv          : Boolean,
                       asList         : Boolean,
                       chs            : EphemeralStream[Tree[String]] = null,
                     )
  implicit lazy val nodeHeaderPvFeq = FastEqUtil[PropsVal] { (a, b) =>
    MNodeStateRender.NodeStateRenderFeq.eqv( a.render, b.render ) &&
    (a.isAdv ==* b.isAdv) &&
    (a.chs ===* b.chs) &&
    (a.asList ==* b.asList)
  }


  type Props_t = PropsVal
  type Props = ModelProxy[Props_t]


  class Backend($: BackendScope[Props, Props_t]) {

    private lazy val _onAdvOnNodeClickCbF = ReactCommonUtil.cbFun1ToJsCb { e: ReactEvent =>
      e.stopPropagationCB
    }

    /** Callback изменения галочки управления размещением текущей карточки на указанном узле. */
    private lazy val _onAdvOnNodeChangedCbF = ReactCommonUtil.cbFun1ToJsCb { e: ReactEventFromInput =>
      val isChecked = e.target.checked
      ReactDiodeUtil.dispatchOnProxyScopeCBf($) { props: Props =>
        AdvOnNodeChanged( props.value.render.nodePath, isEnabled = isChecked )
      }
    }

    def render(propsProxy: Props, s: Props_t): VdomElement = {
      val st = s.render.state
      val advPot = st.advHasAdvPot
      val infoOpt = st.infoPot.toOption

      // TODO Пока делаем однострочный список, хотя лучше задействовать что-то иное (тулбар?).
      val listItemRendered = MuiListItem()(

        // Иконка-значок типа узла:
        infoOpt
          .flatMap(_.ntype)
          .orElse {
            st.role match {
              case MTreeRoles.BeaconSignal | MTreeRoles.BeaconsDetected =>
                Some( MNodeTypes.BleBeacon )
              case _ => None
            }
          }
          .whenDefinedNode { ntype =>
            MuiListItemIcon()(
              (ntype match {
                case MNodeTypes.BleBeacon                     => Mui.SvgIcons.BluetoothAudio
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
          val _textPrimary = infoOpt
            .flatMap( _.name )
            .orElse( st.beaconUidOpt )
            .orUndefined

          val _textSecondary = infoOpt
            .flatMap(_.ntype)
            .map { ntype =>
              crCtxP.message( ntype.singular ).rawNode
            }
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
          MuiListItemSecondaryAction()(
            MuiTypoGraphy(
              new MuiTypoGraphyProps {
                override val variant = MuiTypoGraphyVariants.caption
                override val color = MuiTypoGraphyColors.textSecondary
              }
            )(
              distanceValueR.component( propsProxy.resetZoom(beacon) ),
            ),
          )
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
          val isChecked = advPot
            .toOption
            .getOrElseFalse
          MuiListItemSecondaryAction()(
            MuiSwitch(
              new MuiSwitchProps {
                override val checked        = isChecked
                override val disabled       = advPot.isPending
                override val onChange       = _onAdvOnNodeChangedCbF
                override val onClick        = _onAdvOnNodeClickCbF
              }
            ),
          )
        },

        // Заголовок виртуальной группы маячков.
        ReactCommonUtil.maybeNode( st.role ==* MTreeRoles.BeaconsDetected ) {
          React.Fragment(
            MuiListItemText()(
              crCtxP.message( MNodeTypes.BleBeacon.plural ),
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
