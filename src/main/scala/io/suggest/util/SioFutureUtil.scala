package io.suggest.util

import scala.concurrent.{ExecutionContext, Promise, Future}
import scala.util.{Success, Failure}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.07.13 10:29
 * Description: Дополнительные функции для фьючерсов.
 */
object SioFutureUtil extends Logs {

  /**
   * Функция последовательного неблокирующего обхода последовательности.
   * @param in входная последовательность
   * @param ignoreErrors если true, то при возникновении ошибки хвост последовательности всё-таки будет пройден.
   * @param f функция обхода
   * @tparam A Тип исходных элементов
   * @tparam B Тип возвращаемых элементов.
   * @return Фьючерс с последовательностью результатов последовательно вызванных фьючерсов.
   */
  def mapLeftSequentally[A, B](in:TraversableOnce[A], ignoreErrors:Boolean = false)(f: A => Future[B])(implicit executor:ExecutionContext): Future[List[B]] = {
    val acc0: List[B] = Nil
    val foldF: (List[B], A) => Future[List[B]] = {
      case (acc, a) => f(a) map( _ :: acc)
    }
    foldLeftSequental(in, acc0, ignoreErrors)(foldF)
      .map(_.reverse)
  }


  /**
   * Запуск последовательного неблокирующего fold'а над множеством элементов.
   * @param in Исходная последовательность.
   * @param acc0 Начальный аккамулятор.
   * @param ignoreErrors Если true, то ошибки будут отправяться в логи и подавляться. По дефолту - false.
   * @param f fold-функция обхода, которая генерит фьючерсы.
   * @tparam A Тип значений исходной последовательности данных.
   * @tparam Acc Тип аккамулятора.
   * @return Конечных аккамулятор.
   */
  def foldLeftSequental[A, Acc](in:TraversableOnce[A], acc0:Acc, ignoreErrors:Boolean = false)(f: (Acc, A) => Future[Acc])(implicit executor:ExecutionContext): Future[Acc] = {
    val p = Promise[Acc]()
    lazy val hash = in.hashCode() * f.hashCode() + acc0.hashCode()
    def foldLeftStep(acc:Acc, inRest:TraversableOnce[A]) {
      if (inRest.isEmpty) {
        // Закончить обход. Вернуть результат
        p success acc

      } else {
        // Продолжаем обход.
        val h :: t = inRest
        val fut: Future[Acc] = try {
          f(acc, h)
        } catch {
          case ex:Throwable => Future.failed(ex)
        }
        fut onComplete {
          case Success(acc1) => foldLeftStep(acc1, t)

          case Failure(ex) =>
            if (ignoreErrors) {
              // Включено подавление ошибок.
              warnError(hash, h, ex)
              foldLeftStep(acc, t)
            } else {
              // Ошибка - остановка.
              p failure TraverseSequentallyException(inRest, acc, ex)
            }
        }
      }
    }
    foldLeftStep(acc0, in)
    p.future
  }

  private def warnError(hash:Int, h:Any, ex:Throwable) {
    warn("foldLeftSequentally()#%s: Suppressed exception in/before future for element %s" format (hash, h), ex)
  }

}


case class TraverseSequentallyException(inRest:TraversableOnce[Any], resultsAcc:Any, cause:Throwable)
  extends Exception("Traversing future exception", cause)

