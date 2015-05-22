package io.suggest.sc.sjs.m.mv.ctx.layout

import io.suggest.sc.ScConstants.Layout._
import io.suggest.sc.sjs.m.mv.ctx.{Cached, Div, Id, ClearableCache}

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
trait RootDiv extends ClearableCache { that =>

  object rootDiv extends Div with Id with Cached {
    override def id = ROOT_ID

    /** Замена корневого div означает, что надо сбросить кеши во всем layout. */
    override def set(v: T): Unit = {
      that.cachesClear()
      super.set(v)
    }
  }

  override def cachesClear(): Unit = {
    super.cachesClear()
    rootDiv.cachesClear()
  }

}


/** Аддон кеша для layout div. */
trait LayoutDiv extends ClearableCache {

  object layoutDiv extends Div with Id with Cached {
    override def id: String = LAYOUT_ID
  }

  override def cachesClear(): Unit = {
    super.cachesClear()
    layoutDiv.cachesClear()
  }
}
