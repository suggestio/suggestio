package io.suggest.common.tags.edit

import io.suggest.common.html.HtmlConstants

import scalaz.{Validation, ValidationNel}
import scalaz.syntax.apply._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.09.15 15:29
 * Description: Контейнер констант для редактора тегов.
 */
object TagsEditConstants {

  /** Константы поиска тегов в редакторе. */
  object Search {

    /** На сколько миллисекунд откладывать запуск поискового запроса тегов. */
    def START_SEARCH_TIMER_MS       = 400

    /** Макс. кол-во тегов в ответе. */
    def LIVE_SEARCH_RESULTS_LIMIT   = 5

    /** Константы выпадающих подсказок поиска тегов в редакторе. */
    object Hints {

      /** Контейнер одного ряда-подсказки в списке рядов  */
      def HINT_ROW_CLASS = "js-shrow"

      def ATTR_TAG_FACE  = HtmlConstants.ATTR_PREFIX + HINT_ROW_CLASS

    }

  }

  /** Ограничения формы. */
  object Constraints {

    /** Максимальная символьная длина одного тега. */
    def TAG_LEN_MAX = 40

    /** Минимальная символьная длина одного тега. */
    def TAG_LEN_MIN = 1

    /** Сколько тегов юзер может добавить за один запрос максимум. */
    def TAGS_PER_ADD_MAX = 20


    /** Валидация названия тега. */
    def tagFaceV(tagFace: String): ValidationNel[String, String] = {
      val l = tagFace.length
      val C = TagsEditConstants.Constraints
      val ePrefix = "e.tag.face.too"
      val maxLenV = Validation.liftNel(l)(_ > C.TAG_LEN_MAX, s"$ePrefix.big")
      val minLenV = Validation.liftNel(l)(_ < C.TAG_LEN_MIN, s"$ePrefix.small")
      (maxLenV |@| minLenV)( (_,_) => tagFace )
    }

  }

}
