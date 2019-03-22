package io.suggest.captcha

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import play.api.libs.json.Format

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.03.19 12:21
  * Description: Модель путей для кукисов, связанных с капчей.
  * Для этих кукисов на стороне сервера выставляется path и expire=session: вычищаются они неявно автоматом в браузере.
  * Тут - модель для генерации корректных URL на стороне сервера.
  */
object MCaptchaCookiePaths extends StringEnum[MCaptchaCookiePath] {

  /** От сервера требуется ссылка на email-pw reg сабмит. */
  case object EpwReg extends MCaptchaCookiePath("a")


  override def values = findValues

}


/** Класс одного элемента модели [[MCaptchaCookiePaths]]. */
sealed abstract class MCaptchaCookiePath(override val value: String) extends StringEnumEntry {
  override final def toString = value
}

object MCaptchaCookiePath {

  @inline implicit def univEq: UnivEq[MCaptchaCookiePath] = UnivEq.derive

  implicit def captchaCookiePathFormat: Format[MCaptchaCookiePath] =
    EnumeratumUtil.valueEnumEntryFormat( MCaptchaCookiePaths )

}
