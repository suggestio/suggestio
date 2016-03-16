package io.suggest.lk.tags.edit.m.madd

import io.suggest.lk.tags.edit.vm.add.AContainer
import org.scalajs.dom.XMLHttpRequest

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.09.15 16:17
 * Description: Модели, описывающие распарсенное возвращаемое значение addTag-экшена.
 */
trait IAddResult

trait IAddFormHtml extends IAddResult {
  def addFormHtml: String

  def addFormVm = AContainer.ofHtml(addFormHtml)
}

/** Возникла проблема при биндинге формы. */
case class AddFormError(addFormHtml: String)
  extends IAddFormHtml


/** Какая-то другая ошибка запроса произошла. */
case class UnexpectedResponse(xhr: XMLHttpRequest)
  extends IAddResult
