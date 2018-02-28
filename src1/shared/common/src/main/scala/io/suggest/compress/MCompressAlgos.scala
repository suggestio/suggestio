package io.suggest.compress

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq._
import play.api.libs.json.Format

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.02.18 22:08
  * Description: Модель алгоритмов сжатия, которыми оперирует s.io.
  */
object MCompressAlgos extends StringEnum[MCompressAlgo] {

  /** GZIP - дефакто, стандарт всего интернета.
    * Быстрый, но менее эффективный, по сравнению с brotli.
    */
  case object Gzip extends MCompressAlgo("g") {
    override def httpContentEncoding = "gzip"
  }

  /** Алгоритм сжатия brotli.
    * Поддерживается браузерами с 2016-2017 годов.
    * На стороне сервера реализован через sbt-web-brotli и brotli-util.
    */
  case object Brotli extends MCompressAlgo("b") {
    override def httpContentEncoding = "br"
  }

  override val values = findValues

  def withHttpContentEncoding(encoding: String): Option[MCompressAlgo] = {
    // Пока без map, т.к. это довольно маленькая модель.
    values
      .find(_.httpContentEncoding ==* encoding)
  }

}


/** Класс одного элемента модели алгоритмов сжатия. */
sealed abstract class MCompressAlgo(override val value: String) extends StringEnumEntry {

  /** Значение HTTP-заголовка кодировки контента. */
  def httpContentEncoding: String

  /** Суффикс имени файла (без точки). */
  def fileExtension: String = httpContentEncoding

  // Чтобы два раза не хранить почти одинаковые строки, переопределяем toString.
  override final def toString = httpContentEncoding

}


object MCompressAlgo {

  implicit def univEq: UnivEq[MCompressAlgo] = UnivEq.derive

  implicit def MCOMPRESS_ALGO_FORMAT: Format[MCompressAlgo] = {
    EnumeratumUtil.valueEnumEntryFormat( MCompressAlgos )
  }

}
