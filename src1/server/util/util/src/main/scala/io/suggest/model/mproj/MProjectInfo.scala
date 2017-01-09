package io.suggest.model.mproj

import java.io.File
import java.net.JarURLConnection

import com.google.inject.Singleton
import io.suggest.util.MacroLogsDyn
import org.joda.time.DateTime

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.12.15 16:43
  * Description: Модель какой-то технической информации о текущем проекте.
  * Обычно инжектируется через DI.
  *
  * 2016.jun.14 Модель стала пока неиспользуемой в web21. И с тех пор она выкинута в util.
  */
@Singleton
class MProjectInfo extends MacroLogsDyn {

  /** Дата последней модификации кода проекта. Берется на основе текущего кода. */
  val PROJECT_CODE_LAST_MODIFIED: DateTime = {
    Option(getClass.getProtectionDomain)
      .flatMap { pd => Option(pd.getCodeSource) }
      .flatMap[Long] { cs =>
        val csUrl = cs.getLocation
        csUrl.getProtocol match {
          case "file" =>
            try {
              val f = new File(csUrl.getFile)
              val lm = f.lastModified()
              Some(lm)
            } catch {
              case ex: Exception =>
                LOGGER.error("Cannot infer last-modifed from file " + csUrl, ex)
                None
            }
          case "jar" =>
            try {
              val connOpt = Option(csUrl.openConnection)
              try {
                connOpt.map {
                  case jaUrlConn: JarURLConnection =>
                    jaUrlConn.getJarEntry.getTime
                }
              } finally {
                connOpt.foreach {
                  _.getInputStream.close()
                }
              }
            } catch {
              case ex: Exception =>
                LOGGER.warn("Cannot get jar entry time last-modified for " + csUrl, ex)
                None
            }
          case other =>
            LOGGER.error("Cannot detect last-modified for class source " + csUrl + " :: Unsupported protocol: " + other)
            None
        }
      }
      .fold(DateTime.now) { new DateTime(_) }
  }

}


/** Интерфейс для поля с DI-инстансом модели [[MProjectInfo]]. */
trait IMProjectInfo {
  def mProjectInfo: MProjectInfo
}
