package io.suggest.sc.v.search.found

import com.materialui.{MuiAnchorOrigin, MuiLinearProgress, MuiLinearProgressClasses, MuiLinearProgressProps, MuiPopOver, MuiPopOverProps, MuiProgressVariants, MuiToolBar, MuiToolBarProps}
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.empty.OptionUtil
import io.suggest.css.Css
import io.suggest.i18n.MCommonReactCtx
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.sc.m.{MScReactCtx, MScRoot}
import io.suggest.sc.m.search.{MNodesFoundRowProps, MScSearchText, NodesFoundPopupOpen, NodesScroll}
import io.suggest.sc.v.search.STextR
import io.suggest.sc.v.styl.ScCssStatic
import io.suggest.sjs.common.empty.JsOptionUtil
import io.suggest.spa.FastEqUtil
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom.html

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.06.2020 12:03
  * Description: Компонент поиска узлов.
  */
final class NodesFoundR(
                         sTextR                   : STextR,
                         nfListR                  : NfListR,
                         scReactCtxP              : React.Context[MScReactCtx],
                         crCtxP                   : React.Context[MCommonReactCtx],
                       ) {

  type Props_t = MScRoot
  type Props = ModelProxy[Props_t]


  case class State(
                    nodesFoundRowsC               : ReactConnectProxy[Seq[MNodesFoundRowProps]],
                    showProgressSomeC             : ReactConnectProxy[Some[Boolean]],
                    usePopOverSomeC               : ReactConnectProxy[Some[Boolean]],
                    popOverOpenedSomeC            : ReactConnectProxy[Some[Boolean]],
                  )

  class Backend($: BackendScope[Props, State]) {

    private var _textFieldHref = Ref[html.Span]

    private val _onPopOverClose = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, NodesFoundPopupOpen( open = false ) )
    }

    /** Скроллинг в списке найденных узлов. */
    private def _onScroll(e: ReactEventFromHtml): Callback = {
      val scrollTop = e.target.scrollTop
      val scrollHeight = e.target.scrollHeight
      ReactDiodeUtil.dispatchOnProxyScopeCB($, NodesScroll(scrollTop, scrollHeight) )
    }


    def render(propsProxy: Props, s: State): VdomElement = {
      // Поисковое текстовое поле:
      val searchToolBar: VdomElement = MuiToolBar(
        new MuiToolBarProps {
          override val disableGutters = true
        }
      )(
        // Элементы строки поиска:
        propsProxy.wrap(_.index.search.text)( sTextR.component.apply )(implicitly, MScSearchText.MScSearchTextFastEq),

        // Горизонтальный прогресс-бар. Не нужен, если список уже не пустой, т.к. скачки экрана вызывает.
        s.showProgressSomeC { showProgressSomeProxy =>
          val isVisible = showProgressSomeProxy.value.value
          val lpCss = new MuiLinearProgressClasses {
            override val root = Css.flat(
              ScCssStatic.Search.NodesFound.progress.htmlClass,
              if (isVisible) Css.Display.VISIBLE else Css.Display.INVISIBLE,
            )
          }
          MuiLinearProgress(
            new MuiLinearProgressProps {
              override val variant = if (isVisible) MuiProgressVariants.indeterminate else MuiProgressVariants.determinate
              override val classes = lpCss
              override val value   = JsOptionUtil.maybeDefined( !isVisible )(0)
            }
          )
        },
      )

      val nodesRows = crCtxP.consume { crCtx =>
        scReactCtxP.consume { scReactCtx =>
          val nfRowR = NfRowR( crCtx, scReactCtx )
          s.nodesFoundRowsC( nfRowR.rows )
        }
      }
      val nodesList: VdomElement = propsProxy.wrap( _.index.search.geo.found )( nfListR.component(_)( nodesRows ) )
      lazy val searchTbWithList = <.div(
        searchToolBar,
        nodesList,
      )

      val forScroll = s.usePopOverSomeC { usePopOverSomeProxy =>
        val usePopOver = usePopOverSomeProxy.value.value
        if (usePopOver) {
          nodesList
        } else {
          searchTbWithList
        }
      }

      // Нода с единым скроллингом
      val scrollable = {
        val onScrollTm =
          ^.onScroll ==> _onScroll
        propsProxy.wrap( _.index.search.geo.css.NodesFound.container.htmlClass ) { cssClassProxy =>
          <.div(
            ^.`class` := cssClassProxy.value,
            onScrollTm,
            forScroll,
          )
        }
      }

      // Если экран позволяет, то рендерить список внутри попапа.
      lazy val popOver: VdomElement = {
        val __getAnchorEl = { () =>
          _textFieldHref
            .get
            .map( js.defined(_) )
            .getOrElse( js.undefined: js.UndefOr[html.Span] )
            .runNow()
        }: js.Function
        val _anchorOrigin = new MuiAnchorOrigin {
          override val vertical = MuiAnchorOrigin.bottom
          override val horizontal = MuiAnchorOrigin.left
        }
        <.div(
          <.span.withRef( _textFieldHref )(
            searchToolBar,
          ),
          s.popOverOpenedSomeC { popOverOpenedSomeProxy =>
            MuiPopOver.component {
              new MuiPopOverProps {
                override val open = popOverOpenedSomeProxy.value.value
                override val onClose = _onPopOverClose
                override val anchorEl = js.defined( __getAnchorEl )
                override val anchorOrigin = _anchorOrigin
                override val marginThreshold = 0
              }
            }( scrollable )
          }
        )
      }

      s.usePopOverSomeC { usePopOverSomeProxy =>
        val usePopOver = usePopOverSomeProxy.value.value
        if (usePopOver) {
          popOver
        } else {
          scrollable
        }
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(

        nodesFoundRowsC = propsProxy.connect { mroot =>
          mroot.index.searchGeoNodesFoundProps
        }( FastEqUtil.CollFastEq(MNodesFoundRowProps.MNodesFoundRowPropsFeq) ),

        showProgressSomeC = propsProxy.connect { props =>
          val nodeSearchReq = props.index.search.geo.found.req
          val showProgress = nodeSearchReq.isPending && !nodeSearchReq.exists(_.resp.nodes.nonEmpty)
          OptionUtil.SomeBool( showProgress )
        },

        usePopOverSomeC = propsProxy.connect { props =>
          val isUsePopOver = props.dev.screen.info.screen.isHeightEnought
          OptionUtil.SomeBool( isUsePopOver )
        },

        popOverOpenedSomeC = propsProxy.connect { props =>
          val visible = props.index.search.geo.found.visible
          OptionUtil.SomeBool( visible )
        },

      )
    }
    .renderBackend[Backend]
    .build

}
