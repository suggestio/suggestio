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

  type MNodeAdsMode = Val


  val ALL_ADS: MNodeAdsMode = new Val("a") {
    override def qsbOption: Option[String] = None
  }

  val ADV_REQ_ADS: MNodeAdsMode = new Val("r") with QsbOptionCurrent

  val ADV_OK_ADS: MNodeAdsMode = new Val("o") with QsbOptionCurrent


  implicit def value2val(x: Value): MNodeAdsMode = x.asInstanceOf[MNodeAdsMode]

  /** qsb-аддон, линкуемый в routes. */
  implicit def qsb(implicit strOptB: QueryStringBindable[Option[String]]) = {
    new QueryStringBindable[MNodeAdsMode] {
      import util.qsb.QsbUtil._

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MNodeAdsMode]] = {
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

      override def unbind(key: String, value: MNodeAdsMode): String = {
        strOptB.unbind(key, value.qsbOption)
      }
    }
  }

}
