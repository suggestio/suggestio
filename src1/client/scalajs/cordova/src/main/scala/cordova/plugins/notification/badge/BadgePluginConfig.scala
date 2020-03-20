package cordova.plugins.notification.badge

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.03.2020 23:18
  * Description: API конфигурации плагина.
  * @see [[https://github.com/katzer/cordova-plugin-badge#configurations]]
  */
trait BadgePluginConfig extends js.Object {

  val autoClear: js.UndefOr[Boolean] = js.undefined

  val indicator: js.UndefOr[BadgePluginConfig.Indicator_t] = js.undefined

}


object BadgePluginConfig {

  type Indicator_t <: js.Any

  object Indicators {
    final def BADGE = "badge".asInstanceOf[Indicator_t]
    final def CIRCULAR = "circular".asInstanceOf[Indicator_t]
    final def DOWNLOAD = "download".asInstanceOf[Indicator_t]
  }

}
