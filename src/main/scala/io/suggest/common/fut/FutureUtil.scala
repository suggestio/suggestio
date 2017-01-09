package io.suggest.common.fut

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.09.15 20:31
 * Description: Утиль для Future.
 */
object FutureUtil {

  /**
   * Перехват синхронных ошибок для вызовов, которые должны возвращать их асинхронно.
   * @param f Функция, потенциально возвращающая Future[X] либо асинхронную ошибку.
   * @tparam X Тип возвращаемого значения.
   * @return Future[X] и никаких исключений.
   */
  def tryCatchFut[X](f: => Future[X]): Future[X] = {
    try {
      f
    } catch {
      case ex: Throwable =>
        Future.failed(ex)
    }
  }


  /**
   * Часто бывает необходимость сверстки Option[T] в фьючерс с опциональным значением.
   * Тут код для укорачивания такой сверстки.
   * @param opt Исходное опциональное значение.
   * @param defined Функция, вызываемая для исходного значения T.
   * @tparam T Тип исходного значения.
   * @tparam R Тип результирующего значения.
   * @return Фьючерс с опциональным результатом.
   */
  def optFut2futOpt[T, R](opt: Option[T])(defined: T => Future[Option[R]]): Future[Option[R]] = {
    opt.fold {
      Future.successful( Option.empty[R] )
    } { v =>
      defined(v)
    }
  }


  /**
   * Иногда бывает нужно опциональное значение трансформировать в неопциональный фьючерс.
   * @param x Option[T]
   * @param ifEmpty Фьючерс, когда нет значения.
   * @tparam T Тип отрабатываемого значения.
   * @return Future[T].
   */
  def opt2future[T](x: Option[T])(ifEmpty: => Future[T]): Future[T] = {
    x.fold(ifEmpty)(Future.successful)
  }

}
