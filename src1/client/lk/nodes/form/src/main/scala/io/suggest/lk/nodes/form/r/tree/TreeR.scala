package io.suggest.lk.nodes.form.r.tree

import diode.ActionType
import diode.react.ModelProxy
import diode.react.ReactPot._
import io.suggest.adv.rcvr.RcvrKey
import io.suggest.common.html.HtmlConstants
import io.suggest.common.radio.BleConstants.Beacon.EddyStone.EXAMPLE_UID
import io.suggest.css.Css
import io.suggest.lk.nodes.form.m._
import io.suggest.sjs.common.i18n.Messages
import io.suggest.sjs.common.vm.spa.LkPreLoader
import japgolly.scalajs.react.{BackendScope, Callback, ReactComponentB, ReactElement, ReactEventI}
import japgolly.scalajs.react.vdom.prefix_<^._
import io.suggest.common.html.HtmlConstants.SPACE

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.02.17 18:19
  * Description: React-компонент дерева узлов.
  */
object TreeR {

  type Props = ModelProxy[MTree]


  /** Ядро react-компонента дерева узлов. */
  class Backend($: BackendScope[Props, _]) {

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
      val name2 = e.target.value
      _dispatchCB( AddSubNodeNameChange(parentKey, name2) )
    }

    /** Callback редактирования id создаваемого узла. */
    def onAddSubNodeIdChange(parentKey: RcvrKey)(e: ReactEventI): Callback = {
      val name2 = e.target.value
      _dispatchCB( AddSubNodeIdChange(parentKey, name2) )
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
      val isEnabled2 = e.target.checked
      _dispatchCB( NodeIsEnabledChanged(rcvrKey, isEnabled2) )
    }


    /** Callback для кнопки редактирования узла. */
    def onNodeEditClick(rcvrKey: RcvrKey): Callback = {
      _dispatchCB( NodeEditClick(rcvrKey) )
    }

    def onNodeEditNameChange(rcvrKey: RcvrKey)(e: ReactEventI): Callback = {
      _dispatchCB( NodeEditNameChange(rcvrKey, e.target.value) )
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


    private def _dispatchCB[A](action: A)(implicit evidence: ActionType[A]): Callback = {
      $.props >>= { p =>
        p.dispatchCB( action )
      }
    }


    private lazy val _msg_BeaconId = Messages("Beacon.id")
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
      LkPreLoader.PRELOADER_IMG_URL
        .fold(_textPreLoader) { url =>
          <.img(
            ^.src   := url,
            ^.alt   := pleaseWait,
            ^.width := widthPx.px
          )
        }
    }

    private lazy val _smallWaitLoader = _waitLoader(16)
    private lazy val _mediumLoader = _waitLoader(22)


    /** Рендер кнопки, либо формы добавления нового узла (маячка). */
    def _renderAddUtil(rcvrKey: RcvrKey, node: MNodeState): TagMod = {
      // Рендерить кнопку добавления нового узла.
      node.addSubNodeState.fold[TagMod] {
        // Форма добавления для текущего узла не существует. Рендерить кнопку добавления.
        <.a(
          ^.`class` := (Css.Buttons.BTN :: Css.Size.M :: Css.Buttons.MINOR :: Nil)
            .mkString( SPACE ),
          ^.onClick --> onAddSubNodeClick(rcvrKey),
          Messages("Add"), HtmlConstants.ELLIPSIS
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
          <.label(
            Messages("Name"), ":",
            <.input(
              ^.`type`      := "text",
              ^.value       := addState.name,
              ^.onChange   ==> onAddSubNodeNameChange(rcvrKey),
              ^.placeholder := _msg_BeaconNameExample,
              disabledAttr
            )
          ),

          // Поля для ввода id маячка.
          <.label(
            _msg_BeaconId,
            " (EddyStone-UID)",
            <.input(
              ^.`type`      := "text",
              ^.value       := addState.id.getOrElse(""),
              ^.onChange   ==> onAddSubNodeIdChange(rcvrKey),
              ^.placeholder := EXAMPLE_UID,
              !isSaving ?= {
                ^.title := Messages("Example.id.0", EXAMPLE_UID)
              },
              disabledAttr
            )
          ),

          // Кнопка сохранения. Активна только когда юзером введено достаточно данных.
          <.a(
            {
              val isEnabled = addState.isValid && !isSaving
              ^.classSet1(
                (Css.Buttons.BTN :: Css.Size.M :: Nil).mkString(SPACE),
                Css.Buttons.MAJOR     -> isEnabled,
                Css.Buttons.DISABLED  -> !isEnabled
              )
            },
            ^.onClick --> onAddSubNodeSaveClick(rcvrKey),
            _msg_Save
          ),

          // Кнопка отмены.
          <.a(
            ^.classSet1(
              (Css.Buttons.BTN :: Css.Size.M :: Nil).mkString(SPACE),
              Css.Buttons.NEGATIVE  -> !isSaving,
              Css.Buttons.DISABLED  -> isSaving
            ),
            ^.onClick --> onAddSubNodeCancelClick(rcvrKey),
            _msg_Cancel
          ),

          // Крутилка ожидания сохранения.
          isSaving ?= _mediumLoader,

          // Вывести инфу, что что-то пошло не так при ошибке сохранения.
          addState.saving.renderFailed { ex =>
            <.span(
              ^.title := ex.toString,
              Messages("Something.gone.wrong")
            )
          }

        ) // div()
      } // addState =>
    }


