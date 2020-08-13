package io.suggest.lk.nodes.form.r.tree

import com.materialui.{Mui, MuiButton, MuiButtonProps, MuiButtonVariants, MuiColorTypes, MuiLink, MuiLinkProps}
import diode.FastEq
import diode.react.ModelProxy
import diode.react.ReactPot.potWithReact
import io.suggest.bill.MPrice
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.lk.nodes.form.m._
import io.suggest.lk.nodes.form.r.menu.{NodeMenuBtnR, NodeMenuR}
import io.suggest.lk.r.LkPreLoaderR
import io.suggest.msg.JsFormatUtil
import io.suggest.react.{Props2ModelProxy, ReactCommonUtil, ReactDiodeUtil}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._
import io.suggest.common.empty.OptionUtil.BoolOptOps
import io.suggest.n2.node.MNodeTypes
import io.suggest.routes.routes
import io.suggest.sc.ScConstants
import io.suggest.scalaz.NodePath_t
import io.suggest.spa.SioPages
import io.suggest.xplay.json.PlayJsonSjsUtil
import play.api.libs.json.Json

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.03.17 18:28
  * Description: Рекурсивный react-компонент одной ноды дерева [[TreeR]].
  * Изначально разрастался прямо внутри [[TreeR]].
  */
class NodeR(
             nodeMenuR    : NodeMenuR,
             nodeMenuBtnR : NodeMenuBtnR,
             crCtxP       : React.Context[MCommonReactCtx],
           ) {

  type Props = PropsVal

  /** Модель props текущего компонента.
    *
    * @param node Корневой узел этого под-дерева.
    * @param nodePathRev Обратный ключ узла в общем дереве.
    *                    Используем обратную адресацию для состояния, т.к. это снижает расход памяти при простое:
    *                    родительские tail'ы шарятся между дочерними элементами.
    * @param proxy Proxy любой модели, нужен для диспатчинга в коллбэках.
    *              Живёт отдельно от данных из-за рекурсивной природы этого компонента.
    */
  case class PropsVal(
                       node          : MNodeState,
                       nodePathRev   : NodePath_t,
                       opened        : Option[NodePath_t],
                       confAdId      : Option[String],
                       chCount       : Int,
                       chCountEnabled: Int,
                       proxy         : ModelProxy[_],
                     ) {
    def nodePath = nodePathRev.reverse.tail
  }
  implicit object NodeRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.node ===* b.node) &&
      (a.nodePathRev ==* b.nodePathRev) &&
      (a.opened ===* b.opened) &&
      (a.confAdId ===* b.confAdId) &&
      (a.chCount ==* b.chCount) &&
      (a.chCountEnabled ==* b.chCountEnabled)
    }
  }
  implicit object PropsVal2ModelProxy extends Props2ModelProxy[PropsVal] {
    override def apply(v1: PropsVal) = v1.proxy
  }


  class Backend($: BackendScope[Props, Unit]) {

    /** Callback клика по заголовку узла. */
    private lazy val onNodeClick: Callback = {
      ReactDiodeUtil.dispatchOnProxyScopeCBf($) { props: Props =>
        NodeNameClick( props.nodePath )
      }
    }

    /** Callback клика по кнопке добавления под-узла для текущего узла. */
    private lazy val onCreateNodeClick: Callback =
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, CreateNodeClick )

    /** Реакция на изменение значения флага активности узла. */
    private def onNodeEnabledChange(e: ReactEventFromInput): Callback = {
      val isChecked = e.target.checked
      ReactDiodeUtil.dispatchOnProxyScopeCBf($) { props: Props =>
        NodeIsEnabledChanged(props.nodePath, isEnabled = isChecked)
      }
    }

    /** Callback для кнопки редактирования узла. */
    private def onNodeEditClick(e: ReactEventFromInput): Callback = {
      e.stopPropagationCB >> ReactDiodeUtil.dispatchOnProxyScopeCBf($) { props: Props =>
        NodeEditClick( props.nodePath )
      }
    }

    private def onNodeEditNameChange(e: ReactEventFromInput): Callback = {
      val name = e.target.value
      ReactDiodeUtil.dispatchOnProxyScopeCBf($) { props: Props =>
        NodeEditNameChange( props.nodePath, name = name )
      }
    }

    /** Callback нажатия по кнопке сохранения отредактированного узла. */
    private lazy val onNodeEditOkClick: Callback = {
      ReactDiodeUtil.dispatchOnProxyScopeCBf($) { props: Props =>
        NodeEditOkClick( props.nodePath )
      }
    }

    /** Callback нажатия по кнопке отмены редактирования узла. */
    private lazy val onNodeEditCancelClick: Callback = {
      ReactDiodeUtil.dispatchOnProxyScopeCBf($) { props: Props =>
        NodeEditCancelClick( props.nodePath )
      }
    }

    /** Callback изменения галочки управления размещением текущей карточки на указанном узле. */
    private def onAdvOnNodeChanged(e: ReactEventFromInput): Callback = {
      val isChecked = e.target.checked
      e.stopPropagationCB >> ReactDiodeUtil.dispatchOnProxyScopeCBf($) { props: Props =>
        AdvOnNodeChanged( props.nodePath, isEnabled = isChecked )
      }
    }

    /** Callback запуска редактирования тарифа текущего узла. */
    private lazy val onTfChangeClick: Callback =
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, TfDailyEditClick )

    private lazy val onTfShowDetailsClick: Callback = {
      ReactDiodeUtil.dispatchOnProxyScopeCBf( $ ) { props: Props =>
        TfDailyShowDetails( props.nodePath )
      }
    }

    /** Callback клика по галочке отображения по дефолту в раскрытом виде. */
    private def onShowOpenedClick(e: ReactEventFromInput): Callback = {
      val isChecked = e.target.checked
      e.stopPropagationCB >> ReactDiodeUtil.dispatchOnProxyScopeCBf($) { props: Props =>
        AdvShowOpenedChange( props.nodePath, isChecked )
      }
    }

    /** Callback клика по галочке отображения по дефолту в раскрытом виде. */
    private def onAlwaysOutlinedClick(e: ReactEventFromInput): Callback = {
      val isChecked = e.target.checked
      e.stopPropagationCB >> ReactDiodeUtil.dispatchOnProxyScopeCBf($) { props: Props =>
        AlwaysOutlinedSet( props.nodePath, isChecked )
      }
    }


    private def _smallWaitLoader  = LkPreLoaderR.AnimSmall

    private val _delim = <.div(
      ^.`class` := Css.Lk.Nodes.DELIM
    )


    // Утиль для таблиц ключ-значение, пояснящих про узел:
    private def _kvTdClasses(tail: List[String]): String = {
      Css.flat1( Css.Table.Td.TD :: Css.Table.Td.WHITE :: Css.Size.M :: tail )
    }
    private val _kvTdKey = <.td(
      ^.`class` := _kvTdClasses( Css.Font.Sz.M :: Css.Colors.LIGHT_GRAY :: Css.Lk.Nodes.KvTable.Td.KEY :: Nil )
    )
    private val _kvTdValue = <.td(
      ^.`class` := _kvTdClasses( Css.Font.Sz.L :: Css.Lk.Nodes.KvTable.Td.VALUE :: Nil )
    )


    /**
      * Рекурсивный рендер под-дерева узлов.
      *
      * @param p Пропертисы.
      * @return React-элемент.
      */
    def render(p: Props): VdomElement = {
      val isShowProps = p.opened contains p.nodePath
      val isShowNodeEditProps = isShowProps && p.confAdId.isEmpty
      val isShowAdvProps = isShowProps && p.confAdId.isDefined

      // Контейнер узла узла + дочерних узлов.
      <.div(
        ^.key := p.node.info.id,

        // Сдвиг слева согласно уровню, чтобы выглядело как дерево.
        ^.marginLeft := (p.nodePathRev.length * 20).px,

        // Разделитель-промежуток от предыдущего элемента сверху.
        _delim,
        // Если на текущем узле отображаются пропертисы, то нужен толстый delimiter
        _delim.when( isShowProps ),

        <.div(
          ^.classSet1(
            Css.flat(
              Css.Table.Td.TD,
              Css.Size.M,
              Css.Lk.Nodes.Name.NAME
            ),
            // CSS-классы режима узла: normal | disabled | editing
            Css.Lk.Nodes.Name.NORMAL   -> p.node.info.isEnabled,
            Css.Lk.Nodes.Name.DISABLED -> !p.node.info.isEnabled,
            Css.Lk.Nodes.Name.SHOWING  -> p.node.editing.isEmpty,
            Css.Lk.Nodes.Name.EDITING  -> p.node.editing.nonEmpty,

            // Закруглять углы только когда узел не раскрыт.
            Css.Table.Td.Radial.FIRST -> !isShowProps,
          ),
          // Во время неРедактирования можно сворачивать-разворачивать блок, кликая по нему.
          ReactCommonUtil.maybe(p.node.isNormal) {
            ^.onClick --> onNodeClick
          },

          p.node.editing.fold[VdomElement] {
            // Рендер названия узла. В зависимости от режима формы, могут быть варианты того, где он будет находиться.
            val nameSpan = <.span(
              ^.`class` := Css.Lk.Nodes.Name.TITLE,
              p.node.info.name,
            )

            // контейнер названия текущего узла
            <.div(
              ^.`class` := Css.flat(Css.Font.Sz.L, Css.Lk.Nodes.Name.CONTENT),

              p.confAdId.fold [VdomElement] (nameSpan) { _ =>
                // Рендерить галочку размещения текущей карточки на данном узле, если режим размещения активен сейчас.
                <.label(
                  ^.`class` := Css.flat( Css.Input.INPUT, Css.CLICKABLE ),
                  ^.onClick  ==> { e: ReactEventFromInput => e.stopPropagationCB },
                  <.input(
                    ^.`type` := HtmlConstants.Input.checkbox,
                    if (p.node.advIsPending) {
                      ^.disabled := true
                    } else {
                      ^.onChange ==> onAdvOnNodeChanged
                    },
                    ^.checked   := p.node.adv
                      .map(_.newIsEnabled)
                      .orElse( p.node.info.adv.map(_.hasAdv) )
                      .getOrElseFalse
                  ),
                  <.span,
                  HtmlConstants.NBSP_STR,
                  nameSpan
                )
              },

              // Рендерить кнопку редактирования имени, если ситуация позволяет.
              ReactCommonUtil.maybeEl( isShowNodeEditProps && p.node.isNormal ) {
                <.span(
                  HtmlConstants.NBSP_STR,
                  HtmlConstants.NBSP_STR,
                  crCtxP.consume { crCtx =>
                    <.span(
                      ^.`class` := Css.Lk.Nodes.Name.EDIT_BTN,
                      ^.onClick ==> onNodeEditClick,
                      ^.title   := crCtx.messages( MsgCodes.`Change` ),
                    )
                  }
                )
              },

              p.node.adv.whenDefined { advState =>
                <.span(
                  HtmlConstants.NBSP_STR,

                  // Если Pending, крутилку отобразить.
                  advState.newIsEnabledPot.renderPending { _ =>
                    _smallWaitLoader
                  },

                  // Если ошибка, то отобразить сообщение о проблеме.
                  advState.newIsEnabledPot.renderFailed { ex =>
                    <.span(
                      ^.title := ex.toString,
                      crCtxP.message( MsgCodes.`Error` )
                    )
                  }
                )
              },

              HtmlConstants.NBSP_STR
                .unless( p.node.advIsPending ),

              // Если инфа по узлу запрашивается с сервера, от отрендерить прелоадер
              p.node.infoPot.renderPending { _ =>
                <.span(
                  HtmlConstants.NBSP_STR,
                  _smallWaitLoader
                )
              },

              // Кнопка удаления узла.
              ReactCommonUtil.maybeEl( isShowNodeEditProps && (p.node.info.canChangeAvailability contains true) ) {
                <.div(
                  ^.`class` := Css.Lk.Nodes.Menu.MENU,
                  ^.onClick ==> ReactCommonUtil.stopPropagationCB,
                  nodeMenuBtnR.component( p.proxy ),
                  nodeMenuR.component( p.proxy ),
                )
              },

            )

          } { ed =>
            <.div(

              <.div(
                ^.`class` := Css.flat( Css.Lk.Nodes.Inputs.INPUT70, Css.Input.INPUT ),

                crCtxP.consume { crCtx =>
                  <.input(
                    ^.placeholder := p.node.info.name,
                    ^.title := (
                      crCtx.messages(MsgCodes.`Type.new.name.for.beacon.0`, p.node.info.name) +
                      HtmlConstants.SPACE +
                      crCtx.messages( MsgCodes.`For.example.0`,
                        crCtx.messages( MsgCodes.`Beacon.name.example` )
                      )
                    ),
                    ^.value := ed.name,
                    // Блокировать поле, пока происходит сохранение на сервер.
                    if (ed.saving.isPending) {
                      ^.disabled := true
                    } else {
                      ^.onChange ==> onNodeEditNameChange
                    }
                  )
                }
              ),

              // Происходит редактирование узла. Отобразить кнопки сохранения.
              <.div(
                ^.`class` := Css.Lk.Nodes.Name.EDITING_BTNS,

                if (ed.saving.isPending) {
                  // Идёт сохранение на сервер прямо сейчас. Отрендерить сообщение о необходимости подождать.
                  crCtxP.consume { crCtx =>
                    <.div(
                      ^.title := crCtx.messages( MsgCodes.`Server.request.in.progress.wait` ),
                      crCtx.messages( MsgCodes.`Please.wait` ),
                      _smallWaitLoader
                    )
                  }

                } else {
                  <.div(
                    // Кнопка сохранения изменений.
                    <.a(
                      ^.`class` := Css.flat(Css.Buttons.BTN, Css.Buttons.MAJOR, Css.Size.M),
                      ^.onClick --> onNodeEditOkClick,
                      crCtxP.message( MsgCodes.`Save` )
                    ),
                    HtmlConstants.SPACE,
                    // Кнопка отмены редактирования.
                    <.a(
                      ^.`class` := Css.flat(Css.Buttons.BTN, Css.Buttons.NEGATIVE, Css.Size.M),
                      ^.onClick --> onNodeEditCancelClick,
                      crCtxP.message( MsgCodes.`Cancel` ),
                    )
                  )
                }
              )

            )
          }
        ),

        // Рендер подробной информации по узлу
        if (isShowNodeEditProps) {
          <.div(

            // Данные по узлу рендерим таблицей вида ключ-значение. Однако, возможна третья колонка с крутилкой.
            <.table(
              ^.`class` := Css.flat( Css.Table.TABLE, Css.Table.Width.XL, Css.Lk.Nodes.KvTable.LKN_TABLE ),

              <.tbody(

                // id узла.
                <.tr(
                  _kvTdKey(
                    crCtxP.message( MsgCodes.`Identifier` ),
                  ),
                  _kvTdValue(
                    p.node.info.id,

                    HtmlConstants.SPACE,

                    // Кнопка перехода в ЛК узла
                    ReactCommonUtil.maybeNode( p.node.info.ntype ==* MNodeTypes.AdnNode ) {
                      MuiLink(
                        new MuiLinkProps {
                          val href = routes.controllers.LkAds.adsPage( p.node.info.id ).url
                        }
                      )(
                        MuiButton(
                          new MuiButtonProps {
                            override val variant = MuiButtonVariants.outlined
                            override val startIcon = Mui.SvgIcons.ArrowForward()().rawNode
                            override val color = MuiColorTypes.primary
                          }
                        )(
                          crCtxP.message( MsgCodes.`Go.into` ),
                        )
                      )
                    },

                    HtmlConstants.SPACE,

                    // Кнопка перехода в выдачу узла:
                    MuiLink(
                      new MuiLinkProps {
                        val target = "_blank"
                        val href = ScConstants.ScJsState.fixJsRouterUrl(
                          routes.controllers.sc.ScSite.geoSite(
                            PlayJsonSjsUtil.toNativeJsonObj(
                              Json.toJsObject(
                                SioPages.Sc3(
                                  nodeId = Some( p.node.info.id ),
                                )
                              )
                            )
                          ).url
                        )
                      }
                    )(
                      MuiButton(
                        new MuiButtonProps {
                          override val variant = MuiButtonVariants.outlined
                          override val startIcon = Mui.SvgIcons.Apps()().rawNode
                          override val color = MuiColorTypes.primary
                        }
                      )(
                        crCtxP.message( MsgCodes.`Showcase` ),
                      )
                    ),
                  ),
                ),

                // Галочка управления активностью узла, если определено.
                p.node.info.canChangeAvailability.whenDefined { cca =>
                  val isEnabledValue = p.node
                    .isEnabledUpd
                    .fold( p.node.info.isEnabled )(_.newIsEnabled)
                  <.tr(
                    _kvTdKey(
                      crCtxP.message( MsgCodes.`Is.enabled` ),
                    ),

                    _kvTdValue(
                      // Чек-бокс для управления isEnabled-флагом узла.
                      <.label(
                        ^.`class` := Css.flat( Css.Input.INPUT, Css.CLICKABLE, Css.Lk.Nodes.Inputs.INPUT70 ),
                        <.input(
                          ^.`type` := HtmlConstants.Input.checkbox,
                          // Текущее значение галочки происходит из нового значения и текущего значения, полученного на сервере.
                          ^.checked := isEnabledValue,
                          // Можно управлять галочкой, если разрешено и если не происходит какого-то запроса с обновлением сейчас.
                          if (cca && p.node.isEnabledUpd.isEmpty) {
                            ^.onChange ==> onNodeEnabledChange
                          } else {
                            ^.disabled := true
                          }
                        ),
                        <.span(),
                        crCtxP.message( MsgCodes.yesNo(isEnabledValue) ),
                      ),

                      // Рендер данные по реквестов обновления флага isEnabled.
                      p.node.isEnabledUpd.whenDefined { upd =>
                        <.span(
                          HtmlConstants.NBSP_STR
                            .when( upd.request.isPending || upd.request.isFailed ),
                          // Крутилка ожидания, если происходит запрос к серверу за обновлением.
                          upd.request.renderPending { _ =>
                            _smallWaitLoader
                          },
                          // Рендер данных по ошибке запроса, если она имеет место.
                          upd.request.renderFailed { ex =>
                            <.span(
                              ^.`class` := Css.Colors.RED,
                              ^.title := ex.toString,
                              crCtxP.message( MsgCodes.`Error` ),
                            )
                          }
                        )
                      }
                    ),
                  )
                }, // for

                // Рендер строки данных по тарифу размещения.
                p.node.info.tf.whenDefined { tfInfo =>
                  val perDay = crCtxP.message( MsgCodes.`_per_.day` )

                  val changeBtn = <.a(
                    ^.`class`  := Css.Lk.LINK,
                    ^.onClick --> onTfChangeClick,
                    crCtxP.message( MsgCodes.`Change` ),
                    HtmlConstants.ELLIPSIS,
                  )

                  <.tr(
                    _kvTdKey(
                      crCtxP.message( MsgCodes.`Adv.tariff` ),
                    ),

                    _kvTdValue(
                      crCtxP.message( tfInfo.mode.msgCode ),

                      // Есть два отображения для тарифа: компактное одной строчкой и развёрнутое
                      if (p.node.tfInfoWide) {
                        // Рендер развёрнутого отображения.
                        <.span(
                          HtmlConstants.COMMA,
                          HtmlConstants.SPACE,
                          changeBtn,

                          // Описать текущий тариф:
                          <.table(
                            ^.`class` := Css.Table.TABLE,
                            <.tbody(
                              tfInfo.clauses.toVdomArray { case (mCalType, mPrice) =>
                                <.tr(
                                  ^.key := mCalType.value,
                                  _kvTdKey(
                                    crCtxP.message( mCalType.i18nCode ),
                                  ),
                                  _kvTdValue(
                                    JsFormatUtil.formatPrice(mPrice),
                                    perDay
                                  )
                                )
                              }
                            )
                          ),

                          <.br,
                          crCtxP.message( MsgCodes.`Comission.0.pct.for.sio`, tfInfo.comissionPct ),
                        )
                      } else {
                        // Рендер компактной инфы по тарифу.
                        crCtxP.consume { crCtx =>
                          <.span(
                            HtmlConstants.COLON,
                            HtmlConstants.SPACE,
                            tfInfo.clauses.toVdomArray { case (mCalType, mPrice) =>
                              <.span(
                                ^.key := mCalType.value,
                                ReactCommonUtil.maybe( mCalType !=* tfInfo.clauses.head._1 ) {
                                  TagMod(
                                    HtmlConstants.SPACE,
                                    HtmlConstants.SLASH,
                                    HtmlConstants.SPACE,
                                  )
                                },
                                ^.title := crCtx.messages( mCalType.i18nCode ),
                                MPrice.amountStr(mPrice)
                              )
                            },
                            <.span(
                              ^.title := crCtx.messages( tfInfo.currency.currencyNameI18n ),
                              crCtx.messages(tfInfo.currency.i18nPriceCode, "")
                            ),
                            perDay,
                            // Кнопка показа подробностей по тарифу.
                            <.a(
                              ^.`class`  := Css.Lk.LINK,
                              ^.title    := crCtx.messages( MsgCodes.`Show.details` ),
                              ^.onClick --> onTfShowDetailsClick,
                              HtmlConstants.ELLIPSIS
                            ),
                            HtmlConstants.COMMA,
                            HtmlConstants.SPACE,

                            changeBtn,
                          )
                        }
                      },
                    ),
                  )
                },

                // Отрендерить обобщённую информацию по под-узлам и поддержку добавления узла?
                ReactCommonUtil.maybe( p.node.info.isDetailed ) {
                  <.tr(
                    _kvTdKey(
                      crCtxP.message( MsgCodes.`Subnodes` ),
                    ),

                    _kvTdValue(
                      ReactCommonUtil.maybeNode( p.chCount > 0 ) {
                        <.span(
                          // Вывести общее кол-во под-узлов.
                          crCtxP.message( MsgCodes.`N.nodes`, p.chCount),

                          // Вывести кол-во выключенных под-узлов, если такие есть.
                          {
                            val countDisabled = p.chCount - p.chCountEnabled
                            ReactCommonUtil.maybeEl(countDisabled > 0) {
                              <.span(
                                HtmlConstants.COMMA,
                                HtmlConstants.NBSP_STR,
                                crCtxP.message( MsgCodes.`N.disabled`, countDisabled ),
                              )
                            }
                          },

                          // Рендерим поддержку добавления нового под-узла:
                          HtmlConstants.COMMA,
                          HtmlConstants.SPACE
                        )
                      },

                      // Форма добавления для текущего узла не существует. Рендерить кнопку добавления.
                      <.a(
                        ^.`class`  := Css.Lk.LINK,
                        ^.onClick --> onCreateNodeClick,
                        crCtxP.message( MsgCodes.`Create` ),
                        HtmlConstants.ELLIPSIS,
                      )
                    )
                  )
                },

              )   // TBODY
            )     // TABLE
          )

        } else ReactCommonUtil.maybeNode(isShowAdvProps) {
          // Рендрить вёрстку редактирования настроек размещения карточки.
          <.div(
            // Данные по узлу рендерим таблицей вида ключ-значение. Однако, возможна третья колонка с крутилкой.
            <.table(
              ^.`class` := Css.flat( Css.Table.TABLE, Css.Table.Width.XL, Css.Lk.Nodes.KvTable.LKN_TABLE ),

              <.tbody(

                // Галочка отображения постоянного раскрытия карточки.
                <.tr(
                  _kvTdKey(
                    crCtxP.message( MsgCodes.`Show.ad.opened` ),
                  ),
                  _kvTdValue {
                    val isShowOpened = p.node.adv
                      .map(_.isShowOpened)
                      .orElse( p.node.info.adv.map(_.advShowOpened) )
                      .getOrElseFalse
                    // Чек-бокс для управления isEnabled-флагом узла.
                    <.label(
                      ^.`class` := Css.flat( Css.Input.INPUT, Css.CLICKABLE, Css.Lk.Nodes.Inputs.INPUT70 ),
                      <.input(
                        ^.`type` := HtmlConstants.Input.checkbox,
                        // Текущее значение галочки происходит из нового значения и текущего значения, полученного на сервере.
                        ^.checked := isShowOpened,
                        ^.onChange ==> onShowOpenedClick,
                      ),
                      <.span(),
                      crCtxP.message( MsgCodes.yesNo( isShowOpened ) )
                    )
                  }
                ),

                <.tr(
                  _kvTdKey(
                    crCtxP.message( MsgCodes.`Always.outlined` ),
                  ),
                  _kvTdValue {
                    val isAlwaysOutlined = p.node.adv
                      .map(_.alwaysOutlined)
                      .orElse( p.node.info.adv.map(_.alwaysOutlined) )
                      .getOrElseFalse
                    // Чек-бокс для управления isEnabled-флагом узла.
                    <.label(
                      ^.`class` := Css.flat( Css.Input.INPUT, Css.CLICKABLE, Css.Lk.Nodes.Inputs.INPUT70 ),
                      <.input(
                        ^.`type` := HtmlConstants.Input.checkbox,
                        // Текущее значение галочки происходит из нового значения и текущего значения, полученного на сервере.
                        ^.checked := isAlwaysOutlined,
                        ^.onChange ==> onAlwaysOutlinedClick,
                      ),
                      <.span(),
                      crCtxP.message( MsgCodes.yesNo( isAlwaysOutlined ) ),
                    )
                  },
                ),

              )
            )
          )

        },

        // При ошибке запроса отрендерить тут что-то про ошибку.
        p.node.infoPot.renderFailed { ex =>
          <.span(
            ^.title   := ex.toString,
            ^.`class` := Css.Colors.RED,
            crCtxP.message( MsgCodes.`Error` ),
          )
        },

        // Если текущий узел раскрыт полностью, то нужен ещё один разделитель снизу, чтобы явно отделить контент текущего узла.
        _delim.when( isShowNodeEditProps ),

      )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .configure( ReactDiodeUtil.propsFastEqShouldComponentUpdate )
    .build

}
