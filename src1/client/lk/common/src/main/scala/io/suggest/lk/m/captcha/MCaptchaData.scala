package io.suggest.lk.m.captcha

import diode.FastEq
import org.scalajs.dom.Blob
import io.suggest.ueq.UnivEqUtil._
import io.suggest.ueq.JsUnivEqUtil._
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.06.19 17:17
  * Description: Данные капчи, полученные с сервера.
  */
object MCaptchaData {

  /** Поддержка FastEq для интсансов [[MCaptchaData]]. */
  implicit object MCaptchaDataFastEq extends FastEq[MCaptchaData] {
    override def eqv(a: MCaptchaData, b: MCaptchaData): Boolean = {
      (a.imgData ===* b.imgData) &&
      (a.secret  ===* b.secret)
    }
  }

  @inline implicit def univEq: UnivEq[MCaptchaData] = UnivEq.derive

}

/** Контейнер данных картинки капчи.
  *
  * @param imgData Байты картинки капчи, полученной с сервера.
  * @param secret Секрет капчи, присланный сервером.
  */
case class MCaptchaData(
                         imgData    : Blob,
                         secret     : String,
                       )
