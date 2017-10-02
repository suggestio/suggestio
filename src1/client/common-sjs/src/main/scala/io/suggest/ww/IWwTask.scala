package io.suggest.ww

import scala.concurrent.Future
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.spa.DAction

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.10.17 9:55
  * Description: Одна задача для исполнения.
  */
trait IWwTask[T] extends DAction {

  // TODO На стороне воркера должен быть некий ActionHandler, который принимает сообщения без run() и запускает какой-то свой код, а не этот.
  //      Не должно быть передачи функций и кода между воркерами. Должно быть явное разделение модели и контроллера, который живёт только на стороне воркера.
  //      Тут run просто для очень раннего этапа реализации поддержки воркеров, времени нет сейчас это реализовывать.

  /** Запуск текущей асинхронной задачи на исполнение. */
  def run(): Future[T]

}


/** Задачка для webworker'а. */
trait IWwSyncTask[T] extends IWwTask[T] {

  /** Запуск текущей задачи на исполнение и ожидание результата. */
  def runSync(): T

  override def run(): Future[T] = Future {
    runSync()
  }

}
