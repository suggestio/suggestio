package io.suggest.adn.edit.api

import diode.ModelRO
import io.suggest.adn.edit.m.{MAdnEditForm, MAdnEditFormConf}
import io.suggest.routes.routes
import io.suggest.sjs.common.xhr.{HttpReq, HttpReqData, Xhr}
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
    val req = HttpReq.routed(
      route = routes.controllers.LkAdnEdit.save( confRO.value.nodeId ),
      data  = HttpReqData(
        timeoutMs = Some(5000),
        body      = Json.toJson( form ).toString(),
        headers   = HttpReqData.headersJsonSendAccept
      )
    )
    Xhr.execute(req)
      .respAuthFut
      .successIf200
      .unJson[MAdnEditForm]
  }

}
