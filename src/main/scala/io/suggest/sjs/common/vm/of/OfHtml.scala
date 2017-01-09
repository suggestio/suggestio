package io.suggest.sjs.common.vm.of

import io.suggest.sjs.common.view.VUtil

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.01.16 14:17
  * Description: Поддержка статического извлечения элемента модели из строки HTML.
  */
trait OfHtml extends OfNode {

  /**
    * Десериализация экземпляра модели из строки HTML.
    * @param html Строка HTML, которая, как ожидается, содержит underlying-элемент этой VM.
    * @return Опциональный экземпляр VM.
    */
  def ofHtml(html: String): Option[T] = {
    val div = VUtil.newDiv(html)
    ofNode( div.firstChild )
  }

}
