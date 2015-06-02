package io.suggest.sjs.common.model.browser

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 01.06.15 10:03
 * Description: Модель доступа к данным о браузере прочему.
 */

/** Инфа по браузеру в целом. */
trait IBrowser extends IBrowserVsn {

  /** Строковое название браузера: firefox, chrome, opera, etc. */
  def name: String

  /** Доступ к механизму префиксинга css-свойств. */
  def CssPrefixing: IVendorPrefixer

  override def toString: String = {
    name + "-" + vsnMajor + "." + vsnMinor
  }

  /**
   * Некоторые браузеры скроллят внешний контейнер вместо внутреннего, когда оба скроллабельны.
   * С помощью это флага можно активировать логику перезаписи scrollbar'а внутренним контейнером,
   * чтобы быстро исправить недоразумение.
   * @return true, когда требуется спровоцировать внутренний скроллбар через шаманство с высотами.
   *         false, когда костыли не нужны (по умолчанию).
   */
  def needOverwriteInnerScroll: Boolean = false

}

