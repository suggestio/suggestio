package io.suggest.lk.old

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.05.15 15:42
 * Description: Доступ к старому API, реализованном в mx_cof.js максом.
 */
@JSName("market")
object Market extends js.Object {

  /** Доступ к API картинок. */
  def img: MarketImg = js.native

}


class MarketImg extends js.Object {

  /** Модуль кропа. */
  def crop: MarketImgCrop = js.native

}


class MarketImgCrop extends js.Object {
  /** Инициализация кроппера для указанного input name. */
  def init(imgName: String): Unit = js.native
}
