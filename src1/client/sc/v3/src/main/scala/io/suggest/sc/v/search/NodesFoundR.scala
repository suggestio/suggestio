package io.suggest.sc.v.search

import chandu0101.scalajs.react.components.materialui._
import diode.FastEq
import diode.data.Pot
import diode.react.ReactPot._
import diode.react.ModelProxy
import io.suggest.geo.MGeoPoint
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.msg.Messages
import io.suggest.react.ReactCommonUtil
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.sc.m.search.{DoNodesSearch, MSearchRespInfo}
import io.suggest.sc.styl.ScCssStatic
import io.suggest.ueq.UnivEqUtil._
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import japgolly.univeq._
import io.suggest.maps.nodes.MGeoNodesResp
import io.suggest.ueq.JsUnivEqUtil._
import io.suggest.css.ScalaCssUtil.Implicits._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 27.07.18 18:28
  * Description: React-компонент, отвечающий за рендер списка найденных узлов: тегов и гео-размещенных узлов.
  */
class NodesFoundR(
                   nodeFoundR             : NodeFoundR,
                   commonReactCtxProv     : React.Context[MCommonReactCtx],
                 ) {

  import NodesFoundR._

  type Props_t = PropsVal
  type Props = ModelProxy[Props_t]


  class Backend($: BackendScope[Props, Unit]) {

    /** Реакция по кнопке сброса списка. */
    private def _onRefreshBtnClick(e: ReactEvent): Callback =
      dispatchOnProxyScopeCB($, DoNodesSearch(clear = true, ignorePending = true) )
    private lazy val _onRefreshBtnClickF = ReactCommonUtil.cbFun1ToJsCb( _onRefreshBtnClick )


    def render(propsProxy: Props): VdomElement = {
      val NodesCSS = ScCssStatic.Search.NodesFound
      val props = propsProxy.value

      <.div(
        // Горизонтальный прогресс-бар. Не нужен, если список уже не пустой, т.к. скачки экрана вызывает.
        ReactCommonUtil.maybe( props.req.isPending && !props.req.exists(_.resp.nodes.nonEmpty) ) {
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

        MuiList {
          val listClasses = new MuiListClasses {
            override val root = (NodesCSS.listDiv :: NodesCSS.nodesList :: Nil).toHtmlClass
          }
          new MuiListProps {
            override val classes = listClasses
          }
        } (
          // Рендер нормального списка найденных узлов.
          props.req.render { nodesRi =>
            if (nodesRi.resp.nodes.isEmpty) {
              // Надо, чтобы юзер понимал, что запрос поиска отработан.
              MuiListItem()(
                MuiListItemText()(
                  commonReactCtxProv.consume { crCtx =>
                    nodesRi.textQuery.fold {
                      crCtx.messages( MsgCodes.`No.tags.here` )
                    } { query =>
                      crCtx.messages( MsgCodes.`No.tags.found.for.1.query`, query )
                    }
                  }
                )
              )
            } else {
              nodesRi
                .resp
                .nodes
                .toVdomArray { mnode =>
                  // Нельзя nodeId.get, т.к. могут быть узлы без id.
                  val nodeId = mnode.props.idOrNameOrEmpty
                  // Рендер одного ряда. На уровне компонента обитает shouldComponentUpdate() для
                  propsProxy.wrap { props =>
                    nodeFoundR.PropsVal(
                      node                = mnode,
                      searchCss           = props.searchCss,
                      withDistanceToNull  = props.withDistanceToNull,
                      selected            = props.selectedIds contains nodeId,
                    )
                  }( nodeFoundR.component.withKey(nodeId)(_) ): VdomNode
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
                    commonReactCtxProv.consume { crCtx =>
                      crCtx.messages( MsgCodes.`Something.gone.wrong` )
                    }
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

  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

  def apply(nodesFound: Props) = component( nodesFound )

}


object NodesFoundR {

  /** Контейнер данных для рендера компонента.
    *
    * @param found Данные найденных тегов/узлов.
    * @param withDistanceTo Рендерить расстояние до указанной локации. Инстанс mapInit.userLoc.
    */
  case class PropsVal(
                       req                  : Pot[MSearchRespInfo[MGeoNodesResp]],
                       hasMore              : Boolean,
                       selectedIds          : Set[String],
                       withDistanceToNull   : MGeoPoint = null,
                       searchCss            : SearchCss,
                     )

  implicit object NodesFoundRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.req ===* b.req) &&
      (a.hasMore ==* b.hasMore) &&
      (a.selectedIds ===* b.selectedIds) &&
      (a.withDistanceToNull ===* b.withDistanceToNull) &&
      // Наверное, проверять css не нужно. Но мы всё же перерендериваем.
      (a.searchCss ===* b.searchCss)
    }
  }

  @inline implicit def univEq: UnivEq[PropsVal] = UnivEq.derive

}
