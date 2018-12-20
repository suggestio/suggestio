package io.suggest.common.geom.coord

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.10.17 16:23
  * Description: Координата в 3d-пространстве.
  */
object MCoords3d {

  /** Поддержка API coord3d. */
  implicit def Coord3dHelper[V <: AnyVal: Numeric]: ICoord3dHelper[MCoords3d[V], V] = {
    new ICoord3dHelper[MCoords3d[V], V] {
      override def getZ(t: MCoords3d[V]) = t.z
      override def getY(t: MCoords3d[V]) = t.y
      override def getX(t: MCoords3d[V]) = t.x
    }
  }

}

case class MCoords3d[V <: AnyVal]( x: V,  y: V,  z: V )
