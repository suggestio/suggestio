package io.suggest.sc.ads

import io.suggest.ad.search.AdSearchConstants._
import japgolly.univeq.UnivEq
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.05.18 22:24
  * Description: Параметры фокусировки.
  */
object MScFocusArgs {

  implicit def mScFocusArgsFormat: Format[MScFocusArgs] = (
    (__ \ FOC_JUMP_ALLOWED_FN).format[Boolean] and
    (__ \ AD_LOOKUP_MODE_FN).formatNullable[MLookupMode] and
    (__ \ AD_ID_LOOKUP_FN).format[String]
  )(apply, unlift(unapply))

  implicit def univEq: UnivEq[MScFocusArgs] = UnivEq.derive

}


/** Контейнер параметров фокусировки карточки.
  *
  * @param focJumpAllowed Разрешён переход в выдачу узла вместо открытия карточки?
  * @param lookupMode Режим перехода между focused-карточками.
  *                   Неактуально для sc3. Будет удалён, если не понадобится.
  * @param lookupAdId id узла фокусируемой карточки.
  */
case class MScFocusArgs(
                       focJumpAllowed  : Boolean,
                       lookupMode      : Option[MLookupMode],
                       lookupAdId      : String,
                     )
