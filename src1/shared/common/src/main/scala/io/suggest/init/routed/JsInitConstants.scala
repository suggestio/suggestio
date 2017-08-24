package io.suggest.init.routed

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.04.15 11:01
 * Description: Шаблон для моделей js-init-целей.
 * Шаблоны в js передают данные для селективной инициализации через аттрибут в body.
 * js считывает спецификацию и производит инициализацию (вызов) указанных scala.js-модулей в заданном порядке.
 */
object JsInitConstants {

  /** Название аттрибута для тега body, куда записывается инфа для направленной инициализации. */
  def RI_ATTR_NAME = "data-ri"

}
