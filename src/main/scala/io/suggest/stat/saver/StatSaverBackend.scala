package io.suggest.stat.saver

import io.suggest.stat.m.MStat

import scala.concurrent.Future



/** Интерфейс для backend'ов сохранения статистики. */
trait StatSaverBackend {
  /** Сохранение. Бэкэнд может отправлять в свою очередь или в хранилище. */
  def save(stat: MStat): Future[_]

  /** Сброс накопленной очереди, если такая имеется. */
  def flush(): Unit

  /** Завершение работы backend'a. */
  def close(): Future[_]
}

