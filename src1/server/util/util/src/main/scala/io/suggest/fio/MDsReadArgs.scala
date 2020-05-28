package io.suggest.fio

import io.suggest.compress.MCompressAlgo
import io.suggest.n2.media.storage.MStorageInfoData
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 27.05.2020 15:05
  * Description: Контейнер метаданных для чтения data-source.
  * Передаётся в IMediaStorage().read().
  */
object MDsReadArgs {
  @inline implicit def univEq: UnivEq[MDsReadArgs] = UnivEq.derive
}

/** Контейнер данных запроса чтения абстрактного data-source.
  *
  * @param returnBody Возвращать данные ответа?
  *                   Для ответов на HEAD-запросы тут должно быть false.
  * @param ptr Описание цели.
  * @param acceptCompression Допускать возвращать ответ в сжатом формате.
  * @param range HTTP Range
  */
final case class MDsReadArgs(
                              ptr                   : MStorageInfoData,
                              acceptCompression     : Iterable[MCompressAlgo]       = Nil,
                              returnBody            : Boolean                       = true,
                              range                 : Option[MDsRangeInfo]          = None,
                            )


object MDsRangeInfo {
  @inline implicit def univEq: UnivEq[MDsRangeInfo] = UnivEq.derive
}
final case class MDsRangeInfo(
                               range      : String,
                               rangeIf    : Option[String]  = None,
                             )
