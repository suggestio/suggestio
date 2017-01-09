package io.suggest.sc.sjs.m.mdom.content

import io.suggest.sjs.common.model.dom.DomListIterator
import org.scalajs.dom.{DOMList, Node, Element}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.06.15 17:54
 * Description: Модель для полиморфного хранения и обработки HTML-верстки в разных форматах.
 */
trait IHtmlContent {

  /** Запись HTML в указанный DOM-элемент. Может произойти как перезапись, так и дописывание в конец. */
  def writeInto(el: Element): Unit
}


/** HTML-строка. */
case class StrHtmlContent(html: String) extends IHtmlContent {
  override def writeInto(el: Element): Unit = {
    el.insertAdjacentHTML("beforeEnd", html)
  }
}

/** Список child-узлов. */
case class NodesHtmlContent[T <: Node](nodes: DOMList[T]) extends IHtmlContent {
  override def writeInto(el: Element): Unit = {
    for (node <- DomListIterator(nodes)) {
      el.appendChild(node)
    }
  }
}

/** Один child-узел. */
case class NodeContent[T <: Node](node: T) extends IHtmlContent {
  override def writeInto(el: Element): Unit = {
    el.appendChild(node)
  }
}
