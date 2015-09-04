package io.suggest.sjs.common.controller

import io.suggest.init.routed.{MJsInitTargetsLigthT, JsInitConstants}
import io.suggest.sjs.common.util.{ISjsLogger, SafeSyncVoid}
import io.suggest.sjs.common.vm.doc.DocumentVm
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
 * 2015.apr.27: RoutedInit переименован в InitRouter.
 *              RoutedInitController переименован в InitController.
 *
 * @see [[http://stackoverflow.com/a/9059603 Идея в общих чертах, но через стандартные аттрибутов]].
 * @see [[http://viget.com/inspire/extending-paul-irishs-comprehensive-dom-ready-execution Причины использования data-аттрибутов]].
 */

object InitRouter {

  /** Метод для нижеуказанных трейтов, чтобы быстро генерить пустой успех работы. */
  protected [controller] def done = Future successful None

}


import InitRouter._


/** Заготовка главного контроллера, который производит инициализацию компонентов в контексте текущей страницы.
  * Контроллеры объединяются в единый роутер через stackable trait pattern. */
trait InitRouter extends ISjsLogger with SafeSyncVoid {

  /** Модель таргетов используется только в роутере, поэтому она тут и живет. */
  object MInitTargets extends MJsInitTargetsLigthT

  /** Тип одного экземпляра модели целей инициализации. Вынесен сюда для удобства построения API. */
  final type MInitTarget = MInitTargets.T

  /** Инициализация одной цели. IR-аддоны должны перезаписывать по цепочке этот метод своей логикой. */
  protected def routeInitTarget(itg: MInitTarget): Future[_] = {
    warn("JS init target not supported: " + itg)
    done
  }

  /** Запуск системы инициализации. Этот метод должен вызываться из main(). */
  def init(): Future[_] = {
    val attrName = JsInitConstants.RI_ATTR_NAME
    val body = DocumentVm().body
    val attrRaw = body.getAttribute(attrName)
    val attrOpt = Option(attrRaw)
      .map { _.trim }
      .filter { !_.isEmpty }
    attrOpt match {
      case Some(attr) =>
        val all = attr.split("\\s*;\\s*")
          .toSeq
          .flatMap { raw =>
            val res = MInitTargets.maybeWithName(raw)
            if (res.isEmpty)
              warn(MInitTargets.getClass.getSimpleName + " does not contain target '" + raw + "'")
            res
          }
        val initFut = Future.traverse(all) { itg =>
          val fut = try {
            routeInitTarget(itg)
          } catch {
            case ex: Throwable => Future failed ex
          }
          // Подавленеи ошибок. init must flow.
          fut recover { case ex: Throwable =>
            error("Init failed to reach target " + itg, ex)
            None
          }
        }
        // Аттрибут со спекой инициализации больше не нужен, можно удалить его.
        body.removeAttribute(attrName)
        initFut

      case None =>
        log("No RI data found in body (" + attrName + "). Initialization skipped.")
        done
    }
  }

}

