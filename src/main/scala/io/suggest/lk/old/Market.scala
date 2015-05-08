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

  /** Доступ к API формы создания/редактирования рекламной карточки. */
  def ad_form: MarketAdForm = js.native

}


trait MarketImg extends js.Object {

  /** Модуль кропа. */
  def crop: MarketImgCrop = js.native

}


trait MarketImgCrop extends js.Object {
  /** Инициализация кроппера для указанного input name. */
  def init(imgName: String): Unit = js.native
}


trait MarketAdForm extends js.Object {

  /** Обновить превьюшку рекламной карточки. */
  def queue_block_preview_request(reqDelay: Int = ???, isWithAuthCrop: Boolean = ???): Unit = js.native

}