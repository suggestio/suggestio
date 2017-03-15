package io.suggest.lk.nodes.form.r.tree

import diode.ActionType
import diode.react.ModelProxy
import diode.react.ReactPot.potWithReact
import io.suggest.adv.rcvr.RcvrKey
import io.suggest.common.html.HtmlConstants
import io.suggest.common.radio.BleConstants.Beacon.EddyStone
import io.suggest.css.Css
import io.suggest.i18n.I18nConstants
import io.suggest.lk.nodes.form.m._
import io.suggest.sjs.common.i18n.Messages
import io.suggest.sjs.common.vm.spa.LkPreLoader
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.03.17 18:28
  * Description: Рекурсивный react-компонент одной ноды дерева [[TreeR]].
  * Изначально разрастался прямо внутри [[TreeR]].
  */
object NodeR { self =>

  type Props = PropsVal

  /** Модель props текущего компонента.
    *
    * @param node Корневой узел этого под-дерева.
    * @param parentRcvrKey Ключ родительского узла [Nil].
    * @param level Уровень [0].
    * @param conf конфигурация.
    */
  case class PropsVal(
                       conf          : MLknOther,
                       mtree         : MTree,
                       node          : MNodeState,
                       parentRcvrKey : RcvrKey,
                       level         : Int,
                       proxy         : ModelProxy[_]
                     )


  class Backend($: BackendScope[Props, Unit]) {

    /** Callback клика по заголовку узла. */
    def onNodeClick(rcvrKey: RcvrKey): Callback = {
      _dispatchCB( NodeNameClick(rcvrKey) )
    }

    /** Callback клика по кнопке добавления под-узла. */
    def onAddSubNodeClick(parentKey: RcvrKey): Callback = {
      _dispatchCB( AddSubNodeClick(parentKey) )
    }


    /** Callback для ввода названия добавляемого под-узла. */
    def onAddSubNodeNameChange(parentKey: RcvrKey)(e: ReactEventI): Callback = {
      _dispatchCB(
        AddSubNodeNameChange(parentKey, name = e.target.value)
      )
    }

    /** Callback редактирования id создаваемого узла. */
    def onAddSubNodeIdChange(parentKey: RcvrKey)(e: ReactEventI): Callback = {
      _dispatchCB(
        AddSubNodeIdChange(parentKey, id = e.target.value)
      )
    }

    /** Callback нажатия на кнопку "сохранить" при добавлении нового узла. */
    def onAddSubNodeSaveClick(parentKey: RcvrKey): Callback = {
      _dispatchCB( AddSubNodeSaveClick(parentKey) )
    }

    def onAddSubNodeCancelClick(parentKey: RcvrKey): Callback = {
      _dispatchCB( AddSubNodeCancelClick(parentKey) )
    }

    /** Реакция на изменение значения флага активности узла. */
    def onNodeEnabledChange(rcvrKey: RcvrKey)(e: ReactEventI): Callback = {
      _dispatchCB(
        NodeIsEnabledChanged(rcvrKey, isEnabled = e.target.checked)
      )
    }


    /** Callback для кнопки редактирования узла. */
    def onNodeEditClick(rcvrKey: RcvrKey)(e: ReactEventI): Callback = {
      e.stopPropagationCB >>= { _ =>
        _dispatchCB( NodeEditClick(rcvrKey) )
      }
    }

    def onNodeEditNameChange(rcvrKey: RcvrKey)(e: ReactEventI): Callback = {
      _dispatchCB(
        NodeEditNameChange(rcvrKey, name = e.target.value)
      )
    }

    /** Callback нажатия по кнопке сохранения отредактированного узла. */
    def onNodeEditOkClick(rcvrKey: RcvrKey): Callback = {
      _dispatchCB( NodeEditOkClick(rcvrKey) )
    }

    /** Callback нажатия по кнопке отмены редактирования узла. */
    def onNodeEditCancelClick(rcvrKey: RcvrKey): Callback = {
      _dispatchCB( NodeEditCancelClick(rcvrKey) )
    }


    /** Callback клика по кнопке удаления узла. */
    def onNodeDeleteClick(rcvrKey: RcvrKey): Callback = {
      _dispatchCB( NodeDeleteClick(rcvrKey) )
    }

