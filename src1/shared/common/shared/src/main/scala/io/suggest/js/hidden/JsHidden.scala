package io.suggest.js.hidden

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.04.15 10:50
 * Description: CSS-класс js-hidden работает как отметка о скрытости элемента.
 * Неоптимальная реализация идеи скрытых элементов на странице, отображаемых по желанию js.
 * То, что есть сейчас можно переписать через <noscript> и display:none + js.
 */
object JsHiddenConstants {

  /** Класс-отметка для процессора js-hidden-элементов. */
  def CSS_CLASS = "js-hidden"

}
