package io.suggest.async

import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import com.google.inject.Inject
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
) {


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

}
