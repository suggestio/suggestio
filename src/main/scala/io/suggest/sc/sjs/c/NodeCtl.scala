package io.suggest.sc.sjs.c

import io.suggest.sc.sjs.m.msrv.index.MNodeIndex
import io.suggest.sc.sjs.v.render.direct.DirectRrr
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.05.15 12:13
 * Description: Контроллер для узлов. Начинался с переключения узлов.
 */
object NodeCtl {

  /**
   * Полная процедура переключения на другой узел.
   *
   * Логика работы исходной версии системы:
   * 1. Отправить запрос index.
   * 2. Запустить анимацию выхода из текущего состояния выдачи.
   * 3. Получив index, отобразить welcome, запустить получение списка карточек.
   * 4. Получив список карточек, сразу отрендерить его.
   * 5. Дождавшись завершения welcome, отобразить список карточек.
   *
   * @param adnIdOpt id узла, если известен.
   *                 None значит пусть сервер сам решит, на какой узел переключаться.
   */
  def switchToNode(adnIdOpt: Option[String]): Unit = {
    val inxFut = MNodeIndex.getIndex(adnIdOpt)
    for {
      minx <- inxFut
    } yield {
      println("index answer received: " + minx)
      DirectRrr.showIndex(minx)
      ???
    }
  }


}
