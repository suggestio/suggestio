package io.suggest.quill.m

import com.quilljs.delta.Delta
import io.suggest.spa.DAction
import org.scalajs.dom

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.09.17 10:57
  * Description: Экшены diode-редактора.
  */
sealed trait IQuillAction extends DAction


/** Юзер редактирует текст.
  *
  * @param diff Дельта изменений.
  *             Используется для выявления свежедобавленных картинок, чтобы их на сервер сливать в фоне.
  *             Например, добавление конца строки: {"ops":[{"retain":12},{"insert":"\n"}]}
  * @param fullDelta Обновлённый полный текст в quill-delta-формате.
  */
case class TextChanged(diff: Delta, fullDelta: Delta) extends IQuillAction


/** Закинуть оригинал файла в состояние, чтобы потом обработать.
  *
  * @param b64Url URL, который будет использован в тексте.
  * @param file Блоб файла.
  */
case class EmbedFile(b64Url: String, file: dom.File) extends IQuillAction
