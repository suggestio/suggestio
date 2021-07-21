package io.suggest.sc.ads

import japgolly.univeq._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import io.suggest.common.empty.OptionUtil.BoolOptJsonFormatOps

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.04.2020 14:54
  * Description: Модель контейнера аргументов рендера плитки, которая передаётся в ScAdsTile.
  */
object MScGridArgs {

  object Fields {
    final def WITH_TITLE = "t"
    final def FOC_AFTER_JUMP = "f"
    final def ALLOW_404 = "a4"
    final def ONLY_RADIO_BEACONS = "b"
  }

  implicit def scGridArgsJson: OFormat[MScGridArgs] = {
    val F = Fields
    (
      (__ \ F.WITH_TITLE).format[Boolean] and
      (__ \ F.FOC_AFTER_JUMP).formatNullable[Boolean] and
      (__ \ F.ALLOW_404).formatNullable[Boolean].formatBooleanOrTrue and
      (__ \ F.ONLY_RADIO_BEACONS).formatNullable[Boolean].formatBooleanOrFalse
    )(apply, unlift(unapply))
  }

  @inline implicit def univEq: UnivEq[MScGridArgs] = UnivEq.derive


  implicit final class ScGridAdsOptOpsExt(private val scGridArgsOpt: Option[MScGridArgs]) extends AnyVal {
    def allow404: Boolean =
      scGridArgsOpt.fold(true)(_.allow404)
  }

}


/** Контейнер аргументов, передаваемых в ScAdsTile, для рендера плитки.
  *
  * @param focAfterJump Повторная фокусировка на карточку после перескока в новый узле (index+grid+foc)
  *                     true - автофокус на карточку при запуске в рамках index+grid+foc.
  * @param withTitle Рендерить в ответе заголовки (meta.name) карточек.
  * @param allow404 Разрешить возвращать 404-карточки?
  * @param onlyRadioBeacons
  */
final case class MScGridArgs(
                              withTitle                   : Boolean                       = false,
                              focAfterJump                : Option[Boolean]               = None,
                              allow404                    : Boolean                       = true,
                              onlyRadioBeacons       : Boolean                       = false,
                            )
