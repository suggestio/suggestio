package util.qsb

import play.api.mvc.QueryStringBindable
import models._
import play.api.Play.{current, configuration}
import io.suggest.ym.model.ad.AdsSearchArgsT

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

import QsbUtil._

object QSBs {

  private def companyNameSuf = ".name"

  /** qsb для MCompany. */
  implicit def mcompanyQSB(implicit strBinder: QueryStringBindable[String]) = {
    new QueryStringBindable[MCompany] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MCompany]] = {
        for {
          maybeCompanyName <- strBinder.bind(key + companyNameSuf, params)
        } yield {
          maybeCompanyName.right.map { companyName =>
            MCompany(name = companyName)
          }
        }
      }

      override def unbind(key: String, value: MCompany): String = {
        strBinder.unbind(key + companyNameSuf, value.name)
      }
    }
  }

}

// TODO Перенести AdSearch в models.

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
          maybeRcvrIdOpt <- strOptBinder.bind(key + ".rcvr", params)

        } yield {
          Right(
            AdSearch(
              receiverIds = maybeRcvrIdOpt,
              producerIds = maybeShopIdOpt,
              catIds  = maybeCatIdOpt,
              levels  = eitherOpt2list(maybeLevelOpt).flatMap(AdShowLevels.maybeWithName),
              qOpt      = maybeQOpt,
              maxResultsOpt = eitherOpt2option(maybeSizeOpt) map { size =>
                Math.max(4,  Math.min(size, MAX_RESULTS_PER_RESPONSE))
              },
              offsetOpt = eitherOpt2option(maybeOffsetOpt) map { offset =>
                Math.max(0,  Math.min(offset,  MAX_PAGE_OFFSET * maybeSizeOpt.getOrElse(10)))
              }
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
          intOptBinder.unbind(key + ".size", value.maxResultsOpt),
          intOptBinder.unbind(key + ".offset", value.offsetOpt)
        ) .filter(!_.isEmpty)
          .mkString("&")
      }
    }
  }

}

case class AdSearch(
  receiverIds : List[String] = Nil,
  producerIds : List[String] = Nil,
  catIds      : List[String] = Nil,
  levels      : List[AdShowLevel] = Nil,
  qOpt: Option[String] = None,
  maxResultsOpt: Option[Int] = None,
  offsetOpt: Option[Int] = None
) extends AdsSearchArgsT {

  /** Абсолютный сдвиг в результатах (постраничный вывод). */
  def offset: Int = if (offsetOpt.isDefined) offsetOpt.get else 0

  /** Макс.кол-во результатов. */
  def maxResults: Int = maxResultsOpt getOrElse AdSearch.MAX_RESULTS_DFLT
}

