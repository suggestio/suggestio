package io.suggest.es.model


import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.09.16 12:42
  * Description: Совсем статическая утиль для динамических индексов.
  */
object EsIndexUtil {

  def DELIM = "-"

  /** java8 dt formatter. */
  def dtSuffixFmt = DateTimeFormatter.ofPattern("yyMMdd-HHmmss")

  /** Генерация нового имени скользящего во времени индекса (с точностью до секунды). */
  def newIndexName(prefix: String): String = {
    prefix + DELIM + dtSuffixFmt.format( ZonedDateTime.now() )
  }

}
