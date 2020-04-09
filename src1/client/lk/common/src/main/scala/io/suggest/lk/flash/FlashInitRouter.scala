package io.suggest.lk.flash

import io.suggest.flash.FlashConstants._
import io.suggest.init.routed.InitRouter
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.common.util.SafeSyncVoid
import io.suggest.sjs.dom2.DomQuick
import japgolly.univeq._
import org.scalajs.dom
import org.scalajs.dom.Element
import org.scalajs.jquery._

import scala.concurrent.{Future, Promise}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.04.15 11:15
 * Description: js для flash-нотификаций. Тут код для скрытия этих нотификаций.
 * Тут нет контроллера, т.к. нотификации очень всеобъемлющи и затрагивают многие части сайта.
 */

/** Аддон для сборки роутера инициализации с поддержкой flashing-уведомлений. */
trait FlashInitRouter extends InitRouter {

  override protected def routeInitTarget(itg: MJsInitTarget): Unit = {
    if (itg ==* MJsInitTargets.Flashing) {
      new FlashInit()
        .init()
    } else {
      super.routeInitTarget(itg)
    }
  }

}


/** Реализация контроллера, который занимается инициализацией flashing-уведомлений. */
class FlashInit extends SafeSyncVoid with Log {

  /** Запуск инициализации flash-уведомлений. */
  def init(): Unit = {
    _safeSyncVoid { () =>
      executeFlash()
    }
  }


  /** Запустить отображение уведомлений, если вообще есть что отображать. */
  private def executeFlash(): Unit = {
   // Для эффективной работы, вместо всего document используем div с флешами, искомый по id.
    val containers = jQuery("#" + CONTAINER_ID)
    if (containers.length > 0) {
      // Нужно сделать последовательное отображение и сокрытие нескольких уведомлений, а не параллельное. Используем цепочку из Future.
      val bars = containers.find("." + BAR_CLASS)
      val allFut = bars
        .toArray()
        .iterator
        .map { _.asInstanceOf[Element] }
        // Обойти, накапливая общий фьючерс. Таким образом в браузере запрограммируется цепочное отображение уведомлений.
        .foldLeft[Future[_]] (Future.successful(None)) { (fut, el) =>
          // Открыть уведомление, когда предыдущее будет закрыто. И повесить но новооткрытое уведомление обработчики.
          fut.flatMap { _ =>
            processBar(el)
          }
        }
      // Разом удалить все уведомления из dom вместе с листенерами и т.д. для высвобождения ресурсов.
      allFut.onComplete { _ =>
        containers.remove()
      }
    } // if (containers)
  }

  /** Выполнить процессинг одной панели уведомления: отобразить, повесить события, вернуть фьючерс сворачивания. */
  private def processBar(el: Element): Future[_] = {
    val bar = jQuery(el)
    _openBar(bar)
    // Нужно сворачивать уведомление по клику или таймауту.
    val closedP = Promise[None.type]()
    // Код сворачивания вызывается через это замыкание:
    val doClose = { () =>
      if (!closedP.isCompleted) {
        _closeBar(bar)
        // Анимации требуется некоторое время, чтобы завершиться, поэтому запускаем исполнения Promise'а в очередь.
        DomQuick.setTimeout(SLIDE_DURATION_MS) { () =>
          closedP.success(None)
        }
      }
    }
    // Сворачивать при таймауте.
    val timeoutId = DomQuick.setTimeout(SHOW_TIMEOUT_MS)(doClose)
    // Сворачивать при клике.
    bar.click { (e: JQueryEventObject) =>
      dom.window.clearTimeout(timeoutId)
      doClose()
    }
    // Вернуть новый фьючерс как аккамулятор
    val fut1 = closedP.future
    fut1
  }

  // TODO Нужно понять, нужно ли отрабатывать isOpened() вообще? Это дело перекопировано из mx_cof.coffee.

  /** Является ли раскрытым текущий bar? */
  private def _isOpened(b: JQuery): Boolean = {
    Option( b.data(IS_OPENED_DATA_ATTR) )
      .map(_.toString)
      .contains(OPENED_VALUE)
  }

  /** Выставить флаг открытости. */
  private def _setOpened(b: JQuery, isOpened: Boolean): Unit = {
    b.data(IS_OPENED_DATA_ATTR, OPENED_VALUE)
  }

  /** Закрыть указанный бар. */
  private def _closeBar(b: JQuery): Unit = {
    if (_isOpened(b)) {
      _setOpened(b, isOpened = false)
      b.slideUp(SLIDE_DURATION_MS)
    }
  }

  /** Раскрыть указанный bar. */
  private def _openBar(b: JQuery): Unit = {
    if (!_isOpened(b)) {
      _setOpened(b, isOpened = true)
      b.slideDown(SLIDE_DURATION_MS)
    }
  }

}
