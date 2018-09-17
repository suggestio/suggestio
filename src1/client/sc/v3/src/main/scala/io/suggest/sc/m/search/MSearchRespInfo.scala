package io.suggest.sc.m.search

import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.07.18 22:11
  * Description: js-only модель-контейнер, для хранения клиентских пометок к поисковым ответам.
  * Необходима для хранения данных по состояния поиска, в рамках которой создан запрос.
  */
object MSearchRespInfo {

  @inline implicit def univEq[T: UnivEq]: UnivEq[MSearchRespInfo[T]] = UnivEq.derive

}


/** Контейнер данных ответа сервера.
  *
  * @param textQuery Состояние строки поиска.
  * @param resp Ответ сервера.
  * @tparam T Тип модели ответа сервера.
  */
case class MSearchRespInfo[T](
                               textQuery      : Option[String],
                               resp           : T
                             )
