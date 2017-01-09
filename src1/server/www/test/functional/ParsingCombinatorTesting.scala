package functional

import scala.util.parsing.combinator.RegexParsers

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 01.12.14 11:31
 * Description: Хелперы для упрощения тестирования комбинируемых парсеров.
 */
trait RegexParsersTesting extends RegexParsers {

  /** Протестить исполнение парсер. */
  protected def parseSuccess[T](p: Parser[T], in: CharSequence): T = {
    parseAll(p, in) match {
      case Success(res, _) =>
        res
      case NoSuccess(msg, next) =>
        throw new IllegalArgumentException(s"Could not parse '$in' near\n${next.pos.longString}:$msg")
    }
  }

}
