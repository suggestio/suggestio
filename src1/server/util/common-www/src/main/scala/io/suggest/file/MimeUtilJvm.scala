package io.suggest.file

import java.nio.file.{Files, Path}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.02.18 15:58
  * Description: server-side утиль для mime-типов.
  *
  * Нужна как изолирующая прослойка между нижележащей системой определения mime-типов и sio2.
  * Раньше был JMagicMatch, который плохо отрабатывал svg, порождая костыли в коде.
  * На смену ему пришёл java-7 Files.probeContentType(), а как оно будет в будущем -- покажет время.
  */
object MimeUtilJvm {

  /** Проверка content-type для файла.
    *
    * @param path Путь к файлу.
    * @return Some(MIME type)
    *         None, если тип остался неизвестен по итогам определения.
    */
  def probeContentType(path: Path): Option[String] = {
    Option(
      Files.probeContentType(path)
    )
  }

}
