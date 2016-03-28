package util.geo.umap

import com.google.inject.Inject
import io.suggest.model.geo.{GsTypes, MultiPolygonGs}
import io.suggest.model.n2.edge.{MNodeEdges, MPredicates}
import models.MNode
import play.api.Configuration

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.09.14 9:14
 * Description: Утиль для работы с фронтендом UMap (leaflet-storage).
 */

class UmapUtil @Inject() (
  configuration: Configuration
) {

  /** 2014.09.23: Umap не поддерживает тип фигур MultiPolygon. Можно их сплиттить на полигоны. */
  val SPLIT_MULTIPOLYGON: Boolean = configuration.getBoolean("umap.mpoly.split") getOrElse true


  /**
   * Из-за особенностей Umap, бывает необходимо допилить список фигур перед рендером json-слоя.
   *
   * @param nodes Коллекция или итератор фигур, которые надо рендерить в слое.
   * @return Обновлённый или тот же список фигур в исходном порядке.
   */
  def prepareDataLayerGeos(nodes: TraversableOnce[MNode]): TraversableOnce[MNode] = {
    // Если включена какая-то опция модификации списка geo-фигур, то нужно запустить обход списка.
    if (SPLIT_MULTIPOLYGON) {
      for (mnode <- nodes) yield {
        val p = MPredicates.NodeLocation

        val edges1iter = mnode.edges
          .iterator
          .map {
            case e if e.predicate == p && e.info.geoShapes.nonEmpty =>
              e.copy(
                info = e.info.copy(
                  geoShapes = e.info.geoShapes.flatMap {
                    case s if s.shape.shapeType == GsTypes.multipolygon =>
                      val mpoly = s.shape.asInstanceOf[MultiPolygonGs]
                      for (poly <- mpoly.polygons.iterator) yield {
                        s.copy(
                          shape = poly,
                          id    = -1
                        )
                      }

                    case s => Seq(s)
                  }
                )
              )

            case e => e
          }

        mnode.copy(
          edges = mnode.edges.copy(
            out = MNodeEdges.edgesToMap1( edges1iter )
          )
        )
      }

    } else {
      // Ничего менять не требуется.
      nodes
    }
  }

}
