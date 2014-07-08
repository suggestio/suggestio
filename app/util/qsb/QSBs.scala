package util.qsb

import play.api.mvc.QueryStringBindable
import models._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.03.14 17:48
 * Description: Здесь складываются небольшие QueryStringBindable для сложных get-реквестов.
 */

object QsbUtil {

  implicit def eitherOpt2option[T](e: Either[_, Option[T]]): Option[T] = {
    e match {
      case Left(_) => None
      case Right(b) => b
    }
  }
}

object QSBs {

  private def companyNameSuf = ".meta.name"

  /** qsb для MCompany. */
  implicit def mcompanyQSB(implicit strBinder: QueryStringBindable[String]) = {
    new QueryStringBindable[MCompany] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MCompany]] = {
        for {
          maybeCompanyName <- strBinder.bind(key + companyNameSuf, params)
        } yield {
          maybeCompanyName.right.map { companyName =>
            MCompany(meta = MCompanyMeta(name = companyName))
          }
        }
      }

      override def unbind(key: String, value: MCompany): String = {
        strBinder.unbind(key + companyNameSuf, value.meta.name)
      }
    }
  }

}
