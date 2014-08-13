package util

import org.jcoffeescript.JCoffeeScriptCompileException
import play.twirl.api._
import org.jcoffeescript

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.09.13 13:48
 * Description: Компиляция кофискрипта из исходного текста. Можно использовать из шаблонов.
 * Использование:
 * В начало шаблона добавить:
 *
 * \@import util.CoffeeScriptTemplate._
 *
 * Далее, можно использовать следующим образом:
 * \@coffee {
 *   a = 1
 * }
 */
object CoffeeScriptTemplate extends PlayLazyMacroLogsImpl {

  import LOGGER._

  def compiler = new jcoffeescript.JCoffeeScriptCompiler()

  def compileString(source: String): String = {
    try {
      compiler compile source
    } catch {
      case ex: JCoffeeScriptCompileException =>
        // Если возникла проблема при компиляции, то стоит напечатать исходный код.
        debug("compileString(): Failed to compile coffee script source:\n" + source)
        throw ex
    }
  }


  def coffee(source: Html): String = {
    val t = source.body
      .replaceAll("&lt;", ">")
      .replaceAll("&gt;", "<")
      .replaceAll("&quot;", "\"")
      .replaceAll("&#x27;", "'")
      .replaceAll("&amp;", "&")
    compileString(t)
  }

  def coffee(source: Txt): String = {
    compileString(source.body)
  }

}

