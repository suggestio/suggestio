package io.suggest.sc.ads

import io.suggest.ad.search.AdSearchConstants._
import io.suggest.scalaz.ScalazUtil
import japgolly.univeq._
import monocle.macros.GenLens
import play.api.libs.functional.syntax._
import play.api.libs.json._
import scalaz.NonEmptyList
import io.suggest.ueq.UnivEqUtil._

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
      // 2020-04-16 lookupAdId Стал допускать несколько карточек, ранее -- допускалась ровно одна карточка.
      (__ \ AD_IDS_FN).format( ScalazUtil.nelOrSingleValueJson[String] )
    )(apply, unlift(unapply))
  }

  @inline implicit def univEq: UnivEq[MScFocusArgs] = UnivEq.derive

  def indexAdOpen     = GenLens[MScFocusArgs](_.indexAdOpen)
  def adIds           = GenLens[MScFocusArgs](_.adIds)

}


/** Контейнер параметров фокусировки карточки.
  *
  * @param indexAdOpen Разрешён переход в выдачу узла вместо открытия карточки?
  * @param adIds id фокусируемых карточек.
  */
case class MScFocusArgs(
                         // !!! Обязательно хотя бы одно обязательное непустое поле !!!
                         indexAdOpen            : Option[MIndexAdOpenQs],
                         adIds                  : NonEmptyList[String],
                       )
