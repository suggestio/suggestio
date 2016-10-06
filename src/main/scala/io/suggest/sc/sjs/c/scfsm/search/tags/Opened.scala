package io.suggest.sc.sjs.c.scfsm.search.tags

import io.suggest.sc.sjs.c.scfsm.search.Base
import io.suggest.sc.sjs.m.msearch.{TagRowClick, MTabs}
import io.suggest.sc.sjs.util.router.srv.routes
import io.suggest.sc.sjs.vm.search.fts.SInput
import io.suggest.sc.sjs.vm.search.tabs.htag.{StListRow, StList}
import io.suggest.sjs.common.msg.{WarnMsgs, ErrorMsgs}
import io.suggest.sjs.common.tags.search.{MTagsSearch, MTagSearchRespTs, MTagSearchResp, MTagSearchArgs}
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx

import scala.util.{Failure, Success}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.08.15 17:19
 * Description: Аддон для сборки состояния нахождения юзера на раскрытой панели поиска со вкладкой тегов.
 */
trait Opened extends Base {

  /** Состояния поиска по хеш-тегам. */
  protected trait OnSearchTagsStateT extends OnSearchStateT {
  }


}
