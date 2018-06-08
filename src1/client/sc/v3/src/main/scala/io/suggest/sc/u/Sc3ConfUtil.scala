package io.suggest.sc.u

import io.suggest.msg.ErrorMsgs
import io.suggest.sc.sc3.MSc3Init
import io.suggest.sjs.common.log.Log
import io.suggest.spa.StateInp
import play.api.libs.json.Json

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.06.18 18:34
  * Description: Конфигурация выдачи.
  */
object Sc3ConfUtil extends Log {

  /** Прочитать конфиг из DOM. */
  def initFromDom(): Option[MSc3Init] = {
    for {
      stateInput <- StateInp.find()
      jsonConfig <- stateInput.value
      scInitParsed <- {
        val jsonParseRes = Json
          .parse(jsonConfig)
          .validate[MSc3Init]
        if (jsonParseRes.isError)
          LOG.error( ErrorMsgs.JSON_PARSE_ERROR, msg = jsonParseRes )
        jsonParseRes.asOpt
      }
    } yield {
      scInitParsed
    }
  }

  // TODO Получить конфиг с сервера и вызывать при запуске.

}
