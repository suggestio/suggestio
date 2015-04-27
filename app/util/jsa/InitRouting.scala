package util.jsa

import models.Context

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.04.15 14:10
 * Description: Интеграция с init-роутером на стороне js. Этот модуль генерит спеки для направленной инициализации.
 */
object InitRouting {

  /** Сгенерить спеку на основе контекста. */
  def toSpec(implicit ctx: Context): Option[String] = {
    val ctl = ctx.controllerSimple
    val hasNoFlash = ctx.request.flash.isEmpty
    if (ctl.isEmpty && hasNoFlash) {
      None
    } else {
      val sb = new StringBuilder(32)

      // отработка текущего контроллера.
      if (ctl.nonEmpty) {
        sb.append(ctl.get)
          .append(':')
        val _action = ctx.action
        if (_action.nonEmpty)
          sb.append(_action.get)
        sb.append(';')
      }

      // В конце инициализируем flashing-контроллер для всплывающих уведомлений на странице.
      if (!hasNoFlash) {
        // Есть уведомления. На стороне js требуется только инициализация соотв.контроллера.
        sb.append(Context.FLASH_INIT_SJS_CTL)
          .append(':')
      }

      Some( sb.toString() )
    }
  }

}
