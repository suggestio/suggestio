package io.suggest.sc.sjs.vm.util.cont

import io.suggest.sc.sjs.m.mdom.content.IHtmlContent
import io.suggest.sjs.common.view.safe.ISafe
import org.scalajs.dom.{Element, Node}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.06.15 17:24
 * Description: DOM-элемент контейнер.
 */
trait IContainer extends ISafe {

  override type T <: Element

  def content_=(html: IHtmlContent): Unit

  def content: String

  def contentNode: Option[Node]

  def clear(): Unit

}


trait ContainerT extends IContainer {

  /** setter для контента. */
  override def content_=(html: IHtmlContent): Unit = {
    // clear() чтобы очистить память браузера от всех возможных listener'ов и прочего:
    clear()
    html.writeInto(_underlying)
  }

  /** Геттер для контента. */
  override def content: String = {
    _underlying.innerHTML
  }

  /** Первая нода контента внутри контейнера. */
  override def contentNode: Option[Node] = {
    Option(_underlying.firstChild)
  }

  override def clear(): Unit = {
    contentNode foreach _underlying.removeChild
  }

}
