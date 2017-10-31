package models.mdr

import io.suggest.es.model.IMust
import io.suggest.es.search.{ILimit, IOffset}
import io.suggest.model.n2.edge.MPredicates
import io.suggest.model.n2.edge.search.{Criteria, ICriteria}
import io.suggest.model.n2.node.MNodeTypes
import io.suggest.model.n2.node.search.{MNodeSearch, MNodeSearchDfltImpl}
import io.suggest.model.play.qsb.QueryStringBindableImpl
import play.api.mvc.QueryStringBindable

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.06.14 16:39
 * Description: Модель для представления данных запроса поиска карточек в контексте s.io-пост-модерации.
 */
object MdrSearchArgs {

  def PRODUCER_ID_FN          = "prodId"
  def OFFSET_FN               = "o"
  def FREE_ADV_IS_ALLOWED_FN  = "f"

  /**
   * Можно скрыть какую-нибудь карточку. Полезно скрывать только что отмодерированную, т.к. она
   * некоторое время ещё будет висеть на этой странице.
   */
  def HIDE_AD_ID_FN           = "h"


  implicit def mdrSearchArgsQsb(implicit
                                strOptB   : QueryStringBindable[Option[String]],
                                intOptB   : QueryStringBindable[Option[Int]],
                                boolOptB  : QueryStringBindable[Option[Boolean]]
                               ): QueryStringBindable[MdrSearchArgs] = {
    new QueryStringBindableImpl[MdrSearchArgs] {
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
              isAllowed         = freeAdvIsAllowed,
              hideAdIdOpt       = hideAdIdOpt
            )
          }
        }
      }

      override def unbind(key: String, value: MdrSearchArgs): String = {
        val k1 = key1F(key)
        _mergeUnbinded1(
          strOptB.unbind (k1(PRODUCER_ID_FN),          value.producerId),
          intOptB.unbind (k1(OFFSET_FN),               value.offsetOpt),
          boolOptB.unbind(k1(FREE_ADV_IS_ALLOWED_FN),  value.isAllowed),
          strOptB.unbind (k1(HIDE_AD_ID_FN),           value.hideAdIdOpt)
        )
      }
    }
  }

  def default = MdrSearchArgs()

}


case class MdrSearchArgs(
  offsetOpt             : Option[Int]       = None,
  producerId            : Option[String]    = None,
  isAllowed             : Option[Boolean]   = None,
  hideAdIdOpt           : Option[String]    = None,
  // limit не задаваем через qs, не было такой необходимости.
  limitOpt              : Option[Int]       = None
)
  extends ILimit
  with IOffset
{ that =>

  override def offset  = offsetOpt.getOrElse(0)
  override def limit   = limitOpt.getOrElse(12)      // 3 ряда по 4 карточки.

  /** Реализация класса аргументов поиска карточек. */
  protected class MdrNodeSearchArgs extends MNodeSearchDfltImpl {

    /** Интересуют только карточки. */
    override def nodeTypes = Seq( MNodeTypes.Ad )

    override def offset  = that.offset
    override def limit   = that.limit

    override def outEdges: Seq[ICriteria] = {
      val must = IMust.MUST

      // Собираем self-receiver predicate, поиск бесплатных размещений начинается с этого
      val srp = Criteria(
        predicates  = Seq( MPredicates.Receiver.Self ),
        must        = must
      )

      // Любое состояние эджа модерации является значимым и определяет результат.
      val isAllowedCr = Criteria(
        predicates  = Seq( MPredicates.ModeratedBy ),
        flag        = isAllowed,
        must        = Some(isAllowed.isDefined)
      )

      var crs = List[Criteria](srp, isAllowedCr)

      // Если задан продьюсер, то закинуть и его в общую кучу.
      for (prodId <- producerId) {
        crs ::= Criteria(
          predicates  = Seq( MPredicates.OwnedBy ),
          nodeIds     = Seq( prodId ),
          must        = must
        )
      }

      crs
    }

    override def withoutIds = hideAdIdOpt.toSeq
  }

  def toNodeSearch: MNodeSearch = {
    new MdrNodeSearchArgs
  }

}
