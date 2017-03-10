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

    private def _dispatchCB[A](action: A)(implicit evidence: ActionType[A]): Callback = {
      $.props >>= { p =>
        p.dispatchCB( action )
      }
    }


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
    def _renderAddUtil(parentRcvrKey: RcvrKey, mtree: MTree): TagMod = {
      // Рендерить кнопку добавления нового узла.
      mtree.addStates.get(parentRcvrKey).fold[TagMod] {
        // Форма добавления для текущего узла не существует. Рендерить кнопку добавления.
        <.a(
          ^.`class` := (Css.Buttons.BTN :: Css.Size.M :: Css.Buttons.MINOR :: Nil).mkString( SPACE ),
          ^.onClick --> onAddSubNodeClick(parentRcvrKey),
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
            ^.title := Messages("Server.request.in.progress.wait")
          },

          // Поле ввода названия маячка.
          <.label(
            Messages("Name"), ":",
            <.input(
              ^.`type`      := "text",
              ^.value       := addState.name,
              ^.onChange   ==> onAddSubNodeNameChange(parentRcvrKey),
              ^.placeholder := Messages("Beacon.name.example"),
              disabledAttr
            )
          ),

          // Поля для ввода id маячка.
          <.label(
            Messages("Beacon.id"),
            " (EddyStone-UID)",
            <.input(
              ^.`type`      := "text",
              ^.value       := addState.id.getOrElse(""),
              ^.onChange   ==> onAddSubNodeIdChange(parentRcvrKey),
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
            ^.onClick --> onAddSubNodeSaveClick(parentRcvrKey),
            Messages("Save")
          ),

          // Кнопка отмены.
          <.a(
            ^.classSet1(
              (Css.Buttons.BTN :: Css.Size.M :: Nil).mkString(SPACE),
              Css.Buttons.NEGATIVE  -> !isSaving,
              Css.Buttons.DISABLED  -> isSaving
            ),
            ^.onClick --> onAddSubNodeCancelClick(parentRcvrKey),
            Messages("Cancel")
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

        // Сдвиг слева согласно уровню, чтобы выглядело как дерево.
        ^.marginLeft := (level * 10).px,

        // контейнер названия текущего узла
        <.div(
          ^.onClick --> onNodeClick(rcvrKey),
          node.info.name,

          // Если инфа по узлу запрашивается с сервера, от отрендерить прелоадер
          node.children.renderPending { _ =>
            _smallWaitLoader
          }
        ),

        // Галочка управления активностью узла, если определено.
        for (cca <- node.info.canChangeAvailability) yield {
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
            Messages("Is.enabled"),

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
        }

      )
    }


    /** Рендер текущего компонента. */
    def render(p: Props): ReactElement = {
      val v = p()
      val parentLevel = 0
      val parentRcvrKey = Nil

      <.div(
        // Рендерить узлы.
        for (node <- v.nodes) yield {
          _renderNode(node, parentRcvrKey, parentLevel)
        },

        // Рендерить кнопку/форму добавления узла.
        _renderAddUtil(parentRcvrKey, v)
      )
    }

  }


  val component = ReactComponentB[Props]("Tree")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(mtreeP: Props) = component(mtreeP)

}
