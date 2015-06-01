package io.suggest.sjs.common.model.browser

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 01.06.15 9:53
 * Description: Для использования некоторых css-возможностей в браузерах требуется указывать vendor prefix:
 * -moz-transform вместо/в добавок к transform.
 * Тут модель для определения и автоматической подстановки необходимых префиксов.
 * @see [[https://en.wikipedia.org/wiki/Vendor_prefix#Prefix_filters Префиксы CSS]]
 */

object VendorPrefix {

  /** По умолчанию префиксов быть не должно: использовать стандартные названия без префиксов. */
  val STANDARD_PREFIXES = List("")

}


/** Интерфейс реализации префиксера для конкретного браузера. */
trait IVendorPrefixer {

  /** Префиксы для поддержки css 2D-трансформов: transform, scale, etc.
    * @see [[https://developer.mozilla.org/en-US/docs/Web/CSS/transform MDN: transforms]]
    * @see [[http://caniuse.com/#feat=transforms2d Диаграмма поддержки в браузерах]]
    * @return Например List("-moz-", ""), означающий, что нужно попробовать с префиксом и без.
    *         Nil означает, что данный функционал не доступен в браузере.
    */
  def transforms2d: List[String] = VendorPrefix.STANDARD_PREFIXES

  /**
   * CSS-префиксы для поддержки css transform3d и прочих.
   * @see [[https://developer.mozilla.org/en-US/docs/Web/CSS/transform MDN: transforms]]
   * @return Список префиксов, которые нужно приписать.
   */
  def transforms3d: List[String] = VendorPrefix.STANDARD_PREFIXES

  /** Дергать, когда поддержка недоступна. */
  protected final def NO_SUPPORT: List[String] = Nil

}
