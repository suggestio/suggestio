package io.suggest.util

import java.util.concurrent.{Executor, ExecutionException}

import scala.concurrent.{ExecutionContext, Promise, Future}
import scala.util.{Success, Failure}
import scala.concurrent.duration.FiniteDuration
import java.util.TimerTask
import com.google.common.util.concurrent.{ListenableFuture => GListenableFuture}

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
  def mapLeftSequentally[A, B](in: Seq[A], ignoreErrors: Boolean = false)(f: A => Future[B])(implicit executor:ExecutionContext): Future[List[B]] = {
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
  def foldLeftSequental[A, Acc](in: Seq[A], acc0: Acc, ignoreErrors: Boolean = false)(f: (Acc, A) => Future[Acc])(implicit executor:ExecutionContext): Future[Acc] = {
    val p = Promise[Acc]()
    lazy val hash = in.hashCode() * f.hashCode() + acc0.hashCode()
    def foldLeftStep(acc:Acc, inRest:Seq[A]) {
      if (inRest.isEmpty) {
        // Закончить обход. Вернуть результат
        p success acc

      } else {
        // Продолжаем обход
        val h = inRest.head
        val fut: Future[Acc] = try {
          f(acc, h)
        } catch {
          case ex:Throwable => Future.failed(ex)
        }
        val t = inRest.tail
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


  private val timer = new java.util.Timer()

  /**
   * Сгенерить фьючерс, который будет исполнен указанным сообщением message:A через указанный интервал времени duration.
   * @param messageFut Фьючерс результата. Обычно, создается через Future.successful() или Future.failed().
   * @param duration Сколько ждать?
   * @tparam A Тип результата фьючерса.
   * @return Future[A]
   */
  def timeoutFuture[A](messageFut: Future[A], duration: FiniteDuration)(implicit ec: ExecutionContext): Future[A] = {
    val p = Promise[A]()
    val task = new TimerTask {
      def run() { p completeWith messageFut }
    }
    timer.schedule(task, duration.toMillis)
    p.future
  }


  private def warnError(hash:Int, h:Any, ex:Throwable) {
    warn("foldLeftSequentally()#%s: Suppressed exception in/before future for element %s" format (hash, h), ex)
  }


  /** Обычно (почти всегда) ExecutionContext является и java-executor'ом. Но всё же предостерегаемся. */
  def ec2jexecutor(ec: ExecutionContext): Executor = {
    ec match {
      case exc: Executor => exc
      case _ => scala.concurrent.ExecutionContext.Implicits.global
    }
  }

  /** Быстрая сборка scala future на базе произвольного фьючерса, поддерживающего listener'ы типа Runnable. */
  trait RunnableListenableFutureWrapper[T] {
    def _ec: ExecutionContext
    def getValue: T
    def addListener(runnable: Runnable, executor: Executor): Unit
    def executor = ec2jexecutor(_ec)

    def future: Future[T] = {
      val p = Promise[T]()
      val listener = new Runnable {
        override def run(): Unit = {
          try {
            p success getValue
          } catch {
            case eex: ExecutionException =>
              p failure eex.getCause
            case ex: Throwable =>
              p failure ex
          }
        }
      }
      // Обычно (почти всегда) ExecutionContext является и java-executor'ом. Но всё же предостерегаемся.
      addListener(listener, executor)
      p.future
    }
  }

  /**
   * Конверсия guava ListenableFuture к scala Future.
   * @param gfut java-фьючерс.
   * @tparam T Тип значения.
   * @return Экземпляр scala.concurrent.Future[T].
   */
  implicit def guavaFuture2scalaFuture[T](gfut: GListenableFuture[T])(implicit ec: ExecutionContext): Future[T] = {
    val w = new RunnableListenableFutureWrapper[T] {
      override def _ec: ExecutionContext = ec
      override def getValue: T = gfut.get()
      override def addListener(runnable: Runnable, executor: Executor): Unit = {
        gfut.addListener(runnable, executor)
      }
    }
    w.future
  }

}


case class TraverseSequentallyException(inRest: Seq[Any], resultsAcc: Any, cause: Throwable)
  extends Exception("Traversing future exception", cause)

