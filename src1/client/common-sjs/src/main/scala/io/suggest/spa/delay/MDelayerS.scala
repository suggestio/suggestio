package io.suggest.spa.delay

import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.10.17 17:17
  * Description: Состояние модуля откладывания экшенов на потом.
  */
object MDelayerS {

  def default = MDelayerS()

  @inline implicit def univEq: UnivEq[MDelayerS] = UnivEq.derive

  def delayed = GenLens[MDelayerS]( _.delayed )

}


/** Класс модели состояния подсистемы отложенных экшенов.
  *
  * @param counter Счётчик для генерации уникальных значений.
  * @param delayed Мапа-каталог из отложенных экшенов.
  */
case class MDelayerS(
                      delayed     : Map[String, MDelayedAction]   = Map.empty
                    )
