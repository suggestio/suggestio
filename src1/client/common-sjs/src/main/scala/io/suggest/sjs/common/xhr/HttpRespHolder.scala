package io.suggest.sjs.common.xhr

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.12.18 12:55
  * Description: Модель асинхронного http-ответа, абстрагированная от нижележащего http-механизма.
  * Содержит фьючерс внутри.
  */
trait HttpRespHolder {

  /** Вернуть обёртку результата запроса. */
  def respFut: Future[HttpResp]

  // TODO Поддержка abort() через AbortController.
  // TODO Поддержка progress'а (для upload'а).

}


object HttpRespHolder {

  implicit class HrhOpsExt( val httpRespHolder: HttpRespHolder ) extends AnyVal {

    /** Вернуть обёртку результата запроса, но делать page reload, если истекла сессия. */
    def respAuthFut: Future[HttpResp] = {
      httpRespHolder
        .respFut
        .reloadIfUnauthorized()
    }

  }

}

