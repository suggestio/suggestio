package io.suggest.sc.v.search

import chandu0101.scalajs.react.components.materialui._
import diode.FastEq
import diode.data.Pot
import diode.react.ReactPot._
import diode.react.ModelProxy
import io.suggest.css.Css
import io.suggest.geo.MGeoPoint
import io.suggest.i18n.MsgCodes
import io.suggest.lk.r.LkPreLoaderR
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
import scalacss.ScalaCssReact._
import io.suggest.maps.nodes.MGeoNodePropsShapes

import scala.scalajs.js
import scala.scalajs.js.{UndefOr, |}

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

    /** Реакция по кнопке сброса списка. */
    private def _onRefreshBtnClick: Callback =
      dispatchOnProxyScopeCB($, DoNodesSearch(clear = true, ignorePending = true) )


    def render(propsProxy: Props): VdomElement = {
      val scCss = getScCssF()
      val NodesCSS = scCss.Search.Tabs.NodesFound

      val props = propsProxy.value
      val _tagRowCss = NodesCSS.nodeRow: TagMod

      <.div(
        NodesCSS.listDiv,
        props.searchCss.NodesFound.nodesList,

        // Подписка на события скроллинга:
        ReactCommonUtil.maybe(props.hasMore && !props.req.isPending) {
          ^.onScroll ==> _onNodesListScroll
        },

        // Рендер нормального списка найденных узлов.
        props.req.render { nodesRi =>
          if (nodesRi.resp.isEmpty) {
            <.div(
              _tagRowCss,
              // Надо, чтобы юзер понимал, что запрос поиска отработан.
              nodesRi.textQuery.fold {
                Messages( MsgCodes.`No.tags.here` )
              } { query =>
                Messages( MsgCodes.`No.tags.found.for.1.query`, query )
              }
            )
          } else {
            <.div(
              MuiList()(
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
                },
              )
            )
          }
        },

        // Рендер крутилки ожидания.
        props.req.renderPending { _ =>
          <.div(
            _tagRowCss,
            MuiCircularProgress(
              new MuiCircularProgressProps {
                override val variant = MuiProgressVariants.indeterminate
                override val size = js.defined( 50 )
              }
            )
          )
        },

        // Рендер ошибки.
        props.req.renderFailed { ex =>
          // TODO Портировать на material-ui.
          // TODO Добавить кнопку reload для повторной загрузки списка.

          VdomArray(
            <.div(
              ^.key := "m",
              ^.`class` := Css.Colors.RED,
              _tagRowCss,
              ex.getMessage
            ),
            <.div(
              // При клике по ошибке надо открывать диалог с техническими подробностями и описанием ошибки.
              Mui.SvgIcons.ErrorOutline()(),
              MuiButton(
                new MuiButtonProps {
                  override val variant = MuiButtonVariants.fab
                  override val onClick: UndefOr[js.Function1[ReactEvent, Unit]] = js.defined { _: ReactEvent =>
                    _onRefreshBtnClick.runNow()
                  }
                  override val color = MuiColorTypes.primary
                }
              )(
                Mui.SvgIcons.Refresh()()
              )
            )
          )
        }

      ) // nodesList
    }

  }

  val component = ScalaComponent.builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

  def apply(nodesFound: Props) = component( nodesFound )

}
