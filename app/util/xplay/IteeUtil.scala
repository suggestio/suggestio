package util.xplay

import java.io.{FileOutputStream, File}

import play.api.libs.iteratee.{Enumerator, Iteratee}

import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.09.15 22:24
 * Description: Утиль для технологий play Enumerator/Iteratee/Enumeratee и смежных вещей.
 */
object IteeUtil {

  /**
   * Асинхронно записать все данные из Enumerator'а в указанный файл.
   * Файл будет перезаписан, либо создан, если не существует.
   * @param data Енумератор сырых данных.
   * @param f [[java.io.File]].
   * @return Фьючерс, обозначающий завершение записи.
   */
  def writeIntoFile(data: Enumerator[Array[Byte]], f: File)
                   (implicit ec: ExecutionContext): Future[_] = {
    val os = new FileOutputStream(f)
    val iteratee = Iteratee.foreach[Array[Byte]] { bytes =>
      os.write(bytes)
    }
    // отправлять байты enumerator'а в iteratee, который будет их записывать в файл.
    val resFut = (data |>>> iteratee)
      // Надо дождаться закрытия файла перед вызовом последующего map, который его откроет для чтения.
      .andThen { case result =>
        os.close()
      }
    // При ошибке нужно удалить файл, т.к. он всё равно уже теперь пустой.
    resFut onFailure {
      case ex => f.delete()
    }
    resFut
  }

}
