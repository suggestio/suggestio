package models

import io.suggest.model.geo.GeoDistanceQuery
import io.suggest.ym.model.common.AdnNodesSearchArgsT
import play.api.mvc.QueryStringBindable
import util.qsb.QsbUtil._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 12.08.14 16:49
 * Description: Набор аргументов для динамического поиска узла.
 */
object MAdnNodeSearch {

  implicit def qsb(implicit
                   strOptB: QueryStringBindable[Option[String]],
                   geoModeB: QueryStringBindable[GeoMode]) = {
    new QueryStringBindable[MAdnNodeSearch] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MAdnNodeSearch]] = {
        ???
      }

      override def unbind(key: String, value: MAdnNodeSearch): String = {
        ???
      }
    }
  }

}

case class MAdnNodeSearch(
  qStr        : Option[String] = None,
  companyIds  : Seq[String] = Nil,
  adnSupIds   : Seq[String] = Nil,
  anyOfPersonIds: Seq[String] = Nil,
  advDelegateAdnIds: Seq[String] = Nil,
  withAdnRighs: Seq[AdnRight] = Nil,
  testNode    : Option[Boolean] = None,
  withoutIds  : Seq[String] = Nil,
  geoDistance : Option[GeoDistanceQuery] = None,
  hasLogo     : Option[Boolean] = None,
  withGeoDistanceSort: Option[GeoPoint] = None,
  maxResults  : Int = 10,
  offset      : Int = 0
) extends AdnNodesSearchArgsT
