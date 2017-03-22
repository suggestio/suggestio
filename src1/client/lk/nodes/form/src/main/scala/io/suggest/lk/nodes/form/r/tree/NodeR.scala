package io.suggest.lk.nodes.form.r.tree

import diode.ActionType
import diode.react.ModelProxy
import diode.react.ReactPot.potWithReact
import io.suggest.adv.rcvr.RcvrKey
import io.suggest.bill.MPrice
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.i18n.{I18nConst, MsgCodes}
import io.suggest.lk.nodes.MLknConf
import io.suggest.lk.nodes.form.m._
import io.suggest.lk.r.LkPreLoaderR
import io.suggest.sjs.common.i18n.Messages
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.common.msg.ErrorMsgs
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.03.17 18:28
  * Description: Рекурсивный react-компонент одной ноды дерева [[TreeR]].
  * Изначально разрастался прямо внутри [[TreeR]].
  */
object NodeR extends Log { self =>

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
                       mtree         : MTree,
                       node          : MNodeState,
                       parentRcvrKey : RcvrKey,
                       level         : Int,
                       proxy         : ModelProxy[_]
                     )


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
    private def onNodeEnabledChange(rcvrKey: RcvrKey)(e: ReactEventI): Callback = {
      _dispatchCB(
        NodeIsEnabledChanged(rcvrKey, isEnabled = e.target.checked)
      )
    }

    /** Callback клика по кнопке удаления узла. */
    private def onNodeDeleteClick(e: ReactEvent): Callback = {
      e.stopPropagationCB >> _dispatchCB( NodeDeleteClick )
    }

    /** Callback для кнопки редактирования узла. */
    private def onNodeEditClick(rcvrKey: RcvrKey)(e: ReactEventI): Callback = {
      e.stopPropagationCB >> _dispatchCB( NodeEditClick(rcvrKey) )
    }

    private def onNodeEditNameChange(rcvrKey: RcvrKey)(e: ReactEventI): Callback = {
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
    private def onAdvOnNodeChanged(rcvrKey: RcvrKey)(e: ReactEventI): Callback = {
      e.stopPropagationCB >> _dispatchCB(
        AdvOnNodeChanged(rcvrKey, isEnabled = e.target.checked)
      )
    }

    /** Callback запуска редактирования тарифа текущего узла. */
    private def onTfChangeClick: Callback = {
      _dispatchCB( TfDailyEditClick )
    }

    private def _dispatchCB[A](action: A)(implicit evidence: ActionType[A]): Callback = {
      $.props >>= { p =>
        p.proxy.dispatchCB( action )
      }
    }


    private def _msg_Cancel     = Messages( MsgCodes.`Cancel` )
    private def _msg_PleaseWait = Messages( MsgCodes.`Please.wait` )


    private def _smallWaitLoader  = LkPreLoaderR.AnimSmall
    private def _mediumLoader     = LkPreLoaderR.AnimMedium

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
    def render(p: Props): ReactElement = {
      import p._

      val rcvrKeyRev = node.info.id :: parentRcvrKey
      val rcvrKey = rcvrKeyRev.reverse

      val isShowProps = p.conf.adIdOpt.isEmpty && p.mtree.showProps.contains(rcvrKey)

      // Контейнер узла узла + дочерних узлов.
      <.div(
        ^.key := node.info.id,

        // Сдвиг слева согласно уровню, чтобы выглядело как дерево.
        ^.marginLeft := (level * 20).px,

        // Разделитель-промежуток от предыдущего элемента сверху.
        _delim,
        // Если на текущем узле отображаются пропертисы, то нужен толстый delimiter
        isShowProps ?= _delim,

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
          node.isNormal ?= {
            ^.onClick --> onNodeClick(rcvrKey)
          },

          node.editing.fold[ReactElement] {
            // Рендер названия узла. В зависимости от режима формы, могут быть варианты того, где он будет находиться.
            val nameSpan = <.span(
              ^.`class` := Css.Lk.Nodes.Name.TITLE,
              node.info.name
            )

            // контейнер названия текущего узла
            <.div(
              ^.`class` := Css.flat(Css.Font.Sz.L, Css.Lk.Nodes.Name.CONTENT),

              p.conf.adIdOpt.fold [ReactElement] (nameSpan) { _ =>
                // Рендерить галочку размещения текущей карточки на данном узле, если режим размещения активен сейчас.
                <.label(
                  ^.`class` := Css.flat( Css.Input.INPUT, Css.CLICKABLE ),
                  ^.onClick  ==> { e: ReactEventI => e.stopPropagationCB },
                  <.input(
                    ^.`type`    := "checkbox",
                    if (node.advIsPending) {
                      ^.disabled := true
                    } else {
                      ^.onChange ==> onAdvOnNodeChanged(rcvrKey)
                    },
                    ^.checked   := node.adv
                      .map(_.newIsEnabled)
                      .orElse(node.info.hasAdv)
                      .getOrElse {
                        LOG.log( ErrorMsgs.AD_ID_IS_EMPTY, msg = rcvrKey )
                        false
                      }
                  ),
                  <.span,
                  HtmlConstants.NBSP_STR,
                  nameSpan
                )
              },

              // Рендерить кнопку редактирования имени, если ситуация позволяет.
              (isShowProps && node.isNormal) ?= {
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

              for (advState <- node.adv) yield {
                <.span(
                  HtmlConstants.NBSP_STR,

                  // Если Pending, крутилку отобразить.
                  advState.req.renderPending { _ =>
                    _smallWaitLoader
                  },

                  // Если ошибка, то отобразить сообщение о проблеме.
                  advState.req.renderFailed { ex =>
                    <.span(
                      ^.title := ex.toString,
                      Messages( MsgCodes.`Error` )
                    )
                  }
                )
              },

              !node.advIsPending ?= {
                HtmlConstants.NBSP_STR
              },

              // Если инфа по узлу запрашивается с сервера, от отрендерить прелоадер
              node.children.renderPending { _ =>
                <.span(
                  HtmlConstants.NBSP_STR,
                  _smallWaitLoader
                )
              },

              // Кнопка удаления узла.
              (isShowProps && node.info.canChangeAvailability.contains(true)) ?= {
                <.span(
                  ^.`class`  := Css.Buttons.CLOSE,
                  ^.onClick ==> onNodeDeleteClick,
                  ^.title    := (Messages( MsgCodes.`Delete` ) + HtmlConstants.ELLIPSIS)
                )
              }

            )

          } { ed =>
            <.div(

              <.div(
                ^.`class` := Css.flat( Css.Lk.Nodes.Inputs.INPUT70, Css.Input.INPUT ),

                <.input(
                  ^.placeholder := node.info.name,
                  ^.title := Messages("Type.new.name.for.beacon.0", node.info.name) + HtmlConstants.SPACE + Messages( MsgCodes.`For.example.0`, Messages( MsgCodes.`Beacon.name.example` )),
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
        isShowProps ?= {
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
                    node.info.id
                  )
                ),

                // Галочка управления активностью узла, если определено.
                for (cca <- node.info.canChangeAvailability) yield {
                  val isEnabledValue = node.isEnabledUpd.fold(node.info.isEnabled)(_.newIsEnabled)
                  <.tr(
                    _kvTdKey(
                      Messages("Is.enabled")
                    ),

                    _kvTdValue(
                      // Чек-бокс для управления isEnabled-флагом узла.
                      <.label(
                        ^.`class` := Css.flat( Css.Input.INPUT, Css.CLICKABLE, Css.Lk.Nodes.Inputs.INPUT70 ),
                        <.input(
                          ^.`type` := "checkbox",
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
                        Messages( I18nConst.yesNo(isEnabledValue) )
                      ),

                      // Рендер данные по реквестов обновления флага isEnabled.
                      for (upd <- node.isEnabledUpd) yield {
                        <.span(
                          (upd.request.isPending || upd.request.isFailed) ?= {
                            HtmlConstants.NBSP_STR
                          },
                          // Крутилка ожидания, если происходит запрос к серверу за обновлением.
                          upd.request.renderPending { _ =>
                            _smallWaitLoader
                          },
                          // Рендер данных по ошибке запроса, если она имеет место.
                          upd.request.renderFailed { ex =>
                            <.span(
                              ^.`class` := Css.Colors.RED,
                              ^.title := ex.toString,
                              Messages("Error")
                            )
                          }
                        )
                      }
                    )

                  )
                }, // for

                // Рендер строки данных по тарифу размещения.
                for (tfInfo <- node.info.tf) yield {
                  val perDay = Messages( MsgCodes.`_per_.day` )
                  <.tr(
                    _kvTdKey(
                      Messages( MsgCodes.`Adv.tariff` )
                    ),

                    _kvTdValue(
                      Messages( tfInfo.mode.msgCode ),
                      HtmlConstants.COMMA,
                      HtmlConstants.SPACE,

                      <.a(
                        ^.`class` := Css.Lk.LINK,
                        ^.onClick --> onTfChangeClick,
                        Messages( MsgCodes.`Change` ),
                        HtmlConstants.ELLIPSIS
                      ),

                      // Описать текущий тариф:
                      for {
                        (mCalType, mPrice) <- tfInfo.clauses
                      } yield {
                        <.span(
                          ^.key := mCalType.strId,
                          <.br,
                          Messages( mCalType.name ),
                          " = ",
                          Messages(mPrice.currency.i18nPriceCode, MPrice.amountStr(mPrice)),
                          perDay,
                          HtmlConstants.COMMA
                        )
                      },

                      <.br,
                      Messages( MsgCodes.`Comission.0.pct.for.sio`, tfInfo.comissionPct )
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
                      children.nonEmpty ?= <.span(
                        // Вывести общее кол-во под-узлов.
                        Messages("N.nodes", children.size),

                        // Вывести кол-во выключенных под-узлов, если такие есть.
                        {
                          val countDisabled = children.count { n =>
                            !n.info.isEnabled
                          }
                          (countDisabled > 0) ?= {
                            <.span(
                              HtmlConstants.COMMA,
                              HtmlConstants.NBSP_STR,
                              Messages("N.disabled", countDisabled)
                            )
                          }
                        },

                        // Рендерим поддержку добавления нового под-узла:
                        HtmlConstants.COMMA,
                        HtmlConstants.SPACE
                      ),

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
        },

        // Рекурсивно отрендерить дочерние элементы:
        node.children.render { children =>
          <.div(
            children.nonEmpty ?= {
              val childLevel = level + 1
              for (subNode <- children) yield {
                val p1 = p.copy(
                  node          = subNode,
                  parentRcvrKey = rcvrKeyRev,
                  level         = childLevel
                )
                self( p1 )
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
        isShowProps ?= _delim

      )
    }

  }


  val component = ReactComponentB[Props]("Node")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(p: Props) = component(p)

}
