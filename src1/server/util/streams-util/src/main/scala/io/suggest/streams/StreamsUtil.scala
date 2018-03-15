package io.suggest.streams

import java.io.{File, FileOutputStream}
import java.nio.charset.StandardCharsets
import javax.inject.Inject

import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl.{Compression, Flow, Keep, Sink, Source}
import akka.util.ByteString
import io.suggest.primo.Var
import io.suggest.util.logs.IMacroLogs
import org.reactivestreams.Publisher
import play.api.libs.json.{JsValue, Json}

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
  def runCount(src: Source[_, _]): Future[Int] = {
    src
      .toMat( Sinks.count )(Keep.right)
      .run()
  }

  object Sinks {

    def count[T]: Sink[T, Future[Int]] = {
      Sink.fold[Int, T](0) { (acc0, _) =>
        acc0 + 1
      }
    }

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
        Source
          .fromFuture(pubFut)
          .flatMapConcat( _.toSource )
      }
    }


    /** Расширения для сорса. */
    implicit class SourceUtil[T, M](val src: Source[T, M]) {

      /** Бывает, что необходимо просто залоггировать длину сорса, когда включена трассировка в логгере.
        * По факту, это и есть основное предназначение count.
        *
        * @param clazz Класса, снабжённый логгером.
        * @param logMessageF Сборка сообщения логгера.
        */
      // TODO Реализовать на уровне FlowOps, т.к. здесь возня уровня Flow, а не Source.
      def maybeTraceCount(clazz: IMacroLogs)(logMessageF: Int => String): Source[T, M] = {
        val logger = clazz.LOGGER

        if (logger.underlying.isTraceEnabled) {
          // Собрать новый синк, который посчитает значения, затем залоггировать итог.
          val sink = Sinks.count[T]
            .mapMaterializedValue { totalFut =>
              for (total <- totalFut)
                logger.trace( logMessageF(total) )
            }
          src.alsoTo( sink )

        } else {
          // Не требуется логгирования.
          src
        }
      }

      /**
        * Конвертация входного потока в множество.
        * @return Set[T].
        */
      def toSetFut: Future[Set[T]] = {
        src
          .toMat( Sink.collection[T, Set[T]] )( Keep.right )
          .run()
      }

    }


    /** Допы для потока JsValue (JSON-сериализованные элементы).
      *
      * @param src Поток JSON-сериазованных данных.
      * @tparam T Тип JsValue.
      * @tparam M Mat-value.
      */
    implicit class JsValuesSourceExt[T <: JsValue, M](val src: Source[T, M]) {

      /**
        * Рендер потока данных в json-массив для chunked-ответа.
        *
        * @return Поток строк, формирующих валидный JSON array.
        */
      def jsValuesToJsonArrayStrings: Source[String, M] = {
        src
          .map { m =>
            Json.stringify(m)
          }
          .intersperse("[", ",", "]")
      }

      /** Рендер потока JsValue в ByteString.
        * Основное отличие в том, что для всех запятых используются расшаренный инстанс ByteString,
        * а не постоянно собирается новый. Это снижает объем производимого мусора на 2+ байта с одного item'а.
        * Профит небольшой, но он есть.
        *
        * @return
        */
      def jsValuesToJsonArrayByteStrings: Source[ByteString, M] = {
        val _s2bs = ByteString.fromString( _: String, StandardCharsets.UTF_8 )
        src
          .map { m =>
            val jsonStr = Json.stringify(m)
            _s2bs(jsonStr)
          }
          .intersperse(
            start  = _s2bs("["),
            inject = _s2bs(","),
            end    = _s2bs("]")
          )
      }

    }


    implicit class ByteStringSinkExtOps[T](sink: Sink[T, Future[ByteString]]) {

      /** Выполнить фоновую подмену результирующей ByteString.
        * Результа
        */
      def asyncCompactedByteString: Sink[T, Future[Var[ByteString]]] = {
        sink.mapMaterializedValue { byteStringFut0 =>
          for (byteString <- byteStringFut0) yield {
            val v = Var(byteString)
            // В фоне органзиовать компакцию ByteString и подмену значения Var.value:
            v.asyncUpgradeUsing { () =>
              byteString.compact
            }
            v
          }
        }
      }

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


  /** Сборка Sink'а, который будет конкатенировать ByteString'и.
    *
    * @return reusable Sink.
    */
  def byteStringAccSink: Sink[ByteString, Future[ByteString]] = {
    // Собираем многоразовый синк, который будет наиболее эффективным путём собирать финальную ByteString.
    // Используется mutable ByteString builder, который инициализируется с фактическим началом потока, поэтому тут lazyInit().
    Sink
      .lazyInit[ByteString, Future[ByteString]](
        // При поступлении первых данных, инициализировать builder и собрать фактический sink:
        sinkFactory = { _ =>
          val b = ByteString.newBuilder
          // Закидываем все данные в общий builder:
          val realSink = Sink.foreach { b.append }
            .mapMaterializedValue { doneFut =>
              for (_ <- doneFut) yield {
                b.result()
              }
            }
          Future.successful( realSink )
        },
        // Нет данных? Не проблема, вернуть пустой ByteString:
        fallback = { () =>
          Future.successful( ByteString.empty )
        }
      )
      .mapMaterializedValue(_.flatten)
  }


  /** Почему-то голый .via(Compression.gzip) выдаёт мусор вместо результата.
    * Поэтому тут костыли и подпорки для сжатия, выковырянные из play.api.libs.streams.GzipFlow
    */
  def gzipFlow(bufferSize: Int = 8192): Flow[ByteString, ByteString, _] = {
    Flow[ByteString]
      .via(new ByteStringsChunker(bufferSize))
      .via(Compression.gzip)
  }

}
