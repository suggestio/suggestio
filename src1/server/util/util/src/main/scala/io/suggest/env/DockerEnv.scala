package io.suggest.env

import io.suggest.util.logs.MacroLogsDyn
import scala.collection.JavaConverters._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.04.19 11:03
  * Description: Вспомогательная утиль для запуска в docker-окружении.
  */
object DockerEnv extends MacroLogsDyn {

  LOGGER.info(s"Current env:\n ${System.getenv().asScala.mkString("\n ")}")

  /** Docker-линки пробрасывают информацию о себе через переменные окружения, имена которых генерятся автоматом.
    * Для извлечения хостнейма и реального remote-порта по линку, можно использовать этот метод.
    *
    * @param service Название сервиса.
    * @param port Ожидаемые expose-порт.
    * @param proto Протокол (tcp/udp/etc). По умолчанию - tcp.
    * @return Хост-порт, есть есть.
    */
  def getLinkHostPort(service: String, port: Int, proto: String = "TCP"): Option[(String, Int)] = {
    val prefix = s"${service.toUpperCase}_PORT_${port}_${proto.toUpperCase}_"
    for {
      linkAddr <- Option( System.getenv( s"${prefix}_ADDR" ) )
      linkPort <- Option( System.getenv( s"${prefix}_PORT" ) )
    } yield {
      (linkAddr, linkPort.toInt)
    }
  }

}
