package io.suggest.itee

import java.io.{File, FileOutputStream}

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
   * @param f java.io.File
   * @param deleteOnError Удалять пустой/неполный файл при ошибке? [true]
   * @return Фьючерс, обозначающий завершение записи.
   */
  def writeIntoFile(data: Enumerator[Array[Byte]], f: File, deleteOnError: Boolean = true)
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
    if (deleteOnError) {
      resFut onFailure {
        case ex => f.delete()
      }
    }
    resFut
  }


  /**
   * Собрать поток кусков Array[Byte] в единый массив.
   * @param data Входной "поток" данных.
   * @return Фьюсерс с результирующим блобом.
   */
  def dumpBlobs(data: Enumerator[Array[Byte]])(implicit ec: ExecutionContext): Future[Array[Byte]] = {
    // Сдампить блобики в один единый блоб.
    val itee = Iteratee.fold [Array[Byte], List[Array[Byte]]] (Nil) {
      (acc0, e) =>
        e :: acc0
    }
    (data |>>> itee)
      .map { arraysRev =>
        // Оптимизация для результатов из ровно одного куска.
        if (arraysRev.tail.isEmpty) {
          arraysRev.head
        } else {
          // Если кусков несколько, от восстановить исходных порядок и склеить в один массив.
          // TODO Opt тут используются промежуточные итераторы и Buffer. Есть более оптимальное решение:
          //      подсчитать финальную длину массива, выделить память под финальный массив и скопипастить туда все массивы.
          arraysRev
            .reverseIterator
            .flatten
            .toArray
        }
      }
  }

}
