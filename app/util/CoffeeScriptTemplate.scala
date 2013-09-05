package util

import play.api.templates.Html
import org.jcoffeescript

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.09.13 13:48
 * Description: Компиляция кофискрипта из исходного текста. Можно использовать из шаблонов.
 */

/* Использование: в начало шаблона добавить:
 *
 *   @import util.CoffeeScriptTemplate._
 *
 * Далее, можно использовать следующим образом:
 * @coffee {
 *   a = 1
 * }
 *
 */
object CoffeeScriptTemplate {

  val compiler = new jcoffeescript.JCoffeeScriptCompiler()

  def coffee(source: Html, options: Seq[String] = Nil): String = {
    compiler.compile(source.body)
  }

}

