package io.suggest.sc.m.dia.first

import io.suggest.conf.ConfConst
import io.suggest.kv.MKvStorage
import io.suggest.msg.ErrorMsgs
import io.suggest.sjs.common.log.Log
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.01.19 16:10
  * Description: Сохраняемое состояние
  */
object MFirstRunStored extends Log {

  /** Список версий. */
  object Versions {

    /** Версия первой инфы о первом запуске. */
    def INITIAL = 0

    /** Текущая версия.
      *
      * 1 - добавлена проверка доступа к Notification API для cordova и browser+DEV-режим.
      */
    def CURRENT = 1

  }


  /** Поддержка play-json. */
  implicit def mFirstRunStoredFormat: OFormat[MFirstRunStored] = {
    (__ \ "v").format[Int]
      .inmap[MFirstRunStored]( apply, _.version )
  }

  @inline implicit def univEq: UnivEq[MFirstRunStored] = UnivEq.derive


  /** Ключ конфига для сохранения сериализованного инстанса. */
  private def _CONF_KEY = ConfConst.SC_FIRST_RUN


  /** Сохранение состояния в постоянное хранилище. */
  def save(m: MFirstRunStored): Unit = {
    val mkv = MKvStorage(
      key   = _CONF_KEY,
      value = m,
    )
    try {
      MKvStorage.save( mkv )
    } catch {
      case ex: Throwable =>
        LOG.warn( ErrorMsgs.KV_STORAGE_ACTION_FAILED, ex, mkv )
    }
  }


  /** Чтение сохранённого состояния из БД. */
  def get(): Option[MFirstRunStored] = {
    try {
      for {
        mkv <- MKvStorage.get[MFirstRunStored]( _CONF_KEY )
      } yield {
        mkv.value
      }
    } catch {
      case ex: Throwable =>
        LOG.warn( ErrorMsgs.KV_STORAGE_ACTION_FAILED, ex, _CONF_KEY )
        None
    }
  }


  /** Очистка. Не ясно, надо ли вообще. */
  def clear(): Unit = {
    try {
      MKvStorage.delete( _CONF_KEY )
    } catch {
      case ex: Throwable =>
        LOG.warn( ErrorMsgs.KV_STORAGE_ACTION_FAILED, ex, _CONF_KEY )
    }
  }

}


/** Контейнер данных итогов первого запуска.
  *
  * @param version Версия полного запуска.
  */
case class MFirstRunStored(
                            version    : Int,
                          )
