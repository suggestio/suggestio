package io.suggest.sjs.common.model.browser.unknown

import io.suggest.sjs.common.model.browser.{EnginePrefix, IVendorPrefixer, IBrowser}
import io.suggest.sjs.common.vm.wnd.WindowVm
import org.scalajs.dom

import scala.scalajs.js.{WrappedDictionary, Dictionary, Any}

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


  /**
   * Поддержка события document visibilitychange характеризуется этими префиксами.
   * @see [[https://developer.mozilla.org/en-US/docs/Web/Guide/User_experience/Using_the_Page_Visibility_API]]
   */
  override lazy val visibilityChange: List[String] = {
    val dd: WrappedDictionary[Any] = dom.document.asInstanceOf[Dictionary[Any]]
    val root = "hidden"
    if (dd.contains(root)) {
      super.visibilityChange
    } else {
      val root2 = root.tail
      EnginePrefix.ALL_PREFIXES
        .filter { maybePrefix =>
          val name = maybePrefix + "H" + root2        // "mozHidden", "webkitHidden", etc...
          dd.contains(name)
        }
        .toList
    }
  }

  override def toString = name
}
