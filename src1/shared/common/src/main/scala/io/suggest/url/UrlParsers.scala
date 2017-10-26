package io.suggest.url

import scala.util.parsing.combinator.RegexParsers

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.10.17 16:56
  * Description: Утиль для поддержки парсинга URL через комбинируемые парсеры.
  */
trait UrlParsers extends RegexParsers {

  protected def proto: Parser[String] = "(?i)^https?://".r

  protected def protoOpt = opt(proto)

  protected def wwwPrefix = "www\\.".r

  protected def wwwPrefixOpt: Parser[Option[String]] = {
    opt( wwwPrefix )
  }

}
