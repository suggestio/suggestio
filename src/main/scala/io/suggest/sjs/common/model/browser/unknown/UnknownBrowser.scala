package io.suggest.sjs.common.model.browser.unknown

import io.suggest.sjs.common.model.browser.mozilla.FirefoxBrowser
import io.suggest.sjs.common.model.browser.webkit.WebKitPrefixing
import io.suggest.sjs.common.model.browser.{EnginePrefix, IBrowser, IVendorPrefixer}
import io.suggest.sjs.common.vm.wnd.WindowVm
import io.suggest.sjs.common.vm.wnd.compstyle.CssStyleDeclKeys
import org.scalajs.dom
import org.scalajs.dom.raw.PageVisibility

import scala.language.implicitConversions
import scala.scalajs.js

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 01.06.15 16:32
 * Description: Реализация неизвестного браузера, которые данные об css-префиксах собирает по обстоятельствам.
 */
class UnknownBrowser extends IBrowser with IVendorPrefixer {

  override def name: String = "unknown"

  override def Prefixing = this

  override def vsnMajor: Int = 0
  override def vsnMinor: Int = 0

  /** Вычислить css-префиксы на основе getComputedStyle(). */
  val CSS_PREFIX: List[String] = {
    val p0 = WindowVm()
      .getComputedStyle( dom.document.documentElement )
      .flatMap { css =>
        val re = ("^(-(" + EnginePrefix.ALL_PREFIXES.mkString("|") + ")-)").r
        CssStyleDeclKeys(css)
          .iterator
          .filter { name =>
            name.charAt(0) == '-'
          }
          .flatMap {
            re.findFirstMatchIn(_)
          }
          .filter { _.groupCount >= 1 }
          .map { _.group(1) }
          // Имитируем headOption для итератора.
          .toStream
          .headOption
          // В оригинале для opera presto ещё использовался css.OLink == "". Похожим образом presto детектится через OperaLegacyDetector.
      }
      .toList
    // Вернуть стандартный прейфикс и найденный, если найден.
    "" :: p0
  }

  // Префиксуем всё по-простому.
  override def transforms2d = CSS_PREFIX
  override def transforms3d = CSS_PREFIX


  /**
    * Поддержка события document visibilitychange характеризуется этими префиксами.
    *
    * @see [[https://developer.mozilla.org/en-US/docs/Web/Guide/User_experience/Using_the_Page_Visibility_API]]
    */
  override lazy val visibilityChange: List[String] = {
    val stub = dom.document: IPageVisibilityStub
    if (stub.hidden.isDefined) {
      super.visibilityChange
    } else if (stub.webkitHidden.isDefined) {
      List( WebKitPrefixing.PREFIX )
    } else if (stub.mozHidden.isDefined) {
      List( FirefoxBrowser.PREFIX )
    } else {
      Nil
    }
  }

  override def toString = name
}


@js.native
sealed trait IPageVisibilityStub extends js.Object {

  def hidden: js.UndefOr[Boolean] = js.native
  def mozHidden: js.UndefOr[Boolean] = js.native
  def webkitHidden: js.UndefOr[Boolean] = js.native

  def visibilitychange: js.UndefOr[_] = js.native

}

object IPageVisibilityStub {
  implicit def apply(el: PageVisibility): IPageVisibilityStub = {
    el.asInstanceOf[IPageVisibilityStub]
  }
}
