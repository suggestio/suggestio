package models

import io.suggest.util.MacroLogsImplLazy
import play.api.mvc.QueryStringBindable

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.08.14 15:54
 * Description: Модель, описывающая допустимые режимы отображения списка рекламных карточек узла.
 */
object MNodeAdsModes extends Enumeration with MacroLogsImplLazy {

  import LOGGER._

  abstract protected class Val(val shortId: String) extends super.Val(shortId) {
    def qsbOption: Option[String]
  }

  protected sealed trait QsbOptionCurrent {
    def shortId: String
    def qsbOption: Option[String] = Some(shortId)
  }

  type T = Val


  val ALL_ADS: T = new Val("a") {
    override def qsbOption: Option[String] = None
  }

  val ADV_REQ_ADS: T = new Val("r") with QsbOptionCurrent

  val ADV_OK_ADS: T = new Val("o") with QsbOptionCurrent


  implicit def value2val(x: Value): T = x.asInstanceOf[T]

  /** qsb-аддон, линкуемый в routes. */
  implicit def qsb(implicit strOptB: QueryStringBindable[Option[String]]): QueryStringBindable[T] = {
    new QueryStringBindable[T] {
      import util.qsb.QsbUtil._

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, T]] = {
        for {
          maybeModeStr <- strOptB.bind(key, params)
        } yield {
          val result = (maybeModeStr : Option[String]).fold(ALL_ADS) { shortId =>
            values.find(_.shortId == shortId) getOrElse {
              warn(s"qsb(): Unknown ads mode: $shortId. Fallbacking to ALL_ADS.")
              ALL_ADS
            }
          }
          Right(result)
        }
      }

      override def unbind(key: String, value: T): String = {
        strOptB.unbind(key, value.qsbOption)
      }
    }
  }

}
