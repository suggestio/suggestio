package io.suggest.sjs.common.model.browser.mozilla

import io.suggest.sjs.common.model.browser.{IBrowser, IVendorPrefixer}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 01.06.15 11:27
 * Description: Аддон для поддержки подбирания css-префиксов под файрфокс.
 */
trait GeckoCssPrefixing extends IVendorPrefixer with IBrowser {

  /** Доступ к механизму префиксинга css-свойств. */
  override def CssPrefixing = this


  /**
   * Поддержка transform для gecko/firefox в зависимости от версии браузера.
   * @see [[https://developer.mozilla.org/en-US/docs/Web/CSS/transform Описание и таблица поддержки в браузерах]]
   */
  override def transforms2d: List[String] = {
    val v = vsnMajor
    if (v >= 16) {
      super.transforms2d

    } else if (v >= 4 || (v == 3 && vsnMinor >= 5)) {
      // firefox >= 3.5 has prefixed support of css-transforsm 2d
      FirefoxBrowser.MOZ_PREFIXING

    } else {
      // Firefox < 3.5 doesn't support any css 2d animations.
      NO_SUPPORT
    }
  }


  /**
   * CSS-префиксы для поддержки css transform3d и прочих.
   * @see [[https://developer.mozilla.org/en-US/docs/Web/CSS/transform MDN: transforms]]
   * @return Список префиксов, которые нужно приписать.
   */
  override def transforms3d: List[String] = {
    val v = vsnMajor
    if (v >= 16) {
      super.transforms3d

    } else if (v >= 10) {
      // Firefox >= 10.0 has prefixed support of 3d transforms.
      FirefoxBrowser.MOZ_PREFIXING

    } else {
      NO_SUPPORT
    }
  }

}
