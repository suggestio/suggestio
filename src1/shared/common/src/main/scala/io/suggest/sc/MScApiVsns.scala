package io.suggest.sc

import enumeratum.values.{IntEnum, IntEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import play.api.libs.json.Format

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.05.15 13:35
  * Description: Версии API системы выдачи, чтобы сервер мог подстраиватсья под разношерстных клиентов.
  *
  * Изначально была версия 1 -- это coffeescript-выдача и совсем голый js-api.
  *
  * Потом возникла вторая версия: json с html'ками отрендеренными внутри.
  * Первая версия постепенно ушла в небытие.
  *
  * 2016.oct.25:
  * При запиливании cordova-приложения возникла необходимость внесения дополнений и корректив в API.
  * Так же возникло понятие "мажорной" версии API. Т.е. контроллер использует для API тот же код,
  * что и обычно, но различия могут быть зарыты в деталях этого кода или шаблонах.
  * По дефолту, мажорщина равна версии API.
  *
  * В будущем наверняка переедем на react.js и client-side render, который потребует нового json API без HTML внутри.
  */

object MScApiVsns extends IntEnum[MScApiVsn] {

  /** Выдача, переписанная на scala.js.
    * Выдача второго поколения или просто "sc v2".
    * HTTP-ответы представляют из себя JSON-объекты (application/json) с отрендеренным html внутри полей.
    */
  case object Sjs1 extends MScApiVsn( 2 ) {

    override def useJdAds = false

  }


  /** Cordova-выдача требует некоторого доп.шаманства в коде относительно API обычного v2. */
  case object Cordova extends MScApiVsn( 3 ) {

    override def useJdAds = false

    /** Различия API с обычной версией только в мелочах, поэтому контроллеры должны реагировать как обычно на v2. */
    override def majorVsn = Sjs1.majorVsn

    /**
      * В cordova есть проблема с относительными ссылками: WebView работает в контексте file:///,
      * и нужно, чтобы картинки и прочая медия была явно определена с полными ссылками.
      */
    override def forceAbsUrls = true

  }


  /** Выдача на React.sjs (sc3). */
  case object ReactSjs3 extends MScApiVsn( 4 )


  override val values = findValues

  /** Какую версию использовать, если версия API не указана? */
  def unknownVsn: MScApiVsn = {
    Sjs1
  }

}


/** Класс одного элемента модели [[MScApiVsns]]. */
sealed abstract class MScApiVsn(override val value: Int) extends IntEnumEntry {

  // TODO Удалить это?
  final def versionNumber = value

  /** Мажорная версия API. Выводится из versonNumber, может переопредяться на минорных реализациях. */
  def majorVsn: Int = versionNumber

  /**
    * Генерить абсолютные внутренние ссылки в выдаче, где возможно.
    * На dyn-картинки, например.
    */
  def forceAbsUrls: Boolean = false

  /** Разрешены ли jd-карточки в данном API?
    * @return true -- разрешены только jd-карточки.
    *         false -- запрещены все jd-карточки.
    */
  def useJdAds: Boolean = true

  override def toString: String = s"v$majorVsn($versionNumber)"

}

/** Статическая утиль для классов [[MScApiVsn]]. */
object MScApiVsn {

  /** Поддержка play-json. */
  implicit def MSC_API_VSN_FORMAT: Format[MScApiVsn] = {
    EnumeratumUtil.valueEnumEntryFormat( MScApiVsns )
  }

  implicit def univEq: UnivEq[MScApiVsn] = UnivEq.derive

}


/** Интерфейс для моделей, предоставляющим поле apiVsn с версией API выдачи. */
trait IScApiVsn {
  def apiVsn: MScApiVsn
}
