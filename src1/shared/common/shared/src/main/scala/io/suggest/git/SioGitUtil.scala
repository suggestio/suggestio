package io.suggest.git

import scala.language.experimental.macros
import scala.reflect.macros.Context
import scala.language.postfixOps

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.11.2019 21:59
  * Description: Утиль для взаимодействия с git'ом.
  */
object SioGitUtil {

  def currentRevision: String = macro currentRevisionImpl


  /** Доступ к данным текущей ревизии git во время компиляции.
    * @return Строка вида "20190101.181818.abcdef1234.9999".
    */
  def currentRevisionImpl(c: Context): c.Expr[String] = {
    import c.universe._
    import sys.process._

    val vsnStr = scala.util.Try {

      val dateTime = ("git show -s --format=%ci HEAD" !!)
        .trim
        .replaceAll("\\s+\\+[0-9]+$", "")
        .replaceAll("\\s+", ".")
        .replaceAll("[:-]", "")

      val revCounter = ("git rev-list --count HEAD" !!).trim

      val rev = ("git rev-parse --short HEAD" !!).trim

      s"$dateTime.$rev.$revCounter"
    }
      .getOrElse("???")

    c.Expr[String]( Literal( Constant(vsnStr) ) )
  }

}
