package io.suggest.sc

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.05.15 16:08
 * Description: Константы для выдачи.
 */
object ScConstants {

  /**
   * Имя js-роутера на странице. val потому что:
   * - в sjs используется в аннотации.
   * - в web21 будет постоянно использоваться с внедрением sjs-выдачи.
   */
  final val JS_ROUTER_NAME = "sioScJsRoutes"

  /** window.NAME - название функции function(), которая будет вызвана  */
  final val JS_ROUTER_ASYNC_INIT_FNAME = JS_ROUTER_NAME + "AsyncInit"

}
