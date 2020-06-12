package io.suggest.sc.v.search.found

import com.materialui.{Mui, MuiCircularProgress, MuiCircularProgressProps, MuiGrid, MuiGridClasses, MuiGridProps, MuiIconButton, MuiIconButtonProps, MuiLinearProgress, MuiLinearProgressClasses, MuiLinearProgressProps, MuiList, MuiListItem, MuiListItemIcon, MuiListItemText, MuiListItemTextClasses, MuiListItemTextProps, MuiProgressVariants, MuiToolTip, MuiToolTipPlacements, MuiToolTipProps}
import diode.data.Pot
import diode.react.ReactPot._
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.empty.OptionUtil
import io.suggest.css.ScalaCssUtil.Implicits._
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.maps.nodes.MGeoNodesResp
import io.suggest.react.ReactCommonUtil
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.sc.m.MScReactCtx
import io.suggest.sc.m.search.{DoNodesSearch, MNodesFoundS, MSearchRespInfo}
import io.suggest.sc.v.styl.ScCssStatic
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 27.07.18 18:28
  * Description: wrap-компонент, отвечающий за рендер списка найденных узлов: тегов и гео-размещенных узлов.
  */
final class NfListR(
                     crCtxProv              : React.Context[MCommonReactCtx],
                     scReactCtxProv         : React.Context[MScReactCtx],
                   ) {

  type Props_t = MNodesFoundS
  type Props = ModelProxy[Props_t]


  case class State(
                    reqPotC               : ReactConnectProxy[Pot[MSearchRespInfo[MGeoNodesResp]]],
                    showProgressSomeC     : ReactConnectProxy[Some[Boolean]],
                  )


  class Backend($: BackendScope[Props, State]) {

    /** Реакция по кнопке сброса списка. */
    private lazy val _onRefreshBtnClickF = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      dispatchOnProxyScopeCB($, DoNodesSearch(clear = true, ignorePending = true) )
    }


    def render(s: State, children: PropsChildren): VdomElement = {
      val NodesCSS = ScCssStatic.Search.NodesFound

      <.div(

        // Горизонтальный прогресс-бар. Не нужен, если список уже не пустой, т.к. скачки экрана вызывает.
        s.showProgressSomeC { showProgressSomeProxy =>
          ReactCommonUtil.maybeEl( showProgressSomeProxy.value.value ) {
            val lpCss = new MuiLinearProgressClasses {
              override val root = NodesCSS.linearProgress.htmlClass
            }
            MuiLinearProgress(
              new MuiLinearProgressProps {
                override val variant = MuiProgressVariants.indeterminate
                override val classes = lpCss
              }
            )
          }
        },

        MuiGrid {
          val listClasses = new MuiGridClasses {
            override val root = (NodesCSS.listDiv :: NodesCSS.nodesList :: Nil).toHtmlClass
          }
          new MuiGridProps {
            override val container = true
            override val classes = listClasses
          }
        } (

          // Рендер нормального списка найденных узлов.
          s.reqPotC { reqPotProxy =>
            val req = reqPotProxy.value

            React.Fragment(

              req.render { nodesRi =>
                if (nodesRi.resp.nodes.isEmpty) {
                  // Надо, чтобы юзер понимал, что запрос поиска отработан.
                  MuiGrid(
                    new MuiGridProps {
                      override val item = true
                    }
                  )(
                    MuiList()(
                      scReactCtxProv.consume { scReactCtx =>
                        MuiListItemText {
                          val css = new MuiListItemTextClasses {
                            override val root = scReactCtx.scCss.fgColor.htmlClass
                          }
                          new MuiListItemTextProps {
                            override val classes = css
                          }
                        } (
                          {
                            val (msgCode, msgArgs) = nodesRi.textQuery.fold {
                              MsgCodes.`No.tags.here` -> List.empty[js.Any]
                            } { query =>
                              MsgCodes.`No.tags.found.for.1.query` -> ((query: js.Any) :: Nil)
                            }
                            crCtxProv.message( msgCode, msgArgs: _* )
                          }
                        )
                      }
                    )
                  )
                } else {
                  children
                }
              },

              // Рендер крутилки ожидания.
              req.renderPending { _ =>
                MuiGrid(
                  new MuiGridProps {
                    override val item = true
                  }
                )(
                  MuiCircularProgress(
                    new MuiCircularProgressProps {
                      override val variant = MuiProgressVariants.indeterminate
                      override val size = 50
                    }
                  )
                )
              },

              // Рендер ошибки.
              req.renderFailed { ex =>
                val errHint = Option(ex.getMessage)
                  .getOrElse(ex.getClass.getName)

                val gridItemProps = new MuiGridProps {
                  override val item = true
                }

                React.Fragment(

                  // Рендер технических подробностей ошибки.
                  MuiToolTip.component.withKey("e")(
                    // TODO Надо tooltip разнести над всем рядом, а не только над иконкой.
                    new MuiToolTipProps {
                      override val title = errHint
                      override val placement = MuiToolTipPlacements.Top
                    }
                  )(
                    MuiGrid( gridItemProps )(
                      MuiListItem()(
                        MuiListItemText()(
                          crCtxProv.message( MsgCodes.`Something.gone.wrong` ),
                        ),
                        MuiListItemIcon()(
                          Mui.SvgIcons.ErrorOutline()()
                        )
                      )
                    )
                  ),

                  // Кнопка reload для повторной загрузки списка.
                  MuiGrid.component.withKey("r")( gridItemProps )(
                    MuiIconButton(
                      new MuiIconButtonProps {
                        override val onClick = _onRefreshBtnClickF
                      }
                    )(
                      Mui.SvgIcons.Refresh()()
                    )
                  ),

                )
              },

            )
          },

        ) // /MuiList

      )
    }

  }

  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(
        reqPotC = propsProxy.connect( _.req ),

        showProgressSomeC = propsProxy.connect { props =>
          val showProgress = props.req.isPending && !props.req.exists(_.resp.nodes.nonEmpty)
          OptionUtil.SomeBool( showProgress )
        },
      )
    }
    .renderBackendWithChildren[Backend]
    .build

}
