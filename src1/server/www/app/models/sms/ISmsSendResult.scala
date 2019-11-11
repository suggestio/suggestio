package models.sms

import io.suggest.bill.MPrice

import scala.collection.MapView

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.05.19 18:11
  * Description: Модели результата отправки смс.
  */

/** Результат запроса отправки одной или нескольких смс. */
trait ISmsSendResult {
  /** Выполнен ли запрос успешно? */
  def isOk           : Boolean
  /** Данные по отправленным смс. */
  def smsInfo        : Map[String, ISmsSendStatus]
  /** Текст ошибки. */
  def statusText     : Option[String]
  /** Остаточный баланс, если возвращается сервисом. */
  def restBalance    : Option[MPrice]
  /** id реквеста отправки смс, если есть. */
  def requestId      : Option[String]
}


/** Статус отправки одной смс на один номер. */
trait ISmsSendStatus {
  /** Отправлено смс на указанный номер? */
  def isOk          : Boolean
  /** Текстовый статус сообщения или ошибки. */
  def statusText    : Option[String]
  /** id отправленной смс. */
  def smsId         : Option[String]
}


case class MSmsSendStatus(
                           override val isOk       : Boolean,
                           override val statusText : Option[String]     = None,
                           override val smsId      : Option[String]     = None,
                         )
  extends ISmsSendStatus

case class MSmsSendResult(
                           override val isOk        : Boolean,
                           override val smsInfo     : Map[String, ISmsSendStatus],
                           override val statusText  : Option[String]    = None,
                           override val restBalance : Option[MPrice]    = None,
                           override val requestId   : Option[String]    = None,
                         )
  extends ISmsSendResult
