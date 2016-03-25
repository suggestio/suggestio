package util.geo.umap

import com.google.inject.Inject
import io.suggest.model.geo.{GsTypes, MultiPolygonGs}
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
   * @param nodes Коллекция или итератор фигур, которые надо рендерить в слое.
   * @return Обновлённый или тот же список фигур в исходном порядке.
   */
  def prepareDataLayerGeos(nodes: TraversableOnce[MNode]): TraversableOnce[MNode] = {
    // Если включена какая-то опция модификации списка geo-фигур, то нужно запустить обход списка.
    if (SPLIT_MULTIPOLYGON) {
      for {
        mnode <- nodes
      } yield {
        val shapes1 =
          mnode.geo.shapes
            .iterator
            .flatMap { shape =>
              // Если включена трансформация мультирополигонов (Umap их не поддерживала), то размножаем инстанс шейпа на полигоны:
              if (shape.shape.shapeType == GsTypes.multipolygon) {
                val mpoly = shape.shape.asInstanceOf[MultiPolygonGs]
                mpoly.polygons.map { poly =>
                  shape.copy(
                    shape = poly,
                    id    = -1
                  )
                }
              } else {
                Seq(shape)
              }
            }
            .toSeq
          mnode.copy(
            geo = mnode.geo.copy(
              shapes = shapes1
            )
          )
      }

    } else {
      // Ничего менять не требуется.
      nodes
    }
  }

}
