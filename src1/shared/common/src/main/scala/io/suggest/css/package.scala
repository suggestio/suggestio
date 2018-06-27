package io.suggest

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.08.17 18:06
  */
package object css {

  /** Пошаренное дефолтовое значение настроек рендера ScalaCSS. */
  // TODO ProdDefaults: проблема с ScCss: id меняются на каждый чих, а перерендер шаблонов мы не делаем (getScCssF).
  // Надо написать свой конфиг, который будет генерить более стабильные названия для css-классов (нужен свой NameGen наподобии alphabet())
  val ScalaCssDefaults = scalacss.DevDefaults

}
