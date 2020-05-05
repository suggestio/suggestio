package io.suggest.stat.m

import io.suggest.common.empty.{EmptyProduct, IEmpty}
import io.suggest.common.geom.d2.MOrientation2d
import io.suggest.es.{IEsMappingProps, MappingDsl}
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.09.16 12:27
  * Description: Суб-модель для описания данных экрана устройства.
  */
object MStatScreen
  extends IEsMappingProps
  with IEmpty
{

  override type T = MStatScreen

  object Fields {

    def ORIENTATION_FN          = "orient"
    def VPORT_PHYS_FN           = "phys"
    def VPORT_QUANTED_FN        = "choosen"

  }


  import Fields._

  implicit val FORMAT: OFormat[MStatScreen] = (
    (__ \ ORIENTATION_FN).formatNullable[MOrientation2d] and
    (__ \ VPORT_PHYS_FN).formatNullable[MViewPort] and
    (__ \ VPORT_QUANTED_FN).formatNullable[MViewPort]
  )(apply, unlift(unapply))


  override def empty = apply()

  override def esMappingProps(implicit dsl: MappingDsl): JsObject = {
    import dsl._

    val vportFields = MViewPort.esMappingProps

    val F = Fields
    Json.obj(
      F.ORIENTATION_FN    -> FKeyWord.indexedJs,
      F.VPORT_PHYS_FN     -> vportFields,
      F.VPORT_QUANTED_FN  -> vportFields,
    )
  }

}


final case class MStatScreen(
  orientation   : Option[MOrientation2d]  = None,
  vportPhys     : Option[MViewPort]       = None,
  vportQuanted  : Option[MViewPort]       = None
)
  extends EmptyProduct
{

  def toStringSb(sb: StringBuilder = new StringBuilder(32)): StringBuilder = {
    sb.append('(')

    for (orient <- orientation)
      sb.append(orient)
        .append(' ')

    for (vpPhy <- vportPhys) {
      sb.append("phy=")
      vpPhy.toStringSb(sb)
      if (vportQuanted.nonEmpty)
        sb.append(' ')
    }

    for (vpQua <- vportQuanted) {
      sb.append("vpQ=")
      vpQua.toStringSb( sb )
    }

    sb.append(')')
  }

  override def toString = super.toString

}
