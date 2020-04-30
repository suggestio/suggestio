package io.suggest.init.routed

import io.suggest.msg.ErrorMsgs
import io.suggest.log.Log
import io.suggest.sjs.common.util.SafeSyncVoid
import io.suggest.sjs.common.vm.doc.DocumentVm

/** Заготовка главного контроллера, который производит инициализацию компонентов в контексте текущей страницы.
  * Контроллеры объединяются в единый роутер через stackable trait pattern. */
trait InitRouter extends Log with SafeSyncVoid {

  // TODO Заинлайнить этот тип и модель
  /** Модель таргетов используется только в роутере, поэтому она тут и живет. */
  val MJsInitTargets = io.suggest.init.routed.MJsInitTargets

  /** Тип одного экземпляра модели целей инициализации. Вынесен сюда для удобства построения API. */
  final type MJsInitTarget = io.suggest.init.routed.MJsInitTarget


  /** Инициализация одной цели. IR-аддоны должны перезаписывать по цепочке этот метод своей логикой. */
  protected def routeInitTarget(itg: MJsInitTarget): Unit = {
    logger.error( ErrorMsgs.INIT_ROUTER_KNOWN_TARGET_NOT_SUPPORTED, msg = itg)
  }

  /** Запуск системы инициализации. Этот метод должен вызываться из main(). */
  def init(): Unit = {
    val attrName = JsInitConstants.RI_ATTR_NAME
    val body = DocumentVm().body
    val attrRaw = body.getAttribute(attrName)

    val attrOpt = Option(attrRaw)
      .map { _.trim }
      .filter { !_.isEmpty }

    attrOpt.fold [Unit] {
      logger.log( ErrorMsgs.INIT_ROUTER_NO_TARGET_SPECIFIED )

    } { attr =>
      val all = attr.split("\\s*;\\s*")
        .iterator
        .flatMap { raw =>
          val res = MJsInitTargets.withValueOpt(raw)
          if (res.isEmpty)
            logger.warn( ErrorMsgs.NOT_IMPLEMENTED, msg = raw )
          res
        }
        .toSeq
      for (itg <- all) {
        try {
          routeInitTarget(itg)
        } catch {
          case ex: Throwable =>
            logger.error(ErrorMsgs.INIT_ROUTER_TARGET_RUN_FAIL, ex, itg)
        }
      }
      // Аттрибут со спекой инициализации больше не нужен, можно удалить его.
      body.removeAttribute(attrName)
    }
  }

}
