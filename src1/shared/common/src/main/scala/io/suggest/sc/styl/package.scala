package io.suggest.sc

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.07.17 13:59
  */
package object styl {

  /** Дефолтовые настройки для генерации CSS выдачи через ScalaCSS. */
  val ScScalaCssDefaults = io.suggest.css.ScalaCssDefaults

  /** Алиас типа для функции-провайдера, возвращающей текущий инстанс ScCss. */
  type GetScCssF = () => ScCss

}
