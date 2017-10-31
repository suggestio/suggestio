package models.msys

import io.suggest.model.play.qsb.QueryStringBindableImpl
import models.{AdnShownType, MNodeType}
import play.api.mvc.QueryStringBindable

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.10.15 21:25
 * Description: Модель qs-аргументов экшена [[controllers.SysMarket.adnNodesList()]].
 */
object MSysNodeListArgs {

  def NTYPE_FN      = "nt"
  def SHOWN_TYPE_FN = "sti"
  def LIMIT_FN      = "l"
  def OFFSET_FN     = "o"

  // TODO Выставлено 1000 пока нет нормального поиска нод в списке
  def LIMIT_DFLT    = 1000
  def OFFSET_DFLT   = 0

  implicit def mSysNodeListArgsQsb(implicit
                                   ntypeOptB  : QueryStringBindable[Option[MNodeType]],
                                   stiOptB    : QueryStringBindable[Option[AdnShownType]],
                                   intOptB    : QueryStringBindable[Option[Int]]
                                  ): QueryStringBindable[MSysNodeListArgs] = {
    new QueryStringBindableImpl[MSysNodeListArgs] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MSysNodeListArgs]] = {
        val k = key1F(key)
        for {
          ntypeOptEith      <- ntypeOptB  .bind(k(NTYPE_FN),        params)
          stiOptEith        <- stiOptB    .bind(k(SHOWN_TYPE_FN),   params)
          limitOptEith      <- intOptB    .bind(k(LIMIT_FN),        params)
          offsetOptEith     <- intOptB    .bind(k(OFFSET_FN),       params)
        } yield {
          for {
            ntypeOpt        <- ntypeOptEith.right
            stiOpt          <- stiOptEith.right
            limitOpt        <- limitOptEith.right
            offsetOpt       <- offsetOptEith.right
          } yield {
            MSysNodeListArgs(
              ntypeOpt = ntypeOpt,
              stiOpt   = stiOpt,
              limit    = limitOpt getOrElse LIMIT_DFLT,
              offset   = offsetOpt getOrElse OFFSET_DFLT
            )
          }
        }
      }

      override def unbind(key: String, value: MSysNodeListArgs): String = {
        val k = key1F(key)
        _mergeUnbinded1(
          ntypeOptB .unbind(k(NTYPE_FN),      value.ntypeOpt),
          stiOptB   .unbind(k(SHOWN_TYPE_FN), value.stiOpt),
          intOptB   .unbind(k(LIMIT_FN),      if (value.limit != LIMIT_DFLT) Some(value.limit) else None),
          intOptB   .unbind(k(OFFSET_FN),     if (value.offset > OFFSET_DFLT) Some(value.offset) else None)
        )
      }
    }
  }


  def default = MSysNodeListArgs()

}


case class MSysNodeListArgs(
  ntypeOpt  : Option[MNodeType]     = None,
  stiOpt    : Option[AdnShownType]  = None,
  limit     : Int                   = MSysNodeListArgs.LIMIT_DFLT,
  offset    : Int                   = MSysNodeListArgs.OFFSET_DFLT
)
