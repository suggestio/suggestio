package io.suggest.sc.sjs.m

import io.suggest.sc.sjs.v.render.IRenderer

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.05.15 19:06
 * Description: View Context.
 * Модель, занимающаяся представлением текущего состояния отображения выдачи для view:
 * - Текущий рендерер, аниматор и прочие компоненты.
 * - Текущее состояние.
 * - Доступ к состоянию приложения в целом.
 * - Контроллеры: доступ к экземпляру контроллеров для передачи действий юзера в контроллеры.
 * Модель собирает/поддерживает контроллер, передавая её экземпляр в вызовы к view выдачи.
 */
trait IViewCtx {

  /** Текущий renderer, используемый для вывода данных на экран. */
  def renderer: IRenderer

}

