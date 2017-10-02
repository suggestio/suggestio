package io.suggest.ww

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.10.17 9:53
  * Description: Система управления WebWorkers.
  *
  * v1 -- просто stub для этого.
  */

trait IWwMgr {

  /** Инициализация WebWorker'ов. */
  def start(poolSize: Int): Future[_]

  /** Отправка задачи на исполнение.
    *
    * @param task Задача.
    * @param moveInstance Использовать перемещение инстанса в воркера вместо memcpy()
    * @tparam T Тип возвращаемого значения.
    * @return Фьючерс с результатом выполнения задачи.
    */
  def runTask[T](task: IWwTask[T], moveInstance: Boolean = false): Future[T]

}


/** Stub. Заглушка WebWorkers-менеджера, работающего вообще без WebWorker'ов. */
protected[ww] object DummyWwMgr extends IWwMgr {

  override def start(poolSize: Int): Future[_] = {
    Future.successful( None )
  }

  override def runTask[T](task: IWwTask[T], moveInstance: Boolean = false): Future[T] = {
    task.run()
  }

}


// TODO Надо бы реализовать нормальный менеджер WebWorker'ов,
// который запускает их из текущего js-файла и управляет ими, результаты пробрасывает куда надо.
