package io.suggest.lk.adv.geo.tags.vm.popup.rcvr

import io.suggest.css.Css
import japgolly.scalajs.react.{BackendScope, Callback, PropsChildren, ReactComponentB, ReactElement, ReactEventI}
import japgolly.scalajs.react.vdom.prefix_<^._
import org.scalajs.dom.raw.{HTMLDivElement, HTMLInputElement}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.12.16 18:31
  * Description: Попытка реализовать содержимое попапов на React.js.
  *
  * Вызывать шаблонизацию можно таким образом:
  * {{{
        RcvrPopupCont(
          for (grp <- groups) yield {
            RcvrNodesGroup(
              RcvrNodesGroup.Props(grp.nameOpt),
              for (node <- grp.nodes) yield {
                RcvrNodeCheckBox(
                  RcvrNodeCheckBox.Props(
                    rcvrId  = rcvrId,
                    groupId = groupIdOpt,
                    node    = node,
                    onChange = { (newValue: Boolean) =>
                      ...
                    }
                  )
                )
              }
            )
          }
        )
  * }}}
  *
  * Считаем, что реюзинг компонентов
  */

object ReactRcvrPopup {

  // TODO Внешняя модель, приходящая с сервера в виде JSON. Вынести за ворота её надо, здесь она должна быть интерфейсом.
  case class RcvrNode(title: String, id: String, checked: Boolean, isCreate: Boolean, intevalOpt: Option[String], isOnlineNow: Boolean)


  /** Компонент для одного чек-бокса узла в списке под-узлов в попапе ресивера. */
  object RcvrNodeCheckBox {

    case class Props(node: RcvrNode, onChange: (Boolean) => Unit)

    case class State(checked: Boolean)

    protected class Backend($: BackendScope[Props, State]) {

      /** Реакция на изменение какого-то поля в форме: надо бы выставить новое значение для этой комбинации. */
      def checkBoxChange(e: ReactEventI): Callback = {
        for {
          _     <- $.modState( _.copy(checked = e.target.checked) )
          props <- $.props
        } yield {
          val newValue = e.target.checked
          println( props.node.id + " = " + newValue )
          props.onChange(newValue)
        }
      }

      def render(props: Props, state: State) = {
        val n = props.node
        <.div(
          ^.`class` := Css.Lk.LK_FIELD,
          <.label(
            ^.classSet1(
              Css.Lk.LK_FIELD_NAME,
              // Красным отображать удаляемые, зелёным -- создаваемые.
              Css.Project.RED     -> (!state.checked && !n.isCreate),
              Css.Project.GREEN   -> (state.checked  && n.isCreate)
            ),
            // Рендер галочки и названия галочки
            <.input(
              ^.`type`   := "checkbox",
              ^.checked  := state.checked,
              ^.onChange ==> checkBoxChange
            ),
            <.span(),
            n.title
          ),

          for (interval <- n.intevalOpt) yield {
            <.span(" ", interval)
          },

          // Отчеканить галочку, если есть хотя бы одно онлайновое размещение.
          n.isOnlineNow ?= " &checkmark"
        )
      }

    }


    val component = ReactComponentB[Props]("RcvrNodeCheckBox")
      .initialState_P { props =>
        State( props.node.checked )
      }
      .renderBackend[Backend]
      .domType[HTMLInputElement]
      .build

    def apply(props: Props) = component(props)

  }



  /** Рендер группы нод. Сами ноды передаются в children. */
  object RcvrNodesGroup {

    case class Props(nameOpt: Option[String])

    protected class Backend($: BackendScope[Props,_]) {

      def render(props: Props, c: PropsChildren) = {
        <.div(
          // Заголовок текущей группы (маячки, прочее), если есть:
          for (name <- props.nameOpt) yield {
            <.h3( name )
          },

          // Содержимое (узлы) текущей группы:
          <.div(c)
        )
      }

    }

    val component = ReactComponentB[Props]("RcvrNodesGroup")
      .stateless
      .renderBackend[Backend]
      .domType[HTMLDivElement]
      .build

    def apply(props: Props, children: Seq[ReactElement]) = component(props, children)

  }



  /**
    * Компонент контейнера попапа.
    * Сделан на всякий случай, чтобы в будущем какие-то функции в него затолкать.
    * Изначально он рендерит только div.
    */
  object RcvrPopupCont {

    val component = ReactComponentB[Unit]("RcvrPopupCont")
      .stateless
      .render_C { children =>
        <.div(children)
      }
      .domType[HTMLDivElement]
      .build

    def apply(children: Seq[ReactElement]) = component(children)

  }

}
