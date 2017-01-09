package io.suggest.sc.sjs.vm.nav.nodelist

import io.suggest.sc.ScConstants.NavPane.CONTENT_ID
import io.suggest.sjs.common.vm.content.SetInnerHtml
import io.suggest.sjs.common.vm.child.ContentElT
import io.suggest.sjs.common.vm.find.FindDiv
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.08.15 15:09
 * Description: vm div content списка узлов на панели навигации.
 */
object NlContent extends FindDiv {
  override type T = NlContent
  override def DOM_ID = CONTENT_ID
}


trait NlContentT extends ContentElT with SetInnerHtml {

  override type T = HTMLDivElement

  /** Динамический контейнер, появляется после заливки его через setContent(). */
  def container = NlContainer.find()

  /** Пустой ли контейнер списка узлов? */
  def isEmpty: Boolean = {
    _underlying.firstChild == null
  }
  /** Залиты ли данные в контейнер списка узлов? */
  def nonEmpty = !isEmpty

}


case class NlContent(
  override val _underlying: HTMLDivElement
)
  extends NlContentT
{
  override lazy val container  = super.container
}
