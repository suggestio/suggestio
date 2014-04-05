package util.qsb

import play.api.mvc.QueryStringBindable
import models._
import play.api.Play.current
import io.suggest.ym.model.ad.AdsSearchArgsT

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.03.14 17:48
 * Description: Здесь складываются небольшие QueryStringBindable для сложных get-реквестов.
 */

object AdSearch {

  /** Максимальное число результатов в ответе на запрос (макс. результатов на странице). */
  val MAX_RESULTS_PER_RESPONSE = current.configuration.getInt("market.search.ad.results.max") getOrElse 50

  /** Макс.кол-во сдвигов в страницах. */
  val MAX_PAGE_OFFSET = current.configuration.getInt("market.search.ad.results.offset.max") getOrElse 20


  private implicit def eitherOpt2option[T](e: Either[_, Option[T]]): Option[T] = {
    e match {
      case Left(_)  => None
      case Right(b) => b
    }
  }

  implicit def queryStringBinder(implicit strOptBinder: QueryStringBindable[Option[String]], intOptBinder: QueryStringBindable[Option[Int]]) = {
    new QueryStringBindable[AdSearch] {
      def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, AdSearch]] = {
        for {
          maybeShopIdOpt <- strOptBinder.bind(key + ".shopId", params)
          maybeCatIdOpt  <- strOptBinder.bind(key + ".catId", params)
          maybeLevelOpt  <- strOptBinder.bind(key + ".level", params)
          maybeQOpt      <- strOptBinder.bind(key + ".q", params)
          maybeSizeOpt   <- intOptBinder.bind(key + ".size", params)
          maybeOffsetOpt <- intOptBinder.bind(key + ".offset", params)

        } yield {
          Right(
            AdSearch(
              producerIdOpt = maybeShopIdOpt,
              catIdOpt  = maybeCatIdOpt,
              levelOpt  = maybeLevelOpt.flatMap(AdShowLevels.maybeWithName),
              qOpt      = maybeQOpt,
              maxResultsOpt = maybeSizeOpt map { size =>
                Math.max(4,  Math.min(size, MAX_RESULTS_PER_RESPONSE))
              },
              offsetOpt = maybeOffsetOpt map { offset =>
                Math.max(0,  Math.min(offset,  MAX_PAGE_OFFSET * maybeSizeOpt.getOrElse(10)))
              }
            )
          )
        }
      }

      def unbind(key: String, value: AdSearch): String = {
        strOptBinder.unbind(key + ".shopId", value.producerIdOpt) + "&" +
        strOptBinder.unbind(key + ".catId", value.catIdOpt) + "&" +
        strOptBinder.unbind(key + ".level", value.levelOpt.map(_.toString)) + "&" +
        strOptBinder.unbind(key + ".q", value.qOpt) +
        intOptBinder.unbind(key + ".size", value.maxResultsOpt) +
        intOptBinder.unbind(key + ".offset", value.offsetOpt)
      }
    }
  }

}

case class AdSearch(
  producerIdOpt: Option[ShopId_t] = None,
  catIdOpt: Option[String] = None,
  levelOpt: Option[AdShowLevel] = None,
  qOpt: Option[String] = None,
  maxResultsOpt: Option[Int] = None,
  offsetOpt: Option[Int] = None
) extends AdsSearchArgsT {

  /** Абсолютный сдвиг в результатах (постраничный вывод). */
  def offset: Int = if (offsetOpt.isDefined) offsetOpt.get else 0

  /** Макс.кол-во результатов. */
  def maxResults: Int = if (maxResultsOpt.isDefined) maxResultsOpt.get else 10
}

