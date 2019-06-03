package models.sms

import java.time.OffsetDateTime

import monocle.macros.GenLens

import scala.concurrent.duration.FiniteDuration


object MSmsSend {

  val isTest = GenLens[MSmsSend](_.isTest)

}

/** Модель описания одной смски.
  *
  * @param msgs Список номеров и отправляемых текстов смс-сообщений.
  * @param timeAt Время отправки/получения.
  * @param from От кого.
  * @param ttl Макс.время жизни смс в очереди на отправку.
  * @param translit Надо ли произвести транслитерацию сообщения?
  * @param isTest Это тестовый запрос? true - никакое сообщение никуда не отправляется.
  */
case class MSmsSend(
                     msgs     : Map[String, Seq[String]],
                     timeAt   : Option[OffsetDateTime]  = None,
                     from     : Option[String]          = None,
                     ttl      : Option[FiniteDuration]  = None,
                     translit : Boolean                 = false,
                     isTest   : Boolean                 = false,
                   )
