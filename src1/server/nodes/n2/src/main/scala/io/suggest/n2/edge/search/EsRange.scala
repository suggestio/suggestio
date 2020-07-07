package io.suggest.n2.edge.search

import java.time.{OffsetDateTime, ZoneOffset}

import enumeratum._
import io.suggest.es.model.{IMust, Must_t}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.07.2020 15:27
  * Description: Описание интервался (range) на scala.
  */
final case class EsRange(
                          rangeClauses      : Seq[EsRangeClause],
                          boost             : Option[Float]           = None,
                          dateFormat        : Option[String]          = None,
                          timeZone          : Option[ZoneOffset]      = None,
                          must              : Must_t                  = IMust.SHOULD,
                        )


final class EsRangeClause private(
                                   val op     : EsRangeOp,
                                   val value  : Any,
                                 )
object EsRangeClause {

  case class op(op: EsRangeOp) {
    def value( s: String ) = new EsRangeClause(op, s)
    def value( dt: OffsetDateTime ) = new EsRangeClause( op, dt )
  }

}


/** Оператор range-границы. */
sealed abstract class EsRangeOp extends EnumEntry
object EsRangeOps extends Enum[EsRangeOp] {

  case object `<` extends EsRangeOp
  case object `<=` extends EsRangeOp
  case object `>` extends EsRangeOp
  case object `>=` extends EsRangeOp

  override def values = findValues

}
