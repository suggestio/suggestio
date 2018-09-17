package io.suggest.ad.blk.ent

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.10.15 16:21
 * Description: Абстрактная модель для entity-значений.
 *
 * Скорее всего её можно будет замёржить внутрь [[TextEnt]]-модель,
 * т.к. необходимость в существовании этой абстрактной value-модели отпала вместе с отказом
 * от float-полей (Price, Discount).
 */

object ValueEnt {

  val FONT_ESFN         = "font"
  val VALUE_ESFN        = "value"

}


/** Трейт, помечающий класс как entity payload.
  * Остался для совместимости с blocks-подсистемой. */
// TODO Можно удалить вслед за blocks ad editor.
trait ValueEnt {

  def font: EntFont

}
