package io.suggest.n2.extra.rsc

import io.suggest.es.{IEsMappingProps, MappingDsl}
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.08.18 11:39
  * Description: Модель данных по индексируемому хосту. Должна жить как Nested object.
  */
object MHostNameIndexed
  extends IEsMappingProps
{

  object Fields {
    val HOST_FN = "h"
  }

  override def esMappingProps(implicit dsl: MappingDsl): JsObject = {
    import dsl._
    val F = Fields
    Json.obj(
      F.HOST_FN -> FKeyWord.indexedJs,
    )
  }

  /** Поддержка play-json. */
  implicit def mHostNameIndexedFormat: OFormat[MHostNameIndexed] = {
    val F = Fields
    (__ \ F.HOST_FN).format[String]
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
