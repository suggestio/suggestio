package util.geo.umap

import javax.inject.Inject

import io.suggest.geo.{GsTypes, MultiPolygonGs}
import io.suggest.model.n2.edge.{MNodeEdges, MPredicates}
import io.suggest.model.n2.node.MNode
import japgolly.univeq._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.09.14 9:14
 * Description: Утиль для работы с фронтендом UMap (leaflet-storage).
 */
class UmapUtil @Inject() () {

  /** 2014.09.23: Umap не поддерживает тип фигур MultiPolygon. Можно их сплиттить на полигоны. */
  private def SPLIT_MULTIPOLYGON = true


  /**
   * Из-за особенностей Umap, бывает необходимо допилить список фигур перед рендером json-слоя.
   *
   * @param nodes Коллекция или итератор фигур, которые надо рендерить в слое.
   * @return Обновлённый или тот же список фигур в исходном порядке.
   */
  def prepareDataLayerGeos(nodes: Iterable[MNode]): Iterable[MNode] = {
    // Если включена какая-то опция модификации списка geo-фигур, то нужно запустить обход списка.
    if (SPLIT_MULTIPOLYGON) {
      val p = MPredicates.NodeLocation
      for {
        // Вместо итератора явно используем lazy-коллекцию.
        mnode <- nodes.to(LazyList)
      } yield {
        val edges1iter = mnode.edges
          .iterator
          .map {
            case e if (e.predicate ==* p) && e.info.geoShapes.nonEmpty =>
              e.copy(
                info = e.info.copy(
                  geoShapes = e.info.geoShapes.flatMap {
                    case s if s.shape.shapeType == GsTypes.MultiPolygon =>
                      val mpoly = s.shape.asInstanceOf[MultiPolygonGs]
                      for (poly <- mpoly.polygons.iterator) yield {
                        s.copy(
                          shape = poly,
                          id    = -1
                        )
                      }

                    case s =>
                      s :: Nil
                  }
                )
              )

            case e => e
          }

        MNode.edges
          .composeLens( MNodeEdges.out )
          .set( MNodeEdges.edgesToMap1( edges1iter ) )(mnode)
      }

    } else {
      // Ничего менять не требуется.
      nodes
    }
  }

}
