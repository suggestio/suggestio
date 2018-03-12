package io.suggest.primo

import scala.concurrent.{ExecutionContext, Future}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.03.18 11:07
  * Description: Контейнер для изменяемого значения.
  * Используется, когда можно вернуть одно значение,
  * но потом подменить его другим, оптимизированным, представлением для всё тех же данных.
  */
case class Var[T](
                   var value: T
                 ) {

  /** Запуск фонового обновления переменной с помощью указанной функции, возвращающей новое значение.
    *
    * @param f Фунция, генерящая новое значение синхронно.
    * @return Результат f()
    */
  def asyncUpgradeUsing(f: () => T)(implicit ec: ExecutionContext): Future[T] = {
    Future {
      val res = f()
      value = res
      res
    }
  }

}
