package io.suggest.lk.nodes.form.r.tree

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
      $.props >>= { p =>
        p.dispatchCB( NodeNameClick(rcvrKey) )
      }
    }


    /** Callback клика по кнопке добавления под-узла. */
    def onAddSubNodeClick(parentKey: RcvrKey): Callback = {
      $.props >>= { p =>
        p.dispatchCB( AddSubNodeClick(parentKey) )
      }
    }


    /** Callback для ввода названия добавляемого под-узла. */
    def onAddSubNodeNameChange(parentKey: RcvrKey)(e: ReactEventI): Callback = {
      val name2 = e.target.value
      $.props >>= { p =>
        p.dispatchCB( AddSubNodeNameChange(parentKey, name2) )
      }
    }

    /** Callback редактирования id создаваемого узла. */
    def onAddSubNodeIdChange(parentKey: RcvrKey)(e: ReactEventI): Callback = {
      val name2 = e.target.value
      $.props >>= { p =>
        p.dispatchCB( AddSubNodeIdChange(parentKey, name2) )
      }
    }

    /** Callback нажатия на кнопку "сохранить" при добавлении нового узла. */
    def onAddSubNodeSaveClick(parentKey: RcvrKey): Callback = {
      $.props >>= { p =>
        p.dispatchCB( AddSubNodeSaveClick(parentKey) )
      }
    }

    def onAddSubNodeCancelClick(parentKey: RcvrKey): Callback = {
      $.props >>= { p =>
        p.dispatchCB( AddSubNodeCancelClick(parentKey) )
      }
    }


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
        // Сейчас открыта форма добавление под-узла для текущего узла.
        <.div(

          // Поле ввода названия маячка.
          <.label(
            Messages("Name"), ":",
            <.input(
              ^.`type`      := "text",
              ^.value       := addState.name,
              ^.onChange   ==> onAddSubNodeNameChange(parentRcvrKey),
              ^.placeholder := Messages("Beacon.name.example")
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
              ^.title       := Messages("Example.id.0", EXAMPLE_UID)
            )
          ),

          // Кнопка сохранения.
          <.a(
            ^.`class` := (Css.Buttons.BTN :: Css.Size.M :: Css.Buttons.MAJOR :: Nil).mkString(SPACE),
            ^.onClick --> onAddSubNodeSaveClick(parentRcvrKey),
            Messages("Save")
          ),

          // Кнопка отмены.
          <.a(
            ^.`class` := (Css.Buttons.BTN :: Css.Size.M :: Css.Buttons.NEGATIVE :: Nil).mkString(SPACE),
            ^.onClick --> onAddSubNodeCancelClick(parentRcvrKey),
            Messages("Cancel")
          )
        )
      }
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
      val rcvrKey = node.info.id :: parentRcvrKey
      // Контейнер узла узла + дочерних узлов.
      <.div(
        ^.key := node.info.id,

        (level > 0) ?= {
          ^.marginLeft := (level * 10).px
        },

        // контейнер названия текущего узла
        <.div(
          ^.onClick --> onNodeClick(rcvrKey),
          node.info.name
        ),

        // Если инфа по узлу запрашивается с сервера, от отрендерить прелоадер
        node.children.renderPending { _ =>
          val pleaseWait = Messages("Please.wait")
          LkPreLoader.PRELOADER_IMG_URL.fold [ReactElement] {
            <.span( pleaseWait )
          } { url =>
            <.img(
              ^.src := url,
              ^.alt := pleaseWait
            )
          }
        },

        // Рекурсивно отрендерить дочерние элементы:
        node.children.render { children =>
          <.div(
            children.nonEmpty ?= {
              val childLevel = level + 1
              for (subNode <- children) yield {
                _renderNode(subNode, rcvrKey, childLevel)
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
