package io.suggest.streams

import java.io.{File, FileOutputStream}
import javax.inject.Inject

import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import io.suggest.util.logs.IMacroLogs
import org.reactivestreams.Publisher
import play.api.libs.json.{JsNull, JsValue, Json}

import scala.concurrent.{ExecutionContext, Future}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.12.16 17:46
  * Description: Утиль для akka streams.
  */
class StreamsUtil @Inject() (
                              implicit private val ec   : ExecutionContext,
                              implicit private val mat  : Materializer
                            ) { outer =>

  /** Подсчёт кол-ва элементов в Source.
    *
    * @param src Источник элементов.
    * @return Фьючерс с кол-вом элементов.
    */
  def count(src: Source[_, _]): Future[Int] = {
    src.runFold(0) { (counter, _) => counter + 1 }
  }

  /** Бывает, что необходимо просто залоггировать длину сорса, когда включена трассировка в логгере.
    * По факту, это и есть основное предназначение count.
    *
    * @param src Источник.
    * @param clazz Класса, снабжённый логгером.
    * @param logMessageF Сборка сообщения логгера.
    */
  def maybeTraceCount(src: Source[_, _], clazz: IMacroLogs)(logMessageF: Int => String): Unit = {
    val logger = clazz.LOGGER
    if (logger.underlying.isTraceEnabled)
      for (totalCount <- count(src))
        logger.trace( logMessageF(totalCount) )
  }


  object Implicits {

    /** Утиль для Publisher'ов. */
    implicit class PublisherUtil[T](val pub: Publisher[T]) {
      def toSource: Source[T, NotUsed] = {
        Source.fromPublisher(pub)
      }
    }


    /** Утиль для Future[Publisher]. */
    implicit class PublisherFutUtil[T](val pubFut: Future[Publisher[T]]) {
      def toSource: Source[T, NotUsed] = {
        Source.fromFuture(pubFut)
          .flatMapConcat( _.toSource )
      }
    }


    /** Расширения для сорса. */
    implicit class SourceUtil[T](val src: Source[T, _]) {

      /**
        * Конвертация входного потока в множество.
        * @return Set[T].
        */
      def toSetFut: Future[Set[T]] = {
        src
          .runFold(Set.newBuilder[T])(_ += _)
          // Выстроить итоговое множество id'шников.
          .map { _.result() }
      }

    }

  }


  /**
    * Рендер потока данных в json-массив для chunked-ответа.
    * Т.к. есть проблемы с запятыми, в качестве последнего элемента добавляется null.
    *
    * @param src Источник объектов, уже сериализованных в JSON.
    * @return Поток строк, формирующих валидный JSON array с null в качестве последнего элемента.
    */
  def jsonSrcToJsonArrayNullEnded(src: Source[JsValue, _]): Source[String, _] = {
    // Сериализуем JSON в поток. Для валидности JSON надо добавить "[" в начале, "]" в конце, и разделители между элементами.
    val delim = ",\n"

    val jsons = src.mapConcat { m =>
      val jsonStr = Json.stringify(m)
      jsonStr :: delim :: Nil
    }

    // Собрать итоговый поток сознания.
    // TODO Тут рукописный генератор JSON. Следует задействовать тот, что *вроде бы* есть в akka-http или где-то ещё.
    Source.single( "[" )
      .concat(jsons)
      .concat {
        Source(
          // TODO Чтобы последняя запятая не вызывала ошибки парсинга, добавляем JsNull в конец потока объектов.
          Json.stringify(JsNull) :: "]" :: Nil
        )
      }
  }

  /**
   * Асинхронно записать все данные из Enumerator'а в указанный файл.
   * Файл будет перезаписан, либо создан, если не существует.
   * @param data Енумератор сырых данных.
   * @param f java.io.File
   * @param deleteOnError Удалять пустой/неполный файл при ошибке? [true]
   * @return Фьючерс, обозначающий завершение записи.
   */
  def sourceIntoFile(data: Source[ByteString, _], f: File, deleteOnError: Boolean = true): Future[_] = {
    val os = new FileOutputStream(f)

    val doneFut = data
      .runForeach { byteStr =>
        os.write( byteStr.toArray )
      }
      .andThen { case _ =>
        os.close()
      }

    if (deleteOnError) {
      // При ошибке нужно удалить файл, т.к. он всё равно уже теперь пустой.
      for (_ <- doneFut.failed) {
        f.delete()
      }
    }

    doneFut
  }



  def mergeByteStrings(src: Source[ByteString, _]): Future[ByteString] = {
    src.runReduce { (a,b) => a ++ b }
  }


}