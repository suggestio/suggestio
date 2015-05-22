package io.suggest.sc.sjs.m.mv.ctx.grid

import io.suggest.sc.ScConstants.Grid._
import io.suggest.sc.sjs.m.mv.ctx.{Cached, Id, Div, ClearableCache}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.05.15 11:59
 * Description: Кеш для tile.
 */
trait GridCtx extends ClearableCache {

  object grid
    extends ClearableCache
    with GridDiv
    with WrapperDiv
    with ContentDiv
    with ContainerDiv
    with LoaderDiv

  override def cachesClear(): Unit = {
    super.cachesClear()
    grid.cachesClear()
  }

}


/** Кеш для div'а всей плитки. */
trait GridDiv extends ClearableCache {
  object rootDiv extends Div with Id with Cached {
    override def id = ROOT_DIV_ID
  }

  override def cachesClear(): Unit = {
    super.cachesClear()
    rootDiv.cachesClear()
  }
}


/** tile wrapper div caching. */
trait WrapperDiv extends ClearableCache {
  object wrapperDiv extends Div with Id with Cached {
    override def id = WRAPPER_DIV_ID
  }

  override def cachesClear(): Unit = {
    super.cachesClear()
    wrapperDiv.cachesClear()
  }
}


/** tile div content. */
trait ContentDiv {
  object contentDiv extends Div with Id {
    override def id = CONTENT_DIV_ID
  }
}


/** tile container div. */
trait ContainerDiv {
  object containerDiv extends Div with Id {
    override def id = CONTAINER_DIV_ID
  }
}


/** tile loader div. */
trait LoaderDiv {
  object loaderDiv extends Div with Id {
    override def id = LOADER_DIV_ID
  }
}