    /**
      * Рекурсивный рендер под-дерева узлов.
      *
      * @param node Корневой узел этого под-дерева.
      * @param parentRcvrKey Ключ родительского узла [Nil].
      * @param level Уровень [0].
      * @return React-элемент.
      */
    def _renderNode(node: MNodeState, parentRcvrKey: RcvrKey, level: Int): ReactElement = {
      val rcvrKeyRev = node.info.id :: parentRcvrKey
      val rcvrKey = rcvrKeyRev.reverse

      // Контейнер узла узла + дочерних узлов.
      <.div(
        ^.key := node.info.id,
        ^.`class` := (Css.Table.Td.TD :: Css.Table.Td.WHITE :: Css.Size.M :: Nil)
          .mkString( HtmlConstants.SPACE ),

        // Сдвиг слева согласно уровню, чтобы выглядело как дерево.
        ^.marginLeft := (level * 10).px,

        node.editing.fold[ReactElement] {
          // контейнер названия текущего узла
          <.div(
            ^.`class` := Css.Font.Sz.L,

            ^.onClick --> onNodeClick(rcvrKey),
            // Рендер названия узла.
            <.strong(
              node.info.name
            ),
            // Если инфа по узлу запрашивается с сервера, от отрендерить прелоадер
            node.children.renderPending { _ =>
              _smallWaitLoader
            }
          )

        } { ed =>
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
        },

        // Рендер подробной информации по узлу
        node.isNodeOpened ?= {
          <.div(

            // Отрендерить неизменяемый id узла (маячка).
            <.div(
              _msg_BeaconId, ":",
              node.info.id
            ),

            node.editing.fold[ReactElement] {

              // Узел НЕ редактируется, а просто показывается. Рендерить основные элементы управления.
              <.div(
                // Галочка управления активностью узла, если определено.
                for (cca <- node.info.canChangeAvailability) yield {
                  // Чек-бокс для управления isEnabled-флагом узла.
                  <.label(
                    <.input(
                      ^.`type`      := "checkbox",
                      // Текущее значение галочки происходит из нового значения и текущего значения, полученного на сервере.
                      ^.value       := node.isEnabledUpd.fold(node.info.isEnabled)(_.newIsEnabled),
                      // Можно управлять галочкой, если разрешено и если не происходит какого-то запроса с обновлением сейчас.
                      if (cca && node.isEnabledUpd.isEmpty) {
                        ^.onChange ==> onNodeEnabledChange(rcvrKey)
                      } else {
                        ^.disabled  := true
                      }
                    ),
                    Messages("Is.enabled")
                  )
                },


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
                        ^.title   := ex.toString,
                        Messages("Error")
                      )
                    }
                  )
                },

                // Кнопка редактирования узла.
                <.a(
                  ^.href      := HtmlConstants.DIEZ,
                  ^.`class`   := (Css.Buttons.BTN :: Css.Buttons.MINOR :: Css.Size.M :: Nil)
                    .mkString( HtmlConstants.SPACE ),
                  ^.onClick  --> onNodeEditClick(rcvrKey),
                  Messages("Edit")
                ),

                // Кнопка удаления узла.
                node.info.canChangeAvailability.contains(true) ?= {
                  <.a(
                    ^.href     := HtmlConstants.DIEZ,
                    ^.`class`  := (Css.Buttons.BTN :: Css.Buttons.NEGATIVE :: Css.Size.M :: Nil)
                      .mkString(HtmlConstants.SPACE),
                    ^.onClick --> onNodeDeleteClick(rcvrKey),
                    Messages("Delete")
                  )
                },

                // Форма подтверждения удаления узла.
                for (delPot <- node.deleting) yield {
                  <.div(
                    // Рендерить форму, когда Pot пуст.
                    delPot.renderEmpty {
                      <.div(
                        Messages("Are.you.sure"),
                        " (", Messages("This.action.cannot.be.undone"), ")",

                        // Кнопка подтверждения удаления, красная.
                        <.a(
                          ^.`class` := (Css.Buttons.BTN :: Css.Buttons.NEGATIVE :: Css.Size.M :: Nil)
                            .mkString( HtmlConstants.SPACE ),
                          ^.onClick --> onNodeDeleteOkClick(rcvrKey),
                          Messages("Yes.delete.it")
                        ),

                        // Кнопка отмены удаления.
                        <.a(
                          ^.`class` := (Css.Buttons.BTN :: Css.Buttons.MINOR :: Css.Size.M :: Nil)
                            .mkString( HtmlConstants.SPACE ),
                          ^.onClick --> onNodeDeleteCancelClick(rcvrKey),
                          Messages("Cancel")
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
                          ^.title := ex.toString,
                          Messages("Error")
                        ),

                        // Кнопка закрытия ошибочной формы.
                        <.a(
                          ^.`class` := (Css.Buttons.BTN :: Css.Buttons.MINOR :: Css.Size.M :: Nil)
                            .mkString( HtmlConstants.SPACE ),
                          ^.onClick --> onNodeDeleteCancelClick(rcvrKey),
                          Messages("Close")
                        )
                      )
                    }

                  )
                }

              )
            } { ed =>
              // Происходит редактирование узла. Отобразить кнопки сохранения.
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
                    ^.`class` := (Css.Buttons.BTN :: Css.Buttons.MAJOR :: Css.Size.M :: Nil)
                      .mkString(HtmlConstants.SPACE),
                    ^.onClick --> onNodeEditOkClick(rcvrKey),
                    _msg_Save
                  ),
                  // Кнопка отмены редактирования.
                  <.a(
                    ^.`class` := (Css.Buttons.BTN :: Css.Buttons.NEGATIVE :: Css.Size.M :: Nil)
                      .mkString(HtmlConstants.SPACE),
                    ^.onClick --> onNodeEditCancelClick(rcvrKey),
                    _msg_Cancel
                  )
                )
              }
            }

          )
        },

        // Рекурсивно отрендерить дочерние элементы:
        node.children.render { children =>
          <.div(
            children.nonEmpty ?= {
              val childLevel = level + 1
              for (subNode <- children) yield {
                _renderNode(subNode, rcvrKeyRev, childLevel)
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
        },

        // Рендерить кнопку/форму добавления узла.
        node.isNodeOpened ?= {
          _renderAddUtil(rcvrKey, node)
        }

      )
    }


    /** Рендер текущего компонента. */
    def render(p: Props): ReactElement = {
      val v = p()
      val parentLevel = 0
      val parentRcvrKey = Nil

      <.div(
        ^.`class` := (Css.Table.TABLE :: Css.Table.Width.XL :: Nil)
          .mkString( HtmlConstants.SPACE ),

        // Рендерить узлы.
        for (node <- v.nodes) yield {
          _renderNode(node, parentRcvrKey, parentLevel)
        }
      )
    }

  }


  val component = ReactComponentB[Props]("Tree")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(mtreeP: Props) = component(mtreeP)

}
