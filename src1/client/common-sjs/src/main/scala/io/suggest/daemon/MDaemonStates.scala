package io.suggest.daemon

import enumeratum.{EnumEntry, Enum}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.04.2020 12:03
  * Description: Модель состояний режима функционирования подсистемы демонизации.
  */
object MDaemonStates extends Enum[MDaemonState] {

  /** Состояние сна с пробуждением по системному (внешнему) таймеру.
    * Ресурсы девайса не расходуются, и ничего не делается. */
  case object Sleep extends MDaemonState

  /** Состояния работы, процессор устройства включён и выполняется какая-то работа.
    * Расходуются ресурсы девайса, поэтому этот режим должен быть максимально скоротечен. */
  case object Work extends MDaemonState


  override def values = findValues

  def fromIsActive(isActive: Boolean): MDaemonState =
    if (isActive) Work else Sleep

}


/** Модель описания одного режима функционирования подсистемы демонизации. */
sealed abstract class MDaemonState extends EnumEntry
