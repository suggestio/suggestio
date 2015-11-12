package models.mdr

import io.suggest.model.n2.edge.MPredicates
import io.suggest.model.n2.edge.search.{Criteria, ICriteria}
import io.suggest.model.n2.node.search.{MNodeSearch, MNodeSearchDfltImpl}
import play.api.Play.{configuration, current}
import play.api.mvc.QueryStringBindable
import util.qsb.QsbKey1T

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.06.14 16:39
 * Description: Модель для представления данных запроса поиска карточек в контексте s.io-пост-модерации.
 */
object MdrSearchArgs {

  // TODO Убрать отсюда размер страницы куда-нить в контроллер/настройки.
  /** Сколько карточек на одну страницу модерации. */
  val FREE_ADVS_PAGE_SZ: Int = configuration.getInt("mdr.freeAdvs.page.size") getOrElse 10

  def PRODUCER_ID_FN          = "prodId"
  def OFFSET_FN               = "o"
  def FREE_ADV_IS_ALLOWED_FN  = "f"

  /**
   * Можно скрыть какую-нибудь карточку. Полезно скрывать только что отмодерированную, т.к. она
   * некоторое время ещё будет висеть на этой странице.
   */
  def HIDE_AD_ID_FN           = "h"


  implicit def qsb(implicit
                   strOptB   : QueryStringBindable[Option[String]],
                   intOptB   : QueryStringBindable[Option[Int]],
                   boolOptB  : QueryStringBindable[Option[Boolean]]
                  ): QueryStringBindable[MdrSearchArgs] = {
    new QueryStringBindable[MdrSearchArgs] with QsbKey1T {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MdrSearchArgs]] = {
        val k1 = key1F(key)
        for {
          maybeOffsetOpt        <- intOptB.bind  (k1(OFFSET_FN),               params)
          maybeProducerIdOpt    <- strOptB.bind  (k1(PRODUCER_ID_FN),          params)
          maybeFreeAdvIsAllowed <- boolOptB.bind (k1(FREE_ADV_IS_ALLOWED_FN),  params)
          maybeHideAdIdOpt      <- strOptB.bind  (k1(HIDE_AD_ID_FN),           params)
        } yield {
          for {
            offsetOpt           <- maybeOffsetOpt.right
            prodIdOpt           <- maybeProducerIdOpt.right
            freeAdvIsAllowed    <- maybeFreeAdvIsAllowed.right
            hideAdIdOpt         <- maybeHideAdIdOpt.right
          } yield {
            MdrSearchArgs(
              offsetOpt         = offsetOpt,
              producerId        = prodIdOpt,
              isAllowed  = freeAdvIsAllowed,
              hideAdIdOpt       = hideAdIdOpt
            )
          }
        }
      }

      override def unbind(key: String, value: MdrSearchArgs): String = {
        val k1 = key1F(key)
        Iterator(
          strOptB.unbind (k1(PRODUCER_ID_FN),          value.producerId),
          intOptB.unbind (k1(OFFSET_FN),               value.offsetOpt),
          boolOptB.unbind(k1(FREE_ADV_IS_ALLOWED_FN),  value.isAllowed),
          strOptB.unbind (k1(HIDE_AD_ID_FN),           value.hideAdIdOpt)
        )
          .filter { qv =>
            !qv.isEmpty && !qv.endsWith("=")
          }
          .mkString("&")
      }
    }
  }

  def default = MdrSearchArgs()

}


import models.mdr.MdrSearchArgs._


case class MdrSearchArgs(
  offsetOpt             : Option[Int]       = None,
  producerId            : Option[String]    = None,
  isAllowed             : Option[Boolean]   = None,
  hideAdIdOpt           : Option[String]    = None
)
{ that =>

  def offset  = offsetOpt getOrElse 0
  def limit   = FREE_ADVS_PAGE_SZ

  def toNodeSearch: MNodeSearch = {
    new MNodeSearchDfltImpl {
      override def offset  = that.offset
      override def limit   = that.limit
      override def outEdges: Seq[ICriteria] = {
        val prodCrOpt = for (prodId <- producerId) yield {
          Criteria(
            nodeIds     = Seq( prodId ),
            predicates  = Seq( MPredicates.OwnedBy ),
            must        = Some(true)
          )
        }
        // Любое состояние поля freeAdvIsAllowed является значимым и определяет результат.
        val isAllowedCr = Criteria(
          predicates  = Seq( MPredicates.ModeratedBy ),
          flag        = isAllowed,
          must        = Some(isAllowed.isDefined)
        )
        // Объеденить два критерия.
        isAllowedCr :: prodCrOpt.toList
      }
    }
  }

}
