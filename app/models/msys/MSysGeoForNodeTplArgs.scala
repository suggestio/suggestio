package models.msys

import io.suggest.model.n2.edge.{MEdgeGeoShape, MPredicates}
import models.MNode

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.10.15 22:39
 * Description: Модель аргументов для шаблона [[views.html.sys1.market.adn.geo.forNodeTpl]].
 */
trait ISysGeoForNodeTplArgs {

  def mnode         : MNode

  def parentsMap    : Map[String, MNode]

  def mapStateHash  : Option[String]

  def shapes: Iterator[MEdgeGeoShape] = {
    mnode.edges
      .withPredicateIter( MPredicates.NodeLocation )
      .flatMap(_.info.geoShapes)
  }

  def countGeoJsonCompat: Int = {
    shapes
      .count( _.shape.shapeType.isGeoJsonCompatible )
  }

}


case class MSysGeoForNodeTplArgs(
  override val mnode         : MNode,
  override val parentsMap    : Map[String, MNode],
  override val mapStateHash  : Option[String]        = None
)
  extends ISysGeoForNodeTplArgs
