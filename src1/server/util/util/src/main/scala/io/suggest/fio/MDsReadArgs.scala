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
  * @param ptr Описание цели.
  * @param params Конкретные параметры чтения файла из хранилища.
  */
final case class MDsReadArgs(
                              ptr             : MStorageInfoData,
                              params          : MDsReadParams         = MDsReadParams.default,
                            )


/** Параметры передачи, отправляемые на самый нижний уровень.
  *
  * @param returnBody Возвращать данные ответа?
  *                   Для ответов на HEAD-запросы тут должно быть false.
  * @param acceptCompression Допускать возвращать ответ в сжатом формате.
  * @param range HTTP Range.
  */
case class MDsReadParams(
                          acceptCompression     : Iterable[MCompressAlgo]       = Nil,
                          returnBody            : Boolean                       = true,
                          range                 : Option[MDsRangeInfo]          = None,
                        )
object MDsReadParams {
  def default = apply()
  @inline implicit def univEq: UnivEq[MDsReadParams] = UnivEq.derive
}


object MDsRangeInfo {
  @inline implicit def univEq: UnivEq[MDsRangeInfo] = UnivEq.derive
}
final case class MDsRangeInfo(
                               range      : String,
                               rangeIf    : Option[String]  = None,
                             )
