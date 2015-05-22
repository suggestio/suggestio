package io.suggest.sc.sjs.m.mv.ctx.layout

import io.suggest.sc.ScConstants.Layout._
import io.suggest.sc.sjs.m.mv.ctx.{Div, IdFind, ClearableCache}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.05.15 12:09
 * Description: Аддоны для контекста для добавления кеширования элементов layout.
 */
trait LayoutCache extends ClearableCache {

  object layout
    extends ClearableCache
    with RootDiv
    with LayoutDiv

  override def cachesClear(): Unit = {
    super.cachesClear()
    layout.cachesClear()
  }

}


/** Аддон кеша для layout root div. */
trait RootDiv extends ClearableCache {

  object rootDiv extends Div with IdFind {
    override def id: String = ROOT_ID
  }

  override def cachesClear(): Unit = {
    super.cachesClear()
    rootDiv.cachesClear()
  }

}


/** Аддон кеша для layout div. */
trait LayoutDiv extends ClearableCache {

  object layoutDiv extends Div with IdFind {
    override def id: String = LAYOUT_ID
  }

  override def cachesClear(): Unit = {
    super.cachesClear()
    layoutDiv.cachesClear()
  }
}
