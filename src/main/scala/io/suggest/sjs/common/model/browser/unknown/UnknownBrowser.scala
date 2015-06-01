package io.suggest.sjs.common.model.browser.unknown

import io.suggest.sjs.common.model.browser.{IVendorPrefixer, IBrowser}
import io.suggest.sjs.common.view.safe.wnd.SafeWindow
import org.scalajs.dom

import scala.scalajs.js.{Dictionary, Any}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 01.06.15 16:32
 * Description: Реализация неизвестного браузера, которые данные об css-префиксах собирает по обстоятельствам.
 */
class UnknownBrowser extends IBrowser with IVendorPrefixer {

  override def name: String = "unknown"

  override def CssPrefixing = this

  override def vsnMajor: Int = 0
  override def vsnMinor: Int = 0

  /** Вычислить css-префиксы на основе getComputedStyle(). */
  val CSS_PREFIX: List[String] = {
    val p0 = SafeWindow()
      .getComputedStyle( dom.document.documentElement )
      .flatMap { css =>
        val re = "^(-(webkit|moz|ms|o)-)".r
        css.asInstanceOf[Dictionary[Any]]
          .iterator
          .map { _._1 }
          .filter { name => name.charAt(0) == '-' }
          .flatMap {
            re.findFirstMatchIn(_)
          }
          .filter { _.groupCount >= 1 }
          .map { _.group(1) }
          // Имитируем headOption для итератора.
          .collectFirst {
            case prefix => prefix
          }
          // В оригинале для opera presto ещё использовался css.OLink == "". Похожим образом presto детектится через OperaLegacyDetector.
      }
      .toList
    // Вернуть стандартный прейфикс и найденный, если найден.
    "" :: p0
  }

  // Префиксуем всё по-простому.
  override def transforms2d = CSS_PREFIX
  override def transforms3d = CSS_PREFIX

  override def toString = name
}
