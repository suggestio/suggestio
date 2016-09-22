package io.suggest.stat.m

import io.suggest.common.menum.{EnumMaybeWithName, StrIdValT}
import io.suggest.model.menum.EnumJsonReadsValT

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.09.16 17:49
  * Description: Типы экшенов, затрагиваемых статистикой.
  */
object MActionTypes extends EnumMaybeWithName with EnumJsonReadsValT with StrIdValT {

  /** Класс для всех экземпляров сий модели. */
  protected case class Val(override val strId: String)
    extends super.Val(strId)
    with ValT


  override type T = Val

  /** Экшен логгирования данных текущего юзера. */
  val CurrUser            = Val("юзер")

  /** Обращение к /sc/site */
  val ScSite              = Val("выдача: сайт")

  /** Для логгирования значения pov_adn_id. */
  val PovNode             = Val("точка зрения узла")

  /** Обращение к /sc/index вывело на узел-ресивер. */
  val ScIndexRcvr         = Val("выдача ресивера")

  /** /sc/index не нашел ресивера, но подобрал или сочинил узел с подходящим к ситуации оформлением. */
  val ScIndexCovering     = Val("выдача с оформлением от узла")

}
