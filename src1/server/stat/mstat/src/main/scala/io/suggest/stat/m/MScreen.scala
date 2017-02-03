package io.suggest.stat.m

import io.suggest.common.empty.{EmptyProduct, IEmpty}
import io.suggest.es.model.IGenEsMappingProps
import io.suggest.model.geom.d2.MOrientation2d
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.09.16 12:27
  * Description: Суб-модель для описания данных экрана устройства.
  */
object MScreen extends IGenEsMappingProps with IEmpty {

  override type T = MScreen

  object Fields {

    def ORIENTATION_FN          = "orient"
    def VPORT_PHYS_FN           = "phys"
    def VPORT_QUANTED_FN        = "choosen"

  }


  import Fields._

  implicit val FORMAT: OFormat[MScreen] = (
    (__ \ ORIENTATION_FN).formatNullable[MOrientation2d] and
    (__ \ VPORT_PHYS_FN).formatNullable[MViewPort] and
    (__ \ VPORT_QUANTED_FN).formatNullable[MViewPort]
  )(apply, unlift(unapply))


  import io.suggest.es.util.SioEsUtil._

  override def generateMappingProps: List[DocField] = {
    val vportFields = MViewPort.generateMappingProps
    List(
      FieldString(ORIENTATION_FN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
      FieldObject(VPORT_PHYS_FN, enabled = true, properties = vportFields),
      FieldObject(VPORT_QUANTED_FN, enabled = true, properties = vportFields)
    )
  }

  override def empty = MScreen()

}


case class MScreen(
  orientation   : Option[MOrientation2d]  = None,
  vportPhys     : Option[MViewPort]       = None,
  vportQuanted  : Option[MViewPort]       = None
)
  extends EmptyProduct
