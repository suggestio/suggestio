package io.suggest.lk.tags.edit.m.madd

import io.suggest.sjs.common.model.Route
import io.suggest.sjs.common.xhr.Xhr
import org.scalajs.dom.FormData

import scala.concurrent.{Future, ExecutionContext}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.09.15 18:05
 * Description: Модель для dry-run запроса добавления тега в форму на экране.
 */
object MTagAdd {

  def add(route: Route, body: FormData)(implicit ec: ExecutionContext): Future[IAddResult] = {
    // Собрать и запустить запрос:
    val respFut = Xhr.send(
      route   = route,
      body    = Some(body)
    )

    // Результат запроса распарсить и завернуть в IAddResult.
    for (xhr <- respFut) yield {
      val s = xhr.status
      //print("add tag xhr -> " + s + " " + xhr.statusText)
      if (s == 200)
        UpdateExisting( xhr.responseText )
      else if (s == 406)
        AddFormError( xhr.responseText )
      else
        UnexpectedResponse( xhr )
    }
  }

}
