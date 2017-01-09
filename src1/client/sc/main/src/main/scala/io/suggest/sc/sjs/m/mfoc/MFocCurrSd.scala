package io.suggest.sc.sjs.m.mfoc

import io.suggest.common.m.mad.IMadId

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.06.16 16:44
  * Description: Модель данных по текущему состоянию карусели focused-выдачи.
  * Тут -- текущая карточки, её относительный внутренний индекс, флаги какие-то.
  * copy() НЕ используется при переключении на следующую карточку, инстанс модели должен собираться заново.
  *
  * @param madId id текущей карточки.
  * @param index Внутренний относительный номер карточки в focused-карусели. Не влияет на сервер.
  * @param forceFocus Флаг принудительной фокусировки запрещает серверу присылать index или что-то ещё.
  */
case class MFocCurrSd(
  madId       : String,
  index       : Int,
  forceFocus  : Boolean = false
)
  extends IMadId
