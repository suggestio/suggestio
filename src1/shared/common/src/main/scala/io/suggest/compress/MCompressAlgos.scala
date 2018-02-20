package io.suggest.compress

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import play.api.libs.json.Format

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.02.18 22:08
  * Description: Модель алгоритмов сжатия, которыми оперирует s.io.
  */
object MCompressAlgos extends StringEnum[MCompressAlgo] {

  /** Алгоритм сжатия brotli.
    * Поддерживается браузерами с 2016-2017 годов.
    * На стороне сервера реализован через sbt-web-brotli и brotli-util.
    */
  case object Brotli extends MCompressAlgo("b")


  override val values = findValues

}


/** Класс одного элемента модели алгоритмов сжатия. */
sealed abstract class MCompressAlgo(override val value: String) extends StringEnumEntry


object MCompressAlgo {

  implicit def univEq: UnivEq[MCompressAlgo] = UnivEq.derive

  implicit def MCOMPRESS_ALGO_FORMAT: Format[MCompressAlgo] = {
    EnumeratumUtil.valueEnumEntryFormat( MCompressAlgos )
  }

}
