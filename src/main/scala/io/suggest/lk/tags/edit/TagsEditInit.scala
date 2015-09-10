package io.suggest.lk.tags.edit

import io.suggest.lk.tags.edit.fsm.TagsEditFsm
import io.suggest.sjs.common.controller.IInit

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.09.15 15:22
 * Description: Поддержка работы подсистемы редактирования тегов.
 */
trait TagsEditInit extends IInit {

  abstract override def init(): Unit = {
    super.init()
    _initTagsEditor()
  }

  /** Запуск инициализации подсистемы редактора тегов на странице. */
  private def _initTagsEditor(): Unit = {
    TagsEditFsm.start()
  }

}
