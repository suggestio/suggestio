package io.suggest.sc.sjs.v.res

import io.suggest.sc.sjs.m.SafeDoc
import org.scalajs.dom
import org.scalajs.dom.{Element, Document}

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

  /** Название тега. */
  def tag: String = "div"

  /** id тега в документе. */
  def id: String

  /**
   * Найти контейнер на странице.
   * @param d Закешированный dom.document, если есть.
   * @return Опционально-найденный элемент.
   */
  def findContainer(d: Document = dom.document): Option[Element] = {
    _findContainer(d, id)
  }

  private def _findContainer(d: Document, _id: String): Option[Element] = {
    Option( d.getElementById(_id) )
  }

  /**
   * Очистить контейнер ресурсов.
   * @param d Закешированный документ, если есть.
   */
  def clear(d: Document = dom.document): Unit = {
    findContainer(d).foreach { el =>
      el.innerHTML = ""
    }
  }

  /** Создать/пересоздать контейнер ресурсов. */
  def recreate(): Unit = {
    val d = dom.document
    val _id = id
    // Найти и удалить старый элемент
    val oldOpt = _findContainer(d, _id)
    if (oldOpt.nonEmpty) {
      val old = oldOpt.get
      old.parentNode.removeChild(old)
    }

    // Пересоздать элемент.
    val el = d.createElement(tag)
    el.setAttribute("id", _id)

    SafeDoc.body.appendChild(el)
  }

}
