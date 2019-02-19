package io.suggest.sc.ads

import io.suggest.ad.search.AdSearchConstants._
import japgolly.univeq.UnivEq
import monocle.macros.GenLens
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
    (__ \ FOC_INDEX_ALLOWED_FN).format[Boolean] and
    (__ \ AD_LOOKUP_MODE_FN).formatNullable[MLookupMode] and
    (__ \ LOOKUP_AD_ID_FN).format[String]
  )(apply, unlift(unapply))

  @inline implicit def univEq: UnivEq[MScFocusArgs] = UnivEq.derive

  val focIndexAllowed = GenLens[MScFocusArgs](_.focIndexAllowed)
  val lookupMode      = GenLens[MScFocusArgs](_.lookupMode)
  val lookupAdId      = GenLens[MScFocusArgs](_.lookupAdId)

}


/** Контейнер параметров фокусировки карточки.
  *
  * @param focIndexAllowed Разрешён переход в выдачу узла вместо открытия карточки?
  * @param lookupMode Режим перехода между focused-карточками.
  *                   Неактуально для sc3. Будет удалён, если не понадобится.
  * @param lookupAdId id узла фокусируемой карточки.
  * @param focAfterIndex Возвращать ли результат фокусировки даже после перехода в узел-продьюсер.
  */
case class MScFocusArgs(
                         focIndexAllowed        : Boolean,
                         lookupMode             : Option[MLookupMode],
                         lookupAdId             : String
                       )
