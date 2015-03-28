package io.suggest.xadv.ext.js.runner.c

import scala.collection.immutable.Queue
import scala.concurrent.{Promise, Future}
// runNow используется для скорейшего переключения на следующий попап без всей остальной очереди.
import scala.scalajs.concurrent.JSExecutionContext.runNow
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.03.15 21:20
 * Description: Очередь попапов. Без очереди будет возникать проблема замусоривания
 * экрана клиента запросами и уведомления. Очередь позволяет гарантировать наличие только одного
 * попапа на экране.
 *
 * В нулевой версии архитектуры adv-ext контроль вел сервер путем удержания запросов к адаптерам.
 * Однако, развитие такой архитектуры привело бы к ненужной сериализации-десериализации кучи данных,
 * сильным бессмысленным трафиком по вебсокету и просто серьезным усложнениям архитектуры адаптеров.
 */
trait IPopupQueue {

  def enqueue[T](f: () => Future[T]): Future[T]
}


// TODO Возможно, в целях безопасности, нужно реализацию вызывать через минимальный враппер. js всё-таки.

/** Дефолтовая реализация менеджера очереди попапов. */
class PopupQueueImpl extends IPopupQueue {

  /** Абстрактный экземпляр одной задачи в очереди. Трейт, чтобы начисто скрыть параметризованный тип отовсюду. */
  trait Task {
    /** Тип значения, возвращаемого внутри фьючерс _f(). */
    type TT
    /** Исходный генератор попапа. */
    def _f: () => Future[TT]
    /** Созданный promise для будущего результата _f(). */
    def _p: Promise[TT]

    /** Запуск _f(), сцепливание его с _p и с логикой обработки очереди. */
    def run(): Unit = {
      try {
        val fut = _f()
        // Когда всё закончится, нужно запустить проверку очереди на наличие новых попапов или же сбросить флаг занятости.
        fut.onComplete { case _ =>
          popupFinished()
        }(runNow)
        // Когда обработка попапов завершена, то нужно сообщить об этом клиенту.
        _p completeWith fut

      } catch {
        case ex: Throwable => _p failure ex
      }
    }
  }

  /** Очередь. */  // TODO Следует ли использовать здесь изменяемую очередь?
  protected [this] var _queue: Queue[Task] = Queue()

  /** А открыт ли сейчас какой-либо popup? */
  protected [this] var _isPopupNow: Boolean = false


  /**
   * Отправить в очередь на отображение логику отображения нового попапа.
   * @param f Функция вызова, порождающая попап.
   * @tparam T Тип результата.
   * @return Фьючерс с результатом.
   */
  override def enqueue[T](f: () => Future[T]): Future[T] = {
    val p1 = Promise[T]()
    val task = new Task {
      override type TT = T
      override def _p: Promise[TT] = p1
      override def _f: () => Future[TT] = f
    }
    if (_queue.nonEmpty || _isPopupNow) {
      _queue = _queue.enqueue(task)
    } else {
      task.run()
      _isPopupNow = true
    }
    p1.future
  }


  /** Когда обработка одного попапа завершена, надо вызвать этот метод для прокручивания очереди вперед. */
  protected def popupFinished(): Unit = {
    if (_queue.isEmpty) {
      _isPopupNow = false
    } else {
      val (task, q1) = _queue.dequeue
      _queue = q1
      task.run()
      //_isPopupNow = true
    }
  }

}
