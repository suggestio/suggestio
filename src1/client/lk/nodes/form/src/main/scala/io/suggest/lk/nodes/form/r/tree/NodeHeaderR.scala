package io.suggest.lk.nodes.form.r.tree

import com.materialui.{Mui, MuiColorTypes, MuiLinearProgress, MuiLinearProgressClasses, MuiLinearProgressProps, MuiList, MuiListItem, MuiListItemIcon, MuiListItemSecondaryAction, MuiListItemText, MuiListItemTextProps, MuiProgressVariants, MuiSvgIconProps, MuiSwitch, MuiSwitchProps, MuiToolTip, MuiToolTipProps}
import diode.react.ModelProxy
import io.suggest.lk.nodes.form.m.{AdvOnNodeChanged, MNodeStateRender}
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import ReactCommonUtil.Implicits._
import io.suggest.common.html.HtmlConstants
import io.suggest.i18n.MCommonReactCtx
import io.suggest.lk.nodes.form.r.LkNodesFormCss
import io.suggest.n2.node.MNodeTypes
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.sjs.common.empty.JsOptionUtil.Implicits._
import io.suggest.spa.FastEqUtil
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.08.2020 14:56
  * Description: Компонент переключения в заголовоке узла.
  * Используется для быстрого неточного управления размещением карточки на узле.
  */
final class NodeHeaderR(
                         lkNodesFormCssP      : React.Context[LkNodesFormCss],
                         crCtxP               : React.Context[MCommonReactCtx],
                       ) {

  case class PropsVal(
                       render         : MNodeStateRender,
                       isAdv          : Boolean,
                     )
  implicit lazy val nodeHeaderPvFeq = FastEqUtil[PropsVal] { (a, b) =>
    MNodeStateRender.NodeStateRenderFeq.eqv( a.render, b.render )
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

    def render(p: Props, s: Props_t): VdomElement = {
      val st = s.render.state
      val advPot = st.advHasAdvPot
      val isAdvPending = advPot.isPending
      val ntype = st.info.ntype

      // TODO Пока делаем однострочный список, хотя лучше задействовать что-то иное (тулбар?).
      MuiList()(
        MuiListItem()(

          // Иконка-значок типа узла:
          MuiListItemIcon()(
            (ntype match {
              case MNodeTypes.BleBeacon                     => Mui.SvgIcons.BluetoothAudio
              case MNodeTypes.Person                        => Mui.SvgIcons.PersonOutlined
              case MNodeTypes.AdnNode                       => Mui.SvgIcons.HomeWorkOutlined
              case MNodeTypes.Ad                            => Mui.SvgIcons.DashboardOutlined
              case MNodeTypes.Tag                           => Mui.SvgIcons.LocalOfferOutlined
              case er if er ==>> MNodeTypes.ExternalRsc     => Mui.SvgIcons.ShareOutlined
              case MNodeTypes.Media.Image                   => Mui.SvgIcons.ImageOutlined
              case f if f ==>> MNodeTypes.Media             => Mui.SvgIcons.InsertDriveFileOutlined
              case _                                        => Mui.SvgIcons.BuildOutlined
            })()(),
          ),

          // Название узла:
          MuiListItemText(
            new MuiListItemTextProps {
              override val primary = st.info.name
              override val secondary = crCtxP.message( ntype.singular ).rawNode
            }
          )(),

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
          ReactCommonUtil.maybeNode( isAdvPending || st.infoPot.isPending ) {
            lkNodesFormCssP.consume { lknCss =>
              val progressCss = new MuiLinearProgressClasses {
                override val root = lknCss.Node.linearProgress.htmlClass
              }
              MuiLinearProgress(
                new MuiLinearProgressProps {
                  override val variant = MuiProgressVariants.indeterminate
                  override val classes = progressCss
                }
              )
            }
          },

          // Если размещение рекламной карточки, то отрендерить свитчер.
          ReactCommonUtil.maybeNode( s.isAdv && ntype.showScLink ) {
            val isChecked = advPot getOrElse false
            MuiListItemSecondaryAction()(
              MuiSwitch(
                new MuiSwitchProps {
                  override val checked        = isChecked
                  override val disabled       = isAdvPending
                  override val onChange       = _onAdvOnNodeChangedCbF
                  override val onClick        = _onAdvOnNodeClickCbF
                }
              ),
            )
          },

        )
      )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps( ReactDiodeUtil.modelProxyValueF )
    .renderBackend[Backend]
    .configure( ReactDiodeUtil.statePropsValShouldComponentUpdate( nodeHeaderPvFeq ) )
    .build

}
