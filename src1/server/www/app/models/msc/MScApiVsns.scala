package models.msc

import io.suggest.common.menum.EnumMaybeWithId
import io.suggest.model.play.qsb.QueryStringBindableImpl
import play.api.mvc.QueryStringBindable
import io.suggest.sc.ScConstants.Vsns
import io.suggest.util.logs.MacroLogsImpl

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
object MScApiVsns extends Enumeration with EnumMaybeWithId with MacroLogsImpl {

  /** Экземпляр модели версий. */
  protected[this] class Val(val versionNumber: Int) extends super.Val(versionNumber) {

    override def toString(): String = id.toString

    /** Мажорная версия API. Выводится из versonNumber, может переопредяться на минорных реализациях. */
    def majorVsn: Int = versionNumber

    /**
      * Генерить абсолютные внутренние ссылки в выдаче, где возможно.
      * На dyn-картинки, например.
      */
    def forceAbsUrls: Boolean = false

  }

  override type T = Val


  // 2016.jul.14 Наконец удаление выдачи v1 Coffee. Удаление полей Val. Осталась только Sjs1 выдача.

  /** Выдача, переписанная на scala.js. Выдача второго поколения или просто "sc v2". */
  val Sjs1: T = new Val( Vsns.SITE_JSONHTML )

  /** Cordova-выдача требует некоторого доп.шаманства в коде относительно API обычного v2. */
  val Cordova: T = new Val( Vsns.CORDOVA_JSONHTML ) {

    /** Различия API с обычной версией только в мелочах, поэтому контроллеры должны реагировать как обычно на v2. */
    override def majorVsn = Sjs1.majorVsn

    /**
      * В cordova есть проблема с относительными ссылками: WebView работает в контексте file:///,
      * и нужно, чтобы картинки и прочая медия была явно определена с полными ссылками.
      */
    override def forceAbsUrls = true

  }


  /** Какую версию использовать, если версия API не указана? */
  def unknownVsn: T = {
    Sjs1
  }

  /** Биндинги для url query string. */
  implicit def qsb(implicit intB: QueryStringBindable[Int]): QueryStringBindable[MScApiVsn] = {
    new QueryStringBindableImpl[MScApiVsn] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MScApiVsn]] = {
        val optRes = for {
          maybeVsn <- intB.bind(key, params)
        } yield {
          maybeVsn.right.flatMap { vsnNum =>
            maybeWithId(vsnNum).toRight {
              // Довольно неожиданная ситуация, что выкинута версия, используемая на клиентах. Или ксакеп какой-то ковыряется.
              val msg = "Unknown API version: " + vsnNum
              LOGGER.warn(msg)
              msg
            }
          }
        }
        // Если версия не задана вообще, то выставить её в дефолтовую. Первая выдача не возвращала никаких версий.
        optRes.orElse {
          Some( Right(unknownVsn) )
        }
      }

      override def unbind(key: String, value: MScApiVsn): String = {
        intB.unbind(key, value.versionNumber)
      }
    }
  }

}

/** Интерфейс для моделей, предоставляющим поле apiVsn с версией API выдачи. */
trait IScApiVsn {
  def apiVsn: MScApiVsn
}
