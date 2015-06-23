package io.suggest.sc.sjs.v.res

import io.suggest.sc.sjs.vm.SafeDoc
import org.scalajs.dom
import org.scalajs.dom.raw.HTMLDivElement
import org.scalajs.dom.Document

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.05.15 15:51
 * Description: Resource-контейнеры используются для хранения и передачи заинлайненных ресурсов в DOM.
 * Например: style-теги со стилями полученной верстки.
 * Тут заготовка для приготовления ресурса.
 *
 * С точки зрения DOM, контейнеры -- это просто теги в начале body.
 */
trait ResourceContainerT {

  type T = HTMLDivElement

  /** Название тега. */
  def tag: String = "div"

  /** id тега в документе. */
  def id: String

  /**
   * Найти контейнер на странице.
   * @return Опционально-найденный элемент.
   */
  def findContainer(): Option[T]

  /**
   * Очистить контейнер ресурсов.
   * @param d Закешированный документ, если есть.
   */
  def clear(d: Document = dom.document): Unit = {
    findContainer().foreach { el =>
      el.innerHTML = ""
    }
  }

  /** Создать/пересоздать контейнер ресурсов. */
  def recreate(): Unit = {
    // Найти и удалить старый элемент
    val oldOpt = findContainer()
    if (oldOpt.nonEmpty) {
      val old = oldOpt.get
      old.parentNode.removeChild(old)
    }

    // Пересоздать элемент.
    val el = dom.document.createElement(tag)
    el.setAttribute("id", id)

    SafeDoc.body.appendChild(el)
  }

  /** Добавить css-ресурс в контейнер. */
  def appendCss(css: String): Unit = {
    findContainer().foreach { cont =>
      val tag = dom.document.createElement("style")
      tag.innerHTML = css
      cont.appendChild(tag)
    }
  }

}
