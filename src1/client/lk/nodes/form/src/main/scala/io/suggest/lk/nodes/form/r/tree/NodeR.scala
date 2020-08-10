package io.suggest.lk.nodes.form.r.tree

import com.materialui.{Mui, MuiButton, MuiButtonProps, MuiButtonVariants, MuiColorTypes, MuiLink, MuiLinkProps}
import diode.{ActionType, FastEq}
import diode.react.ModelProxy
import diode.react.ReactPot.potWithReact
import io.suggest.adv.rcvr.RcvrKey
import io.suggest.bill.MPrice
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.i18n.MsgCodes
import io.suggest.lk.nodes.MLknConf
import io.suggest.lk.nodes.form.m._
import io.suggest.lk.nodes.form.r.menu.{NodeMenuBtnR, NodeMenuR}
import io.suggest.lk.r.LkPreLoaderR
import io.suggest.msg.{JsFormatUtil, Messages}
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.log.Log
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._
import io.suggest.common.empty.OptionUtil.BoolOptOps
import io.suggest.n2.node.MNodeTypes
import io.suggest.routes.routes
import io.suggest.sc.ScConstants
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
             nodeMenuBtnR : NodeMenuBtnR
           )
  extends Log { self =>

  type Props = PropsVal

  /** Модель props текущего компонента.
    *
    * @param node Корневой узел этого под-дерева.
    * @param parentRcvrKey Ключ родительского узла [Nil].
    * @param level Уровень [0].
    * @param conf конфигурация.
    * @param proxy Proxy любой модели, нужен для диспатчинга в коллбэках.
    *              Живёт отдельно от данных из-за рекурсивной природы этого компонента.
    */
  case class PropsVal(
                       conf          : MLknConf,
                       // TODO mtree Всегда меняется. Нужно его выкинуть из node-состояния ИЛИ заменить на isShowProps:Boolean. С помощью scalaz.Tree и рефакторинга рендера. Иначе дерево формы рендерится целиком на каждый чих.
                       mtree         : MTree,
                       node          : MNodeState,
                       parentRcvrKey : RcvrKey,
                       level         : Int,
                       proxy         : ModelProxy[_]
                     )
  implicit object NodeRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      MTree.MTreeFastEq.eqv(a.mtree, b.mtree) &&
        (a.conf ===* b.conf) &&
        (a.node ===* b.node) &&
        (a.parentRcvrKey ===* b.parentRcvrKey) &&
        (a.level ==* b.level)
    }
  }


  class Backend($: BackendScope[Props, Unit]) {

    /** Callback клика по заголовку узла. */
    private def onNodeClick(rcvrKey: RcvrKey): Callback = {
      _dispatchCB( NodeNameClick(rcvrKey) )
    }

    /** Callback клика по кнопке добавления под-узла для текущего узла. */
    private def onCreateNodeClick: Callback = {
      _dispatchCB( CreateNodeClick )
    }

    /** Реакция на изменение значения флага активности узла. */
    private def onNodeEnabledChange(rcvrKey: RcvrKey)(e: ReactEventFromInput): Callback = {
      _dispatchCB(
        NodeIsEnabledChanged(rcvrKey, isEnabled = e.target.checked)
      )
    }

    /** Callback для кнопки редактирования узла. */
    private def onNodeEditClick(rcvrKey: RcvrKey)(e: ReactEventFromInput): Callback = {
      e.stopPropagationCB >> _dispatchCB( NodeEditClick(rcvrKey) )
    }

    private def onNodeEditNameChange(rcvrKey: RcvrKey)(e: ReactEventFromInput): Callback = {
      _dispatchCB(
        NodeEditNameChange(rcvrKey, name = e.target.value)
      )
    }

    /** Callback нажатия по кнопке сохранения отредактированного узла. */
    private def onNodeEditOkClick(rcvrKey: RcvrKey): Callback = {
      _dispatchCB( NodeEditOkClick(rcvrKey) )
    }

    /** Callback нажатия по кнопке отмены редактирования узла. */
    private def onNodeEditCancelClick(rcvrKey: RcvrKey): Callback = {
      _dispatchCB( NodeEditCancelClick(rcvrKey) )
    }

    /** Callback изменения галочки управления размещением текущей карточки на указанном узле. */
    private def onAdvOnNodeChanged(rcvrKey: RcvrKey)(e: ReactEventFromInput): Callback = {
      e.stopPropagationCB >> _dispatchCB(
        AdvOnNodeChanged(rcvrKey, isEnabled = e.target.checked)
      )
    }

    /** Callback запуска редактирования тарифа текущего узла. */
    private def onTfChangeClick: Callback = {
      _dispatchCB( TfDailyEditClick )
    }

    private def onTfShowDetailsClick(rcvrKey: RcvrKey): Callback = {
      _dispatchCB( TfDailyShowDetails(rcvrKey) )
    }

    /** Callback клика по галочке отображения по дефолту в раскрытом виде. */
    private def onShowOpenedClick(rcvrKey: RcvrKey)(e: ReactEventFromInput): Callback = {
      e.stopPropagationCB >> _dispatchCB(
        AdvShowOpenedChange(rcvrKey, e.target.checked)
      )
    }

    /** Callback клика по галочке отображения по дефолту в раскрытом виде. */
    private def onAlwaysOutlinedClick(rcvrKey: RcvrKey)(e: ReactEventFromInput): Callback = {
      e.stopPropagationCB >> _dispatchCB(
        AlwaysOutlinedSet(rcvrKey, e.target.checked)
      )
    }


    private def _dispatchCB[A](action: A)(implicit evidence: ActionType[A]): Callback = {
      $.props >>= { p =>
        p.proxy.dispatchCB( action )
      }
    }


    private def _msg_Cancel     = Messages( MsgCodes.`Cancel` )
    private def _msg_PleaseWait = Messages( MsgCodes.`Please.wait` )


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
      import p._

      val rcvrKeyRev = node.info.id :: parentRcvrKey
      val rcvrKey = rcvrKeyRev.reverse

      val isShowProps = p.mtree.showProps.contains(rcvrKey)
      val isShowNodeEditProps = isShowProps && p.conf.adIdOpt.isEmpty
      val isShowAdvProps = isShowProps && p.conf.adIdOpt.isDefined

      // Контейнер узла узла + дочерних узлов.
      <.div(
        ^.key := node.info.id,

        // Сдвиг слева согласно уровню, чтобы выглядело как дерево.
        ^.marginLeft := (level * 20).px,

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
            Css.Lk.Nodes.Name.NORMAL   -> node.info.isEnabled,
            Css.Lk.Nodes.Name.DISABLED -> !node.info.isEnabled,
            Css.Lk.Nodes.Name.SHOWING  -> node.editing.isEmpty,
            Css.Lk.Nodes.Name.EDITING  -> node.editing.nonEmpty,

            // Закруглять углы только когда узел не раскрыт.
            Css.Table.Td.Radial.FIRST -> !isShowProps
          ),
          // Во время неРедактирования можно сворачивать-разворачивать блок, кликая по нему.
          ReactCommonUtil.maybe(node.isNormal) {
            ^.onClick --> onNodeClick(rcvrKey)
          },

          node.editing.fold[VdomElement] {
            // Рендер названия узла. В зависимости от режима формы, могут быть варианты того, где он будет находиться.
            val nameSpan = <.span(
              ^.`class` := Css.Lk.Nodes.Name.TITLE,
              node.info.name
            )

            // контейнер названия текущего узла
            <.div(
              ^.`class` := Css.flat(Css.Font.Sz.L, Css.Lk.Nodes.Name.CONTENT),

              p.conf.adIdOpt.fold [VdomElement] (nameSpan) { _ =>
                // Рендерить галочку размещения текущей карточки на данном узле, если режим размещения активен сейчас.
                <.label(
                  ^.`class` := Css.flat( Css.Input.INPUT, Css.CLICKABLE ),
                  ^.onClick  ==> { e: ReactEventFromInput => e.stopPropagationCB },
                  <.input(
                    ^.`type` := HtmlConstants.Input.checkbox,
                    if (node.advIsPending) {
                      ^.disabled := true
                    } else {
                      ^.onChange ==> onAdvOnNodeChanged(rcvrKey)
                    },
                    ^.checked   := node.adv
                      .map(_.newIsEnabled)
                      .orElse(node.info.hasAdv)
                      .getOrElseFalse
                  ),
                  <.span,
                  HtmlConstants.NBSP_STR,
                  nameSpan
                )
              },

              // Рендерить кнопку редактирования имени, если ситуация позволяет.
              ReactCommonUtil.maybeEl( isShowNodeEditProps && node.isNormal ) {
                <.span(
                  HtmlConstants.NBSP_STR,
                  HtmlConstants.NBSP_STR,
                  <.span(
                    ^.`class` := Css.Lk.Nodes.Name.EDIT_BTN,
                    ^.onClick ==> onNodeEditClick(rcvrKey),
                    ^.title   := Messages( MsgCodes.`Change` )
                  )
                )
              },

              node.adv.whenDefined { advState =>
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
                      Messages( MsgCodes.`Error` )
                    )
                  }
                )
              },

              HtmlConstants.NBSP_STR
                .unless( node.advIsPending ),

              // Если инфа по узлу запрашивается с сервера, от отрендерить прелоадер
              node.children.renderPending { _ =>
                <.span(
                  HtmlConstants.NBSP_STR,
                  _smallWaitLoader
                )
              },

              // Кнопка удаления узла.
              ReactCommonUtil.maybeEl( isShowNodeEditProps && node.info.canChangeAvailability.contains(true) ) {
                <.div(
                  ^.`class` := Css.Lk.Nodes.Menu.MENU,
                  ^.onClick ==> ReactCommonUtil.stopPropagationCB,
                  nodeMenuBtnR( p.proxy ),
                  nodeMenuR( p.proxy )
                )
              }

            )

          } { ed =>
            <.div(

              <.div(
                ^.`class` := Css.flat( Css.Lk.Nodes.Inputs.INPUT70, Css.Input.INPUT ),

                <.input(
                  ^.placeholder := node.info.name,
                  ^.title := Messages( MsgCodes.`Type.new.name.for.beacon.0`, node.info.name) + HtmlConstants.SPACE + Messages( MsgCodes.`For.example.0`, Messages( MsgCodes.`Beacon.name.example` )),
                  ^.value := ed.name,
                  // Блокировать поле, пока происходит сохранение на сервер.
                  if (ed.saving.isPending) {
                    ^.disabled := true
                  } else {
                    ^.onChange ==> onNodeEditNameChange(rcvrKey)
                  }
                )
              ),

              // Происходит редактирование узла. Отобразить кнопки сохранения.
              <.div(
                ^.`class` := Css.Lk.Nodes.Name.EDITING_BTNS,

                if (ed.saving.isPending) {
                  // Идёт сохранение на сервер прямо сейчас. Отрендерить сообщение о необходимости подождать.
                  <.div(
                    ^.title := Messages( MsgCodes.`Server.request.in.progress.wait` ),
                    _msg_PleaseWait,
                    _smallWaitLoader
                  )

                } else {
                  <.div(
                    // Кнопка сохранения изменений.
                    <.a(
                      ^.`class` := Css.flat(Css.Buttons.BTN, Css.Buttons.MAJOR, Css.Size.M),
                      ^.onClick --> onNodeEditOkClick(rcvrKey),
                      Messages( MsgCodes.`Save` )
                    ),
                    HtmlConstants.SPACE,
                    // Кнопка отмены редактирования.
                    <.a(
                      ^.`class` := Css.flat(Css.Buttons.BTN, Css.Buttons.NEGATIVE, Css.Size.M),
                      ^.onClick --> onNodeEditCancelClick(rcvrKey),
                      _msg_Cancel
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
                    Messages( MsgCodes.`Identifier` )
                  ),
                  _kvTdValue(
                    node.info.id,

                    HtmlConstants.SPACE,

                    // Кнопка перехода в ЛК узла
                    ReactCommonUtil.maybeNode( node.info.ntype ==* MNodeTypes.AdnNode ) {
                      MuiLink(
                        new MuiLinkProps {
                          val href = routes.controllers.LkAds.adsPage( node.info.id ).url
                        }
                      )(
                        MuiButton(
                          new MuiButtonProps {
                            override val variant = MuiButtonVariants.outlined
                            override val startIcon = Mui.SvgIcons.ArrowForward()().rawNode
                            override val color = MuiColorTypes.primary
                          }
                        )(
                          Messages( MsgCodes.`Go.into` ),
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
                                  nodeId = Some( node.info.id ),
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
                        Messages( MsgCodes.`Showcase` ),
                      )
                    ),
                  ),
                ),

                // Галочка управления активностью узла, если определено.
                node.info.canChangeAvailability.whenDefined { cca =>
                  val isEnabledValue = node.isEnabledUpd.fold(node.info.isEnabled)(_.newIsEnabled)
                  <.tr(
                    _kvTdKey(
                      Messages( MsgCodes.`Is.enabled` )
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
                          if (cca && node.isEnabledUpd.isEmpty) {
                            ^.onChange ==> onNodeEnabledChange(rcvrKey)
                          } else {
                            ^.disabled := true
                          }
                        ),
                        <.span(),
                        Messages( MsgCodes.yesNo(isEnabledValue) )
                      ),

                      // Рендер данные по реквестов обновления флага isEnabled.
                      node.isEnabledUpd.whenDefined { upd =>
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
                              Messages( MsgCodes.`Error` )
                            )
                          }
                        )
                      }
                    )

                  )
                }, // for

                // Рендер строки данных по тарифу размещения.
                node.info.tf.whenDefined { tfInfo =>
                  val perDay = Messages( MsgCodes.`_per_.day` )

                  val changeBtn = <.a(
                    ^.`class`  := Css.Lk.LINK,
                    ^.onClick --> onTfChangeClick,
                    Messages( MsgCodes.`Change` ),
                    HtmlConstants.ELLIPSIS
                  )

                  <.tr(
                    _kvTdKey(
                      Messages( MsgCodes.`Adv.tariff` )
                    ),

                    _kvTdValue(
                      Messages( tfInfo.mode.msgCode ),

                      // Есть два отображения для тарифа: компактное одной строчкой и развёрнутое
                      if (node.tfInfoWide) {
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
                                    Messages( mCalType.i18nCode )
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
                          Messages( MsgCodes.`Comission.0.pct.for.sio`, tfInfo.comissionPct )
                        )
                      } else {
                        // Рендер компактной инфы по тарифу.
                        <.span(
                          HtmlConstants.COLON,
                          HtmlConstants.SPACE,
                          tfInfo.clauses.toVdomArray { case (mCalType, mPrice) =>
                            <.span(
                              ^.key := mCalType.value,
                              (HtmlConstants.SPACE + HtmlConstants.SLASH + HtmlConstants.SPACE)
                                .unless( mCalType == tfInfo.clauses.head._1 ),
                              ^.title := Messages( mCalType.i18nCode ),
                              MPrice.amountStr(mPrice)
                            )
                          },
                          <.span(
                            ^.title := Messages( tfInfo.currency.currencyNameI18n ),
                            Messages(tfInfo.currency.i18nPriceCode, "")
                          ),
                          perDay,
                          // Кнопка показа подробностей по тарифу.
                          <.a(
                            ^.`class`  := Css.Lk.LINK,
                            ^.title    := Messages( MsgCodes.`Show.details` ),
                            ^.onClick --> onTfShowDetailsClick(rcvrKey),
                            HtmlConstants.ELLIPSIS
                          ),
                          HtmlConstants.COMMA,
                          HtmlConstants.SPACE,

                          changeBtn
                        )
                      }
                    )
                  )
                },

                // Отрендерить обобщённую информацию по под-узлам и поддержку добавления узла.
                node.children.render { children =>

                  <.tr(
                    _kvTdKey(
                      Messages( MsgCodes.`Subnodes` )
                    ),

                    _kvTdValue(
                      ReactCommonUtil.maybeNode(children.nonEmpty) {
                        <.span(
                          // Вывести общее кол-во под-узлов.
                          Messages( MsgCodes.`N.nodes`, children.size),

                          // Вывести кол-во выключенных под-узлов, если такие есть.
                          {
                            val countDisabled = children.count { n =>
                              !n.info.isEnabled
                            }
                            ReactCommonUtil.maybeEl(countDisabled > 0) {
                              <.span(
                                HtmlConstants.COMMA,
                                HtmlConstants.NBSP_STR,
                                Messages( MsgCodes.`N.disabled`, countDisabled )
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
                        Messages( MsgCodes.`Create` ), HtmlConstants.ELLIPSIS
                      )
                    )
                  )
                }

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
                    Messages( MsgCodes.`Show.ad.opened` )
                  ),
                  _kvTdValue {
                    val isShowOpened = node.adv
                      .map(_.isShowOpened)
                      .orElse(node.info.advShowOpened)
                      .getOrElseFalse
                    // Чек-бокс для управления isEnabled-флагом узла.
                    <.label(
                      ^.`class` := Css.flat( Css.Input.INPUT, Css.CLICKABLE, Css.Lk.Nodes.Inputs.INPUT70 ),
                      <.input(
                        ^.`type` := HtmlConstants.Input.checkbox,
                        // Текущее значение галочки происходит из нового значения и текущего значения, полученного на сервере.
                        ^.checked := isShowOpened,
                        ^.onChange ==> onShowOpenedClick(rcvrKey)
                      ),
                      <.span(),
                      Messages( MsgCodes.yesNo( isShowOpened ) )
                    )
                  }
                ),

                <.tr(
                  _kvTdKey(
                    Messages( MsgCodes.`Always.outlined` )
                  ),
                  _kvTdValue {
                    val isAlwaysOutlined = node.adv
                      .map(_.alwaysOutlined)
                      .orElse(node.info.alwaysOutlined)
                      .getOrElseFalse
                    // Чек-бокс для управления isEnabled-флагом узла.
                    <.label(
                      ^.`class` := Css.flat( Css.Input.INPUT, Css.CLICKABLE, Css.Lk.Nodes.Inputs.INPUT70 ),
                      <.input(
                        ^.`type` := HtmlConstants.Input.checkbox,
                        // Текущее значение галочки происходит из нового значения и текущего значения, полученного на сервере.
                        ^.checked := isAlwaysOutlined,
                        ^.onChange ==> onAlwaysOutlinedClick(rcvrKey)
                      ),
                      <.span(),
                      Messages( MsgCodes.yesNo( isAlwaysOutlined ) )
                    )
                  },
                ),

              )
            )
          )

        },

        // Рекурсивно отрендерить дочерние элементы:
        node.children.render { children =>
          <.div(
            ReactCommonUtil.maybeNode(children.nonEmpty) {
              val childLevel = level + 1
              children.toVdomArray { subNode =>
                val p1 = p.copy(
                  node          = subNode,
                  parentRcvrKey = rcvrKeyRev,
                  level         = childLevel
                )
                component.withKey(subNode.info.id)( p1 )
              }
            }
          )
        },

        // При ошибке запроса отрендерить тут что-то про ошибку.
        node.children.renderFailed { ex =>
          <.span(
            ^.title   := ex.toString,
            ^.`class` := Css.Colors.RED,
            Messages( MsgCodes.`Error` )
          )
        },

        // Если текущий узел раскрыт полностью, то нужен ещё один разделитель снизу, чтобы явно отделить контент текущего узла.
        _delim.when( isShowNodeEditProps )

      )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .configure( ReactDiodeUtil.propsFastEqShouldComponentUpdate )
    .build

  def apply(p: Props) = component(p)

}
