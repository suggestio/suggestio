package io.suggest.file

import io.suggest.util.logs.MacroLogsImpl
import org.apache.tika.Tika

import java.nio.file.Path
import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.02.18 15:58
  * Description: server-side утиль для mime-типов.
  *
  * Нужна как изолирующая прослойка между нижележащей системой определения mime-типов и sio2.
  * Раньше был JMagicMatch, который плохо отрабатывал svg, порождая костыли в коде.
  *
  * На смену ему пришёл java-7 Files.probeContentType(), но плоховато он работает после обновления до java 11.
  * https://stackoverflow.com/q/65966437
  *
  * Далее, испытывается apache tika.
  */
final class MimeUtilJvm extends MacroLogsImpl {

  /** Проверка content-type для файла.
    *
    * @param path Путь к файлу.
    * @return Some(MIME type)
    *         None, если тип остался неизвестен по итогам определения.
    */
  def probeContentType(path: Path): Option[String] = {
    lazy val logPrefix = s"probeContentType($path):"

    val tryRes = Try {
      val tika = new Tika()
      tika.detect(path)
    }

    // Логгируем результат работы:
    tryRes.fold[Unit](
      {ex =>
        LOGGER.warn(s"$logPrefix Failed to probe content type", ex)
      },
      {ct =>
        LOGGER.trace(s"$logPrefix => $ct")
      }
    )

    tryRes.toOption
  }

}
