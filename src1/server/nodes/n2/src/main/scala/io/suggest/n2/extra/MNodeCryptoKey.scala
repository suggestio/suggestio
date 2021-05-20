package io.suggest.n2.extra

import io.suggest.es.{IEsMappingProps, MappingDsl}
import japgolly.univeq._
import play.api.libs.functional.syntax._
import play.api.libs.json._

/** Storage for crypto-keys inside MNode.
  * Model is used for lk-adv-ext storage.
  *
  * Previosly, MAsymKeys model inside separate ES index _type was used.
  */
object MNodeCryptoKey extends IEsMappingProps {

  object Fields {
    def PUB_KEY_FN = "pubKey"
    def SEC_KEY_FN = "secKey"
  }

  override def esMappingProps(implicit dsl: MappingDsl): JsObject = {
    import dsl._
    val F = Fields
    Json.obj(
      F.PUB_KEY_FN       -> FText.notIndexedJs,
      F.SEC_KEY_FN       -> FText.notIndexedJs,
    )
  }

  implicit def nodeCalendarJson: OFormat[MNodeCryptoKey] = {
    val F = Fields
    (
      (__ \ F.PUB_KEY_FN).format[String] and
      (__ \ F.SEC_KEY_FN).formatNullable[String]
    )( apply, unlift(unapply) )
  }

  @inline implicit def univEq: UnivEq[MNodeCryptoKey] = UnivEq.derive

}


/** Crypto-key storage inside MNode.
  *
  * @param pubKey Public key data.
  * @param secKey Secret key data, if any.
  */
case class MNodeCryptoKey(
                           pubKey: String,
                           secKey: Option[String],
                           // TODO algo/type?
                         )
