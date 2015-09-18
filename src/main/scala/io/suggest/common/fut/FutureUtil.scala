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
        Future failed ex
    }
  }

}
