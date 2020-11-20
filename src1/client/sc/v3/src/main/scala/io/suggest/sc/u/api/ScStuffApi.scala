package io.suggest.sc.u.api

import io.suggest.proto.http.client.HttpClient
import io.suggest.proto.http.model.{HttpClientConfig, HttpReq, HttpReqData}
import io.suggest.routes.routes
import io.suggest.sc.index.{MSc3IndexResp, MScIndexes}
import play.api.libs.json.Json

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.11.2020 18:06
  * Description: API для доступа к ScStuff-контроллеру.
  */
trait IScStuffApi {

  /** Заполнение корректными данными указанного списка узлов.
    *
    * @param currNodes Перечисление текущих узлов.
    * @return Фьючерс со списком узлов из ответа.
    */
  def fillNodesList( currNodes: MScIndexes ): Future[List[MSc3IndexResp]]

}


final class ScStuffApiHttp(
                            httpClientConfig: () => HttpClientConfig,
                          )
  extends IScStuffApi
{

  override def fillNodesList(currNodes: MScIndexes): Future[List[MSc3IndexResp]] = {
    // TODO indexes2 - надо почистить, оставив только id узлов, координаты и может что-то ещё.
    HttpClient
      .execute(
        HttpReq.routed(
          route = routes.controllers.sc.ScStuff.fillNodesList(),
          data = HttpReqData(
            headers = HttpReqData.headersJsonSendAccept,
            body = Json
              .toJson( currNodes )
              .toString(),
            timeoutMs = Some( 5000 ),
            config = httpClientConfig(),
          )
        )
      )
      .resultFut
      .unJson[List[MSc3IndexResp]]
  }

}
