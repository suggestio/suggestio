package io.suggest.xadv.ext.js.fb.c.hi

import io.suggest.xadv.ext.js.fb.c.low.FbLow
import io.suggest.xadv.ext.js.fb.m.FbInitOptions

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.03.15 18:01
 * Description: Высокоуровневая обвязка над facebook js API. Скрывает всю сериализацию-десериализацию и прочее.
 */

object Fb {

  /**
   * Высокоуровневая асинхронная инициализация facebook js API.
   * @param opts Экземпляр параметров инициализации.
   * @return Фьчерс
   */
  def init(opts: FbInitOptions): Future[_] = {
    Future {
      FbLow init opts.toJson
    }
  }

}
