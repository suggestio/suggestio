package io.suggest.stat.saver

import io.suggest.stat.m.MStat

import scala.concurrent.Future

/** Бэкэнд сохранения статистики, который сохраняет всё в /dev/null. */
class DummySaverBackend extends StatSaverBackend {

  /** Сохранение. Бэкэнд может отправлять в свою очередь или в хранилище. */
  override def save(stat: MStat): Future[_] = {
    Future.successful(Nil)
  }

  /** Сброс накопленной очереди, если такая имеется. */
  override def flush(): Unit = {}

  /** Завершение работы backend'a. */
  override def close(): Future[_] = {
    Future.successful(None)
  }

}
