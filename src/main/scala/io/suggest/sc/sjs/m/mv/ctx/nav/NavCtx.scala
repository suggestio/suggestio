package io.suggest.sc.sjs.m.mv.ctx.nav

import io.suggest.sc.sjs.m.mv.ctx.{Div, Id, ClearableCache}
import io.suggest.sc.ScConstants.NavPane._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.05.15 15:31
 * Description: Аддон view-контекста для поддержки быстрого доступа к панели навигации.
 */
trait NavCtx extends ClearableCache {

  object nav
    extends ClearableCache
    with RootDiv
    with NodeListDiv
    with WrapperDiv
    with ContentDiv
    with ShowPaneBtn

  override def cachesClear(): Unit = {
    super.cachesClear()
    nav.cachesClear()
  }

}


trait RootDiv {
  object rootDiv extends Div with Id {
    override def id = ROOT_ID
  }
}


trait NodeListDiv {
  object nodeListDiv extends Div with Id {
    override def id = NODE_LIST_ID
  }
}


trait WrapperDiv {
  object wrapperDiv extends Div with Id {
    override def id = WRAPPER_ID
  }
}


trait ContentDiv {
  object contentDiv extends Div with Id {
    override def id = CONTENT_ID
  }
}


trait ShowPaneBtn {
  object showPaneBtn extends Div with Id {
    override def id = SHOW_PANE_BTN_ID
  }
}
