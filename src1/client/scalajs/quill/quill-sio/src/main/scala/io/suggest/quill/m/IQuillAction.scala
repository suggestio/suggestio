package io.suggest.quill.m

import com.quilljs.delta.Delta
import io.suggest.sjs.common.spa.DAction

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.09.17 10:57
  * Description: Экшены diode-редактора.
  */
sealed trait IQuillAction extends DAction


/** Юзер редактирует текст.
  *
  * @param fullDelta Обновлённый полный текст в quill-delta-формате.
  * @param html строка готового HTML-рендера.
  */
case class TextChanged(fullDelta: Delta, html: String) extends IQuillAction

