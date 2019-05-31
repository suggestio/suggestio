package models.sms

import java.time.OffsetDateTime

import monocle.macros.GenLens

import scala.concurrent.duration.FiniteDuration


object MSms {

  val isTest = GenLens[MSms](_.isTest)

}

/** Модель описания одной смски.
  *
  * @param text Текст смс-сообщения.
  * @param numbers Номера получателей.
  * @param timeAt Время отправки/получения.
  * @param from От кого.
  * @param ttl Макс.время жизни смс в очереди на отправку.
  * @param translit Надо ли произвести транслитерацию сообщения?
  * @param isTest Это тестовый запрос? true - никакое сообщение никуда не отправляется.
  */
case class MSms(
                 numbers  : Iterable[String],
                 text     : String,
                 timeAt   : Option[OffsetDateTime] = None,
                 from     : Option[String] = None,
                 ttl      : Option[FiniteDuration] = None,
                 translit : Boolean = false,
                 isTest   : Boolean = false,
               )

