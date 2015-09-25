package io.suggest.model.n2.node.meta

import io.suggest.model.IGenEsMappingProps
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.09.15 12:16
 * Description: Модель метаданных одного узла графа N2.
 * Из-за множества полей, является над-моделью, которая уже содержит внутри себя
 * более конкретные поля.
 */

object MMeta extends IGenEsMappingProps {

  val BASIC_FN    = "b"
  val PERSON_FN   = "p"
  val ADDRESS_FN  = "a"

  /** Поддержка JSON. */
  implicit val FORMAT: OFormat[MMeta] = (
    (__ \ BASIC_FN).formatNullable[MBasicMeta]
      .inmap[MBasicMeta](
        _ getOrElse MBasicMeta(),
        Some.apply
      ) and
    (__ \ PERSON_FN).formatNullable[MPersonMeta]
      .inmap[MPersonMeta] (
        _ getOrElse MPersonMeta.empty,
        {mpm => if (mpm.isEmpty) None else Some(mpm)}
      ) and
    (__ \ ADDRESS_FN).formatNullable[MAddress]
      .inmap [MAddress] (
        _ getOrElse MAddress.empty,
        { addr => if (addr.isEmpty) None else Some(addr) }
      )
  )(apply, unlift(unapply))


  import io.suggest.util.SioEsUtil._
  override def generateMappingProps: List[DocField] = {
    List(
      FieldObject(BASIC_FN, enabled = true, properties = MBasicMeta.generateMappingProps),
      FieldObject(PERSON_FN, enabled = true, properties = MPersonMeta.generateMappingProps),
      FieldObject(ADDRESS_FN, enabled = true, properties = MAddress.generateMappingProps)
    )
  }

}


/** Дичайшая убер-модель метаданных, например. */
case class MMeta(
  basic         : MBasicMeta,
  person        : MPersonMeta    = MPersonMeta.empty,
  address       : MAddress       = MAddress.empty
)
