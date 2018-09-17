package io.suggest

import scalacss.defaults.Exports
import scalacss.internal.{Compose, Renderer, StringRenderer}
import scalacss.internal.mutable.Register.{ErrorHandler, MacroName, NameGen}
import scalacss.internal.mutable.Settings

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.08.17 18:06
  */
package object css {

  /** Пошаренное дефолтовое значение настроек рендера ScalaCSS. */
  // TODO ProdDefaults: проблема с ScCss: id меняются на каждый чих, а перерендер шаблонов мы не делаем (getScCssF).
  // Надо написать свой конфиг, который будет генерить более стабильные названия для css-классов (нужен свой NameGen наподобии alphabet())
  val ScalaCssDefaults: Exports with Settings = Prod // scalacss.DevDefaults

  object Prod extends Exports with Settings {
    override          def cssRegisterNameGen     : NameGen          = NameGen.short()

    //Ignore  // TODO Есть проблемы со стилями в ScCss при перерендерах и кэшировании инстанса. Если Ignore, то можно задействовать штатный ProdDefaults
    override          def cssRegisterMacroName   : MacroName        = MacroName.Use

    override          val cssRegisterErrorHandler: ErrorHandler     = ErrorHandler.silent
    override implicit def cssStringRenderer      : Renderer[String] = StringRenderer.formatTiny
    override implicit def cssComposition         : Compose          = Compose.trust
  }

}
