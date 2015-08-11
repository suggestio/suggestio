package io.suggest.sc.sjs.c.scfsm

import io.suggest.sc.sjs.m.msearch.MTabs

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.08.15 17:19
 * Description: Аддон для сборки состояния нахождения юзера на раскрытой панели поиска со вкладкой хеш-тегов.
 */
trait OnGridSearchHashTags extends OnGridSearch {

  /** Состояния поиска по хеш-тегам. */
  protected trait OnGridSearchHashTagsStateT extends OnGridSearchStateT {

    override protected def _nowOnTab = MTabs.HashTags

    // TODO receiverPart(): Тут нужны сигналы выбора хеш-тега в списке и получения ответа от сервера.

    /** Запуск поискового запроса в рамках текущего состояния. */
    override protected def _ftsLetsStartRequest(): Unit = {
      // TODO Собрать поисковый реквест хеш-тегов, отправить на сервер, отработать получение ответа (отрендерить список хеш-тегов).
      ???
    }
  }

}
