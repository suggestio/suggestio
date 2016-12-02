package io.suggest.async

import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import com.google.inject.Inject
import org.reactivestreams.Publisher

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
