package io.suggest.sjs.common.vm.content

import io.suggest.sjs.common.view.VUtil
import io.suggest.sjs.common.vm.find.IApplyEl

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.09.15 14:42
 * Description: Аддон для поддержки сборки экземпляра VM из outerHTML.
 */
trait ApplyFromOuterHtml extends IApplyEl {
  
  /**
   * Сборка экземпляра vm и тега на базе сырого outerHTML.
   * @param outerHtml Строка HTML, включая текущий тег.
   * @return Экземпялр VM.
   */
  def apply(outerHtml: String): T = {
    // Парсим через innerHTML вне DOM. Тут по сути имитируется заливка outerHTML.
    val div = VUtil.newDiv()
    div.innerHTML = outerHtml
    val rootDiv = div.firstChild.asInstanceOf[Dom_t]
    apply(rootDiv)
  }

}
