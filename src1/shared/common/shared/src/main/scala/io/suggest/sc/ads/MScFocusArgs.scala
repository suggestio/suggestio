package io.suggest.sc.ads

import io.suggest.ad.search.AdSearchConstants._
import japgolly.univeq._
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

  implicit def mScFocusArgsFormat: Format[MScFocusArgs] = {
    (
      ({
        // TODO 2021-02-12 Тут код для переходного периода с Boolean на Option[MIndexAdOpenQs]. Упростить этот код до финального через пол-года/год.
        val inxAdOpenPath = (__ \ FOC_INDEX_AD_OPEN_FN)
        val inxOpenFmt0 = inxAdOpenPath.formatNullable[MIndexAdOpenQs]

        val inxAdOpenFallbackReads = inxOpenFmt0 orElse {
          inxAdOpenPath
            .read[Boolean]
            .map( MIndexAdOpenQs.fromFocIndexAdOpenEnabled )
        }
        OFormat( inxAdOpenFallbackReads, inxOpenFmt0 )
      }) and
      (__ \ AD_LOOKUP_MODE_FN).formatNullable[MLookupMode] and
      (__ \ LOOKUP_AD_ID_FN).format[String]
    )(apply, unlift(unapply))
  }

  @inline implicit def univEq: UnivEq[MScFocusArgs] = UnivEq.derive

  def indexAdOpen     = GenLens[MScFocusArgs](_.indexAdOpen)
  def lookupMode      = GenLens[MScFocusArgs](_.lookupMode)
  def lookupAdId      = GenLens[MScFocusArgs](_.lookupAdId)

}


/** Контейнер параметров фокусировки карточки.
  *
  * @param indexAdOpen Разрешён переход в выдачу узла вместо открытия карточки?
  * @param lookupMode Режим перехода между focused-карточками.
  *                   Неактуально для sc3. Будет удалён, если не понадобится.
  * @param lookupAdId id узла фокусируемой карточки.
  */
case class MScFocusArgs(
                         // !!! Обязательно хотя бы одно обязательное непустое поле !!!
                         indexAdOpen            : Option[MIndexAdOpenQs],
                         lookupMode             : Option[MLookupMode],
                         lookupAdId             : String,
                       )
