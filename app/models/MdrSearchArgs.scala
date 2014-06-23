package models

import io.suggest.ym.model.ad.MdrSearchArgsI
import play.api.Play.{current, configuration}
import play.api.mvc.QueryStringBindable
import util.qsb.QsbUtil._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.06.14 16:39
 * Description: Модель для представления данных запроса поиска карточек в контексте s.io-пост-модерации.
 */
object MdrSearchArgs {

  /** Сколько карточек на одну страницу модерации. */
  val FREE_ADVS_PAGE_SZ: Int = configuration.getInt("mdr.freeAdvs.page.size") getOrElse 10

  val PRODUCER_ID_SUF = ".producerId"
  val PAGE_ID_SUF     = ".page"
  val FREE_ADV_IS_ALLOWED_SUF = ".fa.ia"

  implicit def queryStringBinder(implicit strOptBinder: QueryStringBindable[Option[String]],
                                 intBinder: QueryStringBindable[Int],
                                 boolOptBinder: QueryStringBindable[Option[Boolean]]) = {
    new QueryStringBindable[MdrSearchArgs] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MdrSearchArgs]] = {
        for {
          maybeProducerIdOpt    <- strOptBinder.bind(key + PRODUCER_ID_SUF, params)
          maybePage             <- intBinder.bind(key + PAGE_ID_SUF, params)
          maybeFreeAdvIsAllowed <- boolOptBinder.bind(key + FREE_ADV_IS_ALLOWED_SUF, params)
        } yield {
          Right(
            MdrSearchArgs(
              page = maybePage.fold({_ => 0}, identity),
              producerId = maybeProducerIdOpt,
              freeAdvIsAllowed = maybeFreeAdvIsAllowed
            )
          )
        }
      }

      override def unbind(key: String, value: MdrSearchArgs): String = {
        List(
          strOptBinder.unbind(key + PRODUCER_ID_SUF, value.producerId),
          intBinder.unbind(key + PAGE_ID_SUF, value.page),
          boolOptBinder.unbind(key + FREE_ADV_IS_ALLOWED_SUF, value.freeAdvIsAllowed)
        )
          .filter { qv => !qv.isEmpty && !qv.endsWith("=") }
          .mkString("&")
      }
    }
  }

}


import MdrSearchArgs._


case class MdrSearchArgs(
  page: Int = 0,
  producerId: Option[String] = None,
  freeAdvIsAllowed: Option[Boolean] = None
) extends MdrSearchArgsI {

  def maxResults = FREE_ADVS_PAGE_SZ
  def offset = page * FREE_ADVS_PAGE_SZ

}
