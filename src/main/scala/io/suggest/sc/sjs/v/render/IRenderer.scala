package io.suggest.sc.sjs.v.render

import io.suggest.adv.ext.model.im.ISize2di
import io.suggest.sc.sjs.m.IAppState
import io.suggest.sc.sjs.m.msrv.index.MNodeIndex

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.05.15 18:01
 * Description: Интерфейс рендерера.
 */
trait IRenderer {

  /**
   * Принудительно узнать размеры viewport'а.
   * @return Двумерный размер в пикселях.
   *         NoSuchElementException, если viewport отсутствует.
   */
  def viewportSize: ISize2di

  /**
   * Отобразить node index на экран.
   * @param minx Данные MNodeIndex от сервера.
   * @param state Доступ к состоянию приложения.
   * @return void, когда всё закончится.
   */
  def showIndex(minx: MNodeIndex)(implicit state: IAppState): Unit

}
