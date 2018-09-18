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

  /** Выдача на React.sjs (sc3). */
  case object ReactSjs3 extends MScApiVsn( 4 )

  /** react-выдача sc3 для cordova-приложений. */
  case object ReactCordova extends MScApiVsn( 5 ) {
    override def majorVsn = ReactSjs3.majorVsn
    override def forceAbsUrls = true
  }


  override val values = findValues

  /** Какую версию использовать, если версия API не указана? */
  def unknownVsn: MScApiVsn = {
    ReactSjs3
  }

}


/** Класс одного элемента модели [[MScApiVsns]]. */
sealed abstract class MScApiVsn(override val value: Int) extends IntEnumEntry {

  /** Мажорная версия API. Выводится из versonNumber, может переопредяться на минорных реализациях. */
  def majorVsn: Int = value

  /**
    * Генерить абсолютные внутренние ссылки в выдаче, где возможно.
    * На dyn-картинки, например.
    */
  def forceAbsUrls: Boolean = false

  override def toString: String = s"v$majorVsn($value)"

}

/** Статическая утиль для классов [[MScApiVsn]]. */
object MScApiVsn {

  /** Поддержка play-json. */
  implicit def MSC_API_VSN_FORMAT: Format[MScApiVsn] = {
    EnumeratumUtil.valueEnumEntryFormat( MScApiVsns )
  }

  @inline implicit def univEq: UnivEq[MScApiVsn] = UnivEq.derive

}

