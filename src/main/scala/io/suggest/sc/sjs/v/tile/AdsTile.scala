package io.suggest.sc.sjs.v.tile

import io.suggest.sc.sjs.m.mv.IVCtx

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.05.15 17:15
 * Description: view плитки рекламный карточек. Он получает тычки от контроллеров или других view'ов,
 * и влияет на отображаемую плитку.
 */
object AdsTile {

  def adjustDom()(implicit vctx: IVCtx): Unit = {
    val div = vctx.tile.tileDiv()
    ???
  }


}
