package io.suggest.model.n2.extra.rsc

import io.suggest.es.model.IGenEsMappingProps
import io.suggest.es.util.SioEsUtil._
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.08.18 11:39
  * Description: Модель данных по индексируемому хосту. Должна жить как Nested object.
  */
object MHostNameIndexed extends IGenEsMappingProps {

  object Fields {
    val HOST_FN = "h"
  }

  /** Спека для индексации. */
  override def generateMappingProps: List[DocField] = {
    List(
      FieldKeyword( Fields.HOST_FN, index = true, include_in_all = true )
    )
  }

  /** Поддержка play-json. */
  implicit def mHostNameIndexedFormat: OFormat[MHostNameIndexed] = {
    (__ \ Fields.HOST_FN).format[String]
      .inmap[MHostNameIndexed](apply, _.host)
  }

}


/** Контейнер данных по хосту.
  *
  * @param host Имя хоста. Или dkey, или ещё что-нибудь.
  */
case class MHostNameIndexed(
                             host     : String
                           // isDkey, isNorm, etc
                           )
