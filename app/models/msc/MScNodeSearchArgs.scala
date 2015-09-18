package models.msc

import models._
import play.api.Play.{configuration, current}
import play.api.mvc.QueryStringBindable
import util.qsb.QsbKey1T
import util.qsb.QsbUtil._
import io.suggest.sc.NodeSearchConstants._
import io.suggest.sc.ScConstants.ReqArgs.VSN
import views.js.sc.m._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 12.08.14 16:49
 * Description: Модель контейнера аргументов для запроса списка узлов выдачи.
 */
case class MScNodeSearchArgs(
  qStr            : Option[String]  = None,
  geoMode         : GeoMode         = GeoNone,
  maxResults      : Option[Int]     = None,
  offset          : Option[Int]     = None,
  currAdnId       : Option[String]  = None,
  isNodeSwitch    : Boolean         = false,
  withNeighbors   : Boolean         = false,
  apiVsn          : MScApiVsn       = MScApiVsns.unknownVsn
)


object MScNodeSearchArgs {

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
  implicit def qsb(implicit
                   strOptB  : QueryStringBindable[Option[String]],
                   geoModeB : QueryStringBindable[GeoMode],
                   intOptB  : QueryStringBindable[Option[Int]],
                   boolOptB : QueryStringBindable[Option[Boolean]],
                   apiVsnB  : QueryStringBindable[MScApiVsn]
                  ): QueryStringBindable[MScNodeSearchArgs] = {

    new QueryStringBindable[MScNodeSearchArgs] with QsbKey1T {

      /** Сбиндить query string в экземпляр [[MScNodeSearchArgs]] */
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MScNodeSearchArgs]] = {
        val k1 = key1F(key)
        for {
          maybeQOpt       <- strOptB.bind   (k1(FTS_QUERY_FN),      params)
          maybeGeo        <- geoModeB.bind  (k1(GEO_FN),            params)
          maybeOffset     <- intOptB.bind   (k1(OFFSET_FN),         params)
          maybeMaxResults <- intOptB.bind   (k1(LIMIT_FN),          params)
          maybeCurAdnId   <- strOptB.bind   (k1(CURR_ADN_ID_FN),    params)
          maybeNodeSwitch <- boolOptB.bind  (k1(NODE_SWITCH_FN),    params)
          maybeWithNeigh  <- boolOptB.bind  (k1(WITH_NEIGHBORS_FN), params)
          maybeApiVsn     <- apiVsnB.bind   (k1(VSN),               params)
        } yield {
          for {
            apiVsn  <- maybeApiVsn.right
          } yield {
            //trace(s"bind($key): q=$maybeQOpt ; geo=$maybeGeo ; offset = $maybeOffset ; limit = $maybeMaxResults")
            MScNodeSearchArgs(
              qStr          = maybeQOpt.map(limitStrLen(_, QSTR_LEN_MAX)),
              geoMode       = maybeGeo,
              offset        = maybeOffset.filter(_ <= OFFSET_LIMIT_HARD),
              maxResults    = maybeMaxResults.filter(_ <= MAX_RESULTS_LIMIT_HARD),
              currAdnId     = maybeCurAdnId,
              isNodeSwitch  = maybeNodeSwitch getOrElse false,
              withNeighbors = maybeWithNeigh getOrElse true,
              apiVsn        = apiVsn
            )
          }
        }
      }

      /** Разбиндить экземплря [[MScNodeSearchArgs]]. */
      override def unbind(key: String, value: MScNodeSearchArgs): String = {
        val k1 = key1F(key)
        Iterator(
          strOptB.unbind  (k1(FTS_QUERY_FN),      value.qStr),
          geoModeB.unbind (k1(GEO_FN),            value.geoMode),
          intOptB.unbind  (k1(OFFSET_FN),         value.offset),
          intOptB.unbind  (k1(LIMIT_FN),          value.maxResults),
          strOptB.unbind  (k1(CURR_ADN_ID_FN),    value.currAdnId),
          boolOptB.unbind (k1(NODE_SWITCH_FN),    Some(value.isNodeSwitch)),
          boolOptB.unbind (k1(WITH_NEIGHBORS_FN), Some(value.withNeighbors)),
          apiVsnB.unbind  (k1(VSN),               value.apiVsn)
        )
          .filter { s => !s.isEmpty && !s.endsWith("=") }
          .mkString("&")
      }

      /** Разбиндить модель на стороне клиента. */
      override def javascriptUnbind: String = {
        scNodeSearchArgsJsUnbindTpl(KEY_DELIM).body
      }
    }
  }

}
