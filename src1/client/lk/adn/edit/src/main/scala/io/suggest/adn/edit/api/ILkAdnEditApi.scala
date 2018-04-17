package io.suggest.adn.edit.api

import diode.ModelRO
import io.suggest.adn.edit.m.{MAdnEditForm, MAdnEditFormConf}
import io.suggest.pick.MimeConst
import io.suggest.proto.HttpConst
import io.suggest.routes.routes
import io.suggest.sjs.common.xhr.Xhr
import play.api.libs.json.Json

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.04.18 21:37
  * Description: Серверное API редактора карточек.
  */
trait ILkAdnEditApi {

  def save(form: MAdnEditForm): Future[MAdnEditForm]

}


class LKAdnEditApiHttp( confRO: ModelRO[MAdnEditFormConf] ) extends ILkAdnEditApi {

  override def save(form: MAdnEditForm): Future[MAdnEditForm] = {
    val jsonMime = MimeConst.APPLICATION_JSON
    Xhr.unJsonResp[MAdnEditForm] {
      Xhr.requestJsonText(
        route = routes.controllers.LkAdnEdit.save( confRO.value.nodeId ),
        timeoutMsOpt = Some(5000),
        body = Json.toJson( form ).toString(),
        headers = List(
          HttpConst.Headers.CONTENT_TYPE  -> jsonMime,
          HttpConst.Headers.ACCEPT        -> jsonMime
        )
      )
    }
  }

}
