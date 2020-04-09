package io.suggest.sc.ads

import japgolly.univeq._
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.04.2020 14:54
  * Description: Модель контейнера аргументов рендера плитки, которая передаётся в ScAdsTile.
  */
object MScGridArgs {

  object Fields {
    val AD_TITLES_FN = "t"
    val FOC_AFTER_JUMP_FN = "f"
  }

  implicit def scGridArgsJson: OFormat[MScGridArgs] = {
    val F = Fields
    (
      (__ \ F.AD_TITLES_FN).format[Boolean] and
      (__ \ F.FOC_AFTER_JUMP_FN).formatNullable[Boolean]
    )(apply, unlift(unapply))
  }

  @inline implicit def univEq: UnivEq[MScGridArgs] = UnivEq.derive

}


/** Контейнер аргументов, передаваемых в ScAdsTile, для рендера плитки.
  *
  * @param focAfterJump Повторная фокусировка на карточку после перескока в новый узле (index+grid+foc)
  *                     true - автофокус на карточку при запуске в рамках index+grid+foc.
  * @param adTitles Рендерить в ответе заголовки (meta.name) карточек.
  */
final case class MScGridArgs(
                              adTitles                    : Boolean                       = false,
                              focAfterJump                : Option[Boolean]               = None,
                            )
