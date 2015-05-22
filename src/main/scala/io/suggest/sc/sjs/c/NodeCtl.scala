package io.suggest.sc.sjs.c

import io.suggest.sc.sjs.m.msrv.index.MNodeIndex
import io.suggest.sc.sjs.v.grid.GridView
import io.suggest.sc.sjs.v.inx.ScIndex
import io.suggest.sc.sjs.v.layout.Layout
import io.suggest.sc.sjs.v.nav.NavPaneView
import org.scalajs.dom
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.05.15 12:13
 * Description: Контроллер для узлов. Начинался с переключения узлов.
 */
object NodeCtl extends CtlT {

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
    implicit val _vctx = vctx
    for {
      minx <- inxFut
    } yield {
      Layout.reDrawLayout()(_vctx)
      ScIndex.showIndex(minx)(_vctx)

      GridView.adjustDom()(_vctx)
      NavPaneView.adjustNodeList()(_vctx)

      // TODO Нужен ли тут этот setTimeout()? Может можно без него вообще или через Future()?
      dom.setTimeout(
        { () => GridView.attachEvents()(_vctx) },
        200
      )

      /*if (minx.isGeo) {
        ???
        NavPaneView.showNavShowBtn(isShown = true)(_vctx)
      }*/

      // Инициализация welcomeAd.

      ???
    }
  }


}
