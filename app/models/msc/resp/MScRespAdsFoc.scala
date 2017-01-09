package models.msc.resp

import play.api.libs.json._
import play.api.libs.functional.syntax._
import io.suggest.sc.ScConstants.Resp._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.09.16 17:03
  * Description: JSON-модель для ответа сервера по запрошенным focused-картчкам.
  */
object MScRespAdsFoc {

  /** Поддержка сериализации foc-ads-ответа в JSON. */
  implicit val WRITES: OWrites[MScRespAdsFoc] = (
    (__ \ FOCUSED_ADS_FN).write[Seq[MFocRenderResult]] and
    (__ \ TOTAL_COUNT_FN).write[Int] and
    (__ \ STYLES_FN).write[String]
  )(unlift(unapply))

}


case class MScRespAdsFoc(
  fads        : Seq[MFocRenderResult],
  totalCount  : Int,
  styles      : String
)
