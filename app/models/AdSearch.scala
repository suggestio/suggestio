package models


import play.api.mvc.QueryStringBindable
import play.api.Play.{current, configuration}
import io.suggest.ym.model.ad.AdsSearchArgsT
import util.qsb.QsbUtil._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.07.14 16:05
 * Description: Модель представления поискового запроса.
 */

object AdSearch {

  /** Максимальное число результатов в ответе на запрос (макс. результатов на странице). */
  val MAX_RESULTS_PER_RESPONSE = configuration.getInt("market.search.ad.results.max") getOrElse 50

  /** Кол-во результатов на страницу по дефолту. */
  val MAX_RESULTS_DFLT = configuration.getInt("market.search.ad.results.count.dflt") getOrElse 20

  /** Макс.кол-во сдвигов в страницах. */
  val MAX_PAGE_OFFSET = configuration.getInt("market.search.ad.results.offset.max") getOrElse 20

  private implicit def eitherOpt2list[T](e: Either[_, Option[T]]): List[T] = {
    e match {
      case Left(_)  => Nil
      case Right(b) => b.toList
    }
  }

  /** QSB для экземпляра сабжа. Неявно дергается из routes. */
  implicit def queryStringBinder(implicit strOptBinder: QueryStringBindable[Option[String]],
                                 intOptB: QueryStringBindable[Option[Int]],
                                 longOptB: QueryStringBindable[Option[Long]],
                                 geoModeB: QueryStringBindable[GeoMode]) = {
    new QueryStringBindable[AdSearch] {

      def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, AdSearch]] = {
        for {
          maybeProdIdOpt <- strOptBinder.bind(key + ".shopId", params)
          maybeCatIdOpt  <- strOptBinder.bind(key + ".catId", params)
          maybeLevelOpt  <- strOptBinder.bind(key + ".level", params)
          maybeQOpt      <- strOptBinder.bind(key + ".q", params)
          maybeSizeOpt   <- intOptB.bind(key + ".size", params)
          maybeOffsetOpt <- intOptB.bind(key + ".offset", params)
          maybeRcvrIdOpt <- strOptBinder.bind(key + ".rcvr", params)
          maybeFirstId   <- strOptBinder.bind(key + ".firstAdId", params)
          maybeGen       <- longOptB.bind(key + ".gen", params)
          maybeGeo       <- geoModeB.bind(key + ".geo", params)

        } yield {
          Right(
            AdSearch(
              receiverIds = maybeRcvrIdOpt,
              producerIds = maybeProdIdOpt,
              catIds      = maybeCatIdOpt,
              levels      = eitherOpt2list(maybeLevelOpt).flatMap(AdShowLevels.maybeWithName),
              qOpt        = maybeQOpt,
              maxResultsOpt = eitherOpt2option(maybeSizeOpt) map { size =>
                Math.max(4,  Math.min(size, MAX_RESULTS_PER_RESPONSE))
              },
              offsetOpt   = eitherOpt2option(maybeOffsetOpt) map { offset =>
                Math.max(0,  Math.min(offset,  MAX_PAGE_OFFSET * maybeSizeOpt.getOrElse(10)))
              },
              forceFirstIds = maybeFirstId,
              generation  = maybeGen,
              geo         = maybeGeo
            )
          )
        }
      }

      def unbind(key: String, value: AdSearch): String = {
        List(
          strOptBinder.unbind(key + ".rcvr", value.receiverIds.headOption),   // TODO Разбиндивать на весь список receivers сразу надо
          strOptBinder.unbind(key + ".shopId", value.producerIds.headOption), // TODO Разбиндивать на весь список producers сразу надо.
          strOptBinder.unbind(key + ".catId", value.catIds.headOption),       // TODO Разбиндивать на весь список catIds надо бы
          strOptBinder.unbind(key + ".level", value.levels.headOption.map(_.toString)),
          strOptBinder.unbind(key + ".q", value.qOpt),
          intOptB.unbind(key + ".size", value.maxResultsOpt),
          intOptB.unbind(key + ".offset", value.offsetOpt),
          strOptBinder.unbind(key + ".firstAdId", value.forceFirstIds.headOption),
          longOptB.unbind(key + ".gen", value.generation),
          strOptBinder.unbind(key + ".geo", value.geo.toQsStringOpt)
        ) .filter(!_.isEmpty)
          .mkString("&")
      }
    }
  }

}


case class AdSearch(
  receiverIds   : List[String] = Nil,
  producerIds   : List[String] = Nil,
  catIds        : List[String] = Nil,
  levels        : List[AdShowLevel] = Nil,
  qOpt          : Option[String] = None,
  maxResultsOpt : Option[Int] = None,
  offsetOpt     : Option[Int] = None,
  forceFirstIds : List[String] = Nil,
  generation    : Option[Long] = None,
  withoutIds    : List[String] = Nil,
  geo           : GeoMode = GeoNone
) extends AdsSearchArgsT {

  /** Абсолютный сдвиг в результатах (постраничный вывод). */
  def offset: Int = if (offsetOpt.isDefined) offsetOpt.get else 0

  /** Макс.кол-во результатов. */
  def maxResults: Int = maxResultsOpt getOrElse AdSearch.MAX_RESULTS_DFLT

}

