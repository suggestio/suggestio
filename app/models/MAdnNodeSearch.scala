package models

import io.suggest.model.geo.{GeoShapeIndexed, GeoShapeQueryData}
import io.suggest.ym.model.common.AdnNodesSearchArgsT
import play.api.mvc.{RequestHeader, QueryStringBindable}
import util.PlayMacroLogsImpl
import util.qsb.QsbUtil._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.Play.{current, configuration}

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
  testNode    : Option[Boolean] = Some(false),
  withoutIds  : Seq[String] = Nil,
  geoDistance : Option[GeoShapeQueryData] = None,    // TODO Не bindable, т.к. geo=ip требует implicit request и производит future.
  intersectsWithPreIndexed: Seq[GeoShapeIndexed] = Nil,
  hasLogo     : Option[Boolean] = None,
  withGeoDistanceSort: Option[GeoPoint] = None,
  withNameSort: Boolean = false,
  shownTypes  : Seq[AdnShownType] = Nil,
  onlyWithSinks: Seq[AdnSink] = Nil,
  withRouting : Seq[String] = Nil,
  maxResults  : Int = 10,
  offset      : Int = 0
) extends AdnNodesSearchArgsT {
  override def shownTypeIds: Seq[String] = shownTypes.map(_.name)
}




/** Bindable-версия искалки. */
case class SimpleNodesSearchArgs(
  qStr            : Option[String] = None,
  geoMode         : GeoMode = GeoNone,
  maxResults      : Option[Int] = None,
  offset          : Option[Int] = None,
  currAdnId       : Option[String] = None,
  isNodeSwitch    : Boolean = false,
  withNeighbors   : Boolean = false
) {

  def toSearchArgs(glevelOpt: Option[NodeGeoLevel])(implicit request: RequestHeader): Future[AdnNodesSearchArgsT] = {
    geoMode.geoSearchInfo.map { gsiOpt =>
      new MAdnNodeSearch(
        qStr          = qStr,
        geoDistance   = gsiOpt
          .flatMap { gsi => glevelOpt.map(_ -> gsi) }
          .map { case (glevel, gsi) => GeoShapeQueryData(gsi.geoDistanceQuery, glevel) },
        withGeoDistanceSort = gsiOpt.map { _.geoPoint },
        maxResults    = maxResults getOrElse SimpleNodesSearchArgs.MAX_RESULTS_DFLT,
        offset        = offset.getOrElse(0),
        withAdnRights = Seq(AdnRights.RECEIVER),
        //withoutIds    = currAdnId.toSeq,
        withNameSort  = true
      ) {
        override def ftsSearchFN: String = AdnMMetadata.NAME_ESFN
      }
    }
  }
  
}


object SimpleNodesSearchArgs extends PlayMacroLogsImpl {

  import LOGGER._

  val Q_SUF                   = ".q"
  val GEO_SUF                 = ".geo"
  val OFFSET_SUF              = ".offset"
  val MAX_RESULTS_SUF         = ".limit"
  val CURR_ADN_ID_SUF         = ".cai"
  val NODE_SWITCH_SUF         = ".nodesw"
  val WITH_NEIGHBORS_SUF      = ".neigh"

  /** Ограничение сверху для значения max results. */
  val MAX_RESULTS_LIMIT_HARD  = configuration.getInt("nodes.search.results.max.hard") getOrElse 50

  /** Дефолтовое значение для max_results. */
  val MAX_RESULTS_DFLT        = configuration.getInt("nodes.search.results.max.dflt") getOrElse MAX_RESULTS_LIMIT_HARD

  /** Ограничение сверху на максимальный сдвиг в выдаче. */
  val OFFSET_LIMIT_HARD       = configuration.getInt("nodes.search.results.offset.max.hard") getOrElse 300

  /** Макс.длина тектового поискового запроса. */
  val QSTR_LEN_MAX            = configuration.getInt("nodes.search.qstr.len.max") getOrElse 70


  /** Урезание длины строки, если она превышает указанный предел. */
  private def limitStrLen(str: String, maxLen: Int): String = {
    if (str.length > maxLen)
      str.substring(0, maxLen)
    else
      str
  }


  /** Биндилка для simpleNodesSearchArgs, вызываемая из routes. */
  implicit def qsb(implicit strOptB: QueryStringBindable[Option[String]],  geoModeB: QueryStringBindable[GeoMode],
                   intOptB: QueryStringBindable[Option[Int]],  boolOptB: QueryStringBindable[Option[Boolean]]) = {
    new QueryStringBindable[SimpleNodesSearchArgs] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, SimpleNodesSearchArgs]] = {
        for {
          maybeQOpt       <- strOptB.bind(key +  Q_SUF, params)
          maybeGeo        <- geoModeB.bind(key + GEO_SUF, params)
          maybeOffset     <- intOptB.bind(key + OFFSET_SUF, params)
          maybeMaxResults <- intOptB.bind(key + MAX_RESULTS_SUF, params)
          maybeCurAdnId   <- strOptB.bind(key + CURR_ADN_ID_SUF, params)
          maybeNodeSwitch <- boolOptB.bind(key + NODE_SWITCH_SUF, params)
          maybeWithNeigh  <- boolOptB.bind(key + WITH_NEIGHBORS_SUF, params)
        } yield {
          trace(s"bind($key): q=$maybeQOpt ; geo=$maybeGeo ; offset = $maybeOffset ; limit = $maybeMaxResults")
          Right(
            SimpleNodesSearchArgs(
              qStr          = maybeQOpt.map(limitStrLen(_, QSTR_LEN_MAX)),
              geoMode       = maybeGeo,
              offset        = maybeOffset.filter(_ <= OFFSET_LIMIT_HARD),
              maxResults    = maybeMaxResults.filter(_ <= MAX_RESULTS_LIMIT_HARD),
              currAdnId     = maybeCurAdnId,
              isNodeSwitch  = maybeNodeSwitch getOrElse false,
              withNeighbors = maybeWithNeigh getOrElse false
            )
          )
        }
      }

      override def unbind(key: String, value: SimpleNodesSearchArgs): String = {
        List(
          strOptB.unbind(key + Q_SUF, value.qStr),
          geoModeB.unbind(key + GEO_SUF, value.geoMode),
          intOptB.unbind(key + OFFSET_SUF, value.offset),
          intOptB.unbind(key + MAX_RESULTS_SUF, value.maxResults),
          strOptB.unbind(key + CURR_ADN_ID_SUF, value.currAdnId),
          boolOptB.unbind(key + NODE_SWITCH_SUF, Some(value.isNodeSwitch)),
          boolOptB.unbind(key + WITH_NEIGHBORS_SUF, Some(value.withNeighbors))
        )
          .filter { s => !s.isEmpty && !s.endsWith("=") }
          .mkString("&")
      }
    }
  }

}
