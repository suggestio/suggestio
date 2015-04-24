package io.suggest.sjs.common.controller

import io.suggest.sjs.common.util.SjsLogs
import io.suggest.sjs.common.view.CommonPage
import org.scalajs.dom
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.04.15 14:29
 * Description: Трейт для сборки систем routed init. Суть работы системы:
 * - При запуске скрипта JSApp дергает RoutedInitImpl.init().
 * - Этот init считывает данные для инициализации из data-аттрибутов тега body:
 *   &lt; body data-ri="ctl1[: action1[, ...] [; ctl2: ...] ]" &gt;
 *   Пример значения аттрибута data-ri:
 *   data-ri="ctl1: action1, action2; ctl2: action3; ctl3."
 * - В ходе обработки аттрибутов автоматически происходят вызовы для инициализации:
 *   RoutedInitImpl.getController("ctl1") => Option[Ctl1]
 *   Ctl1.init() => Future[_]
 *   Ctl1.riAction("action1") => Future[_]
 *   Ctl1.riAction("action2") => Future[_]
 *   ...
 *   RoutedInitImpl.initFinished() => _
 *
 * Таким образом можно эффективно управлять инициализацией используемого кода в fat js.
 * Реализация формируется с помощью stackable trait pattern.
 *
 * Отсутсвующие контроллеры или экшены будут отображены в логах, но инициализация при этом не прерывается.
 *
 * @see [[http://stackoverflow.com/a/9059603 Идея в общих чертах, но через стандартные аттрибутов]].
 * @see [[http://viget.com/inspire/extending-paul-irishs-comprehensive-dom-ready-execution Причины использования data-аттрибутов]].
 */

object RoutedInit {

  /** Название аттрибута для тега body, куда записывается инфа для направленной инициализации. */
  def RI_ATTR_NAME = "data-ri"

  /** Метод для нижеуказанных трейтов, чтобы быстро генерить пустой успех работы. */
  protected [controller] def done = Future successful None

}


import RoutedInit._


/** Заготовка главного контроллера, который производит инициализацию компонентов в контексте текущей страницы.
  * Контроллеры объединяются в единый роутер через stackable trait pattern. */
trait RoutedInit extends SjsLogs {

  /** Запуск системы инициализации. Этот метод должен вызываться из main(). */
  def init(): Future[_] = {
    val attrName = RI_ATTR_NAME
    val attrRaw = CommonPage.body.getAttribute(attrName)
    val attrOpt = Option(attrRaw)
      .map { _.trim }
      .filter { !_.isEmpty }
    attrOpt match {
      case Some(attr) =>
        val allCtlsAndActs = attr.split("\\s*;\\s*").toSeq
        Future.traverse(allCtlsAndActs) { next =>
          val fut = try {
            initCtlAdnActs(next)
          } catch {
            case ex: Throwable => Future failed ex
          }
          // Подавленеи ошибок. init must flow.
          fut recover { case ex: Throwable =>
            dom.console.error("Failed to initialize sjs controller: " + next + " " + ex.getMessage)
            None
          }
        }

      case None =>
        log("No RI data found in body (" + attrName + "). Initialization skipped.")
        done
    }
  }

  /** Инициализация одного контроллера и его экшенов на основе переданной спеки. */
  protected def initCtlAdnActs(raw: String): Future[_] = {
    val l = raw.split("\\s*:\\s*").toList
    if (l.nonEmpty) {
      // Есть что-то, похожее на спеку контроллера.
      val ctlName = l.head
      val tl = l.tail
      getController(ctlName) match {
        case Some(ctl) =>
          // Инициализируем контроллер Future(), чтобы удобнее комбинировать и перехватывать ошибки.
          Future {
            ctl.riInit()
          } flatMap { _ =>
            if (tl.nonEmpty) {
              val actsRaw = tl.head
              val acts = actsRaw.split("\\s*,\\s*").toSeq
              Future.traverse(acts) { act =>
                ctl.riAction(act)
              }
            } else {
              // Нет экшенов. Видимо разработчик подразумевал только инициализацию соотв.контроллера.
              done
            }
          }

        case None =>
          error("Controller not found: " + ctlName + " . Following actions are skipped: " + tl.headOption.getOrElse(""))
          done
      }
      
    } else {
      // Не получается распарсить описанный контроллер и вызываемые экшены.
      error("Controller init spec is invalid and skipped: " + raw)
      done
    }
  }

  /** Поиск ri-контроллера с указанным именем (ключом).
    * Реализующие трейты должны переопределять этот метод под себя, сохраняя super...() вызов. */
  protected def getController(name: String): Option[RoutedInitController] = {
    None
  }

}


/** Интерфейс init-контроллера, занимающегося роутингом экшенов.
  * Экшены объединяются через stackable trait pattern. */
trait RoutedInitController extends SjsLogs {

  /** Синхронная инициализация контроллера, если необходима. */
  def riInit(): Unit = {}

  /**
   * Запустить на исполнение экшен.
   * @param name Название экшена, заданное в body.
   * @return Фьючерс с результатом.
   */
  def riAction(name: String): Future[_] = {
    error("Action ''" + name + "'' not found in controller " + getClass.getName + ". Skipping...")
    done
  }

}
