package models.mdr

import io.suggest.ym.model.ad.MdrSearchArgsI
import play.api.Play.{configuration, current}
import play.api.mvc.QueryStringBindable
import util.qsb.QsbKey1T
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

  def PRODUCER_ID_FN          = "producerId"
  def PAGE_ID_FN              = "page"
  def FREE_ADV_IS_ALLOWED_FN  = "fa.ia"

  implicit def queryStringBinder(implicit strOptBinder: QueryStringBindable[Option[String]],
                                 intBinder: QueryStringBindable[Int],
                                 boolOptBinder: QueryStringBindable[Option[Boolean]]) = {
    new QueryStringBindable[MdrSearchArgs] with QsbKey1T {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MdrSearchArgs]] = {
        val k1 = key1F(key)
        for {
          maybeProducerIdOpt    <- strOptBinder.bind  (k1(PRODUCER_ID_FN),          params)
          maybePage             <- intBinder.bind     (k1(PAGE_ID_FN),              params)
          maybeFreeAdvIsAllowed <- boolOptBinder.bind (k1(FREE_ADV_IS_ALLOWED_FN),  params)
        } yield {
          Right(
            MdrSearchArgs(
              page        = maybePage.fold({_ => 0}, identity),
              producerId  = maybeProducerIdOpt,
              freeAdvIsAllowed = maybeFreeAdvIsAllowed
            )
          )
        }
      }

      override def unbind(key: String, value: MdrSearchArgs): String = {
        val k1 = key1F(key)
        Iterator(
          strOptBinder.unbind (k1(PRODUCER_ID_FN),          value.producerId),
          intBinder.unbind    (k1(PAGE_ID_FN),              value.page),
          boolOptBinder.unbind(k1(FREE_ADV_IS_ALLOWED_FN),  value.freeAdvIsAllowed)
        )
          .filter { qv => !qv.isEmpty && !qv.endsWith("=") }
          .mkString("&")
      }
    }
  }

}


import models.mdr.MdrSearchArgs._


case class MdrSearchArgs(
  page              : Int             = 0,
  producerId        : Option[String]  = None,
  freeAdvIsAllowed  : Option[Boolean] = None
) extends MdrSearchArgsI {

  def maxResults = FREE_ADVS_PAGE_SZ
  def offset = page * FREE_ADVS_PAGE_SZ

}