    /** Callback клика по кнопке ПОДТВЕРЖДЕНИЯ удаления узла. */
    def onNodeDeleteOkClick(rcvrKey: RcvrKey): Callback = {
      _dispatchCB( NodeDeleteOkClick(rcvrKey) )
    }

    /** Callback нажатия кнопки ОТМЕНЫ удаления узла. */
    def onNodeDeleteCancelClick(rcvrKey: RcvrKey): Callback = {
      _dispatchCB( NodeDeleteCancelClick(rcvrKey) )
    }

    /** Callback изменения галочки управления размещением текущей карточки на указанном узле. */
    def onAdvOnNodeChanged(rcvrKey: RcvrKey)(e: ReactEventI): Callback = {
      _dispatchCB(
        AdvOnNodeChanged(rcvrKey, isEnabled = e.target.checked)
      )
    }

    private def _dispatchCB[A](action: A)(implicit evidence: ActionType[A]): Callback = {
      $.props >>= { p =>
        p.proxy.dispatchCB( action )
      }
    }


    private lazy val _msg_NodeId = Messages("Identifier")
    private lazy val _msg_BeaconNameExample = Messages("Beacon.name.example")
    private lazy val _msg_ServerReqInProgressWait = Messages("Server.request.in.progress.wait")
    private lazy val _msg_Save = Messages("Save")
    private lazy val _msg_Cancel = Messages("Cancel")

    private val pleaseWait = Messages("Please.wait")
    private lazy val _textPreLoader: ReactElement = {
      <.span(
        pleaseWait,
        HtmlConstants.ELLIPSIS
      )
    }

    private def _waitLoader(widthPx: Int): ReactElement = {
      <.span(
        // Чтобы alt был маленькими буквами, если картинка не подгрузилась
        ^.`class` := Css.Font.Sz.S,
        LkPreLoader.PRELOADER_IMG_URL.fold(_textPreLoader) { url =>
          <.img(
            ^.src   := url,
            ^.alt   := pleaseWait,
            ^.width := widthPx.px
          )
        }
      )
    }

    private lazy val _smallWaitLoader = _waitLoader(16)
    private lazy val _mediumLoader = _waitLoader(22)

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


    /** Рендер кнопки, либо формы добавления нового узла (маячка). */
    def _renderAddUtil(rcvrKey: RcvrKey, node: MNodeState): TagMod = {
      // Рендерить кнопку добавления нового узла.
      node.addSubNodeState.fold[TagMod] {
        // Форма добавления для текущего узла не существует. Рендерить кнопку добавления.
        <.a(
          ^.`class` := Css.Lk.LINK, // flat(Css.Buttons.BTN, Css.Size.M, Css.Buttons.MINOR),
          ^.onClick --> onAddSubNodeClick(rcvrKey),
          Messages("Create"), HtmlConstants.ELLIPSIS
        )

      } { addState =>
        val isSaving = addState.saving.isPending
        val disabledAttr = isSaving ?= {
          ^.disabled := true
        }

        // Сейчас открыта форма добавление под-узла для текущего узла.
        <.div(
          isSaving ?= {
            ^.title := _msg_ServerReqInProgressWait
          },

          // Поле ввода названия маячка.
          <.div(
            ^.`class` := Css.Input.INPUT,

            <.label(
              Messages("Name"), ":",
              <.input(
                ^.`type`      := "text",
                ^.value       := addState.name,
                ^.onChange   ==> onAddSubNodeNameChange(rcvrKey),
                ^.placeholder := _msg_BeaconNameExample,
                disabledAttr
              )
            )
          ),

          // Поля для ввода id маячка.
          <.div(
            ^.`class` := Css.Input.INPUT,
            <.label(
              _msg_NodeId,
              " (EddyStone-UID)",
              <.input(
                ^.`type`      := "text",
                ^.value       := addState.id.getOrElse(""),
                ^.onChange   ==> onAddSubNodeIdChange(rcvrKey),
                ^.placeholder := EddyStone.EXAMPLE_UID,
                !isSaving ?= {
                  ^.title := Messages("Example.id.0", EddyStone.EXAMPLE_UID)
                },
                disabledAttr
              )
            )
          ),

          // Кнопки сохранения/отмены.
          <.div(
            // Кнопка сохранения. Активна только когда юзером введено достаточно данных.
            addState.saving.renderEmpty {
              val isSaveBtnEnabled = addState.isValid
              <.span(
                <.a(
                  ^.classSet1(
                    Css.flat(Css.Buttons.BTN, Css.Size.M),
                    Css.Buttons.MAJOR     -> isSaveBtnEnabled,
                    Css.Buttons.DISABLED  -> !isSaveBtnEnabled
                  ),
                  isSaveBtnEnabled ?= {
                    ^.onClick --> onAddSubNodeSaveClick(rcvrKey)
                  },
                  _msg_Save
                ),
                HtmlConstants.SPACE,

                // Кнопка отмены.
                <.a(
                  ^.`class` := Css.flat(Css.Buttons.BTN, Css.Size.M, Css.Buttons.NEGATIVE),
                  ^.onClick --> onAddSubNodeCancelClick(rcvrKey),
                  _msg_Cancel
                )
              )
            },

            // Крутилка ожидания сохранения.
            isSaving ?= _mediumLoader,

            // Вывести инфу, что что-то пошло не так при ошибке сохранения.
            addState.saving.renderFailed { ex =>
              <.span(
                ^.title := ex.toString,
                Messages("Something.gone.wrong")
              )
            }

          )

        ) // div()
      } // addState =>
    }


