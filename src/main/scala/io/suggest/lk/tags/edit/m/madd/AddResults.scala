package io.suggest.lk.tags.edit.m.madd

import org.scalajs.dom.XMLHttpRequest

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.09.15 16:17
 * Description: Модели, описывающие распарсенное возвращаемое значение addTag-экшена.
 */
trait IAddResult

/** Возникла проблема при биндинге формы. */
case class AddFormError(formHtml: String)
  extends IAddResult

/** Тег добавлен в список существующих. Сервер вернул новый отрендеренный список тегов. */
case class UpdateExisting(existingHtml: String)
  extends IAddResult

/** Какая-то другая ошибка запроса произошла. */
case class UnexpectedResponse(xhr: XMLHttpRequest)
  extends IAddResult
