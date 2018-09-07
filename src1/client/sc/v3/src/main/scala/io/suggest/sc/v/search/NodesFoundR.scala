package io.suggest.sc.v.search

import chandu0101.scalajs.react.components.materialui._
import diode.FastEq
import diode.data.Pot
import diode.react.ReactPot._
import diode.react.ModelProxy
import io.suggest.common.html.HtmlConstants
import io.suggest.geo.MGeoPoint
import io.suggest.i18n.MsgCodes
import io.suggest.msg.Messages
import io.suggest.react.ReactCommonUtil
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.sc.m.search.{DoNodesSearch, MSearchRespInfo, NodesScroll}
import io.suggest.sc.styl.GetScCssF
import io.suggest.ueq.UnivEqUtil._
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import japgolly.univeq._
import io.suggest.maps.nodes.MGeoNodePropsShapes
import io.suggest.sjs.common.empty.JsOptionUtil

import scala.scalajs.js.UndefOr

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 27.07.18 18:28
  * Description: React-компонент, отвечающий за рендер списка найденных узлов: тегов и гео-размещенных узлов.
  */
class NodesFoundR(
                   nodeFoundR       : NodeFoundR,
                   getScCssF        : GetScCssF
                 ) {

  /** Контейнер данных для рендера компонента.
    *
    * @param found Данные найденных тегов/узлов.
    * @param withDistanceTo Рендерить расстояние до указанной локации. Инстанс mapInit.userLoc.
    */
  case class PropsVal(
                       req              : Pot[MSearchRespInfo[Seq[MGeoNodePropsShapes]]],
                       hasMore          : Boolean,
                       selectedIds      : Set[String],
                       withDistanceTo   : MGeoPoint,
                       searchCss        : SearchCss
                     )
  implicit object NodesFoundRPropsValFastEq extends FastEq[PropsVal] {
    import io.suggest.ueq.JsUnivEqUtil._
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.req ===* b.req) &&
        (a.hasMore ==* b.hasMore) &&
        (a.selectedIds ===* b.selectedIds) &&
        (a.withDistanceTo ===* b.withDistanceTo) &&
        // Наверное, проверять css не нужно. Но мы всё же перерендериваем.
        (a.searchCss ===* b.searchCss)
    }
  }


  type Props_t = PropsVal
  type Props = ModelProxy[Props_t]


  class Backend($: BackendScope[Props, Unit]) {

    /** Скроллинг в списке найденных узлов. */
    private def _onNodesListScroll(e: ReactEventFromHtml): Callback = {
      val scrollTop = e.target.scrollTop
      val scrollHeight = e.target.scrollHeight
      dispatchOnProxyScopeCB($, NodesScroll(scrollTop, scrollHeight) )
    }
    private lazy val _onNodesListScrollJsF = ReactCommonUtil.cbFun1ToJsCb( _onNodesListScroll )


    /** Реакция по кнопке сброса списка. */
    private def _onRefreshBtnClick(e: ReactEvent): Callback =
      dispatchOnProxyScopeCB($, DoNodesSearch(clear = true, ignorePending = true) )
    private lazy val _onRefreshBtnClickF = ReactCommonUtil.cbFun1ToJsCb( _onRefreshBtnClick )


    def render(propsProxy: Props): VdomElement = {
      val scCss = getScCssF()
      val NodesCSS = scCss.Search.Tabs.NodesFound
      val props = propsProxy.value

      val listClasses = new MuiListClasses {
        override val root: UndefOr[String] = {
          (NodesCSS.listDiv.htmlClass :: props.searchCss.NodesFound.nodesList.htmlClass :: Nil)
            .mkString( HtmlConstants.SPACE )
        }
      }
      val onScrollUndefF = JsOptionUtil.maybeDefined(props.req.exists(_.resp.nonEmpty) && props.hasMore && !props.req.isPending) {
        _onNodesListScrollJsF
      }
      <.div(
        // Горизонтальный прогресс-бар.
        ReactCommonUtil.maybe( props.req.isPending ) {
          val lpCss = new MuiLinearProgressClasses {
            override val root = NodesCSS.linearProgress.htmlClass
          }
          MuiLinearProgress(
            new MuiLinearProgressProps {
              override val variant = MuiProgressVariants.indeterminate
              override val classes = lpCss
            }
          )
        },

        MuiList(
          new MuiListProps {
            override val classes = listClasses
            override val onScroll = onScrollUndefF
          }
        )(
          // Рендер нормального списка найденных узлов.
          props.req.render { nodesRi =>
            if (nodesRi.resp.isEmpty) {
              // Надо, чтобы юзер понимал, что запрос поиска отработан.
              MuiListItem()(
                MuiListItemText()(
                  nodesRi.textQuery.fold {
                    Messages( MsgCodes.`No.tags.here` )
                  } { query =>
                    Messages( MsgCodes.`No.tags.found.for.1.query`, query )
                  }
                )
              )
            } else {
              nodesRi
                .resp
                .iterator
                .toVdomArray { mnode =>
                  // Рендер одного ряда. На уровне компонента обитает shouldComponentUpdate() для
                  propsProxy.wrap { props =>
                    nodeFoundR.PropsVal(
                      node            = mnode,
                      searchCss       = props.searchCss,
                      withDistanceTo  = props.withDistanceTo,
                      selected        = props.selectedIds contains mnode.props.nodeId
                    )
                  }( nodeFoundR.component.withKey(mnode.props.nodeId)(_) ): VdomNode
                }
            }
          },

          // Рендер крутилки ожидания.
          props.req.renderPending { _ =>
            MuiListItem()(
              MuiCircularProgress(
                new MuiCircularProgressProps {
                  override val variant = MuiProgressVariants.indeterminate
                  override val size = 50
                }
              )
            )
          },

          // Рендер ошибки.
          props.req.renderFailed { ex =>
            val emptyProps = MuiListItemProps.empty
            val errHint = Option(ex.getMessage)
              .getOrElse(ex.getClass.getName)
            VdomArray(
              // Рендер технических подробностей ошибки.
              MuiToolTip.component.withKey("e")(
                // TODO Надо tooltip разнести над всем рядом, а не только над иконкой.
                new MuiToolTipProps {
                  override val title = errHint
                  override val placement = MuiToolTipPlacements.Top
                }
              )(
                MuiListItem(emptyProps)(
                  MuiListItemText()(
                    Messages( MsgCodes.`Something.gone.wrong` )
                  ),
                  MuiListItemIcon()(

                    Mui.SvgIcons.ErrorOutline()()
                  )
                )
              ),
              // Кнопка reload для повторной загрузки списка.
              MuiListItem.component.withKey("r")(emptyProps)(
                MuiIconButton(
                  new MuiIconButtonProps {
                    override val onClick = _onRefreshBtnClickF
                  }
                )(
                  Mui.SvgIcons.Refresh()()
                )
              )
            )
          }

        ) // nodesList
      )
    }

  }

  val component = ScalaComponent.builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

  def apply(nodesFound: Props) = component( nodesFound )

}