    /**
      * Рекурсивный рендер под-дерева узлов.
      *
      * @param p Пропертисы.
      * @return React-элемент.
      */
    def render(p: Props) /*  node: MNodeState, adIdOpt: Option[String], parentRcvrKey: RcvrKey, level: Int)*/: ReactElement = {
      import p._

      val rcvrKeyRev = node.info.id :: parentRcvrKey
      val rcvrKey = rcvrKeyRev.reverse

      val isShowProps = p.mtree.showProps.contains(rcvrKey)

      // Контейнер узла узла + дочерних узлов.
      <.div(
        ^.key := node.info.id,

        // Сдвиг слева согласно уровню, чтобы выглядело как дерево.
        ^.marginLeft := (level * 20).px,

        // Разделитель-промежуток от предыдущего элемента сверху.
        _delim,

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
          node.editing.isEmpty ?= {
            ^.onClick --> onNodeClick(rcvrKey)
          },

          node.editing.fold[ReactElement] {
            // контейнер названия текущего узла
            <.div(
              ^.`class` := Css.flat(Css.Font.Sz.L, Css.Lk.Nodes.Name.CONTENT),

              // Рендерить галочку размещения текущей карточки на данном узле, если режим размещения активен сейчас.
              for (_ <- p.conf.adIdOpt) yield {
                <.input(
                  ^.`type`    := "checkbox",
                  ^.onChange ==> onAdvOnNodeChanged(rcvrKey)
                )
              },

              // Рендер названия узла.
              <.span(
                ^.`class` := Css.Lk.Nodes.Name.TITLE,
                node.info.name
              ),

              // Рендерить кнопку редактирования имени.
              isShowProps ?= {
                <.span(
                  HtmlConstants.NBSP_STR,
                  HtmlConstants.NBSP_STR,
                  <.span(
                    ^.`class` := Css.Lk.Nodes.Name.EDIT_BTN,
                    ^.onClick ==> onNodeEditClick(rcvrKey),
                    ^.title   := Messages("Change")
                  )
                )
              },

              // Если инфа по узлу запрашивается с сервера, от отрендерить прелоадер
              node.children.renderPending { _ =>
                <.span(
                  HtmlConstants.NBSP_STR,
                  _smallWaitLoader
                )
              }
            )

          } { ed =>
            <.div(

              <.div(
                ^.`class` := Css.flat(Css.Input.INPUT),

                <.input(
                  ^.placeholder := node.info.name,
                  ^.title := Messages("Type.new.name.for.beacon.0", node.info.name) + HtmlConstants.SPACE + Messages("For.example.0", _msg_BeaconNameExample),
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
                    ^.title := _msg_ServerReqInProgressWait,
                    pleaseWait,
                    _smallWaitLoader
                  )

                } else {
                  <.div(
                    // Кнопка сохранения изменений.
                    <.a(
                      ^.`class` := Css.flat(Css.Buttons.BTN, Css.Buttons.MAJOR, Css.Size.M),
                      ^.onClick --> onNodeEditOkClick(rcvrKey),
                      _msg_Save
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
        p.mtree.showProps.contains(rcvrKey) ?= {
          <.div(
            // Данные по узлу рендерим таблицей вида ключ-значение. Однако, возможна третья колонка с крутилкой.
            <.table(
              ^.`class` := Css.flat( Css.Table.TABLE, Css.Table.Width.XL, Css.Lk.Nodes.KvTable.LKN_TABLE ),

              <.tbody(

                // id узла.
                <.tr(
                  _kvTdKey(
                    _msg_NodeId
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
                        ^.`class` := Css.flat( Css.Input.INPUT, Css.CLICKABLE ),
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
                        Messages( I18nConstants.yesNo(isEnabledValue) )
                      ),

                      // Рендер данные по реквестов обновления флага isEnabled.
                      for (upd <- node.isEnabledUpd) yield {
                        <.span(
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


                // Рендерить поддержку удаления узла.
                <.tr(
                  _kvTdKey(
                    Messages("Deletion")
                  ),

                  _kvTdValue(
                    node.deleting.fold {
                      // Нормальный режим, удаления узла не происходит. Рендерить обычные кнопки управления.
                      <.div(
                        // Кнопка удаления узла.
                        node.info.canChangeAvailability.contains(true) ?= {
                          <.a(
                            ^.`class`  := Css.flat(Css.Colors.RED, Css.Lk.LINK),
                            ^.onClick --> onNodeDeleteClick(rcvrKey),
                            Messages("Delete"),
                            HtmlConstants.ELLIPSIS
                          )
                        }
                      )

                    } { delPot =>
                      // Происходит удаление узла или подготовка к этому.
                      <.div(
                        // Рендерить форму, когда Pot пуст.
                        delPot.renderEmpty {
                          <.div(
                            Messages("Are.you.sure"),
                            HtmlConstants.SPACE,

                            // Кнопки поменяны местами для защиты от двойных нажатий.
                            // Кнопка отмены удаления:
                            <.a(
                              ^.`class`  := Css.flat(Css.Buttons.BTN, Css.Buttons.MINOR, Css.Size.M),
                              ^.onClick --> onNodeDeleteCancelClick(rcvrKey),
                              _msg_Cancel
                            ),

                            HtmlConstants.SPACE,

                            // Кнопка подтверждения удаления, красная.
                            <.a(
                              ^.`class`  := Css.flat(Css.Buttons.BTN, Css.Buttons.NEGATIVE, Css.Size.M),
                              ^.onClick --> onNodeDeleteOkClick(rcvrKey),
                              Messages("Yes.delete.it")
                            )
                          )
                        },

                        // Когда идёт запрос к серверу, рендерить ожидание
                        delPot.renderPending { _ =>
                          <.div(
                            _mediumLoader,
                            pleaseWait
                          )
                        },

                        // Ошибку удаления можно выводить на экран.
                        delPot.renderFailed { ex =>
                          <.div(
                            <.span(
                              ^.`class` := Css.Colors.RED,
                              ^.title   := ex.toString,
                              Messages("Error")
                            ),

                            // Кнопка закрытия ошибочной формы.
                            <.a(
                              ^.`class`  := Css.flat(Css.Buttons.BTN, Css.Buttons.MINOR, Css.Size.M),
                              ^.onClick --> onNodeDeleteCancelClick(rcvrKey),
                              Messages("Close")
                            )
                          )
                        }

                      )
                    }
                  )
                ),


                // Отрендерить обобщённую информацию по под-узлам и поддержку добавления узла.
                node.children.render { children =>

                  <.tr(
                    _kvTdKey(
                      Messages("Subnodes")
                    ),

                    _kvTdValue(
                      (node.addSubNodeState.isEmpty && children.nonEmpty) ?= <.span(
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
                      _renderAddUtil(rcvrKey, node)
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
            Messages("Error")
          )
        }

      )
    }

  }


  val component = ReactComponentB[Props]("Node")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(p: Props) = component(p)

}