package io.suggest.model.n2.edge.search

import io.suggest.model.geo.IToEsQueryFn
import io.suggest.ym.model.NodeGeoLevel

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.03.16 11:14
  * Description: Модель для поиска/фильтрации по
  */
trait IGsCriteria {

  /** Есть указанный/любой шейп на указанных гео-уровнях. */
  def levels: Seq[NodeGeoLevel]

  /** Геошейпы, по которым нужно матчить. */
  def shapes: Seq[IToEsQueryFn]

  /** Искать/фильтровать по флагу совместимости с GeoJSON. */
  def gjsonCompat: Option[Boolean]

  override def toString: String = {
    getClass.getSimpleName +
      "([" + levels.mkString(",") + "]," +
      shapes.size + "gs," +
      gjsonCompat.fold("")(_.toString) + ")"
  }

}


/** Дефолтовая реализация модели [[IGsCriteria]]. */
case class GsCriteria(
  override val levels             : Seq[NodeGeoLevel]   = Nil,
  override val shapes             : Seq[IToEsQueryFn]   = Nil,
  override val gjsonCompat        : Option[Boolean]     = None
)
  extends IGsCriteria
{
  override def toString = super.toString
}