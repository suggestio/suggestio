package models

import io.suggest.model.geo.GeoDistanceQuery
import io.suggest.ym.model.common.AdnNodesSearchArgsT
import play.api.mvc.{RequestHeader, QueryStringBindable}
import util.qsb.QsbUtil._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 12.08.14 16:49
 * Description: Набор аргументов для динамического поиска узла.
 */

case class MAdnNodeSearch(
  qStr        : Option[String] = None,
  companyIds  : Seq[String] = Nil,
  adnSupIds   : Seq[String] = Nil,
  anyOfPersonIds: Seq[String] = Nil,
  advDelegateAdnIds: Seq[String] = Nil,
  withAdnRights: Seq[AdnRight] = Nil,
  testNode    : Option[Boolean] = None,
  withoutIds  : Seq[String] = Nil,
  geoDistance : Option[GeoDistanceQuery] = None,    // TODO Не bindable, т.к. geo=ip требует implicit request и производит future.
  hasLogo     : Option[Boolean] = None,
  withGeoDistanceSort: Option[GeoPoint] = None,
  withNameSort: Boolean = false,
  maxResults  : Int = 10,
  offset      : Int = 0
) extends AdnNodesSearchArgsT




/** Bindable-версия искалки. */
case class SimpleNodesSearchArgs(
  qStr        : Option[String] = None,
  geoMode     : GeoMode = GeoNone,
  offset      : Option[Int] = None
) {

  def toSearchArgs(implicit request: RequestHeader): Future[AdnNodesSearchArgsT] = {
    geoMode.geoSearchInfo.map { gsiOpt =>
      new MAdnNodeSearch(
        qStr          = qStr,
        geoDistance   = gsiOpt.map { _.geoDistanceQuery },
        withGeoDistanceSort = gsiOpt.map { _.geoPoint },
        offset        = offset.getOrElse(0),
        withAdnRights = Seq(AdnRights.RECEIVER),
        testNode      = Some(false),
        withNameSort  = true
      ) {
        override def ftsSearchFN: String = AdnMMetadata.NAME_ESFN
      }
    }
  }
  
}


object SimpleNodesSearchArgs {

  val Q_SUF = ".q"
  val GEO_SUF = ".geo"
  val OFFSET_SUF = ".offset"


  implicit def qsb(implicit strOptB: QueryStringBindable[Option[String]],
                   geoModeB: QueryStringBindable[GeoMode],
                   intOptB: QueryStringBindable[Option[Int]]) = {
    new QueryStringBindable[SimpleNodesSearchArgs] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, SimpleNodesSearchArgs]] = {
        for {
          maybeQOpt     <- strOptB.bind(key +  Q_SUF, params)
          maybeGeo      <- geoModeB.bind(key + GEO_SUF, params)
          maybeOffset   <- intOptB.bind(key + OFFSET_SUF, params)
        } yield {
          Right(
            SimpleNodesSearchArgs(
              qStr    = maybeQOpt,
              geoMode = maybeGeo,
              offset  = maybeOffset
            )
          )
        }
      }

      override def unbind(key: String, value: SimpleNodesSearchArgs): String = {
        List(
          strOptB.unbind(key + Q_SUF, value.qStr),
          geoModeB.unbind(key + GEO_SUF, value.geoMode)
        )
          .filter { s => !s.isEmpty && !s.endsWith("=") }
          .mkString("&")
      }
    }
  }

}
