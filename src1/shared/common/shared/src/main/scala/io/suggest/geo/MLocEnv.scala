package io.suggest.geo

import io.suggest.ble.MUidBeacon
import io.suggest.common.empty.{EmptyProduct, EmptyUtil, IEmpty}
import io.suggest.text.StringUtil
import japgolly.univeq.UnivEq
import monocle.macros.GenLens
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.07.17 11:31
  * Description: Модель описания географической и физической локации.
  */

object MLocEnv extends IEmpty {

  override type T = MLocEnv

  override val empty = apply()

  object Fields {
    final def GEO_LOC_FN = "e"
    final def BEACONS_FN = "b"
  }

  /** Поддержка JSON для инстансов [[MLocEnv]].
    * В первую очередь для js-роутера и qs.
    */
  implicit val MLOC_ENV_FORMAT: OFormat[MLocEnv] = {
    val F = Fields
    (
      (__ \ F.GEO_LOC_FN).formatNullable[MGeoLoc] and
      (__ \ F.BEACONS_FN).formatNullable[Seq[MUidBeacon]]
        .inmap[Seq[MUidBeacon]](
          EmptyUtil.opt2ImplEmptyF(Nil),
          seq => Option.when( seq.nonEmpty )(seq)
        )
    )(apply, unlift(unapply))
  }

  @inline implicit def univEq: UnivEq[MLocEnv] = {
    import io.suggest.ueq.UnivEqUtil._
    UnivEq.derive
  }

  def geoLocOpt   = GenLens[MLocEnv](_.geoLocOpt)
  def beacons     = GenLens[MLocEnv](_.beacons)

}


/**
  * Класс экземпляров модели информации о локации.
  *
  * @param geoLocOpt Данные геолокации.
  * @param beacons Данные видимых radio-маячков.
  */
case class MLocEnv(
                    geoLocOpt     : Option[MGeoLoc]    = None,
                    beacons       : Seq[MUidBeacon]    = Nil
                  )
  extends EmptyProduct
{

  override def toString: String = {
    StringUtil.toStringHelper(this, 64) { renderF =>
      val F = MLocEnv.Fields
      geoLocOpt foreach renderF( F.GEO_LOC_FN )
      if (beacons.nonEmpty)
        renderF( F.BEACONS_FN )( beacons.mkString("[", ", ", "]") )
    }
  }

}
