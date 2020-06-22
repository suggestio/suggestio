package io.suggest.sc.v.search.found

import com.materialui.{Mui, MuiFormControl, MuiFormControlClasses, MuiFormControlProps, MuiIconButton, MuiIconButtonClasses, MuiIconButtonProps, MuiInput, MuiInputClasses, MuiInputProps, MuiInputPropsMargins, MuiLinearProgress, MuiLinearProgressClasses, MuiLinearProgressProps, MuiProgressVariants, MuiToolBar, MuiToolBarProps}
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.empty.OptionUtil
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.sc.m.{MScReactCtx, MScRoot}
import io.suggest.sc.m.search.{MNodesFoundRowProps, NodesFoundPopupOpen, NodesScroll, SearchTextChanged}
import io.suggest.sc.v.hdr.RightR
import io.suggest.sc.v.styl.ScCssStatic
import io.suggest.sjs.common.empty.JsOptionUtil
import io.suggest.spa.FastEqUtil
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom
import org.scalajs.dom.raw.HTMLInputElement
import scalacss.ScalaCssReact._

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.06.2020 12:03
  * Description: Компонент поиска узлов.
  */
final class NodesFoundR(
                         rightR                   : RightR,
                         nfRowR                   : NfRowR,
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
                    queryC                        : ReactConnectProxy[String],
                    queryEmptySomeC               : ReactConnectProxy[Some[Boolean]],
                  )

  class Backend($: BackendScope[Props, State]) {

    /** Происходит ввод текста в input. */
    private val _onInputJsF = ReactCommonUtil.cbFun1ToJsCb { e: ReactEventFromInput =>
      val text = e.target.value
      dispatchOnProxyScopeCB($, SearchTextChanged(text))
    }


    /** Callback для клика по кнопке очистики поискового поля. */
    private lazy val _onClearClickJsF = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      var cb = dispatchOnProxyScopeCB($, SearchTextChanged("", noWait = true))
      // focus на поле надо:
      if (_htmlInputRef.nonEmpty) {
        cb = cb >> Callback {
          for (htmlInput <- _htmlInputRef)
            htmlInput.focus()
        }
      }
      // И вернуть итоговый callback:
      cb
    }


    /** Инстанс нативного элемента, чтобы фокусом отсюда управлять. */
    private var _htmlInputRef: Option[HTMLInputElement] = None
    /** Callback для перехвата ref'а DOM input-ноды. */
    private lazy val _htmlInputRefHandlerJsF: js.Function1[HTMLInputElement, Unit] = {
      el: HTMLInputElement =>
        _htmlInputRef = Some( el )
    }


    /** На touch-устройствах, надо скрывать экранную клавиатуру, когда начинается скроллинг списка.
      * Если началась какая-то touch-возня в списке, а text input в фокусе, нужна расфокусировка.
      */
    private val _onNfListTouchMove = { e: ReactUIEventFromHtml =>
      val tg = e.target
      if (dom.document.activeElement eq tg) {
        Callback.empty
      } else {
        Callback( tg.focus() )
      }
    }


    /** Скроллинг в списке найденных узлов. */
    private def _onScroll(e: ReactEventFromHtml): Callback = {
      val scrollTop = e.target.scrollTop
      val scrollHeight = e.target.scrollHeight
      ReactDiodeUtil.dispatchOnProxyScopeCB($, NodesScroll(scrollTop, scrollHeight) )
    }


    def render(propsProxy: Props, s: State): VdomElement = {
      // Рендер текстового поля с input'ом.
      val TextBarCSS = ScCssStatic.Search.TextBar

      // Поисковое текстовое поле:
      val searchToolBar: VdomElement = MuiToolBar(
        new MuiToolBarProps {
          override val disableGutters = true
        }
      )(

        // Элементы строки поиска:
        <.div(
          TextBarCSS.bar,

          MuiFormControl {
            val formCtlCss = new MuiFormControlClasses {
              override val root = TextBarCSS.inputFormControl.htmlClass
            }
            new MuiFormControlProps {
              override val classes = formCtlCss
            }
          }(
            // Текстовое поле поисковой строки:
            crCtxP.consume { crCtx =>
              val startSearchTypingMsg = crCtx.messages( MsgCodes.`Search.start.typing` )

              scReactCtxP.consume { scReactCtx =>
                s.queryC { queryProxy =>
                  MuiInput {
                    val query = queryProxy.value
                    val inputCss = new MuiInputClasses {
                      override val underline = scReactCtx.scCss.Search.TextBar.underline.htmlClass
                      override val root = TextBarCSS.input.htmlClass
                    }
                    new MuiInputProps {
                      override val classes = inputCss
                      override val `type` = HtmlConstants.Input.text
                      override val onChange = _onInputJsF
                      override val placeholder = startSearchTypingMsg
                      override val value = js.defined( query )
                      override val margin = if (query.length > 15) MuiInputPropsMargins.dense else MuiInputPropsMargins.none
                      // clear-кнопка:
                      //override val endAdornment = clearBtnUndef
                      override val inputRef = js.defined( _htmlInputRefHandlerJsF )
                    }
                  }
                }
              }
            },

            // Крестик быстрой очистки поля поиска:
            {
              val clearIcon = Mui.SvgIcons.HighlightOffOutlined()()
              s.queryEmptySomeC { queryEmptySomeProxy =>
                // Кнопка быстрой очистки поля.
                MuiIconButton {
                  val iconBtnCss = new MuiIconButtonClasses {
                    override val root = {
                      if (queryEmptySomeProxy.value.value) Css.Display.INVISIBLE
                      else Css.Display.VISIBLE
                    }
                  }
                  new MuiIconButtonProps {
                    override val classes = iconBtnCss
                    override val onClick = _onClearClickJsF
                    override val disableRipple = true
                  }
                } (
                  clearIcon,
                )
              }
            },

            // Кнопка сворачивания:
            propsProxy.wrap(_ => None)( rightR.apply ),
          ),

        ),

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

      val nodesRows = nfRowR( s.nodesFoundRowsC )
      val nodesList: VdomElement = propsProxy.wrap( _.index.search.geo.found ) { foundProxy =>
        val props = nfListR.PropsVal(
          nodesFoundProxy   = foundProxy,
          onTouchStartF     = Some( _onNfListTouchMove ),
        )
        nfListR.component(props)( nodesRows )
      }
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
      val scrollable = <.div(
        ^.`class` := propsProxy.value.index.search.geo.css.NodesFound.container.htmlClass,
        ^.onScroll ==> _onScroll,
        forScroll,
      )

      // Если экран позволяет, то рендерить список внутри попапа.
      lazy val popOver: VdomElement = {
        <.div(
          //<.span(
            searchToolBar,
          //),
          s.popOverOpenedSomeC { popOverOpenedSomeProxy =>
            <.div(
              if ( popOverOpenedSomeProxy.value.value ) ^.display.block else ^.display.none,
              scrollable,
            )
          },
        )
      }

      s.usePopOverSomeC { usePopOverSomeProxy =>
        if ( usePopOverSomeProxy.value.value ) {
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


        queryC = propsProxy.connect( _.index.search.text.query ),

        queryEmptySomeC = propsProxy.connect { props =>
          OptionUtil.SomeBool( props.index.search.text.query.isEmpty )
        },

      )
    }
    .renderBackend[Backend]
    .build

}
