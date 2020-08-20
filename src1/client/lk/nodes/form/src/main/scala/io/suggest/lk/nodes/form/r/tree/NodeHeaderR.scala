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

  type Props_t = MNodeStateRender
  type Props = ModelProxy[Props_t]


  class Backend($: BackendScope[Props, Props_t]) {

    /** Callback изменения галочки управления размещением текущей карточки на указанном узле. */
    private lazy val _onAdvOnNodeChangedCbF = ReactCommonUtil.cbFun1ToJsCb { e: ReactEventFromInput =>
      val isChecked = e.target.checked
      ReactDiodeUtil.dispatchOnProxyScopeCBf($) { props: Props =>
        AdvOnNodeChanged( props.value.nodePath, isEnabled = isChecked )
      }
    }

    def render(s: Props_t, children: PropsChildren): VdomElement = {
      val advPot = s.state.advHasAdvPot
      val isAdvPending = advPot.isPending
      val ntype = s.state.info.ntype

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
              override val primary = s.state.info.name
              override val secondary = crCtxP.message( ntype.singular ).rawNode
            }
          )(),

          // Доп.children:
          children,

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
          ReactCommonUtil.maybeNode( isAdvPending || s.state.infoPot.isPending ) {
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
          advPot
            .toOption
            .whenDefinedNode { isChecked =>
              MuiListItemSecondaryAction()(
                MuiSwitch(
                  new MuiSwitchProps {
                    override val checked        = isChecked
                    override val disabled       = isAdvPending
                    override val onChange       = _onAdvOnNodeChangedCbF
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
    .renderBackendWithChildren[Backend]
    .configure( ReactDiodeUtil.statePropsValShouldComponentUpdate( MNodeStateRender.NodeStateRenderFeq ) )
    .build

}
