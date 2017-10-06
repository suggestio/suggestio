package io.suggest.common.geom.coord

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.10.17 16:23
  * Description: Координата в 3d-пространстве.
  */

case class MCoords3d[V <: AnyVal](
                                   override val x: V,
                                   override val y: V,
                                   override val z: V
                                 )
  extends ICoord3d[V]

