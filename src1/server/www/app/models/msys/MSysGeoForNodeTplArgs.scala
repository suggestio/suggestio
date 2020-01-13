package models.msys

import io.suggest.n2.edge.{MEdgeGeoShape, MPredicates}
import io.suggest.n2.node.MNode

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.10.15 22:39
 * Description: Модель аргументов для шаблона [[views.html.sys1.market.adn.geo.forNodeTpl]].
 */
case class MSysGeoForNodeTplArgs(
                                  mnode         : MNode,
                                  mapStateHash  : Option[String]
                                ) {

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
